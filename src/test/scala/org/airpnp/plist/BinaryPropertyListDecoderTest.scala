package org.airpnp.plist

import scala.collection.JavaConversions._
import org.fest.assertions.Assertions.assertThat
import org.testng.annotations.Test
import java.io.ByteArrayInputStream
import org.testng.annotations.DataProvider
import java.util.GregorianCalendar
import java.util.TimeZone
import java.util.Calendar

class BinaryPropertyListDecoderTest {
  @Test(expectedExceptions = scala.Array(classOf[PropertyListFormatException]))
  def shouldThrowOnInvalidSignature() {
    val is = new ByteArrayInputStream("hello world".getBytes())
    new BinaryPropertyListDecoder(is).decode()
  }

  @Test
  def shouldDecodeSetPropertyPlist() {
    val data = "bplist00\u00d1\u0001\u0002Uvalue\u00d4\u0003\u0004\u0005\u0006\u0007\u0007\u0007\u0007YtimescaleUvalueUepochUflags\u0010\u0000\u0008\u000b\u0011\u001a$*06\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0008\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u00008"
    val is = new ByteArrayInputStream(data.getBytes())
    val plist = new BinaryPropertyListDecoder(is).decode()
    val rootValue = plist.root.asInstanceOf[Dict].getValue
    assertThat(rootValue.contains("value")).isTrue()
  }

  @DataProvider
  def binaryData(): java.util.Iterator[scala.Array[Object]] = {

    val cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"))
    cal.set(2011, 6, 23, 15, 0, 0)
    cal.set(Calendar.MILLISECOND, 0)

    List(scala.Array[Object]("true.bin", true: java.lang.Boolean),
      scala.Array[Object]("false.bin", false: java.lang.Boolean),
      scala.Array[Object]("int8.bin", 255l: java.lang.Long),
      scala.Array[Object]("int16.bin", 65535l: java.lang.Long),
      scala.Array[Object]("int32.bin", 4294967295l: java.lang.Long),
      scala.Array[Object]("int63.bin", 9223372036854775807l: java.lang.Long),
      scala.Array[Object]("intneg1.bin", -1l: java.lang.Long),
      scala.Array[Object]("float32.bin", 1.0e10d: java.lang.Double),
      scala.Array[Object]("float64.bin", 1.0e100d: java.lang.Double),
      scala.Array[Object]("date.bin", cal.getTime()),
      scala.Array[Object]("string.bin", "hello"),
      scala.Array[Object]("longstring.bin", "hello there, world!"),
      scala.Array[Object]("unicode.bin", "non-ascii \u00e5\u00e4\u00f6")).iterator
  }

  @Test(dataProvider = "binaryData")
  def shouldDecodeBinary(file: java.lang.String, expected: AnyRef) {
    val is = getClass().getResourceAsStream(file)
    val plist = new BinaryPropertyListDecoder(is).decode()
    assertThat(plist.root.getValue).isEqualTo(expected)
  }

  
  @Test
  def shouldDecodeBinaryData() {
    val is = getClass().getResourceAsStream("data.bin")
    val plist = new BinaryPropertyListDecoder(is).decode()
    val arr = plist.root.asInstanceOf[Data].getValue.toArray
    assertThat(arr).isEqualTo("pleasure.".getBytes())
  }

  @Test
  def shouldDecodeBinaryArray() {
    val is = getClass().getResourceAsStream("array.bin")
    val plist = new BinaryPropertyListDecoder(is).decode()
    val arr = plist.root.asInstanceOf[Array].getValue

    val result = arr.map(_.getValue).toArray

    assertThat(result).isEqualTo(scala.Array(1l, 2l, 3l))
  }

  @Test
  def shouldDecodeBinarySet() {
    val is = getClass().getResourceAsStream("set.bin")
    val plist = new BinaryPropertyListDecoder(is).decode()
    val arr = plist.root.asInstanceOf[Array].getValue

    val result = arr.map(_.getValue).toArray

    assertThat(result).isEqualTo(scala.Array(1l, 2l, 3l))
  }

  @Test
  def shouldDecodeBinaryDict() {
    val expected = Map(("Content-Location", "http://v9.lscache4.googlevideo.com/videoplayback?id=3eac4bbd43c31217&itag=18&uaopt=no-save&el=related&client=ytapi-apple-iphone&devKey=AdW2Kh1KB1Jkhso4mAT4nHgO88HsQjpE1a8d1GxQnGDm&app=youtube_gdata&ip=0.0.0.0&ipbits=0&expire=1313568456&sparams=id,itag,uaopt,ip,ipbits,expire&signature=625BB56F7EF7AB65ED34C5D2B09539AA90B4F6B4.4227E5A20028E6F86621FAB7F15827A79E31C9EE&key=yta1"),
      ("Start-Position", 0.0005364880198612809d))

    val is = getClass().getResourceAsStream("airplay.bin")
    val plist = new BinaryPropertyListDecoder(is).decode()

    val map = plist.root.asInstanceOf[Dict].getValue

    assertThat(map).isEqualTo(expected)
  }
}