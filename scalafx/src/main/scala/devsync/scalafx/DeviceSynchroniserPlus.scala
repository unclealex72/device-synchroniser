package devsync.scalafx

import cats.data.EitherT
import cats.instances.future._
import com.typesafe.scalalogging.StrictLogging
import devsync.scalafx.presenter.{DiscoveryPresenter, Presenter}
import devsync.scalafx.util.ObservableValues._
import devsync.scalafx.view._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalafx.application.JFXApp.PrimaryStage
import scalafx.application.{JFXApp, Platform}
import scalafx.beans.property.ObjectProperty
import scalafx.Includes._
import scalafx.scene.Scene
import scalafx.scene.control.Alert
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.layout.Pane
import scalafx.scene.text.Font
/**
  * Main entry point for the Device Synchroniser Plus app.
  * Created by alex on 14/04/17
  **/
object DeviceSynchroniserPlus extends JFXApp with StrictLogging {

  implicit val defaultFont: Font = new Font("Ubuntu", 18)

  def ui(callback: =>Unit): EitherT[Future, Exception, Unit] = EitherT.right {
    Platform.runLater(callback)
    Future.successful()
  }

  val observableState: ObjectProperty[Option[Presenter]] = ObjectProperty(None)

  stage = new PrimaryStage {
    title = "Device Synchroniser+"
    width = 800
    height = 600
  }

  val pane = new Pane
  val scene = new Scene
  stage.scene = scene

  observableState.onAltered {
    case Some(state) =>
      val content = state.content()
      scene.root = content
      content match {
        case dimensions: Dimensions =>
          stage.width = dimensions.dimensions.width
          stage.height = dimensions.dimensions.height
        case _ =>
      }
      val task = state.initialise()
      task.onFailed = handle {
        val ex = task.exception.value
        logger.error("An error occurred", ex)
        new Alert(AlertType.Error) {
          initOwner(stage)
          title = "Device Synchroniser+ Error"
          headerText = "An error occurred"
          contentText = ex.getMessage
          resizable = true
        }.showAndWait()
        stage.close()
      }
      Future { state.initialise().run() }
    case None => stage.close()
  }
  observableState.value = Some(DiscoveryPresenter(observableState))
}
