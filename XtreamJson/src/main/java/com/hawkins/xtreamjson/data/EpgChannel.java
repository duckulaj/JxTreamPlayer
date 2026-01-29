package com.hawkins.xtreamjson.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EpgChannel(
        @JacksonXmlProperty(isAttribute = true) String id,
        @JacksonXmlProperty(localName = "display-name") String displayName,
        @JacksonXmlProperty(localName = "icon") Icon icon,
        String streamUrl,
        String categoryId) {
    public record Icon(
            @JacksonXmlProperty(isAttribute = true) String src) {
    }

    public EpgChannel withDisplayName(String newName) {
        return new EpgChannel(id, newName, icon, streamUrl, categoryId);
    }

    public EpgChannel withStreamInfo(String newStreamUrl, String newCategoryId) {
        return new EpgChannel(id, displayName, icon, newStreamUrl, newCategoryId);
    }
}
