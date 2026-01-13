package com.hawkins.xtreamjson.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hawkins.xtreamjson.data.EpgContainer;
import com.hawkins.xtreamjson.model.IptvProvider;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EpgService {
    private final IptvProviderService iptvProviderService;
    private final HttpClient httpClient;
    private final XmlMapper xmlMapper;

    public EpgService(IptvProviderService iptvProviderService) {
        this.iptvProviderService = iptvProviderService;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.xmlMapper = new XmlMapper();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void downloadEpgXml() throws IOException, InterruptedException {
        String baseUrl = ((IptvProvider) this.iptvProviderService.getSelectedProvider().get()).getApiUrl();
        String username = ((IptvProvider) this.iptvProviderService.getSelectedProvider().get()).getUsername();
        String password = ((IptvProvider) this.iptvProviderService.getSelectedProvider().get()).getPassword();
        String epgUrl = String.format("%s/xmltv.php?username=%s&password=%s", baseUrl, username, password);
        File tempFile = new File("epg.xml.tmp");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(epgUrl))
                .timeout(Duration.ofSeconds(90))
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to download EPG XML: HTTP " + response.statusCode());
        }

        try (InputStream inputStream = response.body();
                FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        File destFile = new File("epg.xml");
        if (destFile.exists()) {
            if (!destFile.delete()) {
                log.warn("Failed to delete existing EPG file before renaming");
            }
        }
        if (!tempFile.renameTo(destFile)) {
            throw new IOException(
                    "Failed to rename temp EPG file to " + destFile.getName() + ". Check file permissions.");
        }
    }

    private static final String EPG_FILE_PATH = "epg.xml";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss Z");

    public EpgContainer loadEpgData() {
        File file = new File(EPG_FILE_PATH);
        if (!file.exists()) {
            log.error("EPG file not found at {}", EPG_FILE_PATH);
            return null;
        }

        if (!file.exists()) {
            log.error("EPG file not found at {}", EPG_FILE_PATH);
            return null;
        }
        try {
            return xmlMapper.readValue(file, EpgContainer.class);
        } catch (IOException e) {
            log.error("Error parsing EPG XML", e);
            return null;
        }
    }

    public LocalDateTime parseEpgDate(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            log.error("Error parsing date: {}", dateStr, e);
            return null;
        }
    }
}
