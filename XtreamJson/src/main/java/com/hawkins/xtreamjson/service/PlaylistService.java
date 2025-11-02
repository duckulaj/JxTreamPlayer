// PlaylistService.java
package com.hawkins.xtreamjson.service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.hawkins.xtreamjson.data.LiveCategory;
import com.hawkins.xtreamjson.data.LiveStream;
import com.hawkins.xtreamjson.repository.LiveCategoryRepository;
import com.hawkins.xtreamjson.repository.LiveStreamRepository;
import com.hawkins.xtreamjson.util.XtreamCodesUtils;

@Service
public class PlaylistService {

    private final LiveCategoryRepository liveCategoryRepository;
    private final LiveStreamRepository liveStreamRepository;
    private final ApplicationPropertiesService applicationPropertiesService;

    public PlaylistService(LiveCategoryRepository liveCategoryRepository, LiveStreamRepository liveStreamRepository, ApplicationPropertiesService applicationPropertiesService) {
        this.liveCategoryRepository = liveCategoryRepository;
        this.liveStreamRepository = liveStreamRepository;
        this.applicationPropertiesService = applicationPropertiesService;
    }

    public void generateFullLibraryPlaylist() {
        StringBuilder playlist = new StringBuilder("#EXTM3U\n");
        AtomicInteger counter = new AtomicInteger(1);

        java.util.Set<String> includedSet = XtreamCodesUtils.getIncludedCountriesSet(applicationPropertiesService);
        
        // Get excluded titles from application properties
        String excludedTitlesRaw = applicationPropertiesService.getCurrentProperties().getExcludedTitles();
        java.util.Set<String> excludedTitlesSet = java.util.Arrays.stream(excludedTitlesRaw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(String::toUpperCase)
            .collect(java.util.stream.Collectors.toSet());
        
        // Fetch all categories, filter by included countries, and sort
        
        List<LiveCategory> categories = liveCategoryRepository.findAll()
                .stream()
                .filter(cat -> XtreamCodesUtils.isIncluded(cat.getCategoryName(), includedSet))
                .sorted(Comparator.comparing(LiveCategory::getCategoryName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        for (LiveCategory category : categories) {
            // Fetch all live streams for this category
            List<LiveStream> streams = liveStreamRepository.findByCategoryId(category.getCategoryId());
            if (streams.isEmpty()) continue;
            playlist.append("\n# ---- Category: ")
                    .append(category.getCategoryName())
                    .append(" ----\n");
            for (LiveStream stream : streams) {
                // Exclude stream if its name contains any excluded title (case-insensitive)
                String streamNameUpper = stream.getName().toUpperCase();
                boolean isExcluded = excludedTitlesSet.stream().anyMatch(streamNameUpper::contains);
                if (!isExcluded) {
                    int index = counter.getAndIncrement();
                    String displayTitle = String.format("(%03d) [%s] %s", index, category.getCategoryName(), stream.getName());
                    playlist.append("#EXTINF:-1 ")
                            .append("tvg-id=\"").append(stream.getEpgChannelId()).append("\" ")
                            .append("tvg-name=\"").append(stream.getName()).append("\" ")
                            .append("tvg-logo=\"").append(stream.getStreamIcon() != null ? stream.getStreamIcon() : "").append("\" ")
                            .append("group-title=\"").append(category.getCategoryName()).append("\", ")
                            .append(displayTitle)
                            .append("\n")
                            .append(stream.getDirectSource())
                            .append("\n");
                }
            }
        }
        
     // Write playlistContent to a file
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get("playlist.m3u8"), playlist.toString().getBytes());
        } catch (java.io.IOException e) {
            // Optionally log or handle the error
            e.printStackTrace();
        }
        
    }
}