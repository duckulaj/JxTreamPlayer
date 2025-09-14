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

@Service
public class JsonService {

	public void retreiveJsonData() {

		URL url = null;
		try {
			url = new URI(Constants.LIVE_CATEGORIES).toURL();
			httpGet(url, "live_categories");
			List<LiveCategory> liveCategories = readListFromFile("live_categories.json", LiveCategory.class);
			System.out.println("Live Categories: " + liveCategories.size());

			url = new URI(Constants.MOVIE_CATEGORIES).toURL();
			httpGet(url, "movie_categories");
			List<MovieCategory> movieCategories = readListFromFile("movie_categories.json", MovieCategory.class);
			System.out.println("Movie Categories: " + movieCategories.size());


			url = new URI(Constants.SERIES_CATEGORIES).toURL();
			httpGet(url, "series_categories");
			List<SeriesCategory> seriesCategories = readListFromFile("movie_categories.json", SeriesCategory.class);
			System.out.println("Series Categories: " + seriesCategories.size());

			url = new URI(Constants.LIVE_STREAMS).toURL();
			httpGet(url, "live_stream");
			List<LiveStream> liveStreams = readListFromFile("live_stream.json", LiveStream.class);
			System.out.println("Live Streams: " + liveStreams.size());
			
			url = new URI(Constants.MOVIE_STREAMS).toURL();
			httpGet(url, "movie_stream");
			List<MovieStream> movieStreams = readListFromFile("movie_stream.json", MovieStream.class);
			System.out.println("Movie Streams: " + movieStreams.size());
			
			url = new URI(Constants.SERIES).toURL();
			httpGet(url, "series");
			List<Series> series = readListFromFile("series.json", Series.class);
			System.out.println("Series: " + series.size());

			url = new URI(Constants.SERIES_BY_CATEGORY).toURL();
			httpGet(url, "series_by_category");

			url = new URI(Constants.SERIES_INFO).toURL();
			httpGet(url, "series_info");

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Finished");

	}
	
	private static void httpGet(URL url, String type) {
		String json = "";
		URLConnection connection;

		try {
			connection = url.openConnection();
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");
			connection.connect();
			java.io.InputStream is = connection.getInputStream();
			java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
			json = s.hasNext() ? s.next() : "";


			// Write JSON to file using type as prefix and .json as suffix
			java.io.FileWriter file = new java.io.FileWriter(type + ".json");

			// Parse JSON using native Java
			javax.json.JsonReader jsonReader = javax.json.Json.createReader(new java.io.StringReader(json));
			javax.json.JsonStructure jsonStructure = jsonReader.read();

			javax.json.JsonWriterFactory writerFactory = javax.json.Json.createWriterFactory(
					java.util.Map.of(javax.json.stream.JsonGenerator.PRETTY_PRINTING, true)
					);
			javax.json.JsonWriter jsonWriter = writerFactory.createWriter(file);

			// Handle both JSON objects and arrays
			if (jsonStructure.getValueType() == javax.json.JsonValue.ValueType.OBJECT) {
				jsonWriter.writeObject(jsonStructure.asJsonObject());
			} else if (jsonStructure.getValueType() == javax.json.JsonValue.ValueType.ARRAY) {
				jsonWriter.writeArray(jsonStructure.asJsonArray());
			}

			jsonWriter.close();


		} catch (Exception e) {
			e.printStackTrace();
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
