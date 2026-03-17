package com.hawkins.xtreamjson.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hawkins.xtreamjson.data.MovieStream;
import com.hawkins.xtreamjson.data.Series;
import com.hawkins.xtreamjson.repository.EpisodeRepository;
import com.hawkins.xtreamjson.repository.LiveCategoryRepository;
import com.hawkins.xtreamjson.repository.LiveStreamRepository;
import com.hawkins.xtreamjson.repository.MovieCategoryRepository;
import com.hawkins.xtreamjson.repository.MovieStreamRepository;
import com.hawkins.xtreamjson.repository.SeasonRepository;
import com.hawkins.xtreamjson.repository.SeriesCategoryRepository;
import com.hawkins.xtreamjson.repository.SeriesRepository;

@ExtendWith(MockitoExtension.class)
class JsonServiceSearchTest {

    @Mock private IptvProviderService providerService;
    @Mock private LiveCategoryRepository liveCategoryRepository;
    @Mock private LiveStreamRepository liveStreamRepository;
    @Mock private MovieCategoryRepository movieCategoryRepository;
    @Mock private MovieStreamRepository movieStreamRepository;
    @Mock private SeriesCategoryRepository seriesCategoryRepository;
    @Mock private SeriesRepository seriesRepository;
    @Mock private SeasonRepository seasonRepository;
    @Mock private EpisodeRepository episodeRepository;
    @Mock private ApplicationPropertiesService applicationPropertiesService;

    // Manually constructed so we can pass a real ObjectMapper
    private JsonService jsonService;

    @BeforeEach
    void setUp() {
        jsonService = new JsonService(
                providerService,
                liveCategoryRepository,
                liveStreamRepository,
                movieCategoryRepository,
                movieStreamRepository,
                seriesCategoryRepository,
                seriesRepository,
                seasonRepository,
                episodeRepository,
                applicationPropertiesService,
                new ObjectMapper());
    }

    // -----------------------------------------------------------------------
    // Series year search
    // -----------------------------------------------------------------------

    @Test
    void searchSeriesByTitle_whenQueryIsYear_callsSearchByReleaseYear() {
        Series s = new Series();
        s.setName("Breaking Bad");
        s.setReleaseDate("2008-01-20");
        when(seriesRepository.searchByReleaseYear("2008")).thenReturn(List.of(s));

        List<Series> results = jsonService.searchSeriesByTitle("2008");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Breaking Bad");
        verify(seriesRepository).searchByReleaseYear("2008");
        verify(seriesRepository, never()).searchByNameContaining(anyString());
    }

    @Test
    void searchSeriesByTitle_whenQueryIsTitle_callsSearchByNameContaining() {
        Series s = new Series();
        s.setName("Breaking Bad");
        when(seriesRepository.searchByNameContaining("Breaking")).thenReturn(List.of(s));

        List<Series> results = jsonService.searchSeriesByTitle("Breaking");

        assertThat(results).hasSize(1);
        verify(seriesRepository).searchByNameContaining("Breaking");
        verify(seriesRepository, never()).searchByReleaseYear(anyString());
    }

    @Test
    void searchSeriesByTitle_whenQueryIsBlank_returnsEmpty() {
        List<Series> results = jsonService.searchSeriesByTitle("  ");

        assertThat(results).isEmpty();
        verify(seriesRepository, never()).searchByNameContaining(anyString());
        verify(seriesRepository, never()).searchByReleaseYear(anyString());
    }

    @Test
    void searchSeriesByTitle_whenQueryIsNull_returnsEmpty() {
        List<Series> results = jsonService.searchSeriesByTitle(null);

        assertThat(results).isEmpty();
    }

    @Test
    void searchSeriesByTitle_whenYearQueryReturnsNoResults_returnsEmptyList() {
        when(seriesRepository.searchByReleaseYear("1999")).thenReturn(List.of());

        List<Series> results = jsonService.searchSeriesByTitle("1999");

        assertThat(results).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Movie year search  (year extracted from title, e.g. "Inception (2010)")
    // -----------------------------------------------------------------------

    @Test
    void searchMoviesByTitle_whenQueryIsYear_filtersMoviesWithMatchingTitleYear() {
        MovieStream match = new MovieStream();
        match.setStreamId(1);
        match.setName("Inception (2010)");

        MovieStream noMatch = new MovieStream();
        noMatch.setStreamId(2);
        noMatch.setName("The Matrix (1999)");

        // The repo's LIKE '%2010%' would return both; service then filters
        when(movieStreamRepository.searchByNameContaining("2010"))
                .thenReturn(List.of(match, noMatch));

        List<MovieStream> results = jsonService.searchMoviesByTitle("2010");

        assertThat(results).hasSize(1);
        // cleanTitle strips the year suffix, so just check id
        assertThat(results.get(0).getStreamId()).isEqualTo(1);
        verify(movieStreamRepository).searchByNameContaining("2010");
    }

    @Test
    void searchMoviesByTitle_whenQueryIsYear_returnsEmptyWhenNoTitleContainsYear() {
        // No movie has "2025" anywhere in its name
        when(movieStreamRepository.searchByNameContaining("2025")).thenReturn(List.of());

        List<MovieStream> results = jsonService.searchMoviesByTitle("2025");

        assertThat(results).isEmpty();
    }

    @Test
    void searchMoviesByTitle_whenQueryIsTitle_delegatesToNameContaining() {
        MovieStream m = new MovieStream();
        m.setStreamId(3);
        m.setName("Inception (2010)");
        when(movieStreamRepository.searchByNameContaining("Inception")).thenReturn(List.of(m));

        List<MovieStream> results = jsonService.searchMoviesByTitle("Inception");

        assertThat(results).hasSize(1);
        verify(movieStreamRepository).searchByNameContaining("Inception");
    }

    @Test
    void searchMoviesByTitle_whenQueryIsBlank_returnsEmpty() {
        List<MovieStream> results = jsonService.searchMoviesByTitle("");

        assertThat(results).isEmpty();
        verify(movieStreamRepository, never()).searchByNameContaining(anyString());
    }

    // -----------------------------------------------------------------------
    // Year-format boundary cases (must be exactly 4 digits to be a year query)
    // -----------------------------------------------------------------------

    @Test
    void searchSeriesByTitle_threeDigitNumber_treatedAsTitle() {
        when(seriesRepository.searchByNameContaining("202")).thenReturn(List.of());

        jsonService.searchSeriesByTitle("202");

        verify(seriesRepository).searchByNameContaining("202");
        verify(seriesRepository, never()).searchByReleaseYear(anyString());
    }

    @Test
    void searchSeriesByTitle_fiveDigitNumber_treatedAsTitle() {
        when(seriesRepository.searchByNameContaining("20234")).thenReturn(List.of());

        jsonService.searchSeriesByTitle("20234");

        verify(seriesRepository).searchByNameContaining("20234");
        verify(seriesRepository, never()).searchByReleaseYear(anyString());
    }
}
