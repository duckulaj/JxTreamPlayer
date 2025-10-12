// PlaylistController.java
package com.hawkins.xtreamjson.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hawkins.xtreamjson.service.PlaylistService;

@RestController
public class PlaylistController {

    private final PlaylistService playlistService;

    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    @GetMapping(value = "/playlist.m3u8", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getPlaylist() {
        String playlistContent = playlistService.generateFullLibraryPlaylist();

        // Write playlistContent to a file
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get("playlist.m3u8"), playlistContent.getBytes());
        } catch (java.io.IOException e) {
            // Optionally log or handle the error
            e.printStackTrace();
        }

        return "home";
    }
}