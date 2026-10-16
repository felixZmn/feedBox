package de._0x2b.http;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UrlSafetyTest {

    @Test
    void requireSafeOutbound_allowsPublicHttps() {
        assertDoesNotThrow(() -> UrlSafety.requireSafeOutbound(URI.create("https://example.com/feed.xml")));
    }

    @Test
    void requireSafeOutbound_allowsDefaultPortsOnly() {
        assertDoesNotThrow(() -> UrlSafety.requireSafeOutbound(URI.create("https://example.com:443/x")));
        assertDoesNotThrow(() -> UrlSafety.requireSafeOutbound(URI.create("http://example.com:80/x")));
        assertThrows(UnsafeUrlException.class,
                () -> UrlSafety.requireSafeOutbound(URI.create("https://example.com:8443/x")));
        assertThrows(UnsafeUrlException.class,
                () -> UrlSafety.requireSafeOutbound(URI.create("http://example.com:8080/x")));
    }

    @Test
    void requireSafeOutbound_rejectsNonHttpSchemes() {
        assertThrows(UnsafeUrlException.class,
                () -> UrlSafety.requireSafeOutbound(URI.create("file:///etc/passwd")));
        assertThrows(UnsafeUrlException.class,
                () -> UrlSafety.requireSafeOutbound(URI.create("ftp://example.com/a")));
    }

    @Test
    void requireSafeOutbound_rejectsRelative() {
        assertThrows(UnsafeUrlException.class,
                () -> UrlSafety.requireSafeOutbound(URI.create("/relative")));
    }

    @Test
    void requireSafeOutbound_rejectsLoopbackLiteral() {
        assertThrows(UnsafeUrlException.class,
                () -> UrlSafety.requireSafeOutbound(URI.create("http://127.0.0.1/")));
        assertThrows(UnsafeUrlException.class,
                () -> UrlSafety.requireSafeOutbound(URI.create("http://[::1]/")));
    }

    @Test
    void requireSafeOutbound_rejectsLinkLocalMetadata() {
        assertThrows(UnsafeUrlException.class,
                () -> UrlSafety.requireSafeOutbound(URI.create("http://169.254.169.254/latest/meta-data/")));
    }

    @Test
    void requireSafeOutbound_rejectsRfc1918Literals() {
        assertThrows(UnsafeUrlException.class,
                () -> UrlSafety.requireSafeOutbound(URI.create("http://10.0.0.1/")));
        assertThrows(UnsafeUrlException.class,
                () -> UrlSafety.requireSafeOutbound(URI.create("http://192.168.1.1/")));
        assertThrows(UnsafeUrlException.class,
                () -> UrlSafety.requireSafeOutbound(URI.create("http://172.16.0.1/")));
    }

    @Test
    void isBlockedAddress_detectsUniqueLocalIpv6() throws UnknownHostException {
        InetAddress ula = InetAddress.getByName("fd12:3456:789a::1");
        assertTrue(UrlSafety.isBlockedAddress(ula));
    }

    @Test
    void isBlockedAddress_detectsZeroNetwork() throws UnknownHostException {
        assertTrue(UrlSafety.isBlockedAddress(InetAddress.getByName("0.0.0.0")));
        assertTrue(UrlSafety.isBlockedAddress(InetAddress.getByName("0.1.2.3")));
    }

    @Test
    void isBlockedAddress_detectsCarrierGradeNat() throws UnknownHostException {
        assertTrue(UrlSafety.isBlockedAddress(InetAddress.getByName("100.64.0.1")));
        assertTrue(UrlSafety.isBlockedAddress(InetAddress.getByName("100.127.255.254")));
        assertFalse(UrlSafety.isBlockedAddress(InetAddress.getByName("100.63.255.255")));
        assertFalse(UrlSafety.isBlockedAddress(InetAddress.getByName("100.128.0.1")));
    }

    @Test
    void resolveValidated_returnsAddressesForPublicHost() {
        List<InetAddress> addresses = UrlSafety.resolveValidated(URI.create("https://example.com/feed.xml"));
        assertFalse(addresses.isEmpty());
        addresses.forEach(a -> assertFalse(UrlSafety.isBlockedAddress(a)));
    }

    @Test
    void requireSafeOutbound_honoursCustomPortAllowList() {
        assertDoesNotThrow(() -> UrlSafety.requireSafeOutbound(
                URI.create("https://example.com:8443/x"), Set.of(443, 8443)));
        assertThrows(UnsafeUrlException.class,
                () -> UrlSafety.requireSafeOutbound(
                        URI.create("https://example.com:8443/x"), Set.of(80, 443)));
    }

    @Test
    void isSafeOutbound_falseForLocalhostHostname() {
        assertFalse(UrlSafety.isSafeOutbound(URI.create("http://localhost/")));
    }
}
