package devsync.scalafx.presenter

import devsync.scalafx.DeviceSynchroniserPlus.ui

import scalafx.beans.property.ObjectProperty
import scalafx.concurrent.Task
import scalafx.scene.Parent

/**
  * Created by alex on 14/04/17
  **/
trait Presenter {

  def content(): Parent
  def initialise(): Task[_]

  def transition(maybeNewPresenter: Option[Presenter]): ObjectProperty[Option[Presenter]] => Unit = currentPresenter => ui {
    currentPresenter.value = maybeNewPresenter
  }

}
