package com.hawkins.xtreamjson.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EpgChannel {
    @JacksonXmlProperty(isAttribute = true)
    private String id;

    @JacksonXmlProperty(localName = "display-name")
    private String displayName;

    @JacksonXmlProperty(localName = "icon")
    private Icon icon;

    @Data
    public static class Icon {
        @JacksonXmlProperty(isAttribute = true)
        private String src;
    }
}
