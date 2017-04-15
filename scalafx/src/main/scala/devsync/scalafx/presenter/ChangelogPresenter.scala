package devsync.scalafx.presenter

import java.io.ByteArrayOutputStream
import java.net.URL
import java.nio.file.Path

import cats.data.EitherT
import cats.instances.future._
import cats.syntax.either._
import devsync.json.DeviceDescriptor
import devsync.remote.ChangesClient
import devsync.scalafx.model.{Album, ChangelogItemModel}
import devsync.scalafx.util.ObservableValues._
import devsync.scalafx.util.{ConstantProgressTask, Services, TaskUpdates}
import devsync.scalafx.view.{ChangelogItemView, DeviceInformationView}

import scala.concurrent.{ExecutionContext, Future}
import scalafx.beans.property.ObjectProperty
import scalafx.collections.ObservableSet
import scalafx.concurrent.Task
import scalafx.scene.{Node, Parent}
import scalafx.scene.text.Font

/**
  * Created by alex on 14/04/17
  **/
case class ChangelogPresenter(
                               currentPresenter: ObjectProperty[Option[Presenter]],
                               serverUrl: URL,
                               devicePath: Path,
                               deviceDescriptor: DeviceDescriptor)
                             (implicit executionContext: ExecutionContext, defaultFont: Font) extends Presenter {
  val items: ObservableSet[ChangelogItemModel] = ObservableSet.empty[ChangelogItemModel]
  val deviceInformationView =
    DeviceInformationView(
      deviceDescriptor.user,
      devicePath,
      deviceDescriptor.maybeLastModified,
      items,
      transition(Some(SynchronisingPresenter(currentPresenter, serverUrl, devicePath, deviceDescriptor)))(currentPresenter))
  val changesClient: ChangesClient = Services.changesClient(serverUrl)
  val changelogTask: ConstantProgressTask[ChangelogItemModel, Int] =
    ConstantProgressTask.fromFuture[ChangelogItemModel, Int] { updates: TaskUpdates[ChangelogItemModel, Int] =>
    EitherT(Future(changesClient.changelogSince(deviceDescriptor.user, deviceDescriptor.maybeLastModified))).map { changelog =>
      changelog.items.foreach { item =>
        val eventualMaybeArtwork = Future {
          val by = new ByteArrayOutputStream()
          changesClient.artwork(item, by).toOption.map(_ => by.toByteArray)
        }
        val eventualRelativePathOrAlbum = Future {
          changesClient.tags(item) match {
            case Right(tags) => Right(Album(tags.album, tags.artist))
            case _ => Left(item.relativePath)
          }
        }
        for {
          maybeArtwork <- eventualMaybeArtwork
          description <- eventualRelativePathOrAlbum
        } yield {
          updates.updateIntermediateValue(Some(ChangelogItemModel(item.at, maybeArtwork, description)))
        }
      }
      changelog.items.length
    }
  }

  changelogTask.intermediateValue.onAltered { maybeItem =>
    items ++= maybeItem
  }

  def content(): Parent = deviceInformationView
  def initialise(): Task[_] = changelogTask
}
