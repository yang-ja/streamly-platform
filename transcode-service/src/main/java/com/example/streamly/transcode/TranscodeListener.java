package com.example.streamly.transcode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;

@Service
public class TranscodeListener {
    private static final Logger log = LoggerFactory.getLogger(TranscodeListener.class);
    private final MinioClient minio;
    private final String bucket;
    private final ObjectMapper mapper = new ObjectMapper();

    public TranscodeListener(MinioClient minio, @Value("${minio.bucket}") String bucket) {
        this.minio = minio;
        this.bucket = bucket;
    }

    @KafkaListener(topics = "transcode.requested", groupId = "transcode")
    public void handle(@Payload String message) {
        try {
            JsonNode node = mapper.readTree(message);
            String id = node.get("contentId").asText();
            String objectKey = node.get("objectKey").asText();

            Path work = Files.createTempDirectory("ffmpeg-" + id + "-");
            Path input = work.resolve("input.mp4");
            Path outDir = work.resolve("hls");
            Files.createDirectories(outDir);

            try (InputStream is = minio.getObject(GetObjectArgs.builder().bucket(bucket).object(objectKey).build())) {
                Files.copy(is, input, StandardCopyOption.REPLACE_EXISTING);
            }

            Path playlist = outDir.resolve("index.m3u8");
            Process p = new ProcessBuilder(
                    "ffmpeg", "-y", "-i", input.toString(),
                    "-vf", "scale=-2:720",
                    "-c:v", "libx264", "-preset", "veryfast",
                    "-c:a", "aac", "-b:a", "128k",
                    "-hls_time", "4", "-hls_playlist_type", "vod",
                    "-f", "hls", playlist.toString()
            ).redirectErrorStream(true).start();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) log.info(line);
            }
            int exit = p.waitFor();
            if (exit != 0) throw new RuntimeException("ffmpeg failed");

            try (DirectoryStream<Path> ds = Files.newDirectoryStream(outDir)) {
                for (Path f : ds) {
                    if (Files.isRegularFile(f)) {
                        String key = "hls/" + id + "/" + f.getFileName();
                        String ct = f.getFileName().toString().endsWith(".m3u8") ?
                                "application/vnd.apple.mpegurl" : "video/MP2T";
                        try (InputStream fis = Files.newInputStream(f)) {
                            minio.putObject(PutObjectArgs.builder()
                                    .bucket(bucket)
                                    .object(key)
                                    .stream(fis, Files.size(f), -1)
                                    .contentType(ct)
                                    .build());
                        }
                    }
                }
            }
            log.info("Transcode complete for {}", id);
        } catch (Exception e) {
            log.error("Transcode error", e);
        }
    }
}
