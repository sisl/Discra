package drone

class Drone(
    var gufi: String,
    var latitude: Double,
    var longitude: Double,
    var heading: Double,
    var speed: Double,
    val noise: Noise)
  extends Serializable {

  def nextState(dt: Double): Unit = {
    this.whiteNoise(dt, noise.noiseTurnRateClear)
  }

  def nextState(advisory: DroneAdvisory): Unit = {
    if (gufi != advisory.gufi) {
      throw new IllegalArgumentException("drone and advisory gufi's do not match")
    } else {
      if (advisory.clearOfConflict) {
        this.whiteNoise(advisory.period, noise.noiseTurnRateClear)
      } else if (advisory.turnRate == 0.0) {
        this.whiteNoise(advisory.period, noise.noiseTurnRateConflict)
      } else {
        this.turn(advisory.period, advisory.turnRate + noise.noiseTurnRateConflict)
      }
    }
  }

  private def whiteNoise(dt: Double, noiseTurnRate: Double): Unit = {
    if (noiseTurnRate == 0.0) {
      this.straight(dt)
    } else {
      this.turn(dt, noiseTurnRate)
    }
  }

  private def straight(dt: Double): Unit = {
    latitude += speed * math.cos(heading) * dt + noise.noiseLatitude
    longitude += speed * math.sin(heading) * dt + noise.noiseLongitude
    heading = DroneUtil.wrapAngle(heading + noise.noiseHeading)
    speed += noise.noiseSpeed
  }

  private def turn(dt: Double, turnRate: Double): Unit = {
    // TODO: check turn rate vs bank angle direction definition
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
