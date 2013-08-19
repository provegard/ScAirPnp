package org.airpnp.upnp

import org.airpnp.http.Response._
import java.io.StringWriter
import java.net.InetSocketAddress
import scala.xml.MinimizeMode
import scala.xml.XML
import org.airpnp.Util
import org.airpnp.http.Request
import org.airpnp.http.Response
import org.airpnp.http.RouteHandler
import org.airpnp.http.RoutingHttpServer
import org.fest.assertions.Assertions.assertThat
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import org.airpnp.TraceLogging

class SoapClientTest extends TraceLogging {
  var port: Int = 0
  var server: RoutingHttpServer = null

  @BeforeClass def startServer() = {
    port = Util.findPort()
    server = new SoapClientTest.SoapServer(new InetSocketAddress("localhost", port))
    server.start
  }

  @AfterClass def stopServer() = {
    server.stop(0)
  }

  private def getUrl(path: String) = "http://localhost:" + port + path

  @Test def shouldHandleRegularPost() {
    val msg = new SoapMessage("someType", "Baz")
    val client = new SoapClient()
    val result = client.sendMessage(getUrl("/post"), msg)
    assertThat(result.getName).isEqualTo("BazReply")
  }

  @Test def shouldFallbackToMPost() {
    val msg = new SoapMessage("someType", "Baz")
    val client = new SoapClient()
    val result = client.sendMessage(getUrl("/mpost"), msg)
    assertThat(result.getName).isEqualTo("BazReply")
  }

  @Test(expectedExceptions = Array(classOf[SoapError]))
  def shouldParseAndThrowSoapError() {
    val msg = new SoapMessage("someType", "Baz")
    val client = new SoapClient()
    client.sendMessage(getUrl("/err"), msg)
  }

  @Test def shouldSupportChunkedTransferEncoding() {
    val msg = new SoapMessage("someType", "Baz")
    val client = new SoapClient()
    val result = client.sendMessage(getUrl("/chunked"), msg)
    assertThat(result.getName).isEqualTo("BazReply")
  }
}

object SoapClientTest {
  private class SoapServer(private val addr: InetSocketAddress) extends RoutingHttpServer(addr) {
    addRoute("/post", new PostHandler())
    addRoute("/chunked", new ChunkedHandler())
    addRoute("/mpost", new MPostHandler())
    addRoute("/err", new ErrHandler())
  }

  private class ChunkedHandler extends RouteHandler {
    override def handlePOST(request: Request, response: Response) {
      val msg = SoapMessage.parse(request.getInputStream)
      val reply = new SoapMessage(msg.getServiceType, msg.getName + "Reply")
      response.respond(withText(reply.toString).andContentType("text/xml").andIsChunked())
    }
  }

  private class PostHandler extends RouteHandler {
    override def handlePOST(request: Request, response: Response) {
      val msg = SoapMessage.parse(request.getInputStream)
      request.getHeader("SOAPACTION").headOption match {
        case Some(x) if x == msg.getSoapAction => {
          val reply = new SoapMessage(msg.getServiceType, msg.getName + "Reply")
          response.respond(withText(reply.toString).andContentType("text/xml"))
        }
        case _ => response.respond(withText("Incorrect SOAPACTION header").andStatusCode(400))
      }
    }
  }

  private class MPostHandler extends RouteHandler {
    override def handlePOST(request: Request, response: Response) {
      response.respond(withText("Use M-POST").andStatusCode(405))
    }
    override def handleUnknown(request: Request, response: Response) {
      val msg = SoapMessage.parse(request.getInputStream)
      request.getMethod match {
        case "M-POST" => {
          request.getHeader("01-SOAPACTION").headOption match {
            case Some(x) if x == msg.getSoapAction => request.getHeader("MAN").headOption match {
              case Some(y) if y == "\"http://schemas.xmlsoap.org/soap/envelope/\"; ns=01" => {
                val reply = new SoapMessage(msg.getServiceType, msg.getName + "Reply")
                response.respond(withText(reply.toString).andContentType("text/xml"))
              }
              case _ => response.respond(withText("Incorrect MAN header").andStatusCode(400))
            }
            case _ => response.respond(withText("Incorrect 01-SOAPACTION header").andStatusCode(400))
          }
        }
        case _ => super.handleUnknown(request, response)
      }
    }
  }

  private class ErrHandler extends RouteHandler {
    override def handlePOST(request: Request, response: Response) {
      val str = createSoapError(123, "Some error").xml
      response.respond(withUtf8Text(str).andStatusCode(500).andContentType("text/xml"))
    }
  }
}
