package uk.co.unclealex.devsync

import android.graphics.{Bitmap, BitmapFactory, Matrix, RectF}
import macroid.ContextWrapper

/**
  * Created by alex on 03/04/17
  * Create a bitmap from an array of bytes.
  **/
object Artwork {

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
