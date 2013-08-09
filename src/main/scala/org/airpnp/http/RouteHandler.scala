package org.airpnp.http;

import org.airpnp.http.Response._

abstract class RouteHandler {
  protected[http] def handleGET(request: Request, response: Response) = handleUnknown(request, response)
  protected[http] def handlePOST(request: Request, response: Response) = handleUnknown(request, response)
  protected[http] def handlePUT(request: Request, response: Response) = handleUnknown(request, response)

  protected[http] def handleUnknown(request: Request, response: Response) = {
    response.respond(withText("Unknown request method.").andStatusCode(405))
  }
}