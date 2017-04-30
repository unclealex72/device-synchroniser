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

package devsync.scalafx.presenter

import cats.data.EitherT
import cats.instances.future._

import scala.concurrent.{ExecutionContext, Future}
import scalafx.application.Platform
import scalafx.beans.property.ObjectProperty
import scalafx.concurrent.Task
import scalafx.scene.Parent

/**
  * The base trait for presenters. Presenters have content, can be initialised and can also transition to the
  * next presenter.
  **/
trait Presenter {

  /**
    * The content (view) for this presenter.
    * @return The content (view) for this presenter.
    */
  def content(): Parent

  /**
    * Initialise this presenter asynchronously.
    * @return A task that initialises this presenter.
    */
  def initialise(): Task[_]

  /**
    * Run a block of code on the UI thread.
    * @param callback The callback to execute on the UI thread.
    * @return Eventually either [[Unit]] or an exception.
    */
  def ui(callback: =>Unit)(implicit ec: ExecutionContext): EitherT[Future, Exception, Unit] =
    EitherT.right[Future, Exception, Unit] {
      Platform.runLater(callback)
      Future.successful()
  }

  /**
    * Transition to the next presenter if one exists.
    * @param maybeNewPresenter The presenter to transition to or none if there are no more presenters.
    * @param currentPresenterProperty A property holding the current presenter.
    * @param ec An execution context used to run the transition task.
    */
  def transition(maybeNewPresenter: Option[Presenter], currentPresenterProperty: ObjectProperty[Option[Presenter]])
                (implicit ec: ExecutionContext): Unit = ui {
    currentPresenterProperty.value = maybeNewPresenter
  }

}
