package br.com.souza.cqrs_pattern.catalog_service.database.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "processed_events")
public class ProcessedEventDocument {

    @Id
    private String eventId;

    @Indexed
    private String aggregateId;

    private String eventType;

    private Instant eventTimestamp;

    private Instant processedAt;
}
