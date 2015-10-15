package spark.driver

object Test {
  def main(args: Array[String]): Unit = {
    println("INFO beginning unit tests")

    Streamer.main(Array("localhost:9092", "conflict", "false"))

    println("INFO unit tests complete")
  }
}