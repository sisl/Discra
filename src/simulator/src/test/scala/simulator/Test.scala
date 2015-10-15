package simulator

/** Test local server. */
object Test {
  def main(args: Array[String]): Unit = {
    println("INFO beginning unit tests")

    Simulator.main(Array("localhost:9092", "false"))

    println("INFO unit tests complete")
  }
}
