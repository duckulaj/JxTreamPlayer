package com.hawkins.xtreamjson.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EpgProgramme {
    @JacksonXmlProperty(isAttribute = true)
    private String start;

    @JacksonXmlProperty(isAttribute = true)
    private String stop;

    @JacksonXmlProperty(isAttribute = true)
    private String channel;

    @JacksonXmlProperty(localName = "title")
    private String title;

    @JacksonXmlProperty(localName = "desc")
    private String desc;

    public long getDurationMinutes() {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss Z");
            LocalDateTime startTime = LocalDateTime.parse(start, formatter);
            LocalDateTime stopTime = LocalDateTime.parse(stop, formatter);
            return ChronoUnit.MINUTES.between(startTime, stopTime);
        } catch (Exception e) {
            return 30; // Default to 30 mins on error
        }
    }
}
