import sbt._

/**
  * Application settings. Configure the build for your application here.
  * You normally don't have to touch the actual build definition after this.
  */
object Settings {
  /** The name of your application */
  val name = "devsync"

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
    val scala = "2.11.8"
    val scalaDom = "0.9.1"
    val scalajsReact = "0.11.3"
    val scalaJsReactComponents = "0.5.0"
    val scalaCSS = "0.5.0"
    val diode = "1.1.0"
    val flexboxgrid = "6.3.0"
    val roboto = "0.4.5"
    val clingCore = "2.1.1"
    val react = "15.3.1"

    val scalaz = "7.2.0"
    val logging = "2.1.2"

    val guice = "4.0.1"
    val ical4j = "1.0.4"
    val specs2 = "3.7"

    val json4s = "3.5.1"
    val enumeratum = "1.3.6"

    val scalaLogging = "3.5.0"
    val logback = "1.1.7"

    val android = "23.1.+"
    val macroid = "2.0"
    val jetty = "8.1.8.v20121106"
  }


  /**
    * These dependencies are shared between JS and JVM projects
    * the special %%% function selects the correct version for each project
    */
  val sharedDependencies: sbt.Def.Initialize[Seq[ModuleID]] = Def.setting(Seq(
    "com.beachape" %% "enumeratum" % versions.enumeratum,
    "io.circe" %% "circe-core" % "0.7.0",
    "io.circe" %% "circe-parser" % "0.7.0",
    "org.fourthline.cling" % "cling-core" % versions.clingCore,
    "com.typesafe.scala-logging" %% "scala-logging" % versions.scalaLogging,
    "ch.qos.logback" % "logback-classic" % versions.logback % Test,
    "org.typelevel" %% "cats" % "0.9.0",
    "org.specs2" %% "specs2-core" % versions.specs2 % Test,
    "org.specs2" %% "specs2-mock" % versions.specs2 % Test
  ))

  val androidDependencies: sbt.Def.Initialize[Seq[ModuleID]] = Def.setting(Seq(


  ))
}
