package de._0x2b.http;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.spi.InetAddressResolver;
import java.net.spi.InetAddressResolverProvider;
import java.util.List;
import java.util.stream.Stream;

/**
 * JDK 18+ DNS SPI: when {@link PinnedDns} has an entry for a host, return those
 * addresses instead of asking the OS resolver again (HttpClient re-resolve).
 */
public final class PinnedInetAddressResolverProvider extends InetAddressResolverProvider {

    @Override
    public InetAddressResolver get(Configuration configuration) {
        InetAddressResolver builtin = configuration.builtinResolver();
        return new InetAddressResolver() {
            @Override
            public Stream<InetAddress> lookupByName(String host, LookupPolicy lookupPolicy)
                    throws UnknownHostException {
                List<InetAddress> pinned = PinnedDns.lookup(host);
                if (pinned != null && !pinned.isEmpty()) {
                    return pinned.stream();
                }
                return builtin.lookupByName(host, lookupPolicy);
            }

            @Override
            public String lookupByAddress(byte[] addr) throws UnknownHostException {
                return builtin.lookupByAddress(addr);
            }
        };
    }

    @Override
    public String name() {
        return "feedbox-pinned-dns";
    }
}
