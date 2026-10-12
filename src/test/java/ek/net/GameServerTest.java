package ek.net;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

class GameServerTest {

    @Test
    void localAddresses_areIpv4AndNeverLoopback() throws SocketException, UnknownHostException {
        for (String address : GameServer.localAddresses()) {
            assertTrue(address.matches("\\d{1,3}(\\.\\d{1,3}){3}"), address);
            assertFalse(InetAddress.getByName(address).isLoopbackAddress(), address);
        }
    }
}
