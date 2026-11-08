package com.cybersentinel.service;

import com.cybersentinel.model.NetworkFlow;
import com.cybersentinel.repository.NetworkFlowRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Computes the same "contextual/aggregate" features the ML model was
 * trained on (dstConnCount60s, uniqueDstPorts60s, repeatConnSameDstPort60s)
 * by looking at flows ALREADY stored in MongoDB in the last 60 seconds -
 * exactly like the offline dataset generator did. This keeps train/serve
 * behaviour consistent and is why the backend (which owns the flow store)
 * computes these fields rather than the stateless ML service.
 */
@Service
public class FlowContextService {

    private static final long WINDOW_SECONDS = 60;

    private final NetworkFlowRepository networkFlowRepository;

    public FlowContextService(NetworkFlowRepository networkFlowRepository) {
        this.networkFlowRepository = networkFlowRepository;
    }

    public void enrichWithContext(NetworkFlow flow) {
        Instant windowStart = flow.getTimestamp().minus(WINDOW_SECONDS, ChronoUnit.SECONDS);

        List<NetworkFlow> toSameDestination =
                networkFlowRepository.findByDestinationIPAndTimestampAfter(flow.getDestinationIP(), windowStart);
        flow.setDstConnCount60s(toSameDestination.size());

        List<NetworkFlow> fromSameSource =
                networkFlowRepository.findBySourceIPAndTimestampAfter(flow.getSourceIP(), windowStart);
        Set<Integer> distinctPorts = fromSameSource.stream()
                .map(NetworkFlow::getDestinationPort)
                .collect(Collectors.toSet());
        flow.setUniqueDstPorts60s(distinctPorts.size());

        List<NetworkFlow> repeatToSameDestPort =
                networkFlowRepository.findBySourceIPAndDestinationIPAndDestinationPortAndTimestampAfter(
                        flow.getSourceIP(), flow.getDestinationIP(), flow.getDestinationPort(), windowStart);
        flow.setRepeatConnSameDstPort60s(repeatToSameDestPort.size());
    }
}
