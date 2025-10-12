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

    public String generateFullLibraryPlaylist() {
        StringBuilder playlist = new StringBuilder("#EXTM3U\n");
        AtomicInteger counter = new AtomicInteger(1);

        // Fetch all live categories
        List<LiveCategory> categories = liveCategoryRepository.findAll()
                .stream()
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
                int index = counter.getAndIncrement();
                String displayTitle = String.format("(%03d) [%s] %s", index, category.getCategoryName(), stream.getName());
                playlist.append("#EXTINF:-1 ")
                        .append("tvg-id=\"").append(stream.getEpgChannelId() != null ? stream.getEpgChannelId() : "").append("\" ")
                        .append("tvg-name=\"").append(stream.getName()).append("\" ")
                        .append("tvg-logo=\"").append(stream.getStreamIcon() != null ? stream.getStreamIcon() : "").append("\" ")
                        .append("group-title=\"").append(category.getCategoryName()).append("\", ")
                        .append(displayTitle)
                        .append("\n")
                        .append(stream.getDirectSource())
                        .append("\n");
            }
        }
        return playlist.toString();
    }
}