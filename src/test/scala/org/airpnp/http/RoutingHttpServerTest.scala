package org.airpnp.http

import org.fest.assertions.Assertions.assertThat

import org.airpnp.http.HttpTestUtil._
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.MalformedURLException
import java.net.URLConnection

import org.airpnp.Util
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

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
  def shouldHandleSimpleGet(): Unit = {
    server.addRoute("/somewhere", new RouteHandler() {
      override def handleGET(request: Request, response: Response) = response.respond("success")
    })
    server.start

    val data = readAllAndClose(openUrl("/somewhere"))
    assertThat(data).isEqualTo("success")
  }

  @Test(expectedExceptions = Array(classOf[IOException]), expectedExceptionsMessageRegExp = ".*response code: 400.*")
  def shouldBePossibleToRespondWithOtherCodes(): Unit = {
    server.addRoute("/somewhere", new RouteHandler() {
      override def handleGET(request: Request, response: Response) = response.respond(400, "text/plain", "Bad request")
    })
    server.start
    openUrl("/somewhere")
  }

  @Test(expectedExceptions = Array(classOf[IOException]), expectedExceptionsMessageRegExp = ".*response code: 405.*")
  def shouldRespondWithMethodNotAllowedIfMethodNotHandled(): Unit = {
    server.addRoute("/somewhere", new RouteHandler() {})
    server.start
    openUrl("/somewhere")
  }

  @Test
  def shouldSupportPost(): Unit = {
    server.addRoute("/reverse", new RouteHandler() {
      override def handlePOST(request: Request, response: Response) {
        val data = readAllAndClose(request.getInputStream())
        response.respond(data.reverse)
      }
    })
    server.start

    val connection = HttpTestUtil.postDataToUrl("/reverse", port,
      "application/x-www-form-urlencoded;charset=utf-8",
      "hello world".getBytes("UTF8"))
    val read = readAllAndClose(connection.getInputStream)
    assertThat(read).isEqualTo("dlrow olleh")
  }

  @Test
  def shouldParseQueryString(): Unit = {
    server.addRoute("/somewhere", new RouteHandler() {
      override def handleGET(request: Request, response: Response) =
        response.respond(request.getArgument("foo").head)
    })
    server.start

    val data = readAllAndClose(openUrl("/somewhere?foo=bar"))
    assertThat(data).isEqualTo("bar")
  }

  @Test
  def shouldSupportUnnamedArgsInQueryString(): Unit = {
    server.addRoute("/somewhere", new RouteHandler() {
      override def handleGET(request: Request, response: Response) =
        response.respond(request.getArgument("").head)
    })
    server.start

    val data = readAllAndClose(openUrl("/somewhere?bar"))
    assertThat(data).isEqualTo("bar")
  }

  @Test(expectedExceptions = Array(classOf[FileNotFoundException]))
  def shouldReturnNotFoundWhenRouteIsntFound(): Unit = {
    server.start
    openUrl("/somewhere")
  }

  @Test
  def shouldSupportQueryingArgThatDoesntExist(): Unit = {
    server.addRoute("/somewhere", new RouteHandler() {
      override def handleGET(request: Request, response: Response) =
        response.respond("" + request.getArgument("foo").length)
    })
    server.start

    val data = readAllAndClose(openUrl("/somewhere?bar"))
    assertThat(data).isEqualTo("0")
  }

  @Test
  def shouldSupportQueryingHeaderThatDoesntExist(): Unit = {
    server.addRoute("/somewhere", new RouteHandler() {
      override def handleGET(request: Request, response: Response) =
        response.respond("" + request.getHeader("X-Foo").size)
    })
    server.start

    val data = readAllAndClose(openUrl("/somewhere?bar"))
    assertThat(data).isEqualTo("0")
  }

  private def openUrl(path: String) = HttpTestUtil.openUrlForReading(path, port)

  private def createServer(port: Int) = new RoutingHttpServer(new InetSocketAddress("localhost", port))
}
