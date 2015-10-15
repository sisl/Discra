package spark.worker

import breeze.linalg._
import breeze.numerics._
import breeze.plot._

import spark.worker.dronestate._
import spark.worker.grid._
import spark.worker.policy._

import scala.math.Pi

object Test {
  def main(args: Array[String]): Unit = {
    println("INFO beginning unit tests")

    testGridInd2X()
    testGridInterpolate()
    val utility = testUtilityRead("/Users/Icarus/Documents/projects/utm-alpha/data/utility.csv")
    val policy: Policy = testPolicyConstructor(utility)
    testLocalState(policy)
    testPolicy(policy)
    println("INFO unit tests complete")
  }

  def time[R](iterations:Int, block: => R): Unit = {
    val t0 = System.nanoTime()
    for (i <- 0 until iterations) {
      block  // call-by-name
    }
    val t1 = System.nanoTime()
    println("elapsed time: " + (t1 - t0) / 1e6 + "ms")
  }

  def deg2rad(deg: Double) = deg * Pi / 180.0

  def rad2deg(rad: Double) = rad * 180.0 / Pi

  def passTest(testname: String): Unit = {
    println("PASS " + testname)
  }

  def testPolicyConstructor(utility: DenseMatrix[Double]): Policy = {
    val grid = Grid(Const.S1, Const.S2, Const.S3, Const.S4, Const.S5)
    val policy = Policy(utility, Const.ActionSet, grid)
    passTest("policy constructor")
    policy
  }

  def testLocalState(policy: Policy, print: Boolean = false): Unit = {
    val localState = DroneLocalState(0, 1, Array(Const.state1, Const.state2))

    def equals(s1: DroneLocalState, s2: DroneLocalState): Boolean = {
      val eq =
        abs(s1.x - s2.x) < Const.Tolerance &&
        abs(s1.y - s2.y) < Const.Tolerance &&
        abs(s1.bearing - s2.bearing) < Const.Tolerance &&
        abs(s1.speedOwnship - s2.speedOwnship) < Const.Tolerance &&
        abs(s1.speedIntruder - s2.speedIntruder) < Const.Tolerance
      eq
    }

    if (print) {
      println(
        "computed local state =\n" +
        localState.x,
        localState.y,
        localState.bearing,
        localState.speedOwnship,
        localState.speedIntruder)
      println(
        "actual local state =\n" +
        Const.actualLocalState.x,
        Const.actualLocalState.y,
        Const.actualLocalState.speedOwnship,
        Const.actualLocalState.bearing,
        Const.actualLocalState.speedIntruder)
    }

    assert(equals(localState, Const.actualLocalState))
    passTest("global to local state constructor")
  }

  def testGridInd2X(): Unit = {
    def samePoint(point1: Array[Double], point2: Array[Double]): Boolean = {
      var same = true
      val pointPair = point1 zip point2
      var ipair = 0
      while (same && ipair < pointPair.length) {
        val (val1, val2) = pointPair(ipair)
        if (abs(val1 - val2) > Const.Tolerance) {
          same = false
          println("point1 = " + point1.mkString(" "))
          println("point2 = " + point2.mkString(" "))
        }
        ipair += 1
      }
      same
    }

    val grid = Grid(Const.Xs, Const.Ys, Const.Zs)
    for (ipoint <- Const.GridPoints.indices) {
      assert(samePoint(grid.ind2x(Const.GridPoints(ipoint)), Const.GridXs(ipoint)))
    }
    passTest("grid ind2x")
  }

  def testGridInterpolate(): Unit = {
    def equals(val1: Double, val2: Double): Boolean = {
      val same: Boolean = abs(val1 - val2) < Const.Tolerance
      if (!same) {
        println("val1 = " + val1)
        println("val2 = " + val2)
      }
      same
    }

    val grid = Grid(Const.Xs, Const.Ys, Const.Zs)
    for (iquery <- Const.GridQueries.indices) {
      val val1 = grid.interpolate(Const.GridQueries(iquery), Const.GridData)
      val val2 = Const.GridInterp(iquery)
      assert(equals(val1, val2))
    }
    passTest("grid interpolate")
  }

  def testUtilityRead(filename: String): DenseMatrix[Double] = {
    Policy.readUtility(filename) match {
      case Some(utility) =>
        assert(utility.cols == Const.Cols && utility.rows == Const.Rows)
        passTest("utility read")
        utility

      case None =>
        println("WARN file read unsuccessful, returning empty matrix")
        new DenseMatrix[Double](0, 0)
    }
  }

  def testPolicy(policy: Policy): Unit = {
    var iheatmap = 0
    getHeatmaps(policy).foreach { heatmap =>
      val f = Figure()
      f.subplot(0) += image(heatmap)
      f.saveas("policy" + iheatmap + ".png")
      iheatmap += 1
    }
    println("INFO policy heatmaps plotted for visual inspection")

    println("INFO time policy search for 8 aircraft conflict")
    println("1 iteration ")
    time(1, {timePolicy(policy)})
    println("1000 iterations ")
    time(1000, {timePolicy(policy)})
  }

  /** Plot heatmaps for a three-drones scenario. */
  def getHeatmaps(policy: Policy): Array[DenseMatrix[Double]] = {
    val heatmaps =
      Array(
        DenseMatrix.zeros[Double](Const.Resolution, Const.Resolution),
        DenseMatrix.zeros[Double](Const.Resolution, Const.Resolution),
        DenseMatrix.zeros[Double](Const.Resolution, Const.Resolution))

    val drones =
      Array(
        DroneGlobalState(0.0, 0.0, 0.0, 10.0),
        DroneGlobalState(1000.0, 800.0, 3.141592653589793, 10.0),
        DroneGlobalState(1000.0, -800.0, 3.141592653589793, 10.0))

    // range of x and y coordinates
    val xs = linrange(-2500.0, 2500.0, Const.Resolution)
    val ys = linrange(-2500.0, 2500.0, Const.Resolution)

    for (ix <- xs.indices; iy <- ys.indices) {

      drones(0).latitude = xs(ix)
      drones(0).longitude = ys(iy)

      val jointAction = policy.searchPolicy(drones)._1

      // assign color for action based on a gradient
      for (idrone <- jointAction.indices) {
        if (jointAction(idrone) == Const.ClearOfConflict) {
          heatmaps(idrone)(ix, iy) = Const.ColorClearOfConflict
        } else {
          heatmaps(idrone)(ix, iy) = jointAction(idrone)
        }
      }
    }

    heatmaps
  }

  def linrange(start: Double, end: Double, numElem: Int) =
    for (i <- 0 until numElem) yield start + i * (end - start) / (numElem - 1)

  /** Generates a simple multi-aircraft scenario for timing purpoeses. */
  def timePolicy(policy: Policy): Unit = {
    val drones =
      Array(
        DroneGlobalState(0.0, 0.0, 0.0, 10.0),
        DroneGlobalState(200.0, 300.0, 3.141592653589793, 10.0),
        DroneGlobalState(200.0, -300.0, 3.141592653589793, 10.0),
        DroneGlobalState(600.0, 500.0, 3.141592653589793, 10.0),
        DroneGlobalState(600.0, -500.0, 3.141592653589793, 10.0),
        DroneGlobalState(1000.0, 800.0, 3.141592653589793, 10.0),
        DroneGlobalState(1000.0, -800.0, 3.141592653589793, 10.0),
        DroneGlobalState(1000.0, 0.0, 3.141592653589793, 10.0))
    policy.searchPolicy(drones)
  }
}