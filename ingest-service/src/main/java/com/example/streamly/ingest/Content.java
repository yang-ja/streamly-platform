package com.example.streamly.ingest;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "content")
public class Content {
    @Id
    @Column(nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    private String title;
    private String subject;
    private String grade;
    private String chapter;

    @Enumerated(EnumType.STRING)
    private Status status = Status.UPLOADED;

    @Column(name = "source_object")
    private String sourceObject;

    @Column(name = "created_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public enum Status { UPLOADED, TRANSCODING, READY, FAILED }

}
