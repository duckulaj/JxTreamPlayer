package com.hawkins.xtreamjson.controller;

import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hawkins.xtreamjson.data.LiveStream;
import com.hawkins.xtreamjson.data.MovieCategory;
import com.hawkins.xtreamjson.data.MovieStream;
import com.hawkins.xtreamjson.service.ApiService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@Slf4j
public class ApiController {

    private final ApiService apiService;

    public ApiController(ApiService apiService) {
        this.apiService = apiService;
    }

    @GetMapping(value = "/epg.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<ByteArrayResource> getEpgXml() {
        byte[] xmlBytes = apiService.generateFilteredEpgXml();

        if (xmlBytes == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setContentLength(xmlBytes.length);
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=epg.xml");

        return ResponseEntity.ok()
                .headers(headers)
                .body(new ByteArrayResource(xmlBytes));
    }

    @GetMapping("/getLiveChannels")
    public List<LiveStream> getLiveChannels() {
        return apiService.getFilteredLiveChannels();
    }

    @GetMapping("/getMovieCategories")
    public List<MovieCategory> getMovieCategories() {
        return apiService.getFilteredMovieCategories();
    }

    @GetMapping("/getMovies")
    public List<MovieStream> getMovies(@org.springframework.web.bind.annotation.RequestParam String categoryId) {
        return apiService.getFilteredMoviesByCategoryId(categoryId);
    }
}
