package spark.worker.policy

import java.io.File

import scala.util.{Try, Success, Failure}
import scala.util.Random.shuffle

import breeze.linalg.{DenseMatrix, SparseVector, VectorBuilder, csvread, min}

import spark.worker.dronestate._
import spark.worker.grid._

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

  def searchPolicy(drones: Array[DroneGlobalState]): (Array[Double], Double, Int) = {
    val currActionIndices = Array.fill[Int](drones.length)(IndexCOC)
    val bestActionIndices = Array.fill[Int](drones.length)(IndexCOC)

    val utils = Array.fill[Double](drones.length)(Double.NegativeInfinity)
    var bestUtil = Double.NegativeInfinity

    var niter = 0
    var solutionImproving = true

    while (niter < Policy.MaxIterSearch && solutionImproving) {
      solutionImproving = false

      val droneOrder = shuffle(drones.indices.toList) // for fairness
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

  def getJointActionIndex(ownshipAction: Int, intruderAction: Int): Int = {
    // column-major order with ownshipAction = row, intruderAction = col
    ownshipAction * DimensionActionSpace + intruderAction
  }

  def consensus(iaction: Int, suggestedActionIndex: Int): Boolean = {
    false
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
}
