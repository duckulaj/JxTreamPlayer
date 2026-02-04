package com.hawkins.xtreamjson.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hawkins.xtreamjson.util.ProxyUrlValidator;
import com.hawkins.xtreamjson.util.ProxyValidationException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class ProxyController {

    private final ProxyUrlValidator proxyUrlValidator;

    public ProxyController(ProxyUrlValidator proxyUrlValidator) {
        this.proxyUrlValidator = proxyUrlValidator;
    }

    @GetMapping("/proxy")
    public void proxyStream(@RequestParam("url") String streamUrl, HttpServletRequest request,
            HttpServletResponse response) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            log.debug("Proxying stream: {}", streamUrl);
            URI initialUri = proxyUrlValidator.validate(streamUrl);
            URL url = initialUri.toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(false); // We handle redirects manually

            // Standard browser-like User-Agent
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

            // Forward request headers (e.g., Range)
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                // Skip headers that should not be forwarded or might conflict
                if (!headerName.equalsIgnoreCase("Host")
                        && !headerName.equalsIgnoreCase("Connection")
                        && !headerName.equalsIgnoreCase("User-Agent")) {
                    String headerValue = request.getHeader(headerName);
                    connection.setRequestProperty(headerName, headerValue);
                }
            }

            int responseCode = connection.getResponseCode();

            // Handle Redirects Manually (up to 5 times)
            int redirectCount = 0;
            while ((responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == 307 ||
                    responseCode == 308) && redirectCount < 5) {

                String newUrl = connection.getHeaderField("Location");
                log.debug("Redirecting proxy from {} to {}", url, newUrl);

                // Close previous connection
                connection.disconnect();

                URI redirectUri = new URI(url.toString()).resolve(newUrl);
                redirectUri = proxyUrlValidator.validate(redirectUri);
                url = redirectUri.toURL();
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

                // Re-apply headers? It's safer to not re-apply Range automatically on redirect
                // unless confirmed,
                // but for simple stream proxying, usually we just want to follow the link.

                responseCode = connection.getResponseCode();
                redirectCount++;
            }

            response.setStatus(responseCode);

            // Forward response headers (excluding Accept-Ranges which we'll set ourselves)
            for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
                String key = entry.getKey();
                if (key != null && !key.equalsIgnoreCase("Transfer-Encoding")
                        && !key.equalsIgnoreCase("Content-Encoding")
                        && !key.equalsIgnoreCase("Accept-Ranges")) {
                    // Exclude Accept-Ranges to avoid upstream's potentially malformed value
                    for (String value : entry.getValue()) {
                        response.addHeader(key, value);
                    }
                }
            }

            // Explicitly set Accept-Ranges header for video seeking
            // This MUST be set to "bytes" (not a range like "0-12345")
            response.setHeader("Accept-Ranges", "bytes");

            // Log Range request handling for debugging
            String rangeHeader = request.getHeader("Range");
            if (rangeHeader != null) {
                log.debug("Handling Range request: {} - Response code: {}", rangeHeader, responseCode);
            }

            if (responseCode >= 400) {
                log.error("Proxy received error code: {}", responseCode);
                // Try to read error stream to log it
                try (InputStream err = connection.getErrorStream()) {
                    if (err != null) {
                        String errBody = new String(err.readAllBytes());
                        log.error("Proxy upstream error body: {}", errBody);
                    }
                }
                return;
            }

            inputStream = connection.getInputStream();
            outputStream = response.getOutputStream();

            byte[] buffer = new byte[16384]; // 16KB buffer
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                outputStream.flush();
            }

        } catch (ProxyValidationException e) {
            log.warn("Blocked proxy request: {}", e.getMessage());
            response.setStatus(e.getStatusCode());
        } catch (org.springframework.web.context.request.async.AsyncRequestNotUsableException e) {
            // This is expected when client disconnects during streaming (e.g., seeking,
            // pausing)
            // The browser cancels the current request and starts a new Range request
            log.debug("Client disconnected during stream (likely due to seeking): {}", streamUrl);
        } catch (Exception e) {
            log.error("Error proxying stream: {}", streamUrl, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @GetMapping("/proxy/image")
    public void proxyImage(@RequestParam("url") String imageUrl, HttpServletResponse response) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            log.debug("Proxying image: {}", imageUrl);
            URI currentUri = proxyUrlValidator.validate(imageUrl);
            URL url = currentUri.toURL();

            int redirectCount = 0;
            int responseCode;

            while (true) {
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.setInstanceFollowRedirects(false); // Handle manually to allow protocol changes

                // Standard User-Agent to avoid blocks
                connection.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

                responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                        responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                        responseCode == 307 ||
                        responseCode == 308) {

                    if (redirectCount >= 5) {
                        log.error("Too many redirects for image: {}", imageUrl);
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }

                    String location = connection.getHeaderField("Location");
                    if (location == null) {
                        log.error("Redirect with no Location header for image: {}", imageUrl);
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }

                    connection.disconnect();
                    URI redirectUri = new URI(url.toString()).resolve(location);
                    redirectUri = proxyUrlValidator.validate(redirectUri);
                    url = redirectUri.toURL();
                    redirectCount++;
                    log.debug("Following image redirect ({}) to: {}", redirectCount, url);
                    continue;
                }
                break;
            }

            if (responseCode >= 400) {
                log.warn("Upstream image returned error {}: {}", responseCode, imageUrl);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            if (connection == null) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            String contentType = connection.getContentType();
            if (contentType != null) {
                response.setContentType(contentType);
            }

            inputStream = connection.getInputStream();
            outputStream = response.getOutputStream();

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();

        } catch (ProxyValidationException e) {
            log.warn("Blocked proxy image request: {}", e.getMessage());
            response.setStatus(e.getStatusCode());
        } catch (Exception e) {
            log.error("Error proxying image: {}", imageUrl, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
