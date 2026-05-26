package com.experimentops.common.kafka.config;


import com.experimentops.common.kafka.utils.DLTExceptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {
    @Value("#{new Long('${kafka.consumer.max.retries}')}")
    private Long kafkaConsumerMaxRetries;
    @Value("#{new Long('${kafka.consumer.retry.interval}')}")
    private Long kafkaConsumerRetryInterval;

    @Bean
    public DefaultErrorHandler errorHandler(DeadLetterPublishingRecoverer deadLetterPublishingRecoverer) {
        BackOff backOff = new FixedBackOff(kafkaConsumerRetryInterval, kafkaConsumerMaxRetries);
        return new DefaultErrorHandler(deadLetterPublishingRecoverer, backOff);
    }

    @Bean
    public DeadLetterPublishingRecoverer publisher(KafkaOperations<String, Object> kafkaOperations) {
        return DLTExceptionUtil.getDeadLetterPublishingRecoverer(kafkaOperations);
    }
}

