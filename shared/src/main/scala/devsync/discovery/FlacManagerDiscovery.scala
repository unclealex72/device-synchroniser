package devsync.discovery

import java.net.URL

import cats.data.EitherT

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alex on 23/03/17
  **/
trait FlacManagerDiscovery {

  /**
    * Try and find a flac manager on the local network.
    * @param dev True if looking for a development server, false otherwise.
    * @param ec An execution context used for running future events.
    * @return A future eventually containing the root url or an error.
    */
  def discover(dev: Boolean)(implicit ec: ExecutionContext): EitherT[Future, Exception, URL]
}
