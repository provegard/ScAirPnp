package org.airpnp.upnp

import org.fest.assertions.Assertions.assertThat
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

class InitializedServiceTest {
  private var service: Service = null

  @BeforeClass
  def createService() {
    val device = buildInitializedMediaRenderer("http://www.base.com")
    service = device.getServiceById("urn:upnp-org:serviceId:AVTransport").get
  }
  
  @Test def shouldExposeActions() {
    assertThat(service.getActions.size).isEqualTo(15)
  }

  @Test
  def shouldExposeServiceType() {
    assertThat(service.getServiceType).isEqualTo("urn:schemas-upnp-org:service:AVTransport:1")
  }

  @Test
  def shouldExposeServiceId() {
    assertThat(service.getServiceId).isEqualTo("urn:upnp-org:serviceId:AVTransport")
  }

  @Test
  def shouldExposeSCPDURLWithBaseUrl() {
    assertThat(service.getSCPDURL).isEqualTo("http://www.base.com/avtransport.xml")
  }

  @Test def shouldExposeControlURLWithBaseUrl() {
    assertThat(service.getControlURL).isEqualTo("http://www.base.com/MediaRenderer_AVTransport/control")
  }

  //  @DataProvider
  //  def urlAttributes(): java.util.Iterator[Array[Object]] = {
  //    return List(Array[Object]("SCPDURL"),
  //      Array[Object]("controlURL"), Array[Object]("eventSubURL"))
  //      .iterator
  //  }
  //  
  //      @Test(dataProvider = "urlAttributes")
  //      def shouldResolveUrlsAgainstBaseUrl(attrName: String) {
  //          // URLs are resolved using the base URL
  //          assertThat(service.attr(attrName)).startsWith(
  //                  "http://www.base.com/MediaRenderer_AVTransport/")
  //      }

  @Test
  def shouldHaveActionsAfterInitialization() {
    val a = service.action("GetCurrentTransportActions")
    assertThat(a.isDefined).isTrue
  }
  // def test_service_action_calls_soap_sender(self):
  // self.service.GetCurrentTransportActions(InstanceID="0")
  // self.assertTrue(self.soap_sender.called)
  //
  // def test_service_action_throws_for_missing_argument(self):
  // self.assertRaises(KeyError, self.service.GetCurrentTransportActions)
  //
  // def test_service_action_soap_sender_args(self):
  // self.service.GetCurrentTransportActions(InstanceID="0")
  // args, _ = self.soap_sender.call_args
  //
  // self.assertEqual(args[0], self.device)
  // self.assertEqual(args[1], self.service.controlURL)
  // self.assertEqual(args[2].__class__, upnp.SoapMessage)
  //
  // def test_service_action_soap_message_contains_in_args(self):
  // self.service.GetCurrentTransportActions(InstanceID="0")
  // args, _ = self.soap_sender.call_args
  //
  // msg = args[2]
  // self.assertEqual(msg.get_arg("InstanceID"), "0")
  //
  // def test_service_action_soap_response_is_returned_as_dict(self):
  // response = upnp.SoapMessage(self.service.serviceType,
  // "GetCurrentTransportActionsResponse")
  // response.set_arg("Actions", "test")
  // self.soap_sender.return_value = response
  //
  // actual = self.service.GetCurrentTransportActions(InstanceID="0")
  // self.assertEqual(actual, {"Actions": "test"})
  //
  // def test_service_action_soap_error_is_decoded_to_exception(self):
  // self.soap_sender.return_value = upnp.SoapError()
  //
  // self.assertRaises(CommandError, self.service.GetCurrentTransportActions,
  // InstanceID="0")
  //
  // def test_service_action_soap_sender_async_args_without_async(self):
  // self.service.GetCurrentTransportActions(InstanceID="0")
  // _, kwargs = self.soap_sender.call_args
  //
  // self.assertEqual(kwargs, {'async': False, 'deferred': None})
  //
  // def test_service_action_soap_sender_async_args_with_async(self):
  // self.service.GetCurrentTransportActions(InstanceID="0", async=True)
  // _, kwargs = self.soap_sender.call_args
  //
  // self.assertEqual(kwargs, {'async': True, 'deferred': None})
  //
  // def
  // test_service_action_soap_sender_async_args_with_async_and_deferred(self):
  // d = defer.Deferred()
  // self.service.GetCurrentTransportActions(InstanceID="0", async=True,
  // deferred=d)
  // _, kwargs = self.soap_sender.call_args
  //
  // self.assertEqual(kwargs, {'async': True, 'deferred': d})
  //
  // def
  // test_service_action_soap_sender_async_args_deferred_requires_async(self):
  // self.service.GetCurrentTransportActions(InstanceID="0", async=False,
  // deferred=defer.Deferred())
  // _, kwargs = self.soap_sender.call_args
  //
  // self.assertEqual(kwargs, {'async': False, 'deferred': None})
  //
  // def test_service_action_async_returns_deferred(self):
  // self.soap_sender.return_value = defer.Deferred()
  // actual = self.service.GetCurrentTransportActions(InstanceID="0",
  // async=True)
  //
  // self.assertEqual(actual.__class__, defer.Deferred)
  //
  // def test_service_action_async_parser_soap_message(self):
  // response = upnp.SoapMessage(self.service.serviceType,
  // "GetCurrentTransportActionsResponse")
  // response.set_arg("Actions", "test")
  // ret = defer.Deferred()
  // self.soap_sender.return_value = ret
  //
  // actual = self.service.GetCurrentTransportActions(InstanceID="0",
  // async=True)
  //
  // ret.callback(response)
  // self.assertEqual(actual.result, {"Actions": "test"})

}