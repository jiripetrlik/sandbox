package com.redhat.service.smartevents.manager;

import java.util.List;

import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryInfo;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.api.models.responses.ProcessorResponse;
import com.redhat.service.smartevents.manager.models.Processor;

public interface ProcessorService {

    Processor getProcessor(String bridgeId, String processorId, String customerId);

    Processor createProcessor(String bridgeId, String customerId, ProcessorRequest processorRequest);

    Processor updateProcessor(String bridgeId, String processorId, String customerId, ProcessorRequest processorRequest);

    List<Processor> findByShardIdWithReadyDependencies(String shardId);

    Processor updateProcessorStatus(ProcessorDTO processorDTO);

    Long getProcessorsCount(String bridgeId, String customerId);

    ListResult<Processor> getProcessors(String bridgeId, String customerId, QueryInfo queryInfo);

    void deleteProcessor(String bridgeId, String processorId, String customerId);

    ProcessorDTO toDTO(Processor processor);

    ProcessorResponse toResponse(Processor processor);
}
