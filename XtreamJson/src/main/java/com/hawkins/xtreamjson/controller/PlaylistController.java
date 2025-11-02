// PlaylistController.java
package com.hawkins.xtreamjson.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hawkins.xtreamjson.service.EpgService;
import com.hawkins.xtreamjson.service.PlaylistService;

@RestController
public class PlaylistController {

    private final PlaylistService playlistService;
    private final EpgService epgService;

    public PlaylistController(PlaylistService playlistService, EpgService epgService) {
        this.playlistService = playlistService;
        this.epgService = epgService;
    }

    @GetMapping(value = "/playlist.m3u8", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getPlaylist() {
        
    	try {
    		playlistService.generateFullLibraryPlaylist();
    		epgService.downloadEpgXml();
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    	return "home";
    }
}