package com.hawkins.xtreamjson.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class LiveStream {
    @Id
    @JsonProperty("stream_id")
    private int streamId;
    @JsonProperty("num")
    private int num;
    @JsonProperty("name")
    private String name;
    @JsonProperty("stream_type")
    private String streamType;
    @JsonProperty("stream_icon")
    private String streamIcon;
    @JsonProperty("epg_channel_id")
    private String epgChannelId;
    @JsonProperty("added")
    private String added;
    @JsonProperty("is_adult")
    private int isAdult;
    @JsonProperty("category_id")
    private String categoryId;
    @JsonProperty("category_ids")
    private List<String> categoryIds;
    @JsonProperty("custom_sid")
    private String customSid;
    @JsonProperty("tv_archive")
    private int tvArchive;
    @JsonProperty("direct_source")
    @Column(length = 8192)
    private String directSource;
    @JsonProperty("tv_archive_duration")
    private int tvArchiveDuration;
}