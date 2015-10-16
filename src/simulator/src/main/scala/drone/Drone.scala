package drone

/** Simulated drone object with Dubin's kinematics model. */
class Drone(
    var gufi: String,
    var latitude: Double,
    var longitude: Double,
    var heading: Double,
    var speed: Double,
    val noise: Noise)
  extends Serializable {

  var advisory: Option[DroneAdvisory] = None

  /** Simulates drone's next state based on whether an advisory exists. */
  def nextState(dt: Double): Unit = {
    // execute advisory if one exists
    this.advisory match {
      case Some(adv) =>
        if (gufi != adv.gufi) {
          throw new IllegalArgumentException("drone and advisory gufi's do not match")
        } else {
          if (adv.clearOfConflict) {
            this.whiteNoise(adv.period, noise.noiseTurnRateClear)
          } else if (adv.turnRate == 0.0) {
            this.whiteNoise(adv.period, noise.noiseTurnRateConflict)
          } else {
            this.turn(adv.period, adv.turnRate + noise.noiseTurnRateConflict)
          }
        }
      case None => this.whiteNoise(dt, noise.noiseTurnRateClear)
    }

    // reset advisory
    this.advisory = None
  }

  /** Simulate drone going straight with white noise acceleration model. */
  private def whiteNoise(dt: Double, noiseTurnRate: Double): Unit = {
    if (noiseTurnRate == 0.0) {
      this.straight(dt)
    } else {
      this.turn(dt, noiseTurnRate)
    }
  }

  /** Simulate drone going straight under conflict advisory. */
  private def straight(dt: Double): Unit = {
    latitude += speed * math.cos(heading) * dt + noise.noiseLatitude
    longitude += speed * math.sin(heading) * dt + noise.noiseLongitude
    heading = DroneUtil.wrapAngle(heading + noise.noiseHeading)
    speed += noise.noiseSpeed
  }

  /** Simulate drone banking/turning under conflict advisory. */
  private def turn(dt: Double, turnRate: Double): Unit = {
    val changeHeading = -turnRate * dt
    val turnRadius = math.abs(speed / turnRate)

    latitude += turnRadius * math.signum(changeHeading) *
      (math.sin(heading) - math.sin(heading - changeHeading)) +
      noise.noiseLatitude

    longitude += turnRadius * math.signum(changeHeading) *
      (-math.cos(heading) + math.cos(heading - changeHeading)) +
      noise.noiseLongitude

    heading = DroneUtil.wrapAngle(heading - changeHeading + noise.noiseHeading)
    speed += noise.noiseSpeed
  }
}

object Drone {
  def apply(
      gufi:String,
      latitude: Double,
      longitude: Double,
      heading: Double,
      speed: Double,
      noise: Noise): Drone = {

    new Drone(
      gufi,
      latitude,
      longitude,
      DroneUtil.wrapAngle(heading),
      speed,
      noise)
  }
}
