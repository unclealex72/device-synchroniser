import com.typesafe.sbt.packager.linux.LinuxSymlink
import sbt.Keys.{mappings, _}
import sbt._
import sbt.Project.projectToRef
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

resolvers += "4thline resolver" at "http://4thline.org/m2"

lazy val shared: Project = (project in file("shared"))
  .settings(
    organization := Settings.organisation,
    name := "device-synchroniser-shared",
    version := (version in ThisBuild).value,
    scalaVersion := Settings.versions.scala,
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
		libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % Settings.versions.circe,
      "io.circe" %% "circe-parser" % Settings.versions.circe,
      "org.fourthline.cling" % "cling-core" % Settings.versions.clingCore,
      "com.typesafe.scala-logging" %% "scala-logging" % Settings.versions.scalaLogging,
      "ch.qos.logback" % "logback-classic" % Settings.versions.logback % Test,
      "org.typelevel" %% "cats" % Settings.versions.cats,
      "org.specs2" %% "specs2-core" % Settings.versions.specs2 % Test,
      "org.specs2" %% "specs2-mock" % Settings.versions.specs2 % Test
    ),
	  exportJars := true
  )

lazy val droid = (project in file("android"))
  .settings(
	  android.useSupportVectors,
    organization := Settings.organisation,
    name := "device-synchroniser-plus-android",
    version := (version in ThisBuild).value,
    scalaVersion := Settings.versions.scala,
	  instrumentTestRunner :=
	    "android.support.test.runner.AndroidJUnitRunner",
	  platformTarget := "android-25",
	  minSdkVersion := "23",
	  javacOptions in Compile ++= "-source" :: "1.7" :: "-target" :: "1.7" :: Nil,
	  libraryDependencies ++= Seq("", "-extras").map { suffix =>
      aar("org.macroid" %% s"macroid$suffix" % Settings.versions.macroid)
    } ++ Seq("server", "servlet", "client").map { suffix =>
      "org.eclipse.jetty" % s"jetty-$suffix" % Settings.versions.jetty
    } ++ Seq("org.slf4j" % "slf4j-android" % "1.7.25"),
    packagingOptions := PackagingOptions(
      excludes = Seq("about.html")
    ),
    dexMulti := true,
    proguardOptions in Android ++= Seq(
      "-dontobfuscate",
      "-dontoptimize",
      "-keepattributes Annotation, InnerClasses, Signature",
      "-printseeds target/seeds.txt",
      "-printusage target/usage.txt",
      "-dontwarn scala.collection.**", // required from Scala 2.11.4
      "-dontwarn org.seamless.**",
      "-dontwarn org.slf4j.**",
      "-dontwarn org.eclipse.jetty.**",
      "-dontwarn com.thoughtworks.paranamer.**",
      "-dontwarn org.fourthline.cling.**",
      "-ignorewarnings",
      "-keep class scala.Dynamic"
    )
  )
  .enablePlugins(AndroidApp)
  .dependsOn(shared)

lazy val scalafx = (project in file("scalafx"))
  .settings(
    organization := Settings.organisation,
    name := "device-synchroniser-plus",
    version := (version in ThisBuild).value,
    scalaVersion := Settings.versions.scala,
    fork in run := true,
    autoCompilerPlugins := true,
    libraryDependencies ++= Seq(
      "org.scalafx" %% "scalafx" % Settings.versions.scalafx,
      "ch.qos.logback" % "logback-classic" % "1.1.7",
      "com.jfoenix" % "jfoenix" % "1.3.0"
    ),
    maintainer in Debian := "alex.jones@unclealex.co.uk",
    mappings in Universal ++= Seq(
        baseDirectory.value / "src" / "main" / "package" / "device-synchroniser-plus.png" ->
          "icons/device-synchroniser-plus.png",
        baseDirectory.value / "src" / "main" / "package" / "device-synchroniser-plus.desktop" ->
          "applications/device-synchroniser-plus.desktop"
    ),
    linuxPackageSymlinks in Linux ++= Seq(
      LinuxSymlink(
        "/usr/share/applications/device-synchroniser-plus.desktop",
        "/usr/share/device-synchroniser-plus/applications/device-synchroniser-plus.desktop")
    )
  )
  .dependsOn(shared)
  .enablePlugins(JavaAppPackaging, DebianPlugin)

versionCode := Some(1)

/* Releases */

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies, // : ReleaseStep
  inquireVersions, // : ReleaseStep
  runTest, // : ReleaseStep
  setReleaseVersion, // : ReleaseStep
  commitReleaseVersion, // : ReleaseStep, performs the initial git checks
  tagRelease, // : ReleaseStep
  releaseStepCommand("scalafx/debian:packageBin"), // : ReleaseStep, build deb file.
  releaseStepCommand("droid/android:package"), // : ReleaseStep, build deb file.
  setNextVersion, // : ReleaseStep
  commitNextVersion, // : ReleaseStep
  pushChanges // : ReleaseStep, also checks that an upstream branch is properly configured
)