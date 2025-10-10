package com.hawkins.xtreamjson.service;

import com.hawkins.xtreamjson.annotations.TrackExecutionTime;
import com.hawkins.xtreamjson.data.MovieStream;
import com.hawkins.xtreamjson.repository.MovieStreamRepository;
import com.hawkins.xtreamjson.util.StreamUrlHelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class StrmService {
    private static final String MOVIES_DIR = "Movies";

    @Autowired
    private MovieStreamRepository movieStreamRepository;
    
    @Autowired
    private IptvProviderService providerService;

    @TrackExecutionTime
    public void generateStrmFiles() throws IOException {
    	
    	var selectedProviderOpt = providerService.getSelectedProvider();
        if (selectedProviderOpt.isEmpty()) {
            throw new IllegalStateException("No IPTV provider selected.");
        }
        
        var selectedProvider = selectedProviderOpt.get();
        Path moviesPath = Paths.get(MOVIES_DIR);
        if (!Files.exists(moviesPath)) {
            Files.createDirectory(moviesPath);
        }

        List<MovieStream> movies = movieStreamRepository.findAll();
        for (MovieStream movie : movies) {
            String safeName = sanitizeFileName(movie.getName());
            Path movieDir = moviesPath.resolve(safeName);
            if (!Files.exists(movieDir)) {
                Files.createDirectory(movieDir);
            }
            String strmFileName = movie.getStreamId() + ".strm";
            Path strmFile = movieDir.resolve(strmFileName);
            // Overwrite file if it already exists
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(strmFile.toFile(), false))) {
            	
            	String url = StreamUrlHelper.buildVodUrl(
                        selectedProvider.getApiUrl(),
                        selectedProvider.getUsername(),
                        selectedProvider.getPassword(),
                        movie
                    );
                writer.write(url != null ? url : "");
            }
        }
    }

    private String sanitizeFileName(String name) {
        // Remove or replace characters not allowed in file/folder names
        return name == null ? "unknown" : name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}