

/**
  * Application settings. Configure the build for your application here.
  * You normally don't have to touch the actual build definition after this.
  */
object Settings {
  /** The name of your application */
  val name = "devsyncplus"

  val organisation = "uk.co.unclealex.devsync"

  /** Options for the scala compiler */
  val scalacOptions = Seq(
    "-Xlint",
    "-unchecked",
    "-deprecation",
    "-feature"
  )

  /** Declare global dependency versions here to avoid mismatches in multi part dependencies */
  object versions {
    val cats = "0.9.0"
    val circe = "0.7.0"
    val clingCore = "2.1.1"
    val jetty = "8.1.8.v20121106"
    val logback = "1.1.7"
    val macroid = "2.0"
    val scala = "2.11.8"
    val scalafx = "8.0.102-R11"
    val scalaLogging = "3.5.0"
    val specs2 = "3.7"
  }
}
