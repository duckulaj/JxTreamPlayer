package com.hawkins.xtreamjson.service;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
@Service
public class TranscodingService {
    private static final Logger log = LoggerFactory.getLogger(TranscodingService.class);
    
    private static class SessionInfo {
        Process process;
        File tempDir;
    }
    private final ConcurrentMap<String, SessionInfo> sessionMap = new ConcurrentHashMap<>();

    /**
     * Start HLS transcoding for a session. If already started, does nothing.
     * Now supports encoder selection and fallback to libx264 if h264_nvenc fails.
     */
    public synchronized void startHlsTranscoding(String sessionId, String filePath, Integer seekSeconds, String encoder) throws IOException {
        if (sessionMap.containsKey(sessionId)) return;
        File tempDir = Files.createTempDirectory("hls-" + sessionId + "-").toFile();
        String playlist = new File(tempDir, "playlist.m3u8").getAbsolutePath();
        String ffmpeg = "ffmpeg";
        String selectedEncoder = encoder != null ? encoder : "h264_nvenc";
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(
            ffmpeg,
            "-hide_banner",
            "-hwaccel", "cuda",
            (seekSeconds != null && seekSeconds > 0) ? "-ss" : null,
            (seekSeconds != null && seekSeconds > 0) ? String.valueOf(seekSeconds) : null,
            "-i", filePath,
            "-c:v", selectedEncoder,
            "-c:a", "aac",
            "-preset", "fast",
            "-f", "hls",
            "-hls_time", "4",
            "-hls_list_size", "6",
            "-hls_flags", "delete_segments+append_list",
            playlist
        );
        pb.command().removeIf(s -> s == null);
        pb.redirectErrorStream(true);
        pb.directory(tempDir);
        log.info("Starting ffmpeg for session {}: {}", sessionId, String.join(" ", pb.command()));
        log.info("Temp dir for session {}: {}", sessionId, tempDir.getAbsolutePath());
        // Use a final array to allow mutation in inner class
        final Process[] processHolder = new Process[1];
        processHolder[0] = pb.start();
        // Capture ffmpeg output to a log file
        File logFile = new File(tempDir, "ffmpeg.log");
        Thread logThread = new Thread(() -> {
            try (InputStream in = processHolder[0].getInputStream(); FileOutputStream out = new FileOutputStream(logFile)) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
            } catch (IOException e) {
                log.error("Error capturing ffmpeg output for session {}", sessionId, e);
            }
        });
        logThread.start();
        // Wait briefly to check if ffmpeg fails immediately (e.g., due to encoder error)
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        if (!processHolder[0].isAlive() || logFile.length() > 0) {
            String logContent = new String(Files.readAllBytes(logFile.toPath()));
            if (logContent.contains("10 bit encode not supported") || logContent.contains("Error while opening encoder") || logContent.contains("Provided device doesn't support required NVENC features")) {
                log.warn("h264_nvenc failed for session {}. Falling back to libx264.", sessionId);
                processHolder[0].destroyForcibly();
                // Try again with libx264
                pb.command().set(pb.command().indexOf(selectedEncoder), "libx264");
                log.info("Restarting ffmpeg for session {}: {}", sessionId, String.join(" ", pb.command()));
                processHolder[0] = pb.start();
                // Start new log thread for fallback, append to log
                new Thread(() -> {
                    try (InputStream in = processHolder[0].getInputStream(); FileOutputStream out = new FileOutputStream(logFile, true)) {
                        byte[] buf = new byte[4096];
                        int len;
                        while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
                    } catch (IOException e) {
                        log.error("Error capturing ffmpeg output for session {} (fallback)", sessionId, e);
                    }
                }).start();
                selectedEncoder = "libx264";
            }
        }
        SessionInfo info = new SessionInfo();
        info.process = processHolder[0];
        info.tempDir = tempDir;
        sessionMap.put(sessionId, info);
        log.info("Transcoding started for session {} using encoder {}", sessionId, selectedEncoder);
    }

    // Overload for backward compatibility
    public synchronized void startHlsTranscoding(String sessionId, String filePath, Integer seekSeconds) throws IOException {
        startHlsTranscoding(sessionId, filePath, seekSeconds, null);
    }

    public File getPlaylistFile(String sessionId) {
        SessionInfo info = sessionMap.get(sessionId);
        if (info == null) return null;
        return new File(info.tempDir, "playlist.m3u8");
    }
    public File getSegmentFile(String sessionId, String segment) {
        SessionInfo info = sessionMap.get(sessionId);
        if (info == null) return null;
        return new File(info.tempDir, segment);
    }
    public File getLogFile(String sessionId) {
        SessionInfo info = sessionMap.get(sessionId);
        if (info == null) return null;
        return new File(info.tempDir, "ffmpeg.log");
    }

    public synchronized void stopTranscoding(String sessionId) {
        SessionInfo info = sessionMap.remove(sessionId);
        if (info != null) {
            if (info.process != null && info.process.isAlive()) {
                info.process.destroyForcibly();
            }
            if (info.tempDir != null && info.tempDir.exists()) {
                for (File f : info.tempDir.listFiles()) f.delete();
                info.tempDir.delete();
            }
        }
    }
}