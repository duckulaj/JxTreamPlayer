package com.hawkins.xtreamjson.util;

public class Constants {

	public static final String API_URL = "http://line.smart-ultra.cc";  
	public static final String USERNAME = "5C2974";
	public static final String PASSWORD = "6A7602";


	public static final String LIVE_CATEGORIES = API_URL + "/player_api.php?username=" + USERNAME + "&password=" + PASSWORD + "&action=get_live_categories";
	public static final String MOVIE_CATEGORIES = API_URL + "/player_api.php?username=" + USERNAME + "&password=" + PASSWORD + "&action=get_vod_categories";
	public static final String SERIES_CATEGORIES = API_URL + "/player_api.php?username=" + USERNAME + "&password=" + PASSWORD + "&action=get_series_categories";
	public static final String LIVE_STREAMS = API_URL + "/player_api.php?username=" + USERNAME + "&password=" + PASSWORD + "&action=get_live_streams";
	public static final String MOVIE_STREAMS = API_URL + "/player_api.php?username=" + USERNAME + "&password=" + PASSWORD + "&action=get_vod_streams";
	public static final String SERIES = API_URL + "/player_api.php?username=" + USERNAME + "&password=" + PASSWORD + "&action=get_series";
	public static final String SERIES_BY_CATEGORY = API_URL + "/player_api.php?username=" + USERNAME + "&password=" + PASSWORD + "&action=get_series&category_id=138";
	public static final String SERIES_INFO = API_URL + "/player_api.php?username=" + USERNAME + "&password=" + PASSWORD + "&action=get_series_info&series_id=2374";

}
