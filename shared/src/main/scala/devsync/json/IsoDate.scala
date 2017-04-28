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

package devsync.json

import java.text.{DateFormat, SimpleDateFormat}
import java.util.Date

import org.fourthline.cling.model.ModelUtil

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

  private val formatString: String = "yyyy-MM-dd'T'HH:mm:ss.SSS" + (if (ModelUtil.ANDROID_RUNTIME) "Z" else "X")
  private def df(): DateFormat = new SimpleDateFormat(formatString)

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