package uk.co.unclealex.devsync

import android.os.AsyncTask

import scala.concurrent.ExecutionContext

/**
  * Created by alex on 25/03/17
  * Holder for an implicit execution context suitable for android.
  **/
object Async {

  implicit val androidExecutionContext: ExecutionContext = ExecutionContext.fromExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

}
