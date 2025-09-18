package com.hawkins.xtreamjson.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class XstreamCredentials {
    private static final Logger log = LoggerFactory.getLogger(XstreamCredentials.class);
    
    private String apiUrl;
    private String username;
    private String password;

    public XstreamCredentials(
        @Value("${xtream.api-url}") String apiUrl,
        @Value("${xtream.username}") String username,
        @Value("${xtream.password}") String password
    ) {
        this.apiUrl = apiUrl;
        this.username = username;
        this.password = password;
        log.debug("XstreamCredentials loaded from application.properties");
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
}