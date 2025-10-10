package com.hawkins.xtreamjson.controller;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.hawkins.xtreamjson.data.LiveStream;
import com.hawkins.xtreamjson.data.MovieStream;
import com.hawkins.xtreamjson.data.Series;
import com.hawkins.xtreamjson.repository.EpisodeRepository;
import com.hawkins.xtreamjson.repository.LiveCategoryRepository;
import com.hawkins.xtreamjson.repository.LiveStreamRepository;
import com.hawkins.xtreamjson.repository.SeasonRepository;
import com.hawkins.xtreamjson.repository.SeriesCategoryRepository;
import com.hawkins.xtreamjson.repository.SeriesRepository;
import com.hawkins.xtreamjson.service.IptvProviderService;
import com.hawkins.xtreamjson.service.JsonService;
import com.hawkins.xtreamjson.service.StrmService;
import com.hawkins.xtreamjson.util.StreamUrlHelper;

@Controller
public class HomeController {
    private final JsonService jsonService;
    private final LiveCategoryRepository liveCategoryRepository;
    private final LiveStreamRepository liveStreamRepository;
    private final IptvProviderService providerService;
    private final SeriesCategoryRepository seriesCategoryRepository;
    private final SeriesRepository seriesRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;
    private final StrmService strmService;

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
    private static final ExecutorService resetExecutor = Executors.newSingleThreadExecutor();
    private static final AtomicReference<Future<?>> resetFutureRef = new AtomicReference<>();

    
    public HomeController(JsonService jsonService, LiveCategoryRepository liveCategoryRepository, LiveStreamRepository liveStreamRepository, IptvProviderService providerService, SeriesCategoryRepository seriesCategoryRepository, SeriesRepository seriesRepository, SeasonRepository seasonRepository, EpisodeRepository episodeRepository, StrmService strmService) {
        this.jsonService = jsonService;
        this.liveCategoryRepository = liveCategoryRepository;
        this.liveStreamRepository = liveStreamRepository;
        this.providerService = providerService;
        this.seriesCategoryRepository = seriesCategoryRepository;
        this.seriesRepository = seriesRepository;
        this.seasonRepository = seasonRepository;
        this.episodeRepository = episodeRepository;
        this.strmService = strmService;
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
        return "fragments/resetStatus :: status";
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
        model.addAttribute("categories", liveCategoryRepository.findAll());
        return "fragments/liveCategoriesDropdown :: live-categories-dropdown";
    }

    @GetMapping("/liveCategoryItems")
    public String liveCategoryItems(@RequestParam("categoryId") String categoryId, Model model) {
        List<LiveStream> items = liveStreamRepository.findByCategoryId(categoryId);
        var credentials = providerService.getSelectedProvider().map(p -> new com.hawkins.xtreamjson.util.XstreamCredentials(p.getApiUrl(), p.getUsername(), p.getPassword())).orElse(new com.hawkins.xtreamjson.util.XstreamCredentials("", "", ""));
        for (var item : items) {
            String url = com.hawkins.xtreamjson.util.StreamUrlHelper.buildLiveUrl(
                credentials.getApiUrl(),
                credentials.getUsername(),
                credentials.getPassword(),
                item
            );
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
    public String movieCategoryItems(@RequestParam("categoryId") String categoryId, Model model) {
        List<MovieStream> movies = jsonService.getMoviesByCategory(categoryId);
        var credentials = providerService.getSelectedProvider().map(p -> new com.hawkins.xtreamjson.util.XstreamCredentials(p.getApiUrl(), p.getUsername(), p.getPassword())).orElse(new com.hawkins.xtreamjson.util.XstreamCredentials("", "", ""));
        for (var movie : movies) {
            String url = com.hawkins.xtreamjson.util.StreamUrlHelper.buildVodUrl(
                credentials.getApiUrl(),
                credentials.getUsername(),
                credentials.getPassword(),
                movie
            );
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
        model.addAttribute("categories", seriesCategoryRepository.findAll());
        return "fragments/seriesCategoriesDropdown :: series-category-dropdown";
    }

    @GetMapping("/stream.html")
    public String stream(@RequestParam(value = "url", required = false) String url, Model model) {
        model.addAttribute("url", url);
        return "stream";
    }

    @GetMapping("/movieCategoryPage")
    public String movieCategoryPage(
            @RequestParam("categoryId") String categoryId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "letter", required = false) String letter,
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
                movie
            );
            movie.setDirectSource(url);
        }
        logger.info("movieCategoryPage: categoryId={}, page={}, size={}, letter={}, moviesFound={}", categoryId, page, size, letter, moviePage.getContent().size());
        List<String> letters = jsonService.getAvailableStartingLetters(categoryId);
        model.addAttribute("moviePage", moviePage);
        model.addAttribute("letters", letters);
        model.addAttribute("selectedLetter", letter);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("pageSize", size);
        return "fragments/movieCategoryPage :: movie-category-page";
    }

    @GetMapping("/seriesCategoryItems")
    public String seriesCategoryItems(@RequestParam("categoryId") String categoryId, Model model) {
        List<Series> seriesList = seriesRepository.findByCategoryId(categoryId);
        model.addAttribute("series", seriesList);
        return "fragments/seriesCategoryItems :: series-list";
    }

    @GetMapping("/seasonsBySeries")
    public String seasonsBySeries(@RequestParam("seriesId") String seriesId,
                                  @RequestParam(value = "seriesImage", required = false) String seriesImage,
                                  Model model) {
        var seasons = seasonRepository.findBySeriesId(seriesId);
        logger.info("[seasonsBySeries] seriesId={}, found {} seasons", seriesId, seasons != null ? seasons.size() : 0);
        // Use utility method for image selection
        seriesImage = com.hawkins.xtreamjson.util.StreamViewUtils.resolveSeriesImage(seriesId, seriesImage, seriesRepository);
        model.addAttribute("seasons", seasons);
        model.addAttribute("seriesImage", seriesImage);
        return "fragments/seasonsBySeries :: seasons-list";
    }

    @GetMapping("/episodesBySeason")
    public String episodesBySeason(@RequestParam("seriesId") String seriesId,
                                   @RequestParam("seasonId") String seasonId,
                                   @RequestParam(value = "seriesImage", required = false) String seriesImage,
                                   Model model) {
        var episodes = episodeRepository.findBySeriesIdAndSeasonId(seriesId, seasonId);
        // Use utility method for image selection
        seriesImage = com.hawkins.xtreamjson.util.StreamViewUtils.resolveSeriesImage(seriesId, seriesImage, seriesRepository);
        // ...existing code for provider/stream setup...
        model.addAttribute("episodes", episodes);
        model.addAttribute("seriesImage", seriesImage);
        return "fragments/episodesBySeason :: episodes-list";
    }

    @GetMapping("/seriesCategoryPage")
    public String seriesCategoryPage(
            @RequestParam("categoryId") String categoryId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "letter", required = false) String letter,
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
            strmService.generateStrmFiles();
            model.addAttribute("streamStatus", "Movie stream folders and .strm files created successfully.");
        } catch (Exception e) {
            logger.error("Error generating .strm files", e);
            model.addAttribute("streamStatus", "Error creating .strm files: " + e.getMessage());
        }
        // return "fragments/resetStatus :: status";
        return "home";
    }
}