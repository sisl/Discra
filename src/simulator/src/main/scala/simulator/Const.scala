package simulator

import scala.util.Random

import scala.collection.mutable

import drone._

object Const {
  final val NoiseLatitude = NoiseVar("gaussian")(0.0, 10.0)  // in m
  final val NoiseLongitude = NoiseVar("gaussian")(0.0, 10.0)  // in m
  final val NoiseHeading = NoiseVar("gaussian")(0.0, 10.0.toRadians)  // in rad
  final val NoiseSpeed = NoiseVar("gaussian")(0.0, 1.0)  // in m/s
  final val NoiseTurnRateClear = NoiseVar("uniform")(0.0, 5.0.toRadians)  // in rad/s
  final val NoiseTurnRateConflict = NoiseVar("gaussian")(0.0, 1.0.toRadians)  // in rad/s

  final val RandSeed = 123456
  final val NoiseSample =
    Noise(
      NoiseLatitude,
      NoiseLongitude,
      NoiseHeading,
      NoiseSpeed,
      NoiseTurnRateClear,
      NoiseTurnRateConflict,
      new Random(RandSeed))

  final val StatusUpdatePeriod = 5000  // in ms
  final val TenSeconds = 10000  // in ms

  final val RandLatLon = 120
  final val LatLonMagic = 111111.0

  final val NDrones = 3
  final val Drone0 = Drone("drone0", 0.0, 0.0, 0.0, 10.0, Const.NoiseSample)
  final val Drone1 = Drone("drone1", 800.0, 400.0, 180.0.toRadians, 10.0, Const.NoiseSample)
  final val Drone2 = Drone("drone2", 600.0, -400.0, 180.0.toRadians, 10.0, Const.NoiseSample)
  final val DummyDrones = mutable.HashMap((Drone0.gufi, Drone0), (Drone1.gufi, Drone1), (Drone2.gufi, Drone2))
}
