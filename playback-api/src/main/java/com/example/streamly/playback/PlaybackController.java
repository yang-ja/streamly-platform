package com.example.streamly.playback;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/contents")
public class PlaybackController {

    private final MinioClient minio;
    private final String bucket;

    public PlaybackController(MinioClient minio, @Value("${minio.bucket}") String bucket) {
        this.minio = minio;
        this.bucket = bucket;
    }

    @GetMapping("/{id}/play")
    public ResponseEntity<?> getPlay(@PathVariable String id) throws Exception {
        String key = "hls/" + id + "/index.m3u8";
        String url = minio.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder().bucket(bucket).object(key).method(Method.GET).build());
        return ResponseEntity.ok(Map.of("hls", url));
    }
}
