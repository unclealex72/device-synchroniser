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

package devsync.scalafx.view

import com.typesafe.scalalogging.StrictLogging

import scalafx.beans.property.LongProperty
import scalafx.scene.control.{Label, ProgressBar}
import scalafx.scene.layout.StackPane
import scalafx.scene.text.Font

/**
  * A progress bar that is annotated by the total number of items of work and the work done.
  * @param defaultLabelPadding The amount of padding to add to the label containing the number of items of work
  *                            and work done.
  * @param defaultFont The default font to use.
  */
class AnnotatedProgressBar(val defaultLabelPadding: Int = 5)(implicit defaultFont: Font) extends StackPane with StrictLogging {

  private val _workDone = new LongProperty(this, "workDone", -1)
  private val _totalWork = new LongProperty(this, "totalWork", -1)

  /**
    * Get the amount of work done.
    * @return The work done property.
    */
  def workDone: LongProperty = _workDone

  /**
    * Set the amount of work done.
    * @param v The amount of work done.
    */
  def workDone_=(v: Long) {
    _workDone() = v
  }

  /**
    * Get the amount of total work.
    * @return The total work property.
    */
  def totalWork: LongProperty = _totalWork

  /**
    * Set the total amount of work.
    * @param v The total amount of work.
    */
  def totalWork_=(v: Long) {
    _totalWork() = v
  }

  private val bar = new ProgressBar
  private val label = new Label {
    font = defaultFont
  }

  Seq(workDone, totalWork).foreach(_.onChange(syncProgress()))

  bar.maxWidth = Double.MaxValue
  children = List(bar, label)
  syncProgress()

  /**
    * Update this progress bar.
    * @param txt The text to display.
    * @param progress The amount of work done as a percentage of the total work.
    */
  def updateTo(txt: String, progress: Double) {
    label.text = txt
    bar.progress = progress
  }

  /**
    * Synchronise this view after a change to the amount of work done.
    */
  def syncProgress(): Unit = {
    val workDone = _workDone.value
    val totalWork = _totalWork.value

    val (txt, progress) = if (workDone < 0 || totalWork < 0) {
      ("", 0d)
    }
    else {
      (s"$workDone/$totalWork", (workDone:Double) / totalWork)
    }
    label.text = txt
    bar.progress = progress
    bar.minHeight = label.getBoundsInLocal.getHeight + defaultLabelPadding * 2
    bar.minWidth = label.getBoundsInLocal.getWidth  + defaultLabelPadding * 2
  }
}