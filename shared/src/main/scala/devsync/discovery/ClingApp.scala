package devsync.discovery

import org.fourthline.cling.DefaultUpnpServiceConfiguration

import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by alex on 23/03/17
  **/
object ClingApp extends App {

  val flacManagerDiscovery: FlacManagerDiscovery = new ClingFlacManagerDiscovery(new DefaultUpnpServiceConfiguration())
  flacManagerDiscovery.discover.foreach(println)
}
