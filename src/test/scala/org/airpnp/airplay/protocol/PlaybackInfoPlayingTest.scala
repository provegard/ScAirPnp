package org.airpnp.airplay.protocol;

import scala.collection.JavaConversions._
import java.util.ArrayList
import java.util.Iterator
import java.util.List
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.airpnp.airplay.DurationAndPosition
import org.airpnp.plist.PropertyList

class PlaybackInfoPlayingTest extends PropertyListTester {

  protected var plist: PropertyList = null

  @BeforeClass
  def setup() = {
    plist = new PlaybackInfo(new DurationAndPosition(20, 2), true).get
  }

  @DataProvider
  def dictOrder(): java.util.Iterator[Array[Object]] = {
    Seq(
      Array[Object]("/", Array("duration", "position", "rate", "playbackBufferEmpty",
        "playbackBufferFull", "playbackLikelyToKeepUp", "readyToPlay", "loadedTimeRanges", "seekableTimeRanges"))).iterator
  }

  @DataProvider
  def expectedData(): java.util.Iterator[Array[Object]] = {
    Seq(
      Array[Object]("/duration", 20d: java.lang.Double),
      Array[Object]("/position", 2d: java.lang.Double),
      Array[Object]("/rate", 1d: java.lang.Double),
      Array[Object]("/playbackBufferEmpty", false: java.lang.Boolean),
      Array[Object]("/playbackBufferFull", false: java.lang.Boolean),
      Array[Object]("/readyToPlay", true: java.lang.Boolean),
      Array[Object]("/loadedTimeRanges/0/duration", 20d: java.lang.Double),
      Array[Object]("/seekableTimeRanges/0/duration", 20d: java.lang.Double)).iterator
  }

}
