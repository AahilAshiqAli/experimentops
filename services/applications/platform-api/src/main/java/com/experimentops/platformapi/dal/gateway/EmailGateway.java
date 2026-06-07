package com.experimentops.platformapi.dal.gateway;

import com.experimentops.common.exceptions.constant.ErrorCode;
import com.experimentops.common.exceptions.runtime.ValidationException;
import com.experimentops.utils.ExperimentOpsLogger;
import com.experimentops.utils.dto.ExperimentOpsHeaders;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailGateway {
    private static final ExperimentOpsLogger log = ExperimentOpsLogger.getLogger(EmailGateway.class);

    private final JavaMailSender mailSender;

    @Value("${experimentops.mail.from}")
    private String from;

    public void sendSimpleNotification(String to, String subject, String body, ExperimentOpsHeaders headers) {
        if (StringUtils.isAnyBlank(to, subject, body)) {
            throw new ValidationException(ErrorCode.REQUIRED_FIELD_MISSING, "to, subject and body are required");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
            log.info(headers, "Sent email notification to: " + to);
        } catch (MailException exception) {
            log.error(headers, "Unable to send email notification to: " + to, exception);
            throw new ValidationException(ErrorCode.GENERAL_ERROR, "Unable to send email notification");
        }
    }
}
