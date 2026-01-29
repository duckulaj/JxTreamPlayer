package com.hawkins.xtreamjson.scheduling;

import com.hawkins.xtreamjson.service.JsonService;
import com.hawkins.xtreamjson.service.StrmService;
import com.hawkins.xtreamjson.service.PlaylistService;
import com.hawkins.xtreamjson.util.XtreamCodesUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.io.IOException;

@Service
@Slf4j
public class ScheduledTasksService {
    private final JsonService jsonService;
    private final StrmService strmService;
    private final PlaylistService playlistService;

    public ScheduledTasksService(JsonService jsonService, StrmService strmService, PlaylistService playlistService) {
        this.jsonService = jsonService;
        this.strmService = strmService;
        this.playlistService = playlistService;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void runDailyTasks() {
        jsonService.retreiveJsonData();
        log.info("Scheduled Task retreiveJsonData completed at {}", XtreamCodesUtils.printNow());
        try {
            strmService.generateAllStrmFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("Scheduled Task generateAllStrmFiles completed at {}", XtreamCodesUtils.printNow());
    }

    @Scheduled(cron = "0 0 */6 * * *")
    public void runPlaylistUpdateTask() {
        playlistService.generateFullLibraryPlaylist();
        log.info("Scheduled Task generateFullLibraryPlaylist completed at {}", XtreamCodesUtils.printNow());
    }
}
