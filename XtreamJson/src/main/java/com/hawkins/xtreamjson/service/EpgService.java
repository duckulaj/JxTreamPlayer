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

import com.hawkins.xtreamjson.model.IptvProvider;

@Service
public class EpgService {
    private final IptvProviderService iptvProviderService;

    public EpgService(IptvProviderService iptvProviderService) {
        this.iptvProviderService = iptvProviderService;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void downloadEpgXml() throws IOException {
        String baseUrl = ((IptvProvider)this.iptvProviderService.getSelectedProvider().get()).getApiUrl();
        String username = ((IptvProvider)this.iptvProviderService.getSelectedProvider().get()).getUsername();
        String password = ((IptvProvider)this.iptvProviderService.getSelectedProvider().get()).getPassword();
        String epgUrl = String.format("%s/xmltv.php?username=%s&password=%s", baseUrl, username, password);
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            int bytesRead;
            URL url = null;
            try {
                url = new URI(epgUrl).toURL();
            }
            catch (MalformedURLException e) {
                e.printStackTrace();
            }
            catch (URISyntaxException e) {
                e.printStackTrace();
            }
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(20000);
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new IOException("Failed to download EPG XML: HTTP " + responseCode);
            }
            inputStream = connection.getInputStream();
            outputStream = new FileOutputStream("epg.xml");
            byte[] buffer = new byte[8192];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        finally {
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
    }
}
