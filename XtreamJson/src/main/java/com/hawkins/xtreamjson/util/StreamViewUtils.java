package com.hawkins.xtreamjson.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hawkins.xtreamjson.data.Series;
import com.hawkins.xtreamjson.repository.SeriesRepository;

public class StreamViewUtils {
    private static final Logger logger = LoggerFactory.getLogger(StreamViewUtils.class);

    /**
     * Returns the cover image for a series, or a placeholder if not available.
     * 
     * @param seriesId         the series ID as String
     * @param seriesImage      the provided image (may be null or empty)
     * @param seriesRepository the repository to fetch the series if needed
     * @return the image URL to use
     */
    public static String resolveSeriesImage(String seriesId, String seriesImage, SeriesRepository seriesRepository) {
        if (seriesImage != null && !seriesImage.isEmpty()) {
            return seriesImage;
        }
        Series series = null;
        try {
            series = seriesRepository.findById(Integer.parseInt(seriesId)).orElse(null);
        } catch (NumberFormatException e) {
            logger.warn("Invalid seriesId for findById: {}", seriesId);
        }
        if (series != null && series.getCover() != null && !series.getCover().toLowerCase().contains("null")
                && !series.getCover().isEmpty()) {
            return series.getCover();
        }
        return "/images/placeholder.png";
    }
}
