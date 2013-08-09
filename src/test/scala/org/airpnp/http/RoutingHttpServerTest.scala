package org.airpnp.http

import java.net.InetSocketAddress
import org.airpnp.Util
import org.airpnp.http.HttpTestUtil._
import org.airpnp.http.Response.{withText, withUtf8Text}
import org.fest.assertions.Assertions.assertThat
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.IOException
import java.io.FileNotFoundException
import java.nio.charset.Charset

class RoutingHttpServerTest {

  private var server: RoutingHttpServer = null
  private var port: Int = 0

  @BeforeMethod
  def setup() = {
    port = Util.findPort
    server = createServer(port)
  }

  @AfterMethod(alwaysRun = true)
  def cleanup() = server.stop(0)

  @Test
  def shouldHandleSimpleGet() {
    server.addRoute("/somewhere", new RouteHandler() {
      override def handleGET(request: Request, response: Response) = response.respond(withText("success"))
    })
    server.start

    val data = readTextAndClose(openUrl("/somewhere"))
    assertThat(data).isEqualTo("success")
  }

  @Test(expectedExceptions = Array(classOf[IOException]), expectedExceptionsMessageRegExp = ".*response code: 400.*")
  def shouldBePossibleToRespondWithOtherCodes() {
    server.addRoute("/somewhere", new RouteHandler() {
      override def handleGET(request: Request, response: Response) = response.respond(withText("Bad request").andStatusCode(400))
    })
    server.start
    openUrl("/somewhere").getInputStream()
  }

  @Test(expectedExceptions = Array(classOf[IOException]), expectedExceptionsMessageRegExp = ".*response code: 405.*")
  def shouldRespondWithMethodNotAllowedIfMethodNotHandled() {
    server.addRoute("/somewhere", new RouteHandler() {})
    server.start
	openUrl("/somewhere").getInputStream()
  }

  @Test
  def shouldSupportPost() {
    server.addRoute("/reverse", new RouteHandler() {
      override def handlePOST(request: Request, response: Response) {
        val data = readTextAndClose(request.getInputStream(), request.getHeader("Content-Type").head)
        response.respond(withText(data.reverse))
      }
    })
    server.start

    val connection = HttpTestUtil.postDataToUrl("/reverse", port,
      "application/x-www-form-urlencoded;charset=utf-8",
      "hello world".getBytes("UTF8"))
    val read = readTextAndClose(connection)
    assertThat(read).isEqualTo("dlrow olleh")
  }

  @Test
  def shouldParseQueryString() {
    server.addRoute("/somewhere", new RouteHandler() {
      override def handleGET(request: Request, response: Response) =
        response.respond(withText(request.getArgument("foo").head))
    })
    server.start

    val data = readTextAndClose(openUrl("/somewhere?foo=bar"))
    assertThat(data).isEqualTo("bar")
  }

  @Test
  def shouldSupportUnnamedArgsInQueryString() {
    server.addRoute("/somewhere", new RouteHandler() {
      override def handleGET(request: Request, response: Response) =
        response.respond(withText(request.getArgument("").head))
    })
    server.start

    val data = readTextAndClose(openUrl("/somewhere?bar"))
    assertThat(data).isEqualTo("bar")
  }

  @Test(expectedExceptions = Array(classOf[FileNotFoundException]))
  def shouldReturnNotFoundWhenRouteIsntFound() {
    server.start
    openUrl("/somewhere").getInputStream()
  }

  @Test
  def shouldSupportQueryingArgThatDoesntExist() {
    server.addRoute("/somewhere", new RouteHandler() {
      override def handleGET(request: Request, response: Response) =
        response.respond(withText("" + request.getArgument("foo").length))
    })
    server.start

    val data = readTextAndClose(openUrl("/somewhere?bar"))
    assertThat(data).isEqualTo("0")
  }

  @Test
  def shouldSupportQueryingHeaderThatDoesntExist() {
    server.addRoute("/somewhere", new RouteHandler() {
      override def handleGET(request: Request, response: Response) =
        response.respond(withText("" + request.getHeader("X-Foo").size))
    })
    server.start

    val data = readTextAndClose(openUrl("/somewhere?bar"))
    assertThat(data).isEqualTo("0")
  }
  
  @Test def shouldHandleUtf8Properly() {
    server.addRoute("/somewhere", new RouteHandler() {
      override def handleGET(request: Request, response: Response) =
        response.respond(withUtf8Text("Test\u1234"))
    })
    server.start

    val data = readTextAndClose(openUrl("/somewhere?bar"))
    assertThat(data).isEqualTo("Test\u1234")
  }

  private def openUrl(path: String) = HttpTestUtil.openUrlConnection(path, port)
  private def createServer(port: Int) = new RoutingHttpServer(new InetSocketAddress("localhost", port))
}
