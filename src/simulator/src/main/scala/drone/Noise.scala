package drone

import scala.util.Random

abstract class NoiseVar extends Serializable {
  def nextVal(randgen: Random): Double
}

/**
 * Instantiate using currying syntax. Currently supports only two
 * types of distributions for noise.
 *
 * Gaussian noise: NoiseVar("gaussian")(<mean>, <stdev>)
 *   <mean> is the mean of the Gaussian noise
 *   <stdev> is the standard deviation of the Gaussian noise
 *
 * Uniform noise: NoiseVar("uniform")(<minval>, <maxval>)
 *   <minval> is the minimum value of the uniform distribution range
 *   <maxval> is the maximum value of the uniform distribution range
 */
object NoiseVar {
  private class GaussianNoiseVar(
      val mean: Double,
      val stdev: Double)
    extends NoiseVar {

    def nextVal(randgen: Random): Double = mean + stdev * randgen.nextGaussian
  }

  private class UniformNoiseVar(
      val minval: Double,
      val maxval: Double)
    extends NoiseVar {

    def nextVal(randgen: Random): Double = minval + (maxval - minval) * randgen.nextDouble
  }

  def apply(noiseType: String) = noiseType.toLowerCase match {
    case "gaussian" =>
      (mean: Double, stdev: Double) => new GaussianNoiseVar(mean, stdev)
    case "uniform" =>
      (minval: Double, maxval: Double) => new UniformNoiseVar(minval, maxval)
  }
}

class Noise(
    val latitude: NoiseVar,
    val longitude: NoiseVar,
    val heading: NoiseVar,
    val speed: NoiseVar,
    val turnRateClear: NoiseVar,
    val turnRateConflict: NoiseVar,
    val randgen: Random)
  extends Serializable {

  def noiseLatitude: Double = latitude.nextVal(randgen)

  def noiseLongitude: Double = longitude.nextVal(randgen)

  def noiseHeading: Double = heading.nextVal(randgen)

  def noiseSpeed: Double = speed.nextVal(randgen)

  def noiseTurnRateClear: Double = turnRateClear.nextVal(randgen)

  def noiseTurnRateConflict: Double = turnRateConflict.nextVal(randgen)
}

object Noise {
  def apply(
      latitude: NoiseVar,
      longitude: NoiseVar,
      heading: NoiseVar,
      speed: NoiseVar,
      turnRateClear: NoiseVar,
      turnRateConflict: NoiseVar,
      randgen: Random = new Random()): Noise = {

    new Noise(
      latitude,
      longitude,
      heading,
      speed,
      turnRateClear,
      turnRateConflict,
      randgen)
  }
}
