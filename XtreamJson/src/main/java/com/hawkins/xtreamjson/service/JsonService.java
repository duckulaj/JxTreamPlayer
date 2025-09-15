package com.hawkins.xtreamjson.service;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hawkins.xtreamjson.data.LiveCategory;
import com.hawkins.xtreamjson.data.LiveStream;
import com.hawkins.xtreamjson.data.MovieCategory;
import com.hawkins.xtreamjson.data.MovieStream;
import com.hawkins.xtreamjson.data.Series;
import com.hawkins.xtreamjson.data.SeriesCategory;
import com.hawkins.xtreamjson.util.Constants;
import com.hawkins.xtreamjson.util.XstreamCredentials;

@Service
public class JsonService {
    private final XstreamCredentials credentials;

    public JsonService(XstreamCredentials credentials) {
        this.credentials = credentials;
    }

	public void retreiveJsonData() {
        // Example usage:
        System.out.println("API URL: " + credentials.getApiUrl());
        System.out.println("Username: " + credentials.getUsername());
        System.out.println("Password: " + credentials.getPassword());

        URL url = null;
        try {
            url = new URI(Constants.LIVE_CATEGORIES).toURL();
            httpGet(url, "live_categories", credentials);
            List<LiveCategory> liveCategories = readListFromFile("live_categories.json", LiveCategory.class);
            System.out.println("Live Categories: " + liveCategories.size());

            url = new URI(Constants.MOVIE_CATEGORIES).toURL();
            httpGet(url, "movie_categories", credentials);
            List<MovieCategory> movieCategories = readListFromFile("movie_categories.json", MovieCategory.class);
            System.out.println("Movie Categories: " + movieCategories.size());

            url = new URI(Constants.SERIES_CATEGORIES).toURL();
            httpGet(url, "series_categories", credentials);
            List<SeriesCategory> seriesCategories = readListFromFile("movie_categories.json", SeriesCategory.class);
            System.out.println("Series Categories: " + seriesCategories.size());

            url = new URI(Constants.LIVE_STREAMS).toURL();
            httpGet(url, "live_stream", credentials);
            List<LiveStream> liveStreams = readListFromFile("live_stream.json", LiveStream.class);
            System.out.println("Live Streams: " + liveStreams.size());
			
			url = new URI(Constants.MOVIE_STREAMS).toURL();
			httpGet(url, "movie_stream", credentials);
			List<MovieStream> movieStreams = readListFromFile("movie_stream.json", MovieStream.class);
			System.out.println("Movie Streams: " + movieStreams.size());
			
			url = new URI(Constants.SERIES).toURL();
			httpGet(url, "series", credentials);
			List<Series> series = readListFromFile("series.json", Series.class);
			System.out.println("Series: " + series.size());

			url = new URI(Constants.SERIES_BY_CATEGORY).toURL();
			httpGet(url, "series_by_category", credentials);

			url = new URI(Constants.SERIES_INFO).toURL();
			httpGet(url, "series_info", credentials);

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		System.out.println("Finished");

	}
	
	private static void httpGet(URL url, String type, XstreamCredentials credentials) {
	    String json = "";
	    java.net.HttpURLConnection connection = null;
	    int responseCode = -1;
	    try {
	        connection = (java.net.HttpURLConnection) url.openConnection();
	        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
	        connection.setConnectTimeout(10000);
	        connection.setReadTimeout(30000);
	        // Add Basic Auth header if credentials are present
	        if (credentials.getUsername() != null && credentials.getPassword() != null) {
	            String auth = credentials.getUsername() + ":" + credentials.getPassword();
	            String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
	            connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
	        }
	        responseCode = connection.getResponseCode();
	        if (responseCode != 200) {
	            String msg = "HTTP error: " + responseCode + " - " + connection.getResponseMessage();
	            if (responseCode == 513) {
	                msg += " (Custom: Possible authentication or server-side error)";
	            }
	            System.err.println("Request to " + url + " failed. " + msg);
	            return;
	        }
	        try (java.io.InputStream is = connection.getInputStream();
	             java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A")) {
	            json = s.hasNext() ? s.next() : "";
	        }
	        // Write JSON to file using type as prefix and .json as suffix
	        try (java.io.FileWriter file = new java.io.FileWriter(type + ".json")) {
	            javax.json.JsonReader jsonReader = javax.json.Json.createReader(new java.io.StringReader(json));
	            javax.json.JsonStructure jsonStructure = jsonReader.read();
	            javax.json.JsonWriterFactory writerFactory = javax.json.Json.createWriterFactory(
	                java.util.Map.of(javax.json.stream.JsonGenerator.PRETTY_PRINTING, true)
	            );
	            javax.json.JsonWriter jsonWriter = writerFactory.createWriter(file);
	            if (jsonStructure.getValueType() == javax.json.JsonValue.ValueType.OBJECT) {
	                jsonWriter.writeObject(jsonStructure.asJsonObject());
	            } else if (jsonStructure.getValueType() == javax.json.JsonValue.ValueType.ARRAY) {
	                jsonWriter.writeArray(jsonStructure.asJsonArray());
	            }
	            jsonWriter.close();
	        }
	    } catch (Exception e) {
	        System.err.println("Exception during HTTP GET to " + url + ": " + e.getMessage());
	        e.printStackTrace();
	    } finally {
	        if (connection != null) {
	            connection.disconnect();
	        }
	    }
	}

	public static <T> List<T> readListFromFile(String filePath, Class<T> clazz) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(
					Paths.get(filePath).toFile(),
					mapper.getTypeFactory().constructCollectionType(List.class, clazz)
					);
		} catch (Exception e) {
			e.printStackTrace();
			return java.util.Collections.emptyList();
		}
	}

    public static <T, R extends JpaRepository<T, ?>> void persistList(R repository, List<T> entities) {
        repository.saveAll(entities);
    }



}