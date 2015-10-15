package spark.driver

object Const {
  // replace the path to the utility data file with the one on your machine
  final val UtilityFile = "/Users/Icarus/Documents/projects/utm-alpha/data/utility.csv"

  final val UtilityRows = 5446
  final val UtilityCols = 36

  final val S1 = Array(-2000.0,-1600.0,-1200.0,-800.0,-400.0,0.0,400.0,800.0,1200.0,1600.0,2000.0)
  final val S2 = Array(-2000.0,-1600.0,-1200.0,-800.0,-400.0,0.0,400.0,800.0,1200.0,1600.0,2000.0)
  final val S3 = Array(0.0,1.5708,3.14159,4.71239,6.28319)
  final val S4 = Array(10.0,15.0,20.0)
  final val S5 = Array(10.0,15.0,20.0)

  final val ClearOfConflict = -1.0
  final val ActionSet =
    Array(
      -0.3490658503988659,
      -0.17453292519943295,
      0.0,
      0.17453292519943295,
      0.3490658503988659,
      ClearOfConflict)

  final val DecisionPeriod = 5  // in seconds

  final val Meter2Feet = 3.28084  // in feet per meter
  final val G = 9.8  // in meter/second^2

  final val MaxNumProducers = 4
}
