package uk.co.unclealex.devsync

import devsync.json.{CirceCodec, JsonCodec}

/**
  * Created by alex on 26/03/17
  * An object that holds singleton services
  **/
object Services {

  val jsonCodec: JsonCodec = new CirceCodec
}
