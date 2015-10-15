package drone

object DroneUtil {
  /** Wraps <angle> in radians to between -math.Pi and math.Pi. */
  def wrapAngle(angle: Double): Double =
    ((angle % (2 * math.Pi)) + 2 * math.Pi) % (2 * math.Pi)
}
