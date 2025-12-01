package com.hawkins.xtreamjson.controller;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.hawkins.xtreamjson.data.LiveStream;
import com.hawkins.xtreamjson.data.MovieStream;
import com.hawkins.xtreamjson.data.Series;
import com.hawkins.xtreamjson.data.SeriesCategory;
import com.hawkins.xtreamjson.repository.EpisodeRepository;
import com.hawkins.xtreamjson.repository.LiveCategoryRepository;
import com.hawkins.xtreamjson.repository.LiveStreamRepository;
import com.hawkins.xtreamjson.repository.SeasonRepository;
import com.hawkins.xtreamjson.repository.SeriesRepository;
import com.hawkins.xtreamjson.service.IptvProviderService;
import com.hawkins.xtreamjson.service.JsonService;
import com.hawkins.xtreamjson.service.StrmService;

import com.hawkins.xtreamjson.service.EpgService;
import com.hawkins.xtreamjson.service.ApplicationPropertiesService;
import com.hawkins.xtreamjson.util.StreamUrlHelper;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class HomeController {
    private final JsonService jsonService;
    private final LiveStreamRepository liveStreamRepository;
    private final IptvProviderService providerService;
    private final SeriesRepository seriesRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;

    private final StrmService strmService;
    private final EpgService epgService;
    private final ApplicationPropertiesService applicationPropertiesService;

    private static final ExecutorService resetExecutor = Executors.newSingleThreadExecutor();
    private static final AtomicReference<Future<?>> resetFutureRef = new AtomicReference<>();

    public HomeController(JsonService jsonService, LiveCategoryRepository liveCategoryRepository,
            LiveStreamRepository liveStreamRepository, IptvProviderService providerService,
            SeriesRepository seriesRepository, SeasonRepository seasonRepository, EpisodeRepository episodeRepository,
            StrmService strmService, EpgService epgService, ApplicationPropertiesService applicationPropertiesService) {
        this.jsonService = jsonService;
        this.liveStreamRepository = liveStreamRepository;
        this.providerService = providerService;
        this.seriesRepository = seriesRepository;
        this.seasonRepository = seasonRepository;
        this.episodeRepository = episodeRepository;
        this.strmService = strmService;
        this.epgService = epgService;
        this.applicationPropertiesService = applicationPropertiesService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("message", "Welcome to XtreamJson Home Page!");
        return "home";
    }

    @GetMapping("/resetDatabase")
    public String resetDatabase(Model model) {
        Future<?> prev = resetFutureRef.get();
        if (prev != null && !prev.isDone()) {
            model.addAttribute("resetStatus", "A reset operation is already running.");
            return "fragments/resetStatus :: status";
        }
        Future<?> future = resetExecutor.submit(() -> {
            try {
                jsonService.retreiveJsonData();
            } catch (Exception e) {
                // Optionally log
            }
        });
        resetFutureRef.set(future);
        model.addAttribute("resetStatus", "Database reset started. You will be notified when it completes.");
        // return "fragments/resetStatus :: status";
        return "home";
    }

    @GetMapping("/cancelResetDatabase")
    public String cancelResetDatabase(Model model) {
        Future<?> future = resetFutureRef.get();
        if (future != null && !future.isDone()) {
            future.cancel(true);
            model.addAttribute("resetStatus", "Database reset cancelled.");
        } else {
            model.addAttribute("resetStatus", "No reset operation is running.");
        }
        return "fragments/resetStatus :: status";
    }

    @GetMapping("/liveCategoriesDropdown")
    public String liveCategoriesDropdown(Model model) {
        if (providerService.getSelectedProvider().isEmpty()) {
            return "redirect:/providers";
        }
        model.addAttribute("categories", jsonService.getAllLiveCategories());
        return "fragments/liveCategoriesDropdown :: live-categories-dropdown";
    }

    @GetMapping("/liveCategoryItems")
    public String liveCategoryItems(@RequestParam String categoryId, Model model) {
        List<LiveStream> items = liveStreamRepository.findByCategoryId(categoryId);
        var credentials = providerService
                .getSelectedProvider().map(p -> new com.hawkins.xtreamjson.util.XstreamCredentials(p.getApiUrl(),
                        p.getUsername(), p.getPassword()))
                .orElse(new com.hawkins.xtreamjson.util.XstreamCredentials("", "", ""));
        for (var item : items) {
            String url = com.hawkins.xtreamjson.util.StreamUrlHelper.buildLiveUrl(
                    credentials.getApiUrl(),
                    credentials.getUsername(),
                    credentials.getPassword(),
                    item);
            item.setDirectSource(url);
        }
        model.addAttribute("items", items);
        return "fragments/liveCategoryItems :: live-category-items";
    }

    @GetMapping("/movieCategoriesDropdown")
    public String movieCategoriesDropdown(Model model) {
        if (providerService.getSelectedProvider().isEmpty()) {
            return "redirect:/providers";
        }
        model.addAttribute("categories", jsonService.getAllMovieCategories());
        return "fragments/movieCategoriesDropdown :: movie-category-dropdown";
    }

    @GetMapping("/movieCategoryItems")
    public String movieCategoryItems(@RequestParam String categoryId, Model model) {
        List<MovieStream> movies = jsonService.getMoviesByCategory(categoryId);
        var credentials = providerService
                .getSelectedProvider().map(p -> new com.hawkins.xtreamjson.util.XstreamCredentials(p.getApiUrl(),
                        p.getUsername(), p.getPassword()))
                .orElse(new com.hawkins.xtreamjson.util.XstreamCredentials("", "", ""));
        for (var movie : movies) {
            String url = com.hawkins.xtreamjson.util.StreamUrlHelper.buildVodUrl(
                    credentials.getApiUrl(),
                    credentials.getUsername(),
                    credentials.getPassword(),
                    movie);
            movie.setDirectSource(url);
        }
        model.addAttribute("items", movies);
        return "fragments/movieCategoryItems :: movie-category-items";
    }

    @GetMapping("/seriesCategoriesDropdown")
    public String seriesCategoriesDropdown(Model model) {
        if (providerService.getSelectedProvider().isEmpty()) {
            return "redirect:/providers";
        }

        List<SeriesCategory> categories = jsonService.getAllSeriesCategories();
        // log.info("seriesCategoriesDropdown: found {} categories", categories.size());
        model.addAttribute("categories", categories);
        return "fragments/seriesCategoriesDropdown :: series-category-dropdown";
    }

    @GetMapping("/stream.html")
    public String stream(@RequestParam(required = false) String url, @RequestParam(required = false) String title,
            Model model) {
        model.addAttribute("url", url);
        model.addAttribute("title", title);
        return "stream";
    }

    @GetMapping("/movieCategoryPage")
    public String movieCategoryPage(
            @RequestParam String categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String letter,
            Model model) {
        var selectedProviderOpt = providerService.getSelectedProvider();
        if (selectedProviderOpt.isEmpty()) {
            return "redirect:/providers";
        }
        var selectedProvider = selectedProviderOpt.get();
        Page<MovieStream> moviePage = jsonService.getMoviesByCategory(categoryId, page, size, letter);
        // Set directSource for each movie in the page
        for (MovieStream movie : moviePage.getContent()) {
            String url = StreamUrlHelper.buildVodUrl(
                    selectedProvider.getApiUrl(),
                    selectedProvider.getUsername(),
                    selectedProvider.getPassword(),
                    movie);
            movie.setDirectSource(url);
        }
        // log.info("movieCategoryPage: categoryId={}, page={}, size={}, letter={},
        // moviesFound={}", categoryId, page, size, letter,
        // moviePage.getContent().size());
        List<String> letters = jsonService.getAvailableStartingLetters(categoryId);
        model.addAttribute("moviePage", moviePage);
        model.addAttribute("letters", letters);
        model.addAttribute("selectedLetter", letter);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("pageSize", size);
        return "fragments/movieCategoryPage :: movie-category-page";
    }

    @GetMapping("/seriesCategoryItems")
    public String seriesCategoryItems(@RequestParam String categoryId, Model model) {
        List<Series> seriesList = seriesRepository.findByCategoryId(categoryId);
        model.addAttribute("series", seriesList);
        return "fragments/seriesCategoryItems :: series-list";
    }

    @GetMapping("/seasonsBySeries")
    public String seasonsBySeries(@RequestParam String seriesId,
            @RequestParam(required = false) String seriesImage,
            Model model) {
        var seasons = seasonRepository.findBySeriesId(seriesId);
        // log.info("[seasonsBySeries] seriesId={}, found {} seasons", seriesId, seasons
        // != null ? seasons.size() : 0);
        // Use utility method for image selection
        seriesImage = com.hawkins.xtreamjson.util.StreamViewUtils.resolveSeriesImage(seriesId, seriesImage,
                seriesRepository);
        model.addAttribute("seasons", seasons);
        model.addAttribute("seriesImage", seriesImage);
        return "fragments/seasonsBySeries :: seasons-list";
    }

    @GetMapping("/episodesBySeason")
    public String episodesBySeason(@RequestParam String seriesId,
            @RequestParam String seasonId,
            @RequestParam(required = false) String seriesImage,
            Model model) {
        var episodes = episodeRepository.findBySeriesIdAndSeasonId(seriesId, seasonId);
        // Use utility method for image selection
        seriesImage = com.hawkins.xtreamjson.util.StreamViewUtils.resolveSeriesImage(seriesId, seriesImage,
                seriesRepository);
        // ...existing code for provider/stream setup...
        model.addAttribute("episodes", episodes);
        model.addAttribute("seriesImage", seriesImage);
        return "fragments/episodesBySeason :: episodes-list";
    }

    @GetMapping("/seriesCategoryPage")
    public String seriesCategoryPage(
            @RequestParam String categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String letter,
            Model model) {
        var selectedProviderOpt = providerService.getSelectedProvider();
        if (selectedProviderOpt.isEmpty()) {
            return "redirect:/providers";
        }
        Page<Series> seriesPage = jsonService.getSeriesByCategory(categoryId, page, size, letter);
        List<String> letters = jsonService.getAvailableSeriesStartingLetters(categoryId);
        model.addAttribute("seriesPage", seriesPage);
        model.addAttribute("letters", letters);
        model.addAttribute("selectedLetter", letter);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("pageSize", size);
        return "fragments/seriesCategoryPage :: series-category-page";
    }

    @GetMapping("/createStreams")
    public String createStreams(Model model) {
        try {
            strmService.generateAllStrmFiles();
            model.addAttribute("streamStatus", "Movie stream folders and .strm files created successfully.");
        } catch (Exception e) {
            // log.error("Error generating .strm files", e);
            model.addAttribute("streamStatus", "Error creating .strm files: " + e.getMessage());
        }
        // return "fragments/resetStatus :: status";
        return "home";
    }

    @GetMapping("/searchTitles")
    public String searchTitles(@RequestParam String query, Model model) {
        var movies = jsonService.searchMoviesByTitle(query);
        var series = jsonService.searchSeriesByTitle(query);
        // Populate directSource for each movie using selected provider credentials (so
        // results behave like movieCategoryItems)
        var credentials = providerService
                .getSelectedProvider().map(p -> new com.hawkins.xtreamjson.util.XstreamCredentials(p.getApiUrl(),
                        p.getUsername(), p.getPassword()))
                .orElse(new com.hawkins.xtreamjson.util.XstreamCredentials("", "", ""));
        for (var movie : movies) {
            String url = com.hawkins.xtreamjson.util.StreamUrlHelper.buildVodUrl(
                    credentials.getApiUrl(),
                    credentials.getUsername(),
                    credentials.getPassword(),
                    movie);
            movie.setDirectSource(url);
        }
        model.addAttribute("movies", movies);
        model.addAttribute("series", series);
        return "fragments/searchResults :: search-results";
    }

    @GetMapping("/ShowEPG")
    public String showEPG(Model model) {
        var epgData = epgService.loadEpgData();
        if (epgData != null) {
            // Filter channels based on IncludedCountries
            String includedCountries = applicationPropertiesService.getCurrentProperties().getIncludedCountries();
            List<String> prefixes = new java.util.ArrayList<>();
            if (includedCountries != null && !includedCountries.isEmpty()) {
                for (String p : includedCountries.split(",")) {
                    prefixes.add(p.trim());
                }
            }

            List<com.hawkins.xtreamjson.data.EpgChannel> filteredChannels = new java.util.ArrayList<>();
            if (!prefixes.isEmpty()) {
                for (var channel : epgData.getChannels()) {
                    for (String prefix : prefixes) {
                        if (channel.getDisplayName().startsWith(prefix)) {
                            filteredChannels.add(channel);
                            break;
                        }
                    }
                }
            } else {
                filteredChannels = epgData.getChannels();
            }

            // 1. Determine Timeline Start (e.g., current hour)
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDateTime timelineStart = now.truncatedTo(java.time.temporal.ChronoUnit.HOURS);

            // 2. Generate Timeline Slots (e.g., next 24 hours in 30 min chunks)
            List<String> timelineSlots = new java.util.ArrayList<>();
            java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
            for (int i = 0; i < 48; i++) { // 24 hours * 2 slots/hour
                timelineSlots.add(timelineStart.plusMinutes(i * 30).format(timeFormatter));
            }

            // 2.5 Prepare Stream URL Map
            java.util.Map<String, String> channelStreamUrlMap = new java.util.HashMap<>();
            var credentials = providerService.getSelectedProvider()
                    .map(p -> new com.hawkins.xtreamjson.util.XstreamCredentials(p.getApiUrl(), p.getUsername(),
                            p.getPassword()))
                    .orElse(null);

            if (credentials != null) {
                List<LiveStream> allStreams = liveStreamRepository.findAll();
                for (LiveStream stream : allStreams) {
                    if (stream.getEpgChannelId() != null && !stream.getEpgChannelId().isEmpty()) {
                        String url = com.hawkins.xtreamjson.util.StreamUrlHelper.buildLiveUrl(
                                credentials.getApiUrl(), credentials.getUsername(), credentials.getPassword(), stream);
                        channelStreamUrlMap.put(stream.getEpgChannelId(), url);
                    }
                }
            }

            // 3. Process Programmes into ViewModels
            java.time.format.DateTimeFormatter xmlFormatter = java.time.format.DateTimeFormatter
                    .ofPattern("yyyyMMddHHmmss Z");
            int pixelsPerMinute = 4;

            java.util.Map<String, List<com.hawkins.xtreamjson.data.EpgProgrammeViewModel>> programmesByChannel = new java.util.HashMap<>();

            // Group raw programmes first
            var rawGrouped = epgData.getProgrammes().stream()
                    .collect(java.util.stream.Collectors
                            .groupingBy(com.hawkins.xtreamjson.data.EpgProgramme::getChannel));

            for (var channel : filteredChannels) {
                String channelId = channel.getId();
                if (!rawGrouped.containsKey(channelId))
                    continue;

                List<com.hawkins.xtreamjson.data.EpgProgramme> rawProgs = rawGrouped.get(channelId);
                List<com.hawkins.xtreamjson.data.EpgProgrammeViewModel> viewModels = new java.util.ArrayList<>();

                for (var prog : rawProgs) {
                    try {
                        // Parse with zone offset and convert to UTC (GMT)
                        java.time.ZonedDateTime startZoned = java.time.ZonedDateTime.parse(prog.getStart(),
                                xmlFormatter);
                        java.time.ZonedDateTime stopZoned = java.time.ZonedDateTime.parse(prog.getStop(), xmlFormatter);

                        // Convert to UTC
                        java.time.LocalDateTime start = startZoned.withZoneSameInstant(java.time.ZoneOffset.UTC)
                                .toLocalDateTime();
                        java.time.LocalDateTime stop = stopZoned.withZoneSameInstant(java.time.ZoneOffset.UTC)
                                .toLocalDateTime();

                        // Skip if program ends before timeline start
                        if (stop.isBefore(timelineStart)) {
                            continue;
                        }

                        // Calculate relative start and duration
                        long minutesFromStart = java.time.temporal.ChronoUnit.MINUTES.between(timelineStart, start);
                        long durationMinutes = java.time.temporal.ChronoUnit.MINUTES.between(start, stop);

                        // Handle clipping if program starts before timeline
                        long displayLeft = minutesFromStart * pixelsPerMinute;
                        long displayWidth = durationMinutes * pixelsPerMinute;

                        if (minutesFromStart < 0) {
                            // Starts before timeline, clip left
                            displayWidth += (minutesFromStart * pixelsPerMinute); // Reduce width
                            displayLeft = 0; // Snap to start
                        }

                        // Skip if effectively invisible
                        if (displayWidth <= 0)
                            continue;

                        com.hawkins.xtreamjson.data.EpgProgrammeViewModel vm = new com.hawkins.xtreamjson.data.EpgProgrammeViewModel();
                        vm.setTitle(prog.getTitle());
                        vm.setDesc(prog.getDesc());
                        vm.setStart(prog.getStart());
                        vm.setStop(prog.getStop());
                        vm.setLeft(displayLeft);
                        vm.setWidth(displayWidth);
                        vm.setStyle("left: " + displayLeft + "px; width: " + displayWidth + "px; position: absolute;");

                        // Set stream URL if available for this channel
                        if (channelStreamUrlMap.containsKey(channelId)) {
                            vm.setStreamUrl(channelStreamUrlMap.get(channelId));
                        }

                        viewModels.add(vm);
                    } catch (Exception e) {
                        // Assuming 'log' is available, otherwise this line will cause a compilation
                        // error.
                        // If 'log' is not defined, you might want to use System.err.println or add a
                        // logger.
                        // log.error("Error processing programme: " + prog.getTitle(), e);
                    }
                }
                programmesByChannel.put(channelId, viewModels);
            }

            // Calculate current time offset
            long nowOffset = java.time.temporal.ChronoUnit.MINUTES.between(timelineStart, now) * pixelsPerMinute;
            if (nowOffset < 0)
                nowOffset = 0; // Should not happen if timelineStart is truncated to hour

            model.addAttribute("channels", filteredChannels);
            model.addAttribute("programmesByChannel", programmesByChannel);
            model.addAttribute("timelineSlots", timelineSlots);
            model.addAttribute("nowOffset", nowOffset);
        }
        return "epg";
    }

}
