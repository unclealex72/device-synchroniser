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

package uk.co.unclealex.devsync

import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import macroid.{ContextWrapper, Ui}

import scala.concurrent.{ExecutionContext, Future}

/**
  * A class used to build and show alert dialogues.
  * @param maybeTitle If set, either a string resource ID or string to use for the dialog box's title.
  * @param maybeMessage If set, either a string resource ID or string to use for the dialog box's message.
  * @param maybePositiveDialogButton If set, a builder for the dialog box's positive button.
  * @param maybeNegativeDialogButton If set, a builder for the dialog box's negative button.
  */
case class DialogBuilder(
                          maybeTitle: Option[Either[Int, String]] = None,
                          maybeMessage: Option[Either[Int, String]] = None,
                          maybePositiveDialogButton: Option[DialogButtonBuilder] = None,
                          maybeNegativeDialogButton: Option[DialogButtonBuilder] = None) {

  /**
    * Set the main message to show for the dialog box to be built.
    * @param messageId A string resource ID to use for the dialog box's message.
    * @return A fluent dialog builder.
    */
  def message(messageId: Int): DialogBuilder = this.copy(maybeMessage = Some(Left(messageId)))

  /**
    * Set the main message to show for the dialog box to be built.
    * @param message A string to use for the dialog box's message.
    * @return A fluent dialog builder.
    */
  def message(message: String): DialogBuilder = this.copy(maybeMessage = Some(Right(message)))

  /**
    * Add a positive button to the dialog box.
    * @param buttonTextId A string resource ID to use for the button's text.
    * @param action The action to perform when the button is clicked.
    * @return A fluent dialog builder.
    */
  def positiveButton(buttonTextId: Int)(action: => Unit): DialogBuilder =
    this.copy(maybePositiveDialogButton = Some(DialogButtonBuilder(Left(buttonTextId), () => action)))

  /**
    * Add a positive button to the dialog box.
    * @param buttonText A string to use for the button's text.
    * @param action The action to perform when the button is clicked.
    * @return A fluent dialog builder.
    */
  def positiveButton(buttonText: String)(action: => Unit): DialogBuilder =
    this.copy(maybePositiveDialogButton = Some(DialogButtonBuilder(Right(buttonText), () => action)))

  /**
    * Add a negative button to the dialog box.
    * @param buttonTextId A string resource ID to use for the button's text.
    * @param action The action to perform when the button is clicked.
    * @return A fluent dialog builder.
    */
  def negativeButton(buttonTextId: Int)(action: => Unit): DialogBuilder =
    this.copy(maybeNegativeDialogButton = Some(DialogButtonBuilder(Left(buttonTextId), () => action)))

  /**
    * Add a negative button to the dialog box.
    * @param buttonText A string to use for the button's text.
    * @param action The action to perform when the button is clicked.
    * @return A fluent dialog builder.
    */
  def negativeButton(buttonText: String)(action: => Unit): DialogBuilder =
    this.copy(maybeNegativeDialogButton = Some(DialogButtonBuilder(Right(buttonText), () => action)))

  /**
    * Build and show the dialog box in a [[https://github.com/47deg/macroid Macroid]] `Ui` block.
    * @param contextWrapper A context wrapper used to provide the context for building the dialog box.
    * @return A `Ui` that can be run to show the dialog box.
    */
  def showUi(implicit contextWrapper: ContextWrapper): Ui[Unit] = Ui {
    val builder = new AlertDialog.Builder(contextWrapper.bestAvailable)
    maybeTitle.foreach(_.fold(builder.setTitle, builder.setTitle))
    maybeMessage.foreach(_.fold(builder.setMessage, builder.setMessage))
    maybePositiveDialogButton.foreach(_.build(builder.setPositiveButton, builder.setPositiveButton))
    maybeNegativeDialogButton.foreach(_.build(builder.setNegativeButton, builder.setNegativeButton))
    builder.create().show()
  }

  /**
    * Build and show the dialog box.
    * @param contextWrapper A context wrapper used to provide the context for building the dialog box.
    * @param executionContext The execution context used to run the dialog builder code.
    * @return A future that will eventually show the dialog box.
    */
  def show(implicit contextWrapper: ContextWrapper, executionContext: ExecutionContext): Future[Unit] = showUi.run
}

/**
  * A case class used to build dialog box buttons.
  * @param buttonText Either a string resource ID or string to use as the button's text.
  * @param buttonAction The code to execute when the button is pressed.
  */
case class DialogButtonBuilder(buttonText: Either[Int, String], buttonAction: () => Unit) {

  /**
    * Add a button to an alert dialog builder.
    * @param idBuilder The function used to add a button with a string resource ID for it's text.
    * @param stringBuilder The function used to add a button with a string for it's text.
    */
  def build(
             idBuilder: (Int, DialogInterface.OnClickListener) => AlertDialog.Builder,
             stringBuilder: (String, DialogInterface.OnClickListener) => AlertDialog.Builder): Unit = {
    val listener = new DialogInterface.OnClickListener {
      override def onClick(dialog: DialogInterface, which: Int): Unit = buttonAction()
    }
    val builder = buttonText.fold(id => idBuilder.curried(id), text => stringBuilder.curried(text))
    builder(listener)
  }
}

/**
  * Consistently create Android dialog boxes.
  */
object DialogBuilder {

  /**
    * Set the title to show for the dialog box to be built.
    * @param titleId A string resource ID to use for the dialog box's title.
    * @return A fluent dialog builder.
    */
  def title(titleId: Int) = new DialogBuilder(maybeTitle = Some(Left(titleId)))

  /**
    * Set the title to show for the dialog box to be built.
    * @param title A string to use for the dialog box's title.
    * @return A fluent dialog builder.
    */
  def title(title: String) = new DialogBuilder(maybeTitle = Some(Right(title)))
}
