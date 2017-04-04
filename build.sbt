import sbt.Keys._
import sbt._
import sbt.Project.projectToRef

resolvers += "4thline resolver" at "http://4thline.org/m2"

lazy val shared: Project = (project in file("shared"))
  .settings(
    organization := Settings.organisation,
    name := "device-synchroniser-shared",
    version := (version in ThisBuild).value,
    scalaVersion := Settings.versions.scala,
		libraryDependencies ++= Settings.sharedDependencies.value,
	  exportJars := true
  )

lazy val droid = (project in file("android"))
  .settings(
	  android.useSupportVectors,
    organization := Settings.organisation,
    name := "device-synchroniser-android",
    version := (version in ThisBuild).value,
    scalaVersion := Settings.versions.scala,
	  instrumentTestRunner :=
	    "android.support.test.runner.AndroidJUnitRunner",
	  platformTarget := "android-25",
	  minSdkVersion := "23",
	  javacOptions in Compile ++= "-source" :: "1.7" :: "-target" :: "1.7" :: Nil,
	  libraryDependencies ++= Seq("", "-extras").map { suffix =>
      aar("org.macroid" %% s"macroid$suffix" % "2.0")
    } ++ Seq("server", "servlet", "client").map { suffix =>
      "org.eclipse.jetty" % s"jetty-$suffix" % "8.1.8.v20121106"
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
    name := "device-synchroniser-scalafx",
    version := (version in ThisBuild).value,
    scalaVersion := Settings.versions.scala
	).dependsOn(shared)

versionCode := Some(1)
version := "0.1-SNAPSHOT"