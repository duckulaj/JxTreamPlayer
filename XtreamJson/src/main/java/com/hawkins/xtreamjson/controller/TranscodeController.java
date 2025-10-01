package com.hawkins.xtreamjson.controller;

import java.io.InputStream;
import java.io.OutputStream;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;

@RestController
public class TranscodeController {
    @GetMapping(value = "/transcode", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void transcodeTsToMp4(@RequestParam("url") String tsUrl, HttpServletResponse response) {
        response.setContentType("video/mp4");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Accept-Ranges", "bytes");
        Process process = null;
        try {
            // Try hardware NVENC first, fallback to libx264 if it fails
            String[] nvencCmd = {
                "ffmpeg",
                "-fflags", "+nobuffer+genpts+discardcorrupt",
                "-err_detect", "ignore_err",
                "-nostdin",
                "-loglevel", "error",
                "-i", tsUrl,
                "-c:v", "h264_nvenc",
                "-preset", "fast",
                "-g", "50",
                "-c:a", "aac",
                "-f", "mp4",
                "-movflags", "+frag_keyframe+empty_moov+default_base_moof",
                "-"
            };
            ProcessBuilder pb = new ProcessBuilder(nvencCmd); // No error file redirection
            process = pb.start();
            // boolean usedNvenc = true;
            try (InputStream in = process.getInputStream(); OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                    out.flush();
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                // usedNvenc = false;
                String[] x264Cmd = {
                    "ffmpeg",
                    "-fflags", "+nobuffer+genpts+discardcorrupt",
                    "-err_detect", "ignore_err",
                    "-nostdin",
                    "-loglevel", "error",
                    "-i", tsUrl,
                    "-c:v", "libx264",
                    "-preset", "veryfast",
                    "-g", "50",
                    "-c:a", "aac",
                    "-f", "mp4",
                    "-movflags", "+frag_keyframe+empty_moov+default_base_moof",
                    "-tune", "zerolatency",
                    "-"
                };
                pb = new ProcessBuilder(x264Cmd); // No error file redirection
                process = pb.start();
                try (InputStream in = process.getInputStream(); OutputStream out = response.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                        out.flush();
                    }
                }
                int x264Exit = process.waitFor();
                if (x264Exit != 0) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (process != null) process.destroy();
        }
    }
}