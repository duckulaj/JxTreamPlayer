package com.hawkins.xtreamjson.data;

import lombok.Data;

@Data
public class EpgProgrammeViewModel {
    private String title;
    private String desc;
    private String start;
    private String stop;
    private long left; // pixels
    private long width; // pixels
    private String style; // computed style string
    private String streamUrl;
}
