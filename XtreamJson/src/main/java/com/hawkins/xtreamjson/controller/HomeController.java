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
import com.hawkins.xtreamjson.service.ApplicationPropertiesService;
import com.hawkins.xtreamjson.service.EpgProcessorService;
import com.hawkins.xtreamjson.service.EpgService;
import com.hawkins.xtreamjson.service.IptvProviderService;
import com.hawkins.xtreamjson.service.JsonService;
import com.hawkins.xtreamjson.service.SeriesService;
import com.hawkins.xtreamjson.service.StrmService;
import com.hawkins.xtreamjson.util.StreamUrlHelper;
import com.hawkins.xtreamjson.util.XstreamCredentials;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class HomeController {
    private final JsonService jsonService;
    private final IptvProviderService providerService;
    private final SeriesService seriesService;
    private final StrmService strmService;
    private final EpgService epgService;
    private final EpgProcessorService epgProcessorService;
    private final ApplicationPropertiesService applicationPropertiesService;

    private static final ExecutorService resetExecutor = Executors.newSingleThreadExecutor();
    private static final AtomicReference<Future<?>> resetFutureRef = new AtomicReference<>();

    public HomeController(JsonService jsonService, IptvProviderService providerService,
            SeriesService seriesService, StrmService strmService, EpgService epgService,
            EpgProcessorService epgProcessorService,
            ApplicationPropertiesService applicationPropertiesService) {
        this.jsonService = jsonService;
        this.providerService = providerService;
        this.seriesService = seriesService;
        this.strmService = strmService;
        this.epgService = epgService;
        this.epgProcessorService = epgProcessorService;
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
        List<LiveStream> items = jsonService.getLiveStreamsByCategory(categoryId);
        var credentials = providerService
                .getSelectedProvider().map(p -> new XstreamCredentials(p.getApiUrl(), p.getUsername(), p.getPassword()))
                .orElse(new XstreamCredentials("", "", ""));
        for (var item : items) {
            String url = StreamUrlHelper.buildLiveUrl(
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
                .getSelectedProvider().map(p -> new XstreamCredentials(p.getApiUrl(), p.getUsername(), p.getPassword()))
                .orElse(new XstreamCredentials("", "", ""));
        for (var movie : movies) {
            String url = StreamUrlHelper.buildVodUrl(
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
        List<Series> seriesList = seriesService.getSeriesByCategory(categoryId);
        model.addAttribute("series", seriesList);
        return "fragments/seriesCategoryItems :: series-list";
    }

    @GetMapping("/seasonsBySeries")
    public String seasonsBySeries(@RequestParam String seriesId,
            @RequestParam(required = false) String seriesImage,
            Model model) {
        var seasons = seriesService.getSeasonsBySeries(seriesId);
        seriesImage = seriesService.resolveSeriesImage(seriesId, seriesImage);
        model.addAttribute("seasons", seasons);
        model.addAttribute("seriesImage", seriesImage);
        return "fragments/seasonsBySeries :: seasons-list";
    }

    @GetMapping("/episodesBySeason")
    public String episodesBySeason(@RequestParam String seriesId,
            @RequestParam String seasonId,
            @RequestParam(required = false) String seriesImage,
            Model model) {
        var episodes = seriesService.getEpisodesBySeriesAndSeason(seriesId, seasonId);
        seriesImage = seriesService.resolveSeriesImage(seriesId, seriesImage);
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
                .getSelectedProvider().map(p -> new XstreamCredentials(p.getApiUrl(), p.getUsername(), p.getPassword()))
                .orElse(new XstreamCredentials("", "", ""));
        for (var movie : movies) {
            String url = StreamUrlHelper.buildVodUrl(
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
        String includedCountries = applicationPropertiesService.getCurrentProperties().getIncludedCountries();
        var credentials = providerService.getSelectedProvider()
                .map(p -> new XstreamCredentials(p.getApiUrl(), p.getUsername(), p.getPassword()))
                .orElse(null);

        var viewModel = epgProcessorService.processEpgData(epgData, includedCountries, credentials);
        if (viewModel != null) {
            model.addAttribute("channels", viewModel.getChannels());
            model.addAttribute("programmesByChannel", viewModel.getProgrammesByChannel());
            model.addAttribute("timelineSlots", viewModel.getTimelineSlots());
            model.addAttribute("nowOffset", viewModel.getNowOffset());
            model.addAttribute("categories", viewModel.getCategories());
        }
        return "epg";
    }

    @GetMapping("/epgFragment")
    public String epgFragment(Model model) {
        var epgData = epgService.loadEpgData();
        String includedCountries = applicationPropertiesService.getCurrentProperties().getIncludedCountries();
        var credentials = providerService.getSelectedProvider()
                .map(p -> new XstreamCredentials(p.getApiUrl(), p.getUsername(), p.getPassword()))
                .orElse(null);

        var viewModel = epgProcessorService.processEpgData(epgData, includedCountries, credentials);
        if (viewModel != null) {
            model.addAttribute("channels", viewModel.getChannels());
            model.addAttribute("programmesByChannel", viewModel.getProgrammesByChannel());
            model.addAttribute("timelineSlots", viewModel.getTimelineSlots());
            model.addAttribute("nowOffset", viewModel.getNowOffset());
            model.addAttribute("categories", viewModel.getCategories());
        }
        return "fragments/epg :: epg-fragment";
    }

}
