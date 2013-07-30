package org.airpnp.upnp

import org.fest.assertions.Assertions.assertThat
import org.airpnp.upnp.UPNP.{ parseDuration, toDuration }
import org.testng.annotations.Test

class UPNPTest {
  class ToDurationTest {
    @Test def test_seconds_only_without_fraction() {
      val hms = toDuration(5)
      assertThat(hms).isEqualTo("0:00:05.000")
    }
    @Test def test_seconds_with_fraction() {
      val hms = toDuration(5.5)
      assertThat(hms).isEqualTo("0:00:05.500")
    }
    @Test def test_minute_conversion() {
      val hms = toDuration(65)
      assertThat(hms).isEqualTo("0:01:05.000")
    }
    @Test def test_hour_conversion() {
      val hms = toDuration(3600)
      assertThat(hms).isEqualTo("1:00:00.000")
    }
    @Test def test_negative_seconds_conversion() {
      val hms = toDuration(-3661.0)
      assertThat(hms).isEqualTo("-1:01:01.000")
    }
  }

  class ParseDurationTest {
    @Test def test_hour_conversion() {
      val sec = parseDuration("1:00:00")
      assertThat(sec).isEqualTo(3600.0)
    }
    @Test def test_minute_conversion() {
      val sec = parseDuration("0:10:00")
      assertThat(sec).isEqualTo(600.0)
    }
    @Test def test_second_conversion() {
      val sec = parseDuration("0:00:05")
      assertThat(sec).isEqualTo(5.0)
    }
    @Test def test_with_fraction() {
      val sec = parseDuration("0:00:05.5")
      assertThat(sec).isEqualTo(5.5)
    }
    @Test def test_with_div_fraction() {
      val sec = parseDuration("0:00:05.1/2")
      assertThat(sec).isEqualTo(5.5)
    }
    @Test def test_with_plus_sign() {
      val sec = parseDuration("+1:01:01")
      assertThat(sec).isEqualTo(3661.0)
    }
    @Test def test_with_minus_sign() {
      val sec = parseDuration("-1:01:01")
      assertThat(sec).isEqualTo(-3661.0)
    }

    @Test(expectedExceptions = Array(classOf[IllegalArgumentException]))
    def test_without_hour_part() {
      parseDuration("00:00")
    }

    @Test(expectedExceptions = Array(classOf[IllegalArgumentException]))
    def test_with_empty_hour_part() {
      parseDuration(":00:00")
    }

    @Test(expectedExceptions = Array(classOf[IllegalArgumentException]))
    def test_with_too_short_minute_part() {
      parseDuration("0:0:00")
    }

    @Test(expectedExceptions = Array(classOf[IllegalArgumentException]))
    def test_with_too_short_second_part() {
      parseDuration("0:00:0")
    }

    @Test(expectedExceptions = Array(classOf[IllegalArgumentException]))
    def test_with_negative_minute() {
      parseDuration("0:-1:00")
    }

    @Test(expectedExceptions = Array(classOf[IllegalArgumentException]))
    def test_with_too_large_minute() {
      parseDuration("0:60:00")
    }

    @Test(expectedExceptions = Array(classOf[IllegalArgumentException]))
    def test_with_negative_second() {
      parseDuration("0:00:-1")
    }

    @Test(expectedExceptions = Array(classOf[IllegalArgumentException]))
    def test_with_too_large_second() {
      parseDuration("0:00:60")
    }

    @Test(expectedExceptions = Array(classOf[IllegalArgumentException]))
    def test_with_div_fraction_unsatisfied_inequality() {
      parseDuration("0:00:05.5/5")
    }
  }
}