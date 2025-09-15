package com.hawkins.xtreamjson.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XstreamCredentials {
	
	
    private String apiUrl;
    private String username;
    private String password;
    
    private static XstreamCredentials thisInstance = null;

    public XstreamCredentials() {
    			// Load from application.properties or environment variables
    	
        this.apiUrl = System.getenv("XSTREAM_API_URL");
        this.username = System.getenv("XSTREAM_USERNAME");
        this.password = System.getenv("XSTREAM_PASSWORD");
        
    }
    
    public XstreamCredentials(String apiUrl, String username, String password) {
        this.apiUrl = apiUrl;
        this.username = username;
        this.password = password;
    }
    
    public static synchronized XstreamCredentials getInstance()
	{
		log.debug("Requesting XstreamCredentials instance");

		if (XstreamCredentials.thisInstance == null)
		{
			XstreamCredentials.thisInstance = new XstreamCredentials();
		}

		return XstreamCredentials.thisInstance;
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