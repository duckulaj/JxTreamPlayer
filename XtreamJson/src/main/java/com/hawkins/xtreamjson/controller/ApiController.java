package com.hawkins.xtreamjson.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hawkins.xtreamjson.data.LiveStream;
import com.hawkins.xtreamjson.repository.LiveStreamRepository;
import com.hawkins.xtreamjson.service.ApplicationPropertiesService;
import com.hawkins.xtreamjson.service.IptvProviderService;
import com.hawkins.xtreamjson.util.StreamUrlHelper;
import com.hawkins.xtreamjson.util.XstreamCredentials;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@Slf4j
public class ApiController {

    private final LiveStreamRepository liveStreamRepository;
    private final ApplicationPropertiesService applicationPropertiesService;
    private final IptvProviderService providerService;

    public ApiController(LiveStreamRepository liveStreamRepository,
            ApplicationPropertiesService applicationPropertiesService,
            IptvProviderService providerService) {
        this.liveStreamRepository = liveStreamRepository;
        this.applicationPropertiesService = applicationPropertiesService;
        this.providerService = providerService;
    }

    @GetMapping("/getLiveChannels")
    public List<LiveStream> getLiveChannels() {
        String includedCountries = applicationPropertiesService.getCurrentProperties().getIncludedCountries();
        List<LiveStream> allStreams = liveStreamRepository.findAll();

        // Parse included countries
        List<String> prefixes = new ArrayList<>();
        if (includedCountries != null && !includedCountries.isEmpty()) {
            for (String p : includedCountries.split(",")) {
                prefixes.add(p.trim());
            }
        }

        List<LiveStream> filteredStreams;
        if (!prefixes.isEmpty()) {
            filteredStreams = allStreams.stream()
                    .filter(stream -> {
                        String name = stream.getName();
                        if (name == null)
                            return false;
                        for (String prefix : prefixes) {
                            if (name.startsWith(prefix)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        } else {
            filteredStreams = new ArrayList<>(allStreams);
        }

        // Populate direct source URLs
        Optional<XstreamCredentials> credentialsOpt = providerService.getSelectedProvider()
                .map(p -> new XstreamCredentials(p.getApiUrl(), p.getUsername(), p.getPassword()));

        // Use empty credentials if no provider selected, to avoid null pointers, though
        // URLs won't work
        XstreamCredentials credentials = credentialsOpt.orElse(new XstreamCredentials("", "", ""));

        for (LiveStream stream : filteredStreams) {
            // Clean the name (remove prefix and colon) similar to EpgProcessorService
            // Note: EpgProcessorService cleans EpgChannel.displayName.
            // Here we are modifying LiveStream.name which acts as the display name.
            // We should probably do it to be consistent if these are "channels" to the
            // user.
            String name = stream.getName();
            int colonIndex = name.indexOf(':');
            if (colonIndex != -1 && colonIndex < name.length() - 1) {
                String cleanedName = name.substring(colonIndex + 1).trim();
                stream.setName(cleanedName);
            }

            String url = StreamUrlHelper.buildLiveUrl(
                    credentials.getApiUrl(),
                    credentials.getUsername(),
                    credentials.getPassword(),
                    stream);
            stream.setDirectSource(url);
        }

        return filteredStreams;
    }
}
