package com.hawkins.xtreamjson.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * POJO for holding Xtream Codes API credentials and base URL.
 */
@Slf4j
public class XstreamCredentials {
        
    private String apiUrl;
    private String username;
    private String password;

    public XstreamCredentials(String apiUrl, String username, String password) {
        this.apiUrl = apiUrl;
        this.username = username;
        this.password = password;
        log.debug("XstreamCredentials loaded from database or runtime");
    }
    
    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "XstreamCredentials{apiUrl='" + apiUrl + "', username='" + username + "', password='***'}";
    }
}