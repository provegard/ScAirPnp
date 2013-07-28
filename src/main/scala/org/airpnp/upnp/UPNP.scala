package org.airpnp.upnp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;

import net.pms.network.UPNPHelper;

object UPNP {
    val IPV4_UPNP_HOST = "239.255.255.250"
    val UPNP_PORT = 1900

//    public static MulticastSocket getNewMulticastSocket() {
//        MulticastSocket multicastSocket = (MulticastSocket) IgnoreMemberAccessibility
//                .invokeStatic(UPNPHelper.class, "getNewMulticastSocket");
//        return multicastSocket;
//    }
//
//    public static InetAddress getUPNPAddress() {
//        InetAddress upnpAddress = (InetAddress) IgnoreMemberAccessibility
//                .invokeStatic(UPNPHelper.class, "getUPNPAddress");
//        return upnpAddress;
//    }
//
//    public static void cleanupMulticastSocket(MulticastSocket socket) {
//        // Clean up the multicast socket nicely
//        try {
//            socket.leaveGroup(getUPNPAddress());
//        } catch (IOException e) {
//        }
//
//        socket.disconnect();
//        socket.close();
//    }
}
