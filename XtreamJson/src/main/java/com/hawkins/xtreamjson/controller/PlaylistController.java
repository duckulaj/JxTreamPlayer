// PlaylistController.java
package com.hawkins.xtreamjson.controller;

import org.springframework.http.MediaType;
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
        
    	playlistService.generateFullLibraryPlaylist();
        return "home";
    }
}