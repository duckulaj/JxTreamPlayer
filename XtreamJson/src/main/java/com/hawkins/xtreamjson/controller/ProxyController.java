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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class ProxyController {

    @GetMapping("/proxy")
    public void proxyStream(@RequestParam("url") String streamUrl, HttpServletRequest request,
            HttpServletResponse response) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            log.info("Proxying stream: {}", streamUrl);
            URL url = new URI(streamUrl).toURL();
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
                log.info("Redirecting proxy from {} to {}", url, newUrl);

                // Close previous connection
                connection.disconnect();

                url = new URI(newUrl).toURL();
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

            // Forward response headers
            for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
                String key = entry.getKey();
                if (key != null && !key.equalsIgnoreCase("Transfer-Encoding")
                        && !key.equalsIgnoreCase("Content-Encoding")) {
                    // Exclude Content-Encoding to avoid double compression issues if we read raw
                    // stream
                    for (String value : entry.getValue()) {
                        response.addHeader(key, value);
                    }
                }
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
            URL url = new URI(imageUrl).toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setInstanceFollowRedirects(true);

            // Fake User-Agent to avoid some basic blocks
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                    || responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                String newUrl = connection.getHeaderField("Location");
                connection = (HttpURLConnection) new URI(newUrl).toURL().openConnection();
                responseCode = connection.getResponseCode();
            }

            if (responseCode >= 400) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            response.setContentType(connection.getContentType());

            inputStream = connection.getInputStream();
            outputStream = response.getOutputStream();

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();

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
