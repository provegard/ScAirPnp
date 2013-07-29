package org.airpnp.airplay.protocol

import org.airpnp.airplay.DurationAndPosition
import org.airpnp.plist.True
import org.airpnp.plist.False
import org.airpnp.plist.Dict
import org.airpnp.plist.KeyValue
import org.airpnp.plist.Real
import org.airpnp.plist.Array
import org.airpnp.plist.PropertyList

class PlaybackInfo(private val scrub: DurationAndPosition, private val playing: Boolean) {

  private val plist = {
    val playbackBufferEmpty = scrub.duration + scrub.position == 0
    val readyToPlay = !playbackBufferEmpty

    val ltrPairs = Seq(
      new KeyValue("duration", new Real(scrub.duration)),
      new KeyValue("start", new Real(0.0)))
    val loadedTimeRanges = new Array(new Dict(ltrPairs: _*))

    val strPairs = Seq(
      new KeyValue("duration", new Real(scrub.duration)),
      new KeyValue("start", new Real(0.0)))
    val seekableTimeRanges = new Array(new Dict(strPairs: _*))

    val pairs = Seq(
      new KeyValue("duration", new Real(scrub.duration)),
      new KeyValue("position", new Real(scrub.position)),
      new KeyValue("rate", new Real(if (playing) 1.0 else 0.0)),
      new KeyValue("playbackBufferEmpty", trueOrFalse(playbackBufferEmpty)),
      new KeyValue("playbackBufferFull", False.INSTANCE),
      new KeyValue("playbackLikelyToKeepUp", True.INSTANCE),
      new KeyValue("readyToPlay", trueOrFalse(readyToPlay)),
      new KeyValue("loadedTimeRanges", loadedTimeRanges),
      new KeyValue("seekableTimeRanges", seekableTimeRanges))

    new PropertyList(new Dict(pairs: _*))
  }

  def get() = plist

  private def trueOrFalse(b: Boolean) = if (b) True.INSTANCE else False.INSTANCE
}