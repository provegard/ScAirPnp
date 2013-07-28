package org.airpnp.http;

abstract class RouteHandler {
  protected[http] def handleGET(request: Request, response: Response) = handleUnknown(request, response)
  protected[http] def handlePOST(request: Request, response: Response) = handleUnknown(request, response)
  protected[http] def handlePUT(request: Request, response: Response) = handleUnknown(request, response)

  protected[http] def handleUnknown(request: Request, response: Response) = {
    val data = "Unknown request method."
    response.respond(405, "text/plain", data)
  }
}