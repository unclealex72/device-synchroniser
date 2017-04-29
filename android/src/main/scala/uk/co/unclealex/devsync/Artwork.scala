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

import android.graphics.{Bitmap, BitmapFactory, Matrix, RectF}
import macroid.ContextWrapper

/**
  * Create a bitmap from an array of bytes or return a default image.
  **/
object Artwork {

  /**
    * Create a bitmap from an array of bytes or return a default image.
    * @param maybeData The image data, if any.
    * @param width The required width of the object.
    * @param height The required height of the object.
    * @param cw An implicit wrapper for the current context.
    * @return A new bitmap meeting the specifications
    */
  def apply(maybeData: Option[Array[Byte]], width: Int, height: Int)(implicit cw: ContextWrapper): Bitmap = {
    val context = cw.bestAvailable
    val maybeUnscaledBitmap = for {
      data <- maybeData
      bitmap <- Option(BitmapFactory.decodeByteArray(data, 0, data.length))
    } yield bitmap
    val unscaledBitmap = maybeUnscaledBitmap.getOrElse(
      BitmapFactory.decodeResource(context.getResources, R.drawable.no_artwork))
    val m = new Matrix
    m.setRectToRect(
      new RectF(0, 0, unscaledBitmap.getWidth, unscaledBitmap.getHeight),
      new RectF(0, 0, width, height),
      Matrix.ScaleToFit.CENTER)
    Bitmap.createBitmap(unscaledBitmap, 0, 0, unscaledBitmap.getWidth, unscaledBitmap.getHeight, m, false)
  }
}
