package devsync.scalafx.presenter

import java.nio.file.Paths

import cats.instances.future._
import devsync.scalafx.util.{ConstantProgressTask, Services, TaskUpdates}
import devsync.scalafx.view.DiscoveryView

import scala.concurrent.ExecutionContext
import scalafx.beans.property.ObjectProperty
import scalafx.concurrent.Task
import scalafx.scene.{Node, Parent}
import scalafx.scene.text.Font

/**
  * Created by alex on 14/04/17
  **/
case class DiscoveryPresenter(currentPresenter: ObjectProperty[Option[Presenter]])
                             (implicit executionContext: ExecutionContext, defaultFont: Font) extends Presenter {

  val discoveryView = DiscoveryView()
  val initialisingTask: ConstantProgressTask[Unit, Unit] = ConstantProgressTask.fromFuture[Unit, Unit] { (updater: TaskUpdates[Unit, Unit]) =>
    for {
      _ <- updater.updateMessageF("Searching for a Flac Manager server")
      url <- Services.flacManagerDiscovery.discover
      _ <- updater.updateMessageF("Searching for a device")
      deviceDescriptorAndPath <- Services.deviceDiscoverer.discover(Paths.get("/media", System.getProperty("user.name")), 2)
    } yield {
      transition(Some(ChangelogPresenter(currentPresenter, url, deviceDescriptorAndPath._2, deviceDescriptorAndPath._1)))(currentPresenter)
    }
  }
  discoveryView.message <== initialisingTask.message
  def content(): Parent = discoveryView
  def initialise(): Task[_] = initialisingTask

}
