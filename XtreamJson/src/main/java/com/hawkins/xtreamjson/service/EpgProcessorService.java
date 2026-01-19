package com.hawkins.xtreamjson.service;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.hawkins.xtreamjson.data.EpgChannel;
import com.hawkins.xtreamjson.data.EpgContainer;
import com.hawkins.xtreamjson.data.EpgProgramme;
import com.hawkins.xtreamjson.data.EpgProgrammeViewModel;
import com.hawkins.xtreamjson.data.LiveStream;
import com.hawkins.xtreamjson.data.LiveCategory;
import com.hawkins.xtreamjson.repository.LiveCategoryRepository;
import com.hawkins.xtreamjson.repository.LiveStreamRepository;
import com.hawkins.xtreamjson.util.StreamUrlHelper;
import com.hawkins.xtreamjson.util.XstreamCredentials;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for processing EPG data including filtering, channel name cleaning,
 * timeline generation, and programme view model creation.
 */
@Service
@Slf4j
public class EpgProcessorService {
    private static final int PIXELS_PER_MINUTE = 4;
    private static final int TIMELINE_HOURS = 24;
    private static final int TIMELINE_SLOT_MINUTES = 30;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter XML_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss Z");

    private final LiveStreamRepository liveStreamRepository;
    private final LiveCategoryRepository liveCategoryRepository;

    public EpgProcessorService(LiveStreamRepository liveStreamRepository,
            LiveCategoryRepository liveCategoryRepository) {
        this.liveStreamRepository = liveStreamRepository;
        this.liveCategoryRepository = liveCategoryRepository;
    }

    /**
     * Processes EPG data and returns a complete EPG view model.
     */
    public EpgViewModel processEpgData(EpgContainer epgData, String includedCountries, XstreamCredentials credentials) {
        if (epgData == null) {
            return null;
        }

        // Filter and clean channels
        List<EpgChannel> filteredChannels = filterAndCleanChannels(epgData.getChannels(), includedCountries);

        // Determine timeline
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timelineStart = now.truncatedTo(ChronoUnit.HOURS);

        // Generate timeline slots
        List<String> timelineSlots = generateTimelineSlots(timelineStart);

        // Prepare info map
        Map<String, ChannelInfo> channelInfoMap = buildChannelStreamUrlMap(credentials);

        // Enrich channels with stream URLs and Category IDs
        for (EpgChannel channel : filteredChannels) {
            var info = channelInfoMap.get(channel.getId());
            if (info != null) {
                channel.setStreamUrl(info.url);
                channel.setCategoryId(info.categoryId);
            }
        }

        // Process programmes into view models
        Map<String, List<EpgProgrammeViewModel>> programmesByChannel = processProgrammes(
                epgData.getProgrammes(), filteredChannels, timelineStart, channelInfoMap);

        // Calculate current time offset
        long nowOffset = ChronoUnit.MINUTES.between(timelineStart, now) * PIXELS_PER_MINUTE;
        if (nowOffset < 0) {
            nowOffset = 0;
        }

        // Calculate active categories
        List<String> activeCategoryIds = filteredChannels.stream()
                .map(EpgChannel::getCategoryId)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        List<LiveCategory> activeCategories = liveCategoryRepository.findAllById(activeCategoryIds);
        // Sort alphabetically to match previous behavior
        activeCategories
                .sort(java.util.Comparator.comparing(LiveCategory::getCategoryName, String.CASE_INSENSITIVE_ORDER));

        return new EpgViewModel(filteredChannels, programmesByChannel, timelineSlots, nowOffset, activeCategories);
    }

    /**
     * Filters channels based on country prefixes and removes country identifiers
     * from display names.
     */
    private List<EpgChannel> filterAndCleanChannels(List<EpgChannel> channels, String includedCountries) {
        List<String> prefixes = new ArrayList<>();
        if (includedCountries != null && !includedCountries.isEmpty()) {
            for (String p : includedCountries.split(",")) {
                prefixes.add(p.trim());
            }
        }

        List<EpgChannel> filteredChannels = new ArrayList<>();
        if (!prefixes.isEmpty()) {
            for (var channel : channels) {
                for (String prefix : prefixes) {
                    if (channel.getDisplayName().startsWith(prefix)) {
                        filteredChannels.add(channel);
                        break;
                    }
                }
            }
        } else {
            filteredChannels = new ArrayList<>(channels);
        }

        // Remove country identifier and colon from channel display names
        for (var channel : filteredChannels) {
            String displayName = channel.getDisplayName();
            int colonIndex = displayName.indexOf(':');
            if (colonIndex != -1 && colonIndex < displayName.length() - 1) {
                String cleanedName = displayName.substring(colonIndex + 1).trim();
                channel.setDisplayName(cleanedName);
            }
        }

        return filteredChannels;
    }

    /**
     * Generates timeline slots for the EPG grid.
     */
    private List<String> generateTimelineSlots(LocalDateTime timelineStart) {
        List<String> timelineSlots = new ArrayList<>();
        int totalSlots = (TIMELINE_HOURS * 60) / TIMELINE_SLOT_MINUTES;
        for (int i = 0; i < totalSlots; i++) {
            timelineSlots.add(timelineStart.plusMinutes(i * TIMELINE_SLOT_MINUTES).format(TIME_FORMATTER));
        }
        return timelineSlots;
    }

    /**
     * Builds a map of EPG channel IDs to stream info (URL and categoryId).
     */
    private Map<String, ChannelInfo> buildChannelStreamUrlMap(XstreamCredentials credentials) {
        Map<String, ChannelInfo> channelInfoMap = new HashMap<>();
        if (credentials != null) {
            List<LiveStream> allStreams = liveStreamRepository.findAll();
            for (LiveStream stream : allStreams) {
                if (stream.getEpgChannelId() != null && !stream.getEpgChannelId().isEmpty()) {
                    String url = StreamUrlHelper.buildLiveUrl(
                            credentials.getApiUrl(), credentials.getUsername(), credentials.getPassword(), stream);
                    channelInfoMap.put(stream.getEpgChannelId(), new ChannelInfo(url, stream.getCategoryId()));
                }
            }
        }
        return channelInfoMap;
    }

    private static class ChannelInfo {
        final String url;
        final String categoryId;

        ChannelInfo(String url, String categoryId) {
            this.url = url;
            this.categoryId = categoryId;
        }
    }

    /**
     * Processes programmes into view models with positioning information.
     */
    private Map<String, List<EpgProgrammeViewModel>> processProgrammes(
            List<EpgProgramme> programmes,
            List<EpgChannel> filteredChannels,
            LocalDateTime timelineStart,
            Map<String, ChannelInfo> channelInfoMap) {

        Map<String, List<EpgProgrammeViewModel>> programmesByChannel = new HashMap<>();

        // Group raw programmes by channel
        var rawGrouped = programmes.stream()
                .collect(Collectors.groupingBy(EpgProgramme::getChannel));

        for (var channel : filteredChannels) {
            String channelId = channel.getId();
            if (!rawGrouped.containsKey(channelId)) {
                continue;
            }

            List<EpgProgramme> rawProgs = rawGrouped.get(channelId);
            List<EpgProgrammeViewModel> viewModels = new ArrayList<>();

            for (var prog : rawProgs) {
                try {
                    EpgProgrammeViewModel vm = createProgrammeViewModel(
                            prog, timelineStart, channelId, channelInfoMap);
                    if (vm != null) {
                        viewModels.add(vm);
                    }
                } catch (Exception e) {
                    log.error("Error processing programme: {}", prog.getTitle(), e);
                }
            }
            programmesByChannel.put(channelId, viewModels);
        }

        return programmesByChannel;
    }

    /**
     * Creates a single programme view model with positioning calculations.
     */
    private EpgProgrammeViewModel createProgrammeViewModel(
            EpgProgramme prog,
            LocalDateTime timelineStart,
            String channelId,
            Map<String, ChannelInfo> channelInfoMap) {

        // Parse with zone offset and convert to UTC
        ZonedDateTime startZoned = ZonedDateTime.parse(prog.getStart(), XML_FORMATTER);
        ZonedDateTime stopZoned = ZonedDateTime.parse(prog.getStop(), XML_FORMATTER);

        // Convert to UTC
        LocalDateTime start = startZoned.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime stop = stopZoned.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();

        // Skip if program ends before timeline start
        if (stop.isBefore(timelineStart)) {
            return null;
        }

        // Calculate relative start and duration
        long minutesFromStart = ChronoUnit.MINUTES.between(timelineStart, start);
        long durationMinutes = ChronoUnit.MINUTES.between(start, stop);

        // Handle clipping if program starts before timeline
        long displayLeft = minutesFromStart * PIXELS_PER_MINUTE;
        long displayWidth = durationMinutes * PIXELS_PER_MINUTE;

        if (minutesFromStart < 0) {
            // Starts before timeline, clip left
            displayWidth += (minutesFromStart * PIXELS_PER_MINUTE); // Reduce width
            displayLeft = 0; // Snap to start
        }

        // Skip if effectively invisible
        if (displayWidth <= 0) {
            return null;
        }

        EpgProgrammeViewModel vm = new EpgProgrammeViewModel();
        vm.setTitle(prog.getTitle());
        vm.setDesc(prog.getDesc());
        vm.setStart(prog.getStart());
        vm.setStop(prog.getStop());
        vm.setLeft(displayLeft);
        vm.setWidth(displayWidth);
        vm.setStyle("left: " + displayLeft + "px; width: " + displayWidth + "px; position: absolute;");

        // Set stream URL if available for this channel
        if (channelInfoMap.containsKey(channelId)) {
            vm.setStreamUrl(channelInfoMap.get(channelId).url);
        }

        return vm;
    }

    /**
     * Data class to hold processed EPG view data.
     */
    public static class EpgViewModel {
        private final List<EpgChannel> channels;
        private final Map<String, List<EpgProgrammeViewModel>> programmesByChannel;
        private final List<String> timelineSlots;
        private final long nowOffset;
        private final List<LiveCategory> categories;

        public EpgViewModel(List<EpgChannel> channels,
                Map<String, List<EpgProgrammeViewModel>> programmesByChannel,
                List<String> timelineSlots,
                long nowOffset,
                List<LiveCategory> categories) {
            this.channels = channels;
            this.programmesByChannel = programmesByChannel;
            this.timelineSlots = timelineSlots;
            this.nowOffset = nowOffset;
            this.categories = categories;
        }

        public List<EpgChannel> getChannels() {
            return channels;
        }

        public Map<String, List<EpgProgrammeViewModel>> getProgrammesByChannel() {
            return programmesByChannel;
        }

        public List<String> getTimelineSlots() {
            return timelineSlots;
        }

        public long getNowOffset() {
            return nowOffset;
        }

        public List<LiveCategory> getCategories() {
            return categories;
        }
    }
}
