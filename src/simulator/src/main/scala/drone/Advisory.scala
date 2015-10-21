package drone

import net.liftweb.json._

case class Advisories(advisories: List[Advisory])
case class Advisory(gufi: String, clearOfConflict: String, waypoints: List[Waypoint])
case class Waypoint(
    lat: String,      // m
    lon: String,      // m
    speed: String,    // m/s
    heading: String,  // rad
    period: String,   // s
    turn: String)     // rad/s

class DroneAdvisory(
    val gufi: String,
    val clearOfConflict: Boolean,
    val turnRate: Double,
    val period: Double) {

  override def toString =
    "\tgufi: " + gufi +
    "\n\tclear of conflict: " + clearOfConflict +
    "\n\tturn rate: " + turnRate +
    "\n\tperiod: " + period
}

object DroneAdvisory {
  implicit val formats = DefaultFormats  // for json

  /**
   * Builds a custom DroneAdvisory from an Advisories-based json string for
   * local interpretation. Assume <raw> only has a single Waypoint for each
   * advisory.
   */
  def getDroneAdvisory(raw: String): DroneAdvisory = {
    val rawAdvisory = parse(raw).extract[Advisory]
    new DroneAdvisory(
      gufi = rawAdvisory.gufi,
      clearOfConflict = rawAdvisory.clearOfConflict match {
        case "true" => true
        case "false" => false
        case _ => throw new IllegalArgumentException("invalid clearOfConflict indicator string")
      },
      turnRate = rawAdvisory.waypoints.head.turn.toDouble,
      period = rawAdvisory.waypoints.head.period.toDouble)
  }

  /**
   * Builds a list of custom DroneAdvisory from an Advisories-based json string
   * for local interpretation. Assume <raw> only has a single Waypoint for each
   * advisory.
   */
  def getDroneAdvisories(raw: String): Array[DroneAdvisory] = {
    val rawAdvisories = parse(raw).extract[Advisories].advisories
    for (idrone <- rawAdvisories.indices.toArray) yield {
      new DroneAdvisory(
        gufi = rawAdvisories(idrone).gufi,
        clearOfConflict = rawAdvisories(idrone).clearOfConflict match {
          case "true" => true
          case "false" => false
          case _ => throw new IllegalArgumentException("invalid clearOfConflict indicator string")
        },
        turnRate = rawAdvisories(idrone).waypoints.head.turn.toDouble,
        period = rawAdvisories(idrone).waypoints.head.period.toDouble)
    }
  }
}
