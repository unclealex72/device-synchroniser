package devsync.scalafx.view

import com.typesafe.scalalogging.StrictLogging

import scalafx.beans.property.LongProperty
import scalafx.scene.control.{Label, ProgressBar}
import scalafx.scene.layout.StackPane
import scalafx.scene.text.Font

/**
  * A progress bar that is annotated by the total number of items of work and the work done.
  */
class AnnotatedProgressBar(val defaultLabelPadding: Int = 5)(implicit defaultFont: Font) extends StackPane with StrictLogging {

  private val _workDone = new LongProperty(this, "workDone", -1)
  private val _totalWork = new LongProperty(this, "totalWork", -1)

  def workDone: LongProperty = _workDone
  def workDone_=(v: Long) {
    _workDone() = v
  }
  def totalWork: LongProperty = _totalWork
  def totalWork_=(v: Long) {
    _totalWork() = v
  }

  val bar = new ProgressBar
  val label = new Label {
    font = defaultFont
  }

  Seq(workDone, totalWork).foreach(_.onChange(syncProgress()))

  bar.maxWidth = Double.MaxValue
  children = List(bar, label)
  syncProgress()

  def updateTo(txt: String, progress: Double) {
    label.text = txt
    bar.progress = progress
  }

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