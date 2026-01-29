package com.hawkins.xtreamjson.data;

public record EpgProgrammeViewModel(
        String title,
        String desc,
        String start,
        String stop,
        long left, // pixels
        long width, // pixels
        String style, // computed style string
        String streamUrl) {
}
