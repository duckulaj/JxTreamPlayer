package com.hawkins.xtreamjson.data;

import java.util.List;

import lombok.Data;

@Data
public class SeriesInformationToSave {

	public List<SeriesCategory> categories;
	public List<Series> series;
	public List<Season> seasons;
	public List<Episode> episodes;

}
