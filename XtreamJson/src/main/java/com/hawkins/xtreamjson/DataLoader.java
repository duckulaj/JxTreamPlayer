package com.hawkins.xtreamjson;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.hawkins.xtreamjson.data.LiveCategory;
import com.hawkins.xtreamjson.data.LiveCategoryRepository;
import com.hawkins.xtreamjson.data.LiveStream;
import com.hawkins.xtreamjson.data.LiveStreamRepository;
import com.hawkins.xtreamjson.data.MovieCategory;
import com.hawkins.xtreamjson.data.MovieCategoryRepository;
import com.hawkins.xtreamjson.data.MovieStream;
import com.hawkins.xtreamjson.data.MovieStreamRepository;
import com.hawkins.xtreamjson.data.Series;
import com.hawkins.xtreamjson.data.SeriesCategory;
import com.hawkins.xtreamjson.data.SeriesCategoryRepository;
import com.hawkins.xtreamjson.data.SeriesRepository;
import com.hawkins.xtreamjson.service.JsonService;

@Component
public class DataLoader implements CommandLineRunner {
    @Autowired
    private LiveCategoryRepository liveCategoryRepository;
    @Autowired
    private MovieCategoryRepository movieCategoryRepository;
    @Autowired
    private SeriesCategoryRepository seriesCategoryRepository;
    @Autowired
    private SeriesRepository seriesRepository;
    @Autowired
    private LiveStreamRepository liveStreamRepository;
    @Autowired
    private MovieStreamRepository movieStreamRepository;
    
    @Autowired
    private JsonService jsonService;

    @Override
    public void run(String... args) throws Exception {
        List<LiveCategory> liveCategories = JsonService.readListFromFile("live_categories.json", LiveCategory.class);
        JsonService.persistList(liveCategoryRepository, liveCategories);

        List<MovieCategory> movieCategories = JsonService.readListFromFile("movie_categories.json", MovieCategory.class);
        JsonService.persistList(movieCategoryRepository, movieCategories);

        List<SeriesCategory> seriesCategories = JsonService.readListFromFile("series_categories.json", SeriesCategory.class);
        JsonService.persistList(seriesCategoryRepository, seriesCategories);

        List<Series> series = JsonService.readListFromFile("series.json", Series.class);
        JsonService.persistList(seriesRepository, series);

        List<LiveStream> liveStreams = JsonService.readListFromFile("live_stream.json", LiveStream.class);
        JsonService.persistList(liveStreamRepository, liveStreams);

        List<MovieStream> movieStreams = JsonService.readListFromFile("movie_stream.json", MovieStream.class);
        JsonService.persistList(movieStreamRepository, movieStreams);
    }
}
