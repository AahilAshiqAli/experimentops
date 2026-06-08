package com.experimentops.platformapi.service;

import com.experimentops.common.kafka.KafkaProducer;
import com.experimentops.common.kafka.model.event.ExperimentOpsMetadataEvent;
import com.experimentops.platformapi.model.entity.User;
import com.experimentops.user.event.UserMutationEvent;
import com.experimentops.utils.ExperimentOpsLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExperimentRunService {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(ExperimentRunService.class);

    private final KafkaProducer kafkaProducer;

    @Value("${experiment.run.requested.topic}")
    private String experimentRunRequestTopic;

    public void publishExperimentRunRequest(UserMutationEvent event){
        kafkaProducer.sendMessage(experimentRunRequestTopic, event, event.getMetadata());
    }
}
