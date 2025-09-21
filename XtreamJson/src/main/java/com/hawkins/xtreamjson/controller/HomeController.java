package com.hawkins.xtreamjson.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.hawkins.xtreamjson.data.LiveStream;
import com.hawkins.xtreamjson.data.MovieStream;
import com.hawkins.xtreamjson.data.Series;
import com.hawkins.xtreamjson.repository.LiveCategoryRepository;
import com.hawkins.xtreamjson.repository.LiveStreamRepository;
import com.hawkins.xtreamjson.repository.MovieCategoryRepository;
import com.hawkins.xtreamjson.repository.MovieStreamRepository;
import com.hawkins.xtreamjson.repository.SeriesCategoryRepository;
import com.hawkins.xtreamjson.repository.SeriesRepository;
import com.hawkins.xtreamjson.service.IptvProviderService;
import com.hawkins.xtreamjson.service.JsonService;
import com.hawkins.xtreamjson.util.StreamUrlHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class HomeController {
    private final JsonService jsonService;
    private final LiveCategoryRepository liveCategoryRepository;
    private final LiveStreamRepository liveStreamRepository;
    private final MovieCategoryRepository movieCategoryRepository;
    private final MovieStreamRepository movieStreamRepository;
    private final IptvProviderService providerService;
    private final SeriesCategoryRepository seriesCategoryRepository;
    private final SeriesRepository seriesRepository;

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    public HomeController(JsonService jsonService, LiveCategoryRepository liveCategoryRepository, LiveStreamRepository liveStreamRepository, MovieCategoryRepository movieCategoryRepository, MovieStreamRepository movieStreamRepository, IptvProviderService providerService, SeriesCategoryRepository seriesCategoryRepository, SeriesRepository seriesRepository) {
        this.jsonService = jsonService;
        this.liveCategoryRepository = liveCategoryRepository;
        this.liveStreamRepository = liveStreamRepository;
        this.movieCategoryRepository = movieCategoryRepository;
        this.movieStreamRepository = movieStreamRepository;
        this.providerService = providerService;
        this.seriesCategoryRepository = seriesCategoryRepository;
        this.seriesRepository = seriesRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("message", "Welcome to XtreamJson Home Page!");
        return "home";
    }

    @GetMapping("/resetDatabase")
    public String resetDatabase(Model model) {
        try {
            jsonService.retreiveJsonData();
            model.addAttribute("resetStatus", "Database reset and JSON data loaded successfully.");
        } catch (Exception e) {
            model.addAttribute("resetStatus", "Error resetting database: " + e.getMessage());
        }
        return "fragments/resetStatus :: status";
    }

    @GetMapping("/liveCategoriesDropdown")
    public String liveCategoriesDropdown(Model model) {
        if (providerService.getSelectedProvider().isEmpty()) {
            return "redirect:/providers";
        }
        model.addAttribute("categories", liveCategoryRepository.findAll());
        return "fragments/liveCategoriesDropdown :: dropdown";
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
        return "fragments/liveCategoryItems :: items";
    }

    @GetMapping("/movieCategoriesDropdown")
    public String movieCategoriesDropdown(Model model) {
        if (providerService.getSelectedProvider().isEmpty()) {
            return "redirect:/providers";
        }
        model.addAttribute("categories", jsonService.getAllMovieCategories());
        return "fragments/movieCategoriesDropdown :: dropdown";
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
        return "fragments/movieCategoryItems :: items";
    }

    @GetMapping("/seriesCategoriesDropdown")
    public String seriesCategoriesDropdown(Model model) {
        if (providerService.getSelectedProvider().isEmpty()) {
            return "redirect:/providers";
        }
        model.addAttribute("categories", seriesCategoryRepository.findAll());
        return "fragments/seriesCategoriesDropdown :: dropdown";
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
        return "fragments/movieCategoryPage :: page";
    }

    @GetMapping("/seriesCategoryItems")
    public String seriesCategoryItems(@RequestParam("categoryId") String categoryId, Model model) {
        List<Series> seriesList = seriesRepository.findByCategoryId(categoryId);
        model.addAttribute("series", seriesList);
        return "fragments/seriesCategoryItems :: items";
    }
}