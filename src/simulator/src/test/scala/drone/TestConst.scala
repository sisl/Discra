package drone

import scala.util.Random

object TestConst {
  final val DummyAdvisories =
    Advisories(List(
      Advisory(
        "fid-test0",
        "false",
        List(
          Waypoint(
            "0.0",
            "0.0",
            "10.0",
            "0.0",
            "5.0",
            "0.175"))),
      Advisory(
        "fid-test1",
        "false",
        List(
          Waypoint(
            "300.0",
            "300.0",
            "12.0",
            "3.142",
            "5.0",
            "-0.175")))))

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
}
