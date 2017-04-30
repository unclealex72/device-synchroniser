/*
 * Copyright 2017 Alex Jones
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
  **/
object DeviceSynchroniserPlus extends JFXApp with StrictLogging {

  private implicit val defaultFont: Font = new Font("Ubuntu", 18)

  private def ui(callback: =>Unit): EitherT[Future, Exception, Unit] = EitherT.right {
    Platform.runLater(callback)
    Future.successful()
  }

  private val observableState: ObjectProperty[Option[Presenter]] = ObjectProperty(None)

  stage = new PrimaryStage {
    title = "Device Synchroniser+"
    width = 900
    height = 600
  }

  private val pane = new Pane
  private val scene = new Scene
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
