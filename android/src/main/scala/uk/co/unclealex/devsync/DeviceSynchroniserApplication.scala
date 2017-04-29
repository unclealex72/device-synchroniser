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

import java.io.File

import android.app.Application
import android.net.http.HttpResponseCache
import android.util.Log
import com.typesafe.scalalogging.StrictLogging
import devsync.common.PassthroughLogging

import scala.util.Try

/**
  * An application that sets up and destroys HTTP caching.
  **/
class DeviceSynchroniserApplication extends Application with StrictLogging {

  /**
    * Create an HTTP cache.
    */
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

  /**
    * Destroy the HTTP cache.
    */
  override def onTerminate(): Unit = {
    Option(HttpResponseCache.getInstalled).foreach(_.flush())
    super.onTerminate()
  }
}
