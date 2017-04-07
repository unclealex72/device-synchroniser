package devsync.sync

import devsync.json._
import org.specs2.mutable.Specification

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by alex on 06/04/17
  **/
class DeviceImplSpec extends Specification {

  "Adding changes to a device with a malformed device descriptor" should {
    val fs: D =
      D.root(
        f("application/json", "device.json", """wrong"""))
    val changesClient = FauxChangesClient(
      "2017-03-13T22:05:01.000Z",
      FR("Napalm Death", "Scum", 12, "You Suffer.mp3"),
      FA("Queen", "Flash Gordon", 1, "Flash's Theme.mp3", "Flash!"),
      FA("Slayer", "Reign in Blood", 5, "Jesus Saves.mp3", "Phew")
    )
    val listener = new LoggingDeviceListener()
    val result: Either[(Exception, Option[Int]), Int] = Await.result(
      new DeviceImpl[FauxFile](new CirceCodec, now("2017-03-13T22:05:01.000Z")).synchronise(
        fs, changesClient, listener), 1.minute)
    "fail fast" in {
      result must beLeft.like {
        case (_: Exception, maybeIdx: Option[Int]) => maybeIdx must beNone
      }
      listener.log must be_==(Seq(
        "START",
        "FAILED"))
    }
    "leave the device alone" in {
      fs.flatten must be_==(Seq(
        "/",
        "/device.json application/json Some(wrong)"
      ))
    }
  }

  "Adding changes to an empty device" should {
    val fs: D =
      D.root(
        f("application/json", "device.json", """{"user": "alex"}"""))
    val changesClient = FauxChangesClient(
      "2017-03-13T22:04:01.000Z",
      FR("Napalm Death", "Scum", 12, "You Suffer.mp3"),
      FA("Queen", "Flash Gordon", 1, "Flash's Theme.mp3", "Flash!"),
      FA("Slayer", "Reign in Blood", 5, "Jesus Saves.mp3", "Phew")
    )
    val listener = new LoggingDeviceListener()
    val result: Either[(Exception, Option[Int]), Int] = Await.result(
      new DeviceImpl[FauxFile](new CirceCodec, now("2017-03-13T22:05:01.000Z")).synchronise(
        fs, changesClient, listener), 1.minute)
    "create new files for all each addition and ignore removals" in {
      fs.flatten must be_==(Seq(
        "/",
        "/Q/", "/Q/Queen/", "/Q/Queen/Flash Gordon/", "/Q/Queen/Flash Gordon/1 Flash's Theme.mp3 audio/mp3 Some(Flash!)",
        "/S/", "/S/Slayer/", "/S/Slayer/Reign in Blood/", "/S/Slayer/Reign in Blood/5 Jesus Saves.mp3 audio/mp3 Some(Phew)",
        "/device.json application/json Some({\"user\":\"alex\",\"lastModified\":\"2017-03-13T22:05:01.000Z\"})"
      ))
    }
    "log both additions and removals" in {
      listener.log must be_==(Seq(
        "START",
        "REMOVING|N/Napalm Death/Scum/12 You Suffer.mp3|0|3", "REMOVED|N/Napalm Death/Scum/12 You Suffer.mp3|0|3",
        "ADDING|Q/Queen/Flash Gordon/1 Flash's Theme.mp3|1|3", "ADDED|Q/Queen/Flash Gordon/1 Flash's Theme.mp3|1|3",
        "ADDING|S/Slayer/Reign in Blood/5 Jesus Saves.mp3|2|3", "ADDED|S/Slayer/Reign in Blood/5 Jesus Saves.mp3|2|3",
        "FINISHED|3"
      ))
    }
    "identify the number of changes made" in {
      result must beRight(3)
    }
  }

  "Adding changes to a populated device" should {
    val fs: D =
      D.root(
        f("application/json", "device.json", """{"user": "alex"}"""),
        d("N", d("Napalm Death", d("Scum", f("audio/mp3", "12 You Suffer.mp3", "But why?")))),
        d("N", d("Nirvana", d("Nevermind", f("audio/mp3", "3 Lithium.mp3", "Grunge")))))
    val changesClient = FauxChangesClient(
      "2017-03-13T22:04:01.000Z",
      FR("Napalm Death", "Scum", 12, "You Suffer.mp3"),
      FA("Queen", "Flash Gordon", 1, "Flash's Theme.mp3", "Flash!"),
      FA("Slayer", "Reign in Blood", 5, "Jesus Saves.mp3", "Phew")
    )
    val listener = new LoggingDeviceListener()
    val result: Either[(Exception, Option[Int]), Int] = Await.result(
      new DeviceImpl[FauxFile](new CirceCodec, now("2017-03-13T22:05:01.000Z")).synchronise(
        fs, changesClient, listener), 1.minute)
    "create new files for all each addition and remove the removals" in {
      fs.flatten must be_==(Seq(
        "/",
        "/N/", "/N/Nirvana/", "/N/Nirvana/Nevermind/", "/N/Nirvana/Nevermind/3 Lithium.mp3 audio/mp3 Some(Grunge)",
        "/Q/", "/Q/Queen/", "/Q/Queen/Flash Gordon/", "/Q/Queen/Flash Gordon/1 Flash's Theme.mp3 audio/mp3 Some(Flash!)",
        "/S/", "/S/Slayer/", "/S/Slayer/Reign in Blood/", "/S/Slayer/Reign in Blood/5 Jesus Saves.mp3 audio/mp3 Some(Phew)",
        "/device.json application/json Some({\"user\":\"alex\",\"lastModified\":\"2017-03-13T22:05:01.000Z\"})"
      ))
    }
    "log both additions and removals " in {
      listener.log must be_==(Seq(
        "START",
        "REMOVING|N/Napalm Death/Scum/12 You Suffer.mp3|0|3", "REMOVED|N/Napalm Death/Scum/12 You Suffer.mp3|0|3",
        "ADDING|Q/Queen/Flash Gordon/1 Flash's Theme.mp3|1|3", "ADDED|Q/Queen/Flash Gordon/1 Flash's Theme.mp3|1|3",
        "ADDING|S/Slayer/Reign in Blood/5 Jesus Saves.mp3|2|3", "ADDED|S/Slayer/Reign in Blood/5 Jesus Saves.mp3|2|3",
        "FINISHED|3"
      ))
    }
    "identify the number of changes made" in {
      result must beRight(3)
    }
  }

  "A set of changes with a failure" should {
    val fs: D =
      D.root(
        f("application/json", "device.json", """{"user": "alex"}"""),
        d("N", d("Nirvana", d("Nevermind", f("audio/mp3", "3 Lithium.mp3", "Grunge")))))
    val changesClient = FauxChangesClient(
      "2017-03-13T22:04:01.000Z",
      FA("Queen", "Flash Gordon", 1, "Flash's Theme.mp3", "Flash!"),
      FF("Slayer", "Reign in Blood", 5, "Jesus Saves.mp3", new Exception("Oh dear")),
      FA("Napalm Death", "Scum", 12, "You Suffer.mp3", "But why?")
    )
    val listener = new LoggingDeviceListener()
    val result: Either[(Exception, Option[Int]), Int] = Await.result(
      new DeviceImpl[FauxFile](new CirceCodec, now("2017-03-13T22:05:01.000Z")).synchronise(
        fs, changesClient, listener), 1.minute)
    "process all changes before the failure and mark it in the device descriptor file" in {
      fs.flatten must be_==(Seq(
        "/",
        "/N/", "/N/Nirvana/", "/N/Nirvana/Nevermind/", "/N/Nirvana/Nevermind/3 Lithium.mp3 audio/mp3 Some(Grunge)",
        "/Q/", "/Q/Queen/", "/Q/Queen/Flash Gordon/", "/Q/Queen/Flash Gordon/1 Flash's Theme.mp3 audio/mp3 Some(Flash!)",
        "/S/", "/S/Slayer/", "/S/Slayer/Reign in Blood/", "/S/Slayer/Reign in Blood/5 Jesus Saves.mp3 audio/mp3 Some()",
        "/device.json application/json Some({\"user\":\"alex\",\"offset\":1})"
      ))
    }
    "log up to the failure" in {
      listener.log must be_==(Seq(
        "START",
        "ADDING|Q/Queen/Flash Gordon/1 Flash's Theme.mp3|0|3", "ADDED|Q/Queen/Flash Gordon/1 Flash's Theme.mp3|0|3",
        "ADDING|S/Slayer/Reign in Blood/5 Jesus Saves.mp3|1|3",
        "FAILED|1"))
    }
    "identify the number of changes made and the failure message" in {
      result must beLeft.like {
        case (e: Exception, maybeIdx: Option[Int]) =>
          maybeIdx must beSome(1)
          e.getMessage must be_==("Change S/Slayer/Reign in Blood/5 Jesus Saves.mp3 is marked as a failure")
      }
    }
  }

  "Continuing from a failure" should {
    val fs: D =
      D.root(
        f("application/json", "device.json", """{"user": "alex", "offset": 1}"""),
        d("N", d("Nirvana", d("Nevermind", f("audio/mp3", "3 Lithium.mp3", "Grunge")))),
        d("Q", d("Queen", d("Flash Gordon", f("audio/mp3", "1 Flash's Theme.mp3", "Flash")))),
        d("S", d("Slayer", d("Reign in Blood", f("audio/mp3", "5 Jesus Saves.mp3", "")))))
    val changesClient = FauxChangesClient(
      "2017-03-13T22:04:01.000Z",
      FA("Queen", "Flash Gordon", 1, "Flash's Theme.mp3", "Flash!"),
      FA("Slayer", "Reign in Blood", 5, "Jesus Saves.mp3", "Phew"),
      FA("Napalm Death", "Scum", 12, "You Suffer.mp3", "But why?")
    )
    val listener = new LoggingDeviceListener()
    val result: Either[(Exception, Option[Int]), Int] = Await.result(
      new DeviceImpl[FauxFile](new CirceCodec, now("2017-03-13T22:05:01.000Z")).synchronise(
        fs, changesClient, listener), 1.minute)
    "continue from the change that previously failed" in {
      fs.flatten must be_==(Seq(
        "/",
        "/N/",
          "/N/Napalm Death/", "/N/Napalm Death/Scum/", "/N/Napalm Death/Scum/12 You Suffer.mp3 audio/mp3 Some(But why?)",
          "/N/Nirvana/", "/N/Nirvana/Nevermind/", "/N/Nirvana/Nevermind/3 Lithium.mp3 audio/mp3 Some(Grunge)",
        "/Q/", "/Q/Queen/", "/Q/Queen/Flash Gordon/", "/Q/Queen/Flash Gordon/1 Flash's Theme.mp3 audio/mp3 Some(Flash)",
        "/S/", "/S/Slayer/", "/S/Slayer/Reign in Blood/", "/S/Slayer/Reign in Blood/5 Jesus Saves.mp3 audio/mp3 Some(Phew)",
        "/device.json application/json Some({\"user\":\"alex\",\"lastModified\":\"2017-03-13T22:05:01.000Z\"})"))
    }
    "not log adding anything before the failure" in {
      listener.log must be_==(Seq(
        "START",
        "ADDING|S/Slayer/Reign in Blood/5 Jesus Saves.mp3|1|3", "ADDED|S/Slayer/Reign in Blood/5 Jesus Saves.mp3|1|3",
        "ADDING|N/Napalm Death/Scum/12 You Suffer.mp3|2|3", "ADDED|N/Napalm Death/Scum/12 You Suffer.mp3|2|3",
        "FINISHED|3"))

    }
    "identify the number of changes made" in {
      result must beRight(3)
    }
  }

  implicit def stringToIsoDate(str: String): IsoDate = IsoDate(str).right.get

  def now(isoDate: IsoDate): IsoClock = new IsoClock {
    override def now: IsoDate = isoDate
  }

  class LoggingDeviceListener extends DeviceListener[FauxFile] {
    val log: mutable.Buffer[String] = mutable.Buffer.empty[String]

    def add(strs: Any*): Unit = {
      log += strs.mkString("|")
    }

    override def synchronisingStarting(): Unit = {
      add("START")
    }

    override def addingMusic(
                              addition: Addition,
                              maybeTags: Option[Tags],
                              maybeArtwork: Option[Array[Byte]],
                              number: Int,
                              total: Int): Unit = {
      add("ADDING", addition.relativePath, number, total)
    }

    override def musicAdded(addition: Addition, maybeTags: Option[Tags], maybeArtwork: Option[Array[Byte]], number: Int, total: Int, resource: FauxFile): Unit = {
      add("ADDED", addition.relativePath, number, total)
    }

    override def removingMusic(removal: Removal, number: Int, total: Int): Unit = {
      add("REMOVING", removal.relativePath, number, total)
    }

    override def musicRemoved(removal: Removal, number: Int, total: Int): Unit = {
      add("REMOVED", removal.relativePath, number, total)
    }

    override def synchronisingFailed(e: Exception, maybeIdx: Option[Int]): Unit = {
      add(Seq("FAILED") ++ maybeIdx :_*)
    }

    override def synchronisingFinished(count: Int): Unit = {
      add("FINISHED", count)
    }
  }
}
