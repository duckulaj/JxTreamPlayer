package com.hawkins.xtreamjson.service;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hawkins.xtreamjson.data.EpgChannel;
import com.hawkins.xtreamjson.data.EpgContainer;
import com.hawkins.xtreamjson.data.EpgProgramme;
import com.hawkins.xtreamjson.data.LiveStream;
import com.hawkins.xtreamjson.data.MovieCategory;
import com.hawkins.xtreamjson.data.MovieStream;
import com.hawkins.xtreamjson.repository.LiveStreamRepository;
import com.hawkins.xtreamjson.repository.MovieCategoryRepository;
import com.hawkins.xtreamjson.repository.MovieStreamRepository;
import com.hawkins.xtreamjson.util.StreamUrlHelper;
import com.hawkins.xtreamjson.util.XstreamCredentials;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ApiService {

    private final LiveStreamRepository liveStreamRepository;
    private final MovieCategoryRepository movieCategoryRepository;
    private final MovieStreamRepository movieStreamRepository;
    private final ApplicationPropertiesService applicationPropertiesService;
    private final IptvProviderService providerService;
    private final EpgService epgService;

    public ApiService(LiveStreamRepository liveStreamRepository,
            MovieCategoryRepository movieCategoryRepository,
            MovieStreamRepository movieStreamRepository,
            ApplicationPropertiesService applicationPropertiesService,
            IptvProviderService providerService,
            EpgService epgService) {
        this.liveStreamRepository = liveStreamRepository;
        this.movieCategoryRepository = movieCategoryRepository;
        this.movieStreamRepository = movieStreamRepository;
        this.applicationPropertiesService = applicationPropertiesService;
        this.providerService = providerService;
        this.epgService = epgService;
    }

    /**
     * Generates filtered EPG XML content based on included countries configuration.
     * 
     * @return XML content as byte array, or null if EPG data cannot be loaded
     */
    public byte[] generateFilteredEpgXml() {
        // Load EPG data
        EpgContainer epgData = epgService.loadEpgData();
        if (epgData == null) {
            log.warn("EPG file not found or could not be loaded");
            return null;
        }

        // Get included countries configuration
        String includedCountries = applicationPropertiesService.getCurrentProperties().getIncludedCountries();

        // Parse included country prefixes
        List<String> prefixes = parseCountryPrefixes(includedCountries);

        // Filter channels based on country prefixes
        List<EpgChannel> filteredChannels = filterEpgChannels(epgData.getChannels(), prefixes);

        // Create a set of filtered channel IDs for quick lookup
        Set<String> filteredChannelIds = filteredChannels.stream()
                .map(EpgChannel::id)
                .collect(Collectors.toSet());

        // Filter programmes to only include those for filtered channels
        List<EpgProgramme> filteredProgrammes = epgData.getProgrammes().stream()
                .filter(programme -> filteredChannelIds.contains(programme.channel()))
                .collect(Collectors.toList());

        // Create a new container with filtered data
        EpgContainer filteredEpgData = new EpgContainer();
        filteredEpgData.setChannels(filteredChannels);
        filteredEpgData.setProgrammes(filteredProgrammes);

        // Serialize to XML
        try {
            XmlMapper xmlMapper = new XmlMapper();
            StringWriter writer = new StringWriter();
            xmlMapper.writeValue(writer, filteredEpgData);
            String xmlContent = writer.toString();
            return xmlContent.getBytes("UTF-8");
        } catch (Exception e) {
            log.error("Error generating filtered EPG XML", e);
            return null;
        }
    }

    /**
     * Retrieves and filters live channels based on included countries
     * configuration.
     * Also populates direct source URLs and cleans channel names.
     * 
     * @return List of filtered and processed live streams
     */
    public List<LiveStream> getFilteredLiveChannels() {
        String includedCountries = applicationPropertiesService.getCurrentProperties().getIncludedCountries();
        List<LiveStream> allStreams = liveStreamRepository.findAll();

        // Parse included countries
        List<String> prefixes = parseCountryPrefixes(includedCountries);

        // Filter streams based on country prefixes
        List<LiveStream> filteredStreams = filterLiveStreams(allStreams, prefixes);

        // Get provider credentials
        Optional<XstreamCredentials> credentialsOpt = providerService.getSelectedProvider()
                .map(p -> new XstreamCredentials(p.getApiUrl(), p.getUsername(), p.getPassword()));

        // Use empty credentials if no provider selected, to avoid null pointers, though
        // URLs won't work
        XstreamCredentials credentials = credentialsOpt.orElse(new XstreamCredentials("", "", ""));

        // Populate direct source URLs and clean names
        for (LiveStream stream : filteredStreams) {
            // Clean the name (remove prefix and colon) similar to EpgProcessorService
            cleanStreamName(stream);

            // Build and set the direct source URL
            String url = StreamUrlHelper.buildLiveUrl(
                    credentials.getApiUrl(),
                    credentials.getUsername(),
                    credentials.getPassword(),
                    stream);
            stream.setDirectSource(url);
        }

        return filteredStreams;
    }

    /**
     * Parses comma-separated country prefixes from configuration string.
     * 
     * @param includedCountries Comma-separated country prefixes
     * @return List of trimmed country prefixes
     */
    private List<String> parseCountryPrefixes(String includedCountries) {
        List<String> prefixes = new ArrayList<>();
        if (includedCountries != null && !includedCountries.isEmpty()) {
            for (String p : includedCountries.split(",")) {
                prefixes.add(p.trim());
            }
        }
        return prefixes;
    }

    /**
     * Filters EPG channels based on country prefixes.
     * 
     * @param channels List of all EPG channels
     * @param prefixes List of country prefixes to include
     * @return Filtered list of EPG channels
     */
    private List<EpgChannel> filterEpgChannels(List<EpgChannel> channels, List<String> prefixes) {
        if (prefixes.isEmpty()) {
            return new ArrayList<>(channels);
        }

        return channels.stream()
                .filter(channel -> {
                    String displayName = channel.displayName();
                    if (displayName == null)
                        return false;
                    for (String prefix : prefixes) {
                        if (displayName.startsWith(prefix)) {
                            return true;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    /**
     * Filters live streams based on country prefixes.
     * 
     * @param streams  List of all live streams
     * @param prefixes List of country prefixes to include
     * @return Filtered list of live streams
     */
    private List<LiveStream> filterLiveStreams(List<LiveStream> streams, List<String> prefixes) {
        if (prefixes.isEmpty()) {
            return new ArrayList<>(streams);
        }

        return streams.stream()
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
    }

    /**
     * Cleans the stream name by removing country prefix and colon.
     * Modifies the stream object in place.
     * 
     * @param stream The live stream to clean
     */
    private void cleanStreamName(LiveStream stream) {
        String name = stream.getName();
        int colonIndex = name.indexOf(':');
        if (colonIndex != -1 && colonIndex < name.length() - 1) {
            String cleanedName = name.substring(colonIndex + 1).trim();
            stream.setName(cleanedName);
        }
    }

    /**
     * Retrieves and filters movie categories based on included countries
     * configuration.
     * Only returns categories that have at least one movie matching the country
     * filter.
     * 
     * @return List of movie categories that have movies from included countries
     */
    public List<MovieCategory> getFilteredMovieCategories() {
        String includedCountries = applicationPropertiesService.getCurrentProperties().getIncludedCountries();

        // Parse included country prefixes
        List<String> prefixes = parseCountryPrefixes(includedCountries);

        // Get all categories
        List<MovieCategory> allCategories = movieCategoryRepository.findAll();

        // If no country filter, return all categories
        if (prefixes.isEmpty()) {
            return allCategories;
        }

        // Get all movies
        List<MovieStream> allMovies = movieStreamRepository.findAll();

        // Filter movies by country prefix
        List<MovieStream> filteredMovies = allMovies.stream()
                .filter(movie -> {
                    String name = movie.getName();
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

        // Get unique category IDs from filtered movies
        Set<String> filteredCategoryIds = filteredMovies.stream()
                .map(MovieStream::getCategoryId)
                .filter(categoryId -> categoryId != null)
                .collect(Collectors.toSet());

        // Return only categories that have filtered movies
        return allCategories.stream()
                .filter(category -> filteredCategoryIds.contains(category.getCategoryId()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves and filters movies for a specific category based on included
     * countries configuration.
     * Also populates direct source URLs and cleans movie names.
     * 
     * @param categoryId The category ID to filter movies by
     * @return List of filtered and processed movie streams
     */
    public List<MovieStream> getFilteredMoviesByCategoryId(String categoryId) {
        String includedCountries = applicationPropertiesService.getCurrentProperties().getIncludedCountries();

        // Parse included country prefixes
        List<String> prefixes = parseCountryPrefixes(includedCountries);

        // Get all movies for the category
        List<MovieStream> allMovies = movieStreamRepository.findByCategoryId(categoryId);

        // Filter movies based on country prefixes
        List<MovieStream> filteredMovies = filterMovieStreams(allMovies, prefixes);

        // Get provider credentials
        Optional<XstreamCredentials> credentialsOpt = providerService.getSelectedProvider()
                .map(p -> new XstreamCredentials(p.getApiUrl(), p.getUsername(), p.getPassword()));

        // Use empty credentials if no provider selected, to avoid null pointers, though
        // URLs won't work
        XstreamCredentials credentials = credentialsOpt.orElse(new XstreamCredentials("", "", ""));

        // Populate direct source URLs and clean names
        for (MovieStream movie : filteredMovies) {
            // Clean the name (remove prefix and colon) similar to live streams
            cleanMovieName(movie);

            // Build and set the direct source URL
            String url = StreamUrlHelper.buildVodUrl(
                    credentials.getApiUrl(),
                    credentials.getUsername(),
                    credentials.getPassword(),
                    movie);
            movie.setDirectSource(url);
        }

        return filteredMovies;
    }

    /**
     * Filters movie streams based on country prefixes.
     * 
     * @param movies   List of all movie streams
     * @param prefixes List of country prefixes to include
     * @return Filtered list of movie streams
     */
    private List<MovieStream> filterMovieStreams(List<MovieStream> movies, List<String> prefixes) {
        if (prefixes.isEmpty()) {
            return new ArrayList<>(movies);
        }

        return movies.stream()
                .filter(movie -> {
                    String name = movie.getName();
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
    }

    /**
     * Cleans the movie name by removing country prefix and colon.
     * Modifies the movie object in place.
     * 
     * @param movie The movie stream to clean
     */
    private void cleanMovieName(MovieStream movie) {
        String name = movie.getName();
        int colonIndex = name.indexOf(':');
        if (colonIndex != -1 && colonIndex < name.length() - 1) {
            String cleanedName = name.substring(colonIndex + 1).trim();
            movie.setName(cleanedName);
        }
    }
}
