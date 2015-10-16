package spark.worker

import breeze.numerics.{pow, sqrt}

import spark.worker.dronestate._

object Const {
  final val UtilityFile = "<InsertYourOwnUtilityCSVAbsolutePath>"

  final val Tolerance = 1e-6

  final val state1 = DroneGlobalState(2.0, 4.0, 3.0, sqrt(pow(2.0, 2) + pow(3.0, 2)))
  final val state2 = DroneGlobalState(410.0, 4.5, 3.5, sqrt(pow(20.0, 2) + pow(1.54, 2)))
  val actualLocalState =
    new DroneLocalState(
      -403.8463786089518,
      -58.07195953672605,
      0.5,
      3.605551275463989,
      20.059202376964045)

  final val Xs = for (i <- (0 until 10).toArray) yield i.toDouble
  final val Ys = for (i <- (10 until 20).toArray) yield i.toDouble
  final val Zs = for (i <- (20 until 30).toArray) yield i.toDouble

  final val GridPoints =
    Array(
      0, 477, 320, 609, 647, 35, 562, 307, 184, 903,
      841, 33, 397, 425, 779, 301, 524, 997, 535, 976)
  final val GridXs =
    Array(
      Array(0.0,10.0,20.0),
      Array(7.0,17.0,24.0),
      Array(0.0,12.0,23.0),
      Array(9.0,10.0,26.0),
      Array(7.0,14.0,26.0),
      Array(5.0,13.0,20.0),
      Array(2.0,16.0,25.0),
      Array(7.0,10.0,23.0),
      Array(4.0,18.0,21.0),
      Array(3.0,10.0,29.0),
      Array(1.0,14.0,28.0),
      Array(3.0,13.0,20.0),
      Array(7.0,19.0,23.0),
      Array(5.0,12.0,24.0),
      Array(9.0,17.0,27.0),
      Array(1.0,10.0,23.0),
      Array(4.0,12.0,25.0),
      Array(7.0,19.0,29.0),
      Array(5.0,13.0,25.0),
      Array(6.0,17.0,29.0))
  final val GridData = for (i <- (0 until 1000).toArray) yield i.toDouble
  final val GridQueries =
    Array(
      Array(4.8,16.9,28.2),
      Array(0.8,14.7,24.4),
      Array(8.4,18.9,21.9),
      Array(4.9,13.8,24.7),
      Array(3.9,12.4,22.7),
      Array(2.9,14.7,26.4),
      Array(7.5,14.9,24.8),
      Array(6.6,16.4,23.1),
      Array(1.4,14.9,24.8),
      Array(1.6,13.0,23.7),
      Array(7.7,11.2,25.2),
      Array(8.2,16.4,20.1),
      Array(0.4,13.4,24.6),
      Array(3.4,18.0,20.3),
      Array(6.1,10.6,22.1),
      Array(8.9,11.8,26.7),
      Array(8.4,12.9,22.3),
      Array(6.6,17.5,25.9),
      Array(2.4,16.3,23.6),
      Array(0.9,18.6,28.8))
  final val GridInterp =
    Array(
      893.8,487.79999999999984,287.39999999999986,512.9,297.89999999999986,
      689.8999999999999,536.5000000000001,380.60000000000014,530.4000000000001,
      401.5999999999999,539.6999999999998,82.20000000000013,494.4000000000002,
      113.40000000000008,222.10000000000014,696.8999999999999,267.4000000000001,
      671.5999999999999,425.40000000000015,966.9)

  final val Cols = 36
  final val Rows = 5446

  final val S1 = Array(-2000.0,-1600.0,-1200.0,-800.0,-400.0,0.0,400.0,800.0,1200.0,1600.0,2000.0)
  final val S2 = Array(-2000.0,-1600.0,-1200.0,-800.0,-400.0,0.0,400.0,800.0,1200.0,1600.0,2000.0)
  final val S3 = Array(0.0,1.5708,3.14159,4.71239,6.28319)
  final val S4 = Array(10.0,15.0,20.0)
  final val S5 = Array(10.0,15.0,20.0)

  final val ClearOfConflict = -1.0
  final val ColorClearOfConflict = 0.05

  final val ActionSet =
    Array(
      -0.3490658503988659,
      -0.17453292519943295,
      0.0,
      0.17453292519943295,
      0.3490658503988659,
      ClearOfConflict)

  final val Resolution = 100
}
