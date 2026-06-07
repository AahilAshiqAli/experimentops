package com.experimentops.platformapi.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class EmailConfig {

    @Bean
    public JavaMailSender javaMailSender(
            @Value("${experimentops.mail.host}") String host,
            @Value("${experimentops.mail.port}") int port,
            @Value("${experimentops.mail.username:}") String username,
            @Value("${experimentops.mail.password:}") String password,
            @Value("${experimentops.mail.auth:false}") boolean auth,
            @Value("${experimentops.mail.ssl:false}") boolean ssl,
            @Value("${experimentops.mail.start-tls:false}") boolean startTls) {

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);

        if (StringUtils.isNotBlank(username)) {
            mailSender.setUsername(username);
            mailSender.setPassword(password);
        }

        Properties properties = mailSender.getJavaMailProperties();
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.auth", Boolean.toString(auth));
        properties.put("mail.smtp.ssl.enable", Boolean.toString(ssl));
        properties.put("mail.smtp.starttls.enable", Boolean.toString(startTls));

        return mailSender;
    }
}
