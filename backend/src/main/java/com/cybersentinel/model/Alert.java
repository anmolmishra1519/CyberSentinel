package com.cybersentinel.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A lightweight alert record, one per non-Normal threat, used to drive the
 * live threat feed and dashboard counters.
 * Collection: alerts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "alerts")
public class Alert {

    @Id
    private String id;

    private String threatId;
    private String threatType;
    private String severity;
    private String status;      // OPEN | ACKNOWLEDGED | RESOLVED
    private Instant createdAt;
}
