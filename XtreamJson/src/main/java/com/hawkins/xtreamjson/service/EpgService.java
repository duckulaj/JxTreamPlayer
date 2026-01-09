package com.hawkins.xtreamjson.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

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

    public EpgService(IptvProviderService iptvProviderService) {
        this.iptvProviderService = iptvProviderService;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void downloadEpgXml() throws IOException {
        String baseUrl = ((IptvProvider) this.iptvProviderService.getSelectedProvider().get()).getApiUrl();
        String username = ((IptvProvider) this.iptvProviderService.getSelectedProvider().get()).getUsername();
        String password = ((IptvProvider) this.iptvProviderService.getSelectedProvider().get()).getPassword();
        String epgUrl = String.format("%s/xmltv.php?username=%s&password=%s", baseUrl, username, password);
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        File tempFile = new File("epg.xml.tmp");

        try {
            int bytesRead;
            URL url;
            try {
                url = new URI(epgUrl).toURL();
            } catch (URISyntaxException | MalformedURLException e) {
                throw new IOException("Invalid EPG URL: " + epgUrl, e);
            }
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(90000);
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new IOException("Failed to download EPG XML: HTTP " + responseCode);
            }
            inputStream = connection.getInputStream();
            outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[8192];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (connection != null) {
                connection.disconnect();
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

        XmlMapper xmlMapper = new XmlMapper();
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
