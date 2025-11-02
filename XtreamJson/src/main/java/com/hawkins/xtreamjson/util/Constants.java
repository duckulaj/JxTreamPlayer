package com.hawkins.xtreamjson.util;

/**
 * Holds endpoint templates for Xtream Codes API. All templates require credentials and base URL to be provided at runtime.
 */
public class Constants {
    public static final String LIVE_CATEGORIES = "%s/player_api.php?username=%s&password=%s&action=get_live_categories";
    public static final String MOVIE_CATEGORIES = "%s/player_api.php?username=%s&password=%s&action=get_vod_categories";
    public static final String SERIES_CATEGORIES = "%s/player_api.php?username=%s&password=%s&action=get_series_categories";
    public static final String LIVE_STREAMS = "%s/player_api.php?username=%s&password=%s&action=get_live_streams";
    public static final String MOVIE_STREAMS = "%s/player_api.php?username=%s&password=%s&action=get_vod_streams";
    public static final String SERIES = "%s/player_api.php?username=%s&password=%s&action=get_series";
    public static final String SERIES_BY_CATEGORY = "%s/player_api.php?username=%s&password=%s&action=get_series&category_id=%s";
    public static final String SERIES_INFO = "%s/player_api.php?username=%s&password=%s&action=get_series_info&series_id=%s";
    public static final String SHOW_INFO = "%s/player_api.php?username=%s&password=%s&action=get_show&series_id=%s";
    public static final String EPISODES_BY_SEASON = "%s/player_api.php?username=%s&password=%s&action=get_episodes&season_id=%s";
    public static final String EPG_XML = "%s/xmltv.php?username=%s&password=%s";
}