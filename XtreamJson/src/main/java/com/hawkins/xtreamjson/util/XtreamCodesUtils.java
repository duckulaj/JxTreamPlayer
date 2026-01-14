package com.hawkins.xtreamjson.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.hawkins.xtreamjson.service.ApplicationPropertiesService;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility methods for Xtream Codes API and file-safe string handling.
 */
@Slf4j
public class XtreamCodesUtils {

    /**
     * Sanitizes a string for safe use as a file or directory name.
     * 
     * @param name Input string
     * @return Sanitized string
     */
    public static String sanitizeName(String name) {
        if (name == null)
            return "";
        return name.replaceAll("[\\/:*?\"<>|]", "_").replaceAll("\s+", " ").trim();
    }

    /**
     * Checks if any of the provided strings are null or empty.
     */
    public static boolean isNullOrEmpty(String... values) {
        if (values == null)
            return true;
        for (String v : values) {
            if (v == null || v.isEmpty())
                return true;
        }
        return false;
    }

    /**
     * Builds a VOD/movie stream URL from credentials and MovieStream.
     */
    public static String buildVodUrl(XstreamCredentials credentials, com.hawkins.xtreamjson.data.MovieStream stream) {
        if (credentials == null || stream == null
                || isNullOrEmpty(credentials.getApiUrl(), credentials.getUsername(), credentials.getPassword()) ||
                isNullOrEmpty(stream.getContainerExtension()) || stream.getStreamId() <= 0) {
            return null;
        }
        return String.format("%s/movie/%s/%s/%d.%s",
                credentials.getApiUrl(),
                credentials.getUsername(),
                credentials.getPassword(),
                stream.getStreamId(),
                stream.getContainerExtension());
    }

    /**
     * Builds a live stream URL from credentials and LiveStream.
     */
    public static String buildLiveUrl(XstreamCredentials credentials, com.hawkins.xtreamjson.data.LiveStream stream) {
        if (credentials == null || stream == null
                || isNullOrEmpty(credentials.getApiUrl(), credentials.getUsername(), credentials.getPassword())
                || stream.getStreamId() <= 0) {
            return null;
        }
        return String.format("%s/live/%s/%s/%d.ts",
                credentials.getApiUrl(),
                credentials.getUsername(),
                credentials.getPassword(),
                stream.getStreamId());
    }

    /**
     * Builds an endpoint URL using a template from Constants and
     * XstreamCredentials.
     * 
     * @param template    The endpoint template from Constants
     * @param credentials The credentials object
     * @param args        Any additional arguments for the template
     * @return The formatted URL
     */
    public static String buildEndpointUrl(String template, XstreamCredentials credentials, Object... args) {
        if (credentials == null
                || isNullOrEmpty(credentials.getApiUrl(), credentials.getUsername(), credentials.getPassword())) {
            return null;
        }
        Object[] fullArgs = new Object[3 + (args != null ? args.length : 0)];
        fullArgs[0] = credentials.getApiUrl();
        fullArgs[1] = credentials.getUsername();
        fullArgs[2] = credentials.getPassword();
        if (args != null && args.length > 0) {
            System.arraycopy(args, 0, fullArgs, 3, args.length);
        }
        return String.format(template, fullArgs);
    }

    /**
     * Removes language prefix, pipe/vertical bar, and any preceding space from the
     * movie title.
     * E.g. "EN ▎ Cursed" or "EN | Cursed" -> "Cursed"
     */
    /**
     * Removes language prefix and separators from the title.
     * E.g. "EN ▎ Cursed", "EN | Cursed", "EN - Cursed" -> "Cursed"
     */
    // Common separators used for parsing titles and categories
    public static final String[] SEPARATORS = { " - ", " | ", "|", "▎", " : ", ":" };

    /**
     * Removes language prefix and separators from the title.
     * E.g. "EN ▎ Cursed", "EN | Cursed", "EN - Cursed" -> "Cursed"
     */
    public static String cleanTitle(String name) {
        if (name == null)
            return "";

        int bestIdx = -1;
        int sepLen = 0;

        for (String sep : SEPARATORS) {
            int idx = name.indexOf(sep);
            if (idx != -1 && (bestIdx == -1 || idx < bestIdx)) {
                bestIdx = idx;
                sepLen = sep.length();
            }
        }

        if (bestIdx != -1 && bestIdx + sepLen < name.length()) {
            return name.substring(bestIdx + sepLen).trim();
        }

        return name.trim();
    }

    /**
     * Extracts preface identified by separators.
     * E.g. "EN - Sports" -> "EN", "* - UK" -> "UK"
     */
    @SuppressWarnings("unused") // Used in JsonService and internal logic
    public static String extractPreface(String name) {
        if (name == null)
            return null;

        int bestIdx = -1;

        for (String sep : SEPARATORS) {
            int idx = name.indexOf(sep);
            if (idx != -1 && (bestIdx == -1 || idx < bestIdx)) {
                bestIdx = idx;
            }
        }

        if (bestIdx != -1) {
            String prefix = name.substring(0, bestIdx).trim();
            // Remove leading "*" if present
            if (prefix.startsWith("*")) {
                prefix = prefix.substring(1).trim();
            }
            return (prefix != null && !prefix.isEmpty()) ? prefix.toUpperCase() : null;
        }
        return null;
    }

    // Helper to get includedCountries as a Set<String>
    public static java.util.Set<String> getIncludedCountriesSet(
            ApplicationPropertiesService applicationPropertiesService) {
        String included = applicationPropertiesService.getCurrentProperties().getIncludedCountries();
        if (included == null || included.isBlank())
            return java.util.Collections.emptySet();
        java.util.Set<String> set = new java.util.HashSet<>();
        for (String s : included.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty())
                set.add(trimmed.toUpperCase());
        }
        return set;
    }

    // Helper to check if a name matches includedCountries
    public static boolean isIncluded(String name, java.util.Set<String> includedSet) {
        if (name == null || name.isEmpty())
            return false;

        if (includedSet == null || includedSet.isEmpty())
            return true;

        String preface = extractPreface(name);
        if (preface != null) {
            return includedSet.contains(preface.toUpperCase());
        }

        // If no recognized preface structure is found, we assume it's a global/generic
        // category
        // and include it by default.
        return true;
    }

    public static String printNow() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        return formatter.format(date);
    }
}