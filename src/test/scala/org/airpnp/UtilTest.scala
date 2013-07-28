package org.airpnp

import org.testng.annotations.Test
import org.fest.assertions.Assertions.assertThat
import org.testng.annotations.DataProvider
import scala.collection.JavaConversions._
import java.io.ByteArrayInputStream

class UtilTest {
  class TestCreateDeviceId {
    @Test
    def shouldCreateIdFromUuid(): Unit = {
      val id = "uuid:f8ecf350-8691-4639-a735-c10ee6ad15c1"
      val did = Util.createDeviceId(id)
      assertThat(did.length).isEqualTo(17)
      assertThat(did.split(":").length).isEqualTo(6)
    }

    @Test
    def shouldCreateIdFromNonUuid(): Unit = {
      val id = "uuid:media_renderer_xyz"
      val did = Util.createDeviceId(id)
      assertThat(did.length).isEqualTo(17)
      assertThat(did.split(":").length).isEqualTo(6)
    }

    @Test
    def shouldNotCreateRandomId(): Unit = {
      val id = "uuid:f8ecf350-8691-4639-a735-c10ee6ad15c1"
      val did1 = Util.createDeviceId(id)
      val did2 = Util.createDeviceId(id)
      assertThat(did2).isEqualTo(did1)
    }
  }

  class TestSplitUsn {
    @Test
    def shouldSplitUsnWithUdnAndType(): Unit = {
      val usn = "uuid:x::type"
      val parts = Util.splitUsn(usn)

      assertThat(parts).isEqualTo(("uuid:x", "type"))
    }

    @Test
    def shouldSplitUsnWithoutType(): Unit = {
      val usn = "uuid:x"
      val parts = Util.splitUsn(usn)

      assertThat(parts).isEqualTo(("uuid:x", ""))
    }
  }

  class TestAreServiceTypesCompatible {
    @DataProvider
    def serviceTypes(): java.util.Iterator[Array[Object]] = {
      List(
        Array[Object]("urn:upnp-org:service:ConnectionManager:1",
          "urn:upnp-org:service:AVTransport:1", java.lang.Boolean.FALSE),
        // same type and version
        Array[Object]("urn:upnp-org:service:ConnectionManager:1",
          "urn:upnp-org:service:ConnectionManager:1", java.lang.Boolean.TRUE),
        // actual has lower version
        Array[Object]("urn:upnp-org:service:ConnectionManager:2",
          "urn:upnp-org:service:ConnectionManager:1", java.lang.Boolean.FALSE),
        // actual has higher version
        Array[Object]("urn:upnp-org:service:ConnectionManager:1",
          "urn:upnp-org:service:ConnectionManager:2", java.lang.Boolean.TRUE),
        // malformed actual
        Array[Object]("urn:upnp-org:service:ConnectionManager:1",
          "ConnectionManager", java.lang.Boolean.FALSE),
        // malformed required
        Array[Object]("ConnectionManager",
          "urn:upnp-org:service:ConnectionManager:1", java.lang.Boolean.FALSE),
        // same type, no version
        Array[Object]("upnp:rootdevice", "upnp:rootdevice", java.lang.Boolean.TRUE),
        // different types, no version
        Array[Object]("upnp:rootdevice", "upnp:smthelse", java.lang.Boolean.FALSE)).iterator
    }

    @Test(dataProvider = "serviceTypes")
    def shouldCheckServiceTypeCompatibility(required: String,
      actual: String, expectedOutcome: Boolean): Unit = {
      val compat = Util.areServiceTypesCompatible(required, actual)
      assertThat(compat).isEqualTo(expectedOutcome)
    }
  }

  class TestGetMaxAge {

    private def createHeaders(key: String, value: String): Map[String, String] = {
      Map((key, value))
    }

    @DataProvider
    def maxAges(): java.util.Iterator[Array[Object]] = {
      List(
        // proper header
        Array[Object]("CACHE-CONTROL", "max-age=10", 10: java.lang.Integer),
        // spaces around eq
        Array[Object]("CACHE-CONTROL", "max-age = 10", 10: java.lang.Integer),
        // missing max-age
        Array[Object]("CACHE-CONTROL", "xyz=10", -1: java.lang.Integer),
        // missing CACHE-CONTROL
        Array[Object]("a", "b", -1: java.lang.Integer),
        // malformed max-age
        Array[Object]("CACHE-CONTROL", "max-age=", -1: java.lang.Integer),
        // additional cache directive
        Array[Object]("CACHE-CONTROL",
          "max-age=10, must-revalidate", 10: java.lang.Integer)).iterator
    }

    @Test(dataProvider = "maxAges")
    def shouldGetMaxAge(key: String, value: String, expected: Int): Unit = {
      val headers = createHeaders(key, value)
      val maxAge = Util.getMaxAge(headers)

      assertThat(maxAge).isEqualTo(expected)
    }
  }

  class TestReadAllBytes {
    @Test def shouldReadAllBytesFromStream(): Unit = {
      val bytes = "testing".getBytes
      var is = new ByteArrayInputStream(bytes)
      var result = Util.readAllBytes(is)
      assertThat(result).isEqualTo(bytes)
    }
  }
}