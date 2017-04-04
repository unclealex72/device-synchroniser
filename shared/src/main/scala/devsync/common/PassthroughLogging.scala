package devsync.common

import com.typesafe.scalalogging.StrictLogging

import scala.util.{Failure, Success, Try}

/**
  * Created by alex on 01/04/17
  * Syntactic sugar for logging in for comprehensions and Try statements
  **/
trait PassthroughLogging extends StrictLogging {

  implicit class AnyImplicits[V](v: V) {
    def info(message: => String): V = {
      logger.info(message)
      v
    }
  }

  implicit class TryImplicits[T](t: Either[Exception, T]) {
    def error(message: => String): Either[Exception, T] = {
      t match {
        case Left(e) => logger.error(message, e)
        case _ =>
      }
      t
    }
  }
}
