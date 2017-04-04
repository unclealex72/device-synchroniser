package uk.co.unclealex.devsync

import java.io.File

import android.app.Application
import android.net.http.HttpResponseCache
import android.util.Log
import com.typesafe.scalalogging.StrictLogging
import devsync.common.PassthroughLogging

import scala.util.Try

/**
  * Created by alex on 01/04/17
  * An application that sets up and destroys HTTP caching.
  **/
class DeviceSynchroniserApplication extends Application with StrictLogging {

  override def onCreate(): Unit = {
    super.onCreate()
    try {
      logger.info("Creating HTTP cache")
      val httpCacheDir = new File(getBaseContext.getCacheDir, "http")
      val httpCacheSize = 10 * 1024 * 1024 // 10 MiB
      HttpResponseCache.install(httpCacheDir, httpCacheSize)
    }
    catch {
      case e: Exception => logger.error("HTTP response cache installation failed", e)
    }
  }

  override def onTerminate(): Unit = {
    Option(HttpResponseCache.getInstalled).foreach(_.flush())
    super.onTerminate()
  }
}
