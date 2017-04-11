package devsync.discovery

import java.net.URL

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alex on 23/03/17
  **/
trait FlacManagerDiscovery {

  /**
    * Try and find a flac manager on the local network.
    * @return A future eventually containing the root url or an error.
    */
  def discover(implicit ec: ExecutionContext): Future[URL]
}
