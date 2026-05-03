package br.com.souza.cqrs_pattern.music_service.database.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEventEntity {

    @Id
    private String id;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "JSON")
    private String payload;

    @Column(name = "trace_id")
    private String traceId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}