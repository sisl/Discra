package spark.worker.dronestate

import breeze.numerics.{cos, sin}

class DroneGlobalState(
    var latitude: Double,
    var longitude: Double,
    var heading: Double,
    var speed: Double)

object DroneGlobalState {
  def apply(
      latitude: Double,
      longitude: Double,
      heading: Double,
      speed: Double): DroneGlobalState = {

    new DroneGlobalState(latitude, longitude, heading, speed)
  }
}

class DroneLocalState(
    var x: Double,
    var y: Double,
    var bearing: Double,
    var speedOwnship: Double,
    var speedIntruder: Double) {

  def isTerminal = this.x == DroneLocalState.TerminalStateValue

  def toArray = Array(this.x, this.y, this.bearing, this.speedOwnship, this.speedIntruder)

}

object DroneLocalState {
  final val DimensionState = 5
  final val Xmin = -2e3
  final val Xmax = 2e3
  final val Ymin = -2e3
  final val Ymax = 2e3
  final val TerminalStateValue = 1e6

  def apply(
      x: Double,
      y: Double,
      bearing: Double,
      speedOwnship: Double,
      speedIntruder: Double): DroneLocalState = {

    new DroneLocalState(x, y, bearing, speedOwnship, speedIntruder)
  }

  def apply(
      iownship: Int,
      intruder: Int,
      drones: Array[DroneGlobalState]): DroneLocalState = {

    val dx = drones(intruder).latitude - drones(iownship).latitude
    val dy = drones(intruder).longitude - drones(iownship).longitude
    val x = dx * cos(drones(iownship).heading) + dy * sin(drones(iownship).heading)
    val y = -dx * sin(drones(iownship).heading) + dy * cos(drones(iownship).heading)
    val bearing = drones(intruder).heading - drones(iownship).heading

    if (outOfRange(x, y)) {
      new DroneLocalState(
        TerminalStateValue,
        TerminalStateValue,
        TerminalStateValue,
        TerminalStateValue,
        TerminalStateValue)
    } else {
      new DroneLocalState(x, y, bearing, drones(iownship).speed, drones(intruder).speed)
    }
  }

  def outOfRange(x: Double, y: Double) = x < Xmin || x > Xmax || y < Ymin || y > Ymax
}