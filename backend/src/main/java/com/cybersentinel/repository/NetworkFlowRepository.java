package com.cybersentinel.repository;

import com.cybersentinel.model.NetworkFlow;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface NetworkFlowRepository extends MongoRepository<NetworkFlow, String> {

    List<NetworkFlow> findByDestinationIPAndTimestampAfter(String destinationIP, Instant after);

    List<NetworkFlow> findBySourceIPAndTimestampAfter(String sourceIP, Instant after);

    List<NetworkFlow> findBySourceIPAndDestinationIPAndDestinationPortAndTimestampAfter(
            String sourceIP, String destinationIP, int destinationPort, Instant after);

    long countByTimestampAfter(Instant after);
}
