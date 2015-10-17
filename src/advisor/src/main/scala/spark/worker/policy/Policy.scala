package spark.worker.policy

import java.io.File

import scala.util.{Try, Success, Failure}
import scala.util.Random.shuffle

import breeze.linalg.{DenseMatrix, SparseVector, VectorBuilder, csvread, min}

import spark.worker.dronestate._
import spark.worker.grid._

/** Policy object that implements the conflict resolution algorithm. */
class Policy(
    private val utility: DenseMatrix[Double],
    private val actionSet: Array[Double],
    private val grid: Grid)
  extends Serializable {

  final val DimensionStateSpace = utility.rows
  final val DimensionActionSpace = actionSet.length
  final val DimensionJointActionSpace = utility.cols
  final val IndexTerminalState = DimensionStateSpace - 1
  final val IndexCOC = DimensionActionSpace - 1

  /** Alternating maximization iterative search for pairwise encounters. */
  def searchPolicy(drones: Array[DroneGlobalState]): (Array[Double], Double, Int) = {
    val currActionIndices = Array.fill[Int](drones.length)(IndexCOC)
    val bestActionIndices = Array.fill[Int](drones.length)(IndexCOC)

    val utils = Array.fill[Double](drones.length)(Double.NegativeInfinity)
    var bestUtil = Double.NegativeInfinity

    var niter = 0
    var solutionImproving = true

    while (niter < Policy.MaxIterSearch && solutionImproving) {
      solutionImproving = false

      val droneOrder = shuffle(drones.indices.toList)  // for fairness
      for (idrone <- droneOrder) {
        val (actionIndex, util) = maxmin(idrone, drones, currActionIndices)
        utils(idrone) = util
        currActionIndices(idrone) = actionIndex

        if (bestUtil < utils.sum) {
          bestUtil = utils.sum
          Array.copy(currActionIndices, 0, bestActionIndices, 0, currActionIndices.length)
          solutionImproving = true
        }
      }
      niter += 1
    }

    if (niter == Policy.MaxIterSearch) {
      println("WARN MaxCountReached: Maximum number of policy search iterations reached")
    }
    (idx2val(bestActionIndices, actionSet), bestUtil, niter)
  }

  /** Compute best of worst-case (max-min) utility for all pairwise encounters with ownship. */
  def maxmin(
      iownship: Int,
      drones: Array[DroneGlobalState],
      droneActionIndices: Array[Int]): (Int, Double) = {

    val utils = Array.fill[Double](actionSet.length)(Double.PositiveInfinity)
    val beliefs = getBeliefs(iownship, drones)

    for (iaction <- actionSet.indices) {
      // compute minimum utility corresponding to ownship selecting iaction
      for (idrone <- drones.indices if idrone != iownship) {
        var util = Double.PositiveInfinity
        val jointActionIndex = getJointActionIndex(iaction, droneActionIndices(idrone))

        if (beliefs(idrone)(IndexTerminalState) == 1.0) {
          if (iaction == IndexCOC)
            util = Policy.UtilityCOC
          else
            util = Policy.UtilityAdvisory
        } else {
          util = utility(::, jointActionIndex) dot beliefs(idrone)
        }

        utils(iaction) = min(utils(iaction), util)
      }
    }

    (indmax(utils), utils.max)
  }

  /** Map continuous drone flight state to multilinear interpolated internal representation. */
  def getBeliefs(iownship: Int, drones: Array[DroneGlobalState]): Array[SparseVector[Double]] = {
    val beliefs = new Array[SparseVector[Double]](drones.length)
    for (intruder <- drones.indices) {
      if (iownship != intruder) {
        val localState = DroneLocalState(iownship, intruder, drones)
        computeBelief(beliefs, localState, intruder)
      }
    }
    beliefs
  }

  /** Helper function for getBeliefs. */
  def computeBelief(
      beliefs: Array[SparseVector[Double]],
      localState: DroneLocalState,
      intruder: Int): Unit = {

    val beliefBuilder = new VectorBuilder[Double](DimensionStateSpace)
    if (localState.isTerminal) {
      // last element corresponds to terminal state
      beliefBuilder.add(beliefBuilder.size - 1, 1.0)
    } else {
      val (indices, weights) = grid.interpolants(localState.toArray)
      for (iweight <- weights.indices) {
        beliefBuilder.add(indices(iweight), weights(iweight))
      }
    }
    beliefs(intruder) = beliefBuilder.toSparseVector
  }

  /** Converts individual action indices into pairwise action index. */
  def getJointActionIndex(ownshipAction: Int, intruderAction: Int): Int = {
    // column-major order with ownshipAction = row, intruderAction = col
    ownshipAction * DimensionActionSpace + intruderAction
  }

  def idx2val(indices: Array[Int], refValues: Array[Double]) =
    for (index <- indices.toArray) yield refValues(index)

  def indmax(a: Array[Double]): Int = a.zipWithIndex.maxBy(_._1)._2
}

object Policy {
  final val ClearOfConflict = -1.0
  final val MaxIterSearch = 100
  final val UtilityCOC = 0.0
  final val UtilityAdvisory = -0.5
  final val RewardConsensus = 0.5
  final val RewardNoConsensus = 0.0

  def apply(
      utility: DenseMatrix[Double],
      actionSet: Array[Double],
      grid: Grid): Policy = {

    new Policy(utility, actionSet, grid)
  }

  /** Loads the utility lookup table into memory. */
  def readUtility(filename: String): Option[DenseMatrix[Double]] = {
    def trycsvread (csvfilename: String): Try[DenseMatrix[Double]] =
      Try(csvread(new File(filename)))

    trycsvread(filename) match {
      case Failure(f) =>
        System.err.println(f)
        None
      case Success(utility) => Some(utility)
    }
  }

  /** Returns the default policy object that resovles conflict for each executor node. */
  def defaultPolicy(): Policy = {
    val utility = Policy.readUtility(Const.UtilityFile) match {
      case Some(rawUtility) =>
        if (rawUtility.cols == Const.UtilityCols && rawUtility.rows == Const.UtilityRows) {
          println("INFO utility read successfully")
        } else {
          println("WARN utility file might have been updated or corrupted")
        }
        rawUtility

      case None =>
        println("WARN utility read unsuccessful, returning empty DenseMatrix")
        new DenseMatrix[Double](0, 0)
    }
    Policy(utility, Const.ActionSet, Grid(Const.S1, Const.S2, Const.S3, Const.S4, Const.S5))
  }

  /** These case classes define the JSON format for each advisory. */
  case class Advisory(gufi: String, clearOfConflict: String, waypoints: List[Waypoint])
  case class Waypoint(
      lat: String,      // m
      lon: String,      // m
      speed: String,    // m/s
      heading: String,  // rad
      period: String,   // s
      turn: String)     // rad/s

  /**
   * Returns a list of JSON-formatted advisories. <conflict> elements are tuples:
   * (gufi, drone global state, advisory = bank angle). Note that each advisory is a
   * bank angle as defined above for the elements of the first return array object
   * in the <searchPolicy> function.
   */
  def advisories(conflict: Array[(String, DroneGlobalState, Double)]): List[Advisory] =
    for (idrone <- conflict.indices.toList) yield {
      val state = conflict(idrone)._2
      val bankAngle = conflict(idrone)._3

      val clearOfConflict = bankAngle match {
        case Const.ClearOfConflict => "true"
        case _ => "false"
      }

      val turnRate = bankAngle2turnRate(bankAngle, state.speed)

      Advisory(
        gufi = conflict(idrone)._1,
        clearOfConflict = clearOfConflict,
        waypoints = List(
          Waypoint(
            state.latitude.toString,
            state.longitude.toString,
            state.speed.toString,
            state.heading.toString,
            Const.DecisionPeriod.toString,
            turnRate.toString)))
  }

  /** Returns turn rate from bank angle in rad/s. */
  private def bankAngle2turnRate(bankAngle: Double, speed: Double): Double = {
    bankAngle match {
      case Const.ClearOfConflict => 0.0
      case _ => Const.G * math.tan(bankAngle) / speed
    }
  }
}
