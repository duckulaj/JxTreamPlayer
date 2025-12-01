package com.hawkins.xtreamjson.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "tv")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EpgContainer {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "channel")
    private List<EpgChannel> channels;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "programme")
    private List<EpgProgramme> programmes;
}
