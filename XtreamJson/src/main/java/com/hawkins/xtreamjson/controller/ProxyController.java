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
            URL url = new URI(streamUrl).toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);

            // Forward request headers (e.g., Range)
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                // Skip headers that should not be forwarded or might conflict
                if (!headerName.equalsIgnoreCase("Host") && !headerName.equalsIgnoreCase("Connection")) {
                    String headerValue = request.getHeader(headerName);
                    connection.setRequestProperty(headerName, headerValue);
                }
            }

            int responseCode = connection.getResponseCode();
            response.setStatus(responseCode);

            // Forward response headers (e.g., Content-Type, Content-Length, Content-Range)
            for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
                String key = entry.getKey();
                if (key != null && !key.equalsIgnoreCase("Transfer-Encoding")) { // Chunked transfer is handled by
                                                                                 // servlet container
                    for (String value : entry.getValue()) {
                        response.addHeader(key, value);
                    }
                }
            }

            inputStream = connection.getInputStream();
            outputStream = response.getOutputStream();

            byte[] buffer = new byte[8192];
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
