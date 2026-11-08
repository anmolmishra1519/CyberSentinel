package com.cybersentinel.repository;

import com.cybersentinel.model.Threat;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface ThreatRepository extends MongoRepository<Threat, String> {

    List<Threat> findAllByOrderByDetectionTimeDesc();

    List<Threat> findByThreatTypeOrderByDetectionTimeDesc(String threatType);

    List<Threat> findBySeverityOrderByDetectionTimeDesc(String severity);

    long countByThreatTypeNot(String threatType);

    long countByThreatType(String threatType);

    long countBySeverity(String severity);

    long countByDetectionTimeAfter(Instant after);

    List<Threat> findTop20ByOrderByDetectionTimeDesc();
}
