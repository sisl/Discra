package drone

import net.liftweb.json._
import net.liftweb.json.Serialization.write

import breeze.linalg._
import breeze.plot._

object Test {
  def main(args: Array[String]): Unit = {
    println("INFO beginning unit tests")

    val jsonString = testJSON()
    testDroneAdvisory(jsonString)
    testNoise()
    testDroneDynamics()

    println("INFO unit tests complete")
  }

  def testJSON(): String = {
    val dummyAdvisory = TestConst.DummyAdvisories
    implicit val formats = DefaultFormats
    write(dummyAdvisory)
  }

  def testDroneAdvisory(raw: String): Unit = {
    println("INFO testing drone advisory")
    DroneAdvisory.getDroneAdvisories(raw).foreach(println)
  }

  def testNoise(): Unit = {
    val noise = TestConst.NoiseSample
    println("INFO sampling dynamics noise")
    println("\tlatitude: " + noise.noiseLatitude + " m")
    println("\tlatitude: " + noise.noiseLongitude + " m")
    println("\theading: " + noise.noiseHeading.toDegrees + " deg")
    println("\tspeed: " + noise.noiseSpeed + " m/s")
    println("\tturnRateClear: " + noise.noiseTurnRateClear.toDegrees + " deg/s")
    println("\tturnRateConflict: " + noise.noiseTurnRateConflict.toDegrees + " deg/s")
  }

  /** Plots drone trajectories for visual inspection. */
  def testDroneDynamics(): Unit = {
    val drone0 = Drone("fid-test0", 0.0, 0.0, 0.0, 10.0, TestConst.NoiseSample)
    val drone1 = Drone("fid-test1", 0.0, 0.0, 0.0, 15.0, TestConst.NoiseSample)

    implicit val formats = DefaultFormats
    val jsonAdvisories = write(TestConst.DummyAdvisories)
    val advisories = DroneAdvisory.getDroneAdvisories(jsonAdvisories)

    // test wrong advisory message
    try {
      drone0.nextState(advisories(1))
    } catch {
      case iae: IllegalArgumentException => println(" exception caught")
    }

    // test trajectory
    val ts = 10
    val x0 = DenseVector.zeros[Double](ts)
    val y0 = DenseVector.zeros[Double](ts)
    val x1 = DenseVector.zeros[Double](ts)
    val y1 = DenseVector.zeros[Double](ts)

    for (it <- 0 until ts) {
      x0(it) = drone0.latitude
      y0(it) = drone0.longitude
      x1(it) = drone1.latitude
      y1(it) = drone1.longitude

      drone0.nextState(advisories(0))
      drone1.nextState(advisories(1))
    }

    val f = Figure()
    val p = f.subplot(0)
    p += plot(x0, y0)
    p += plot(x1, y1)

    p.xlabel = "latitude"
    p.ylabel = "longitude"
    f.saveas("drone-test.png")

    println("INFO trajectories plotted and saved to drone-test.png")
  }
}
