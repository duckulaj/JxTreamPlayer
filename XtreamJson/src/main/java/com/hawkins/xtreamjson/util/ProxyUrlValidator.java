package com.hawkins.xtreamjson.util;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProxyUrlValidator {
    private final Set<String> allowedHosts;
    private final Set<String> allowedSchemes;
    private final boolean blockPrivateIps;

    public ProxyUrlValidator(
            @Value("${proxy.allowed-hosts:}") String allowedHostsRaw,
            @Value("${proxy.allowed-schemes:http,https}") String allowedSchemesRaw,
            @Value("${proxy.block-private-ips:true}") boolean blockPrivateIps) {
        this.allowedHosts = parseCsv(allowedHostsRaw);
        this.allowedSchemes = parseCsv(allowedSchemesRaw);
        this.blockPrivateIps = blockPrivateIps;
    }

    public URI validate(String rawUrl) {
        URI uri;
        try {
            uri = new URI(rawUrl);
        } catch (URISyntaxException e) {
            throw new ProxyValidationException(400, "Invalid URL");
        }
        return validate(uri);
    }

    public URI validate(URI uri) {
        if (uri == null || uri.getScheme() == null) {
            throw new ProxyValidationException(400, "Invalid URL scheme");
        }
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!allowedSchemes.contains(scheme)) {
            throw new ProxyValidationException(400, "Unsupported URL scheme");
        }
        if (uri.getUserInfo() != null) {
            throw new ProxyValidationException(400, "User info not allowed in URL");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new ProxyValidationException(400, "Invalid URL host");
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (!allowedHosts.isEmpty() && !isAllowedHost(normalizedHost)) {
            throw new ProxyValidationException(403, "Host not allowed");
        }
        if (blockPrivateIps && resolvesToPrivateAddress(normalizedHost)) {
            throw new ProxyValidationException(403, "Host resolves to a private address");
        }
        return uri;
    }

    private boolean isAllowedHost(String host) {
        for (String allowed : allowedHosts) {
            if (allowed.startsWith(".")) {
                if (host.endsWith(allowed)) {
                    return true;
                }
            } else if (host.equals(allowed) || host.endsWith("." + allowed)) {
                return true;
            }
        }
        return false;
    }

    private boolean resolvesToPrivateAddress(String host) {
        if ("localhost".equals(host) || host.endsWith(".localhost")) {
            return true;
        }
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isMulticastAddress()) {
                    return true;
                }
                if (address instanceof Inet6Address && isUniqueLocalAddress((Inet6Address) address)) {
                    return true;
                }
            }
        } catch (Exception e) {
            throw new ProxyValidationException(400, "Host resolution failed");
        }
        return false;
    }

    private boolean isUniqueLocalAddress(Inet6Address address) {
        byte[] bytes = address.getAddress();
        int first = bytes[0] & 0xff;
        return (first & 0xfe) == 0xfc;
    }

    private Set<String> parseCsv(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<>();
        Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .forEach(result::add);
        return result;
    }
}
