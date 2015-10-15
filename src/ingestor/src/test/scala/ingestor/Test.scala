package ingestor

/** Test local server. */
object Test {
  def main(args: Array[String]): Unit = {
    println("INFO beginning unit tests")

    Ingestor.main(Array("localhost:9092", "conflict", "false"))

    println("INFO unit tests complete")
  }
}
