package devsync.json

import java.text.{DateFormat, SimpleDateFormat}
import java.util.Date

import scala.util.Try

/**
  * Created by alex on 24/03/17
  **/
case class IsoDate(date: Date, fmt: String) {

  def format(df: String): String = {
    new SimpleDateFormat(df).format(date)
  }
}

object IsoDate {

  private def df(): DateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

  def apply(date: Date): IsoDate = {
    IsoDate(date, df().format(date))
  }

  def apply(date: String): Either[Exception, IsoDate] = {
    try {
      Right(IsoDate(df().parse(date.replace("Z", "+0000")), date))
    }
    catch {
      case e: Exception => Left(e)
    }
  }

  def apply(millis: Long): IsoDate = {
    apply(new Date(millis))
  }

  implicit def isoDateToDate: IsoDate => Date = _.date
  implicit def isoDateToString: IsoDate => String = _.fmt
  implicit def dateToIsoDate: Date => IsoDate = apply
}

trait IsoClock {
  def now: IsoDate
}

object SystemIsoClock extends IsoClock {
  def now: IsoDate = IsoDate(System.currentTimeMillis())
}