package org.airpnp.upnp

object MSearchRequest {
  private val CRLF = "\r\n"
}

class MSearchRequest(private val st: String, private val mx: Int) {
  override def toString() = {
    val sb = new StringBuilder()

    sb.append("M-SEARCH * HTTP/1.1").append(MSearchRequest.CRLF)
    sb.append("HOST: ").append(UPNP.IPV4_UPNP_HOST).append(":").append(UPNP.UPNP_PORT).append(MSearchRequest.CRLF)
    sb.append("MAN: \"ssdp:discover\"").append(MSearchRequest.CRLF)
    sb.append("MX: ").append(mx).append(MSearchRequest.CRLF)
    sb.append("ST: ").append(st).append(MSearchRequest.CRLF)
    sb.append(MSearchRequest.CRLF)
    sb.toString()
  }
}
