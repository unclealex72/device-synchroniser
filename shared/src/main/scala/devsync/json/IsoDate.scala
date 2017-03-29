package devsync.json

import java.text.{DateFormat, SimpleDateFormat}
import java.util.Date

import scala.util.Try

/**
  * Created by alex on 24/03/17
  **/
case class IsoDate(date: Date, fmt: String)

object IsoDate {

  private def df(): DateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

  def apply(date: Date): IsoDate = {
    IsoDate(date, df().format(date))
  }

  def apply(date: String): Try[IsoDate] = Try {
    IsoDate(df().parse(date.replace("Z", "+0000")), date)
  }

  def apply(millis: Long): IsoDate = {
    apply(new Date(millis))
  }

  implicit def isoDateToDate: IsoDate => Date = _.date
  implicit def isoDateToString: IsoDate => String = _.fmt
  implicit def dateToIsoDate: Date => IsoDate = apply
}
