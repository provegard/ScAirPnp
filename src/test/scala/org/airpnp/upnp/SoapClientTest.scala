package org.airpnp.upnp

import org.fest.assertions.Assertions.assertThat
import scala.concurrent.ExecutionContext.Implicits.global
import org.airpnp.http.RoutingHttpServer
import java.net.InetSocketAddress
import org.airpnp.http.RouteHandler
import org.airpnp.http.Request
import org.airpnp.http.Response
import scala.xml.XML
import java.io.StringWriter
import scala.xml.MinimizeMode
import org.testng.annotations.Test
import org.testng.annotations.BeforeClass
import org.airpnp.Util
import java.net.InetAddress
import org.testng.annotations.AfterClass
import scala.util.Success
import scala.util.Failure
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration.MILLISECONDS
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import scala.concurrent.Future
import java.util.concurrent.TimeoutException

class SoapClientTest {
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
    val f = client.sendMessage(getUrl("/post"), msg)
    val result = Await.result(f, Duration.create(500, MILLISECONDS))
    assertThat(result.getName).isEqualTo("BazReply")
  }

  @Test def shouldFallbackToMPost() {
    val msg = new SoapMessage("someType", "Baz")
    val client = new SoapClient()
    val f = client.sendMessage(getUrl("/mpost"), msg)
    val result = Await.result(f, Duration(500, MILLISECONDS))
    assertThat(result.getName).isEqualTo("BazReply")
  }

  @Test(expectedExceptions = Array(classOf[SoapError]))
  def shouldParseAndThrowSoapError() {
    val msg = new SoapMessage("someType", "Baz")
    val client = new SoapClient()
    val f = client.sendMessage(getUrl("/err"), msg)
    Await.result(f, Duration.create(500, MILLISECONDS))
  }

  @Test def shouldSupportChunkedTransferEncoding() {
    val msg = new SoapMessage("someType", "Baz")
    val client = new SoapClient()
    val f = client.sendMessage(getUrl("/chunked"), msg)
    val result = Await.result(f, Duration.create(500, MILLISECONDS))
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
      response.respond(reply.toString, contentType = "text/xml", chunked = true)
    }
  }

  private class PostHandler extends RouteHandler {
    override def handlePOST(request: Request, response: Response) {
      val msg = SoapMessage.parse(request.getInputStream)
      request.getHeader("SOAPACTION").headOption match {
        case Some(x) if x == msg.getHeader => {
          val reply = new SoapMessage(msg.getServiceType, msg.getName + "Reply")
          response.respond(reply.toString, contentType = "text/xml")
        }
        case _ => response.respond("Incorrect SOAPACTION header", 400)
      }
    }
  }

  private class MPostHandler extends RouteHandler {
    override def handlePOST(request: Request, response: Response) {
      response.respond("Use M-POST", 405)
    }
    override def handleUnknown(request: Request, response: Response) {
      val msg = SoapMessage.parse(request.getInputStream)
      request.getMethod match {
        case "M-POST" => {
          request.getHeader("01-SOAPACTION").headOption match {
            case Some(x) if x == msg.getHeader => request.getHeader("MAN").headOption match {
              case Some(y) if y == "\"http://schemas.xmlsoap.org/soap/envelope/\"; ns=01" => {
                val reply = new SoapMessage(msg.getServiceType, msg.getName + "Reply")
                response.respond(reply.toString, contentType = "text/xml")
              }
              case _ => response.respond("Incorrect MAN header", 400)
            }
            case _ => response.respond("Incorrect 01-SOAPACTION header", 400)
          }
        }
        case _ => super.handleUnknown(request, response)
      }
    }
  }

  private class ErrHandler extends RouteHandler {
    override def handlePOST(request: Request, response: Response) {
      val doc = <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                  <s:Body>
                    <s:Fault>
                      <faultcode>s:Client</faultcode>
                      <faultstring>UPnPError</faultstring>
                      <detail>
                        <UPnPError xmlns="urn:schemas-upnp-org:control-1-0">
                          <errorCode>123</errorCode>
                          <errorDescription>Some error</errorDescription>
                        </UPnPError>
                      </detail>
                    </s:Fault>
                  </s:Body>
                </s:Envelope>
      val sw = new StringWriter
      val str = XML.write(sw, doc, "UTF-8", true, null, MinimizeMode.Default)
      response.respond(sw.toString, 500, "text/xml")
    }
  }
}
