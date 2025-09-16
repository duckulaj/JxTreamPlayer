package com.hawkins.xtreamjson.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.hawkins.xtreamjson.data.LiveCategoryRepository;
import com.hawkins.xtreamjson.data.LiveStreamRepository;
import com.hawkins.xtreamjson.data.MovieCategoryRepository;
import com.hawkins.xtreamjson.data.MovieStreamRepository;
import com.hawkins.xtreamjson.service.JsonService;

@Controller
public class HomeController {
    private final JsonService jsonService;
    private final LiveCategoryRepository liveCategoryRepository;
    private final LiveStreamRepository liveStreamRepository;
    private final MovieCategoryRepository movieCategoryRepository;
    private final MovieStreamRepository movieStreamRepository;

    @Autowired
    public HomeController(JsonService jsonService, LiveCategoryRepository liveCategoryRepository, LiveStreamRepository liveStreamRepository, MovieCategoryRepository movieCategoryRepository, MovieStreamRepository movieStreamRepository) {
        this.jsonService = jsonService;
        this.liveCategoryRepository = liveCategoryRepository;
        this.liveStreamRepository = liveStreamRepository;
        this.movieCategoryRepository = movieCategoryRepository;
        this.movieStreamRepository = movieStreamRepository;
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
        model.addAttribute("categories", liveCategoryRepository.findAll());
        return "fragments/liveCategoriesDropdown :: dropdown";
    }

    @GetMapping("/liveCategoryItems")
    public String liveCategoryItems(@RequestParam("categoryId") String categoryId, Model model) {
        model.addAttribute("items", liveStreamRepository.findByCategoryId(categoryId));
        return "fragments/liveCategoryItems :: items";
    }

    @GetMapping("/movieCategoriesDropdown")
    public String movieCategoriesDropdown(Model model) {
        model.addAttribute("categories", jsonService.getAllMovieCategories());
        return "fragments/movieCategoriesDropdown :: dropdown";
    }

    @GetMapping("/movieCategoryItems")
    public String movieCategoryItems(@RequestParam("categoryId") String categoryId, Model model) {
        model.addAttribute("items", jsonService.getMoviesByCategory(categoryId));
        return "fragments/movieCategoryItems :: items";
    }
}