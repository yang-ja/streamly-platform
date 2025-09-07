package com.example.streamly.ingest;

import io.minio.*;
import jakarta.validation.constraints.NotBlank;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

@RestController
@RequestMapping("/v1/contents")
@Validated
public class UploadController {

    private final ContentRepository repo;
    private final MinioClient minio;
    private final KafkaTemplate<String, String> kafka;
    private final String bucket;

    public UploadController(ContentRepository repo, MinioClient minio, KafkaTemplate<String, String> kafka,
                            @Value("${minio.bucket}") String bucket) {
        this.repo = repo;
        this.minio = minio;
        this.kafka = kafka;
        this.bucket = bucket;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> upload(
            @RequestParam @NotBlank String title,
            @RequestParam String subject,
            @RequestParam String grade,
            @RequestParam String chapter,
            @RequestPart("file") MultipartFile file) throws Exception {

        Content c = new Content();
        c.setTitle(title);
        c.setSubject(subject);
        c.setGrade(grade);
        c.setChapter(chapter);

        String key = "uploads/" + c.getId() + "/source.mp4";
        try (InputStream is = file.getInputStream()) {
            minio.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(is, file.getSize(), -1)
                    .contentType(file.getContentType() != null ? file.getContentType() : "video/mp4")
                    .build());
        }

        c.setSourceObject(key);
        repo.save(c);

        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                Map.of("contentId", c.getId(), "bucket", bucket, "objectKey", key));
        kafka.send(new ProducerRecord<>("transcode.requested", c.getId(), json));

        return ResponseEntity.accepted().body(Map.of("id", c.getId(), "status", c.getStatus().name()));
    }
}
