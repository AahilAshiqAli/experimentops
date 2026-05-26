package com.experimentops.common.kafka;

import com.experimentops.common.kafka.constant.KafkaConstants;
import com.experimentops.common.kafka.utils.ExperimentOpsMetadataUtil;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import com.simplifi.common.kafka.model.event.ExperimentOpsMetadataEvent;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static com.experimentops.common.kafka.constant.KafkaConstants.NOT_AVAILABLE;
import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class KafkaProducer {

    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(KafkaProducer.class);
    private static final String PUBLISHED_MESSAGE = "Published message | topic: %s | message: %s";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public KafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String topic, Object eventMessage, ExperimentOpsMetadataEvent metadataEvent) {
        ExperimentOpsHeaders headers = ExperimentOpsMetadataUtil.extractHeaders(metadataEvent);

        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(topic, metadataEvent.getUuid(), eventMessage);
        attachHeaders(producerRecord, metadataEvent);

        kafkaTemplate.send(producerRecord);
        log.info(headers, String.format(PUBLISHED_MESSAGE, topic, eventMessage));
    }

    private void attachHeaders(ProducerRecord<String, Object> producerRecord,
                               ExperimentOpsMetadataEvent metadataEvent) {
        String requestUuid   = StringUtils.defaultIfBlank(metadataEvent.getTraceUuid(), NOT_AVAILABLE);
        String workspaceUuid = StringUtils.defaultIfBlank(metadataEvent.getWorkspaceUuid(), NOT_AVAILABLE);
        String publisherClass = StringUtils.defaultIfBlank(metadataEvent.getClassName(), NOT_AVAILABLE);

        producerRecord.headers().add(KafkaConstants.REQUEST_UUID,    requestUuid.getBytes(UTF_8));
        producerRecord.headers().add(KafkaConstants.WORKSPACE_UUID,  workspaceUuid.getBytes(UTF_8));
        producerRecord.headers().add(KafkaConstants.PUBLISHER_CLASS, publisherClass.getBytes(UTF_8));
    }
}