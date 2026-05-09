package com.alexander.springboot.insumotronics.service.email.impl;

import com.alexander.springboot.insumotronics.service.email.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Value("${spring.mail.from:noreply@insumotronics.com}")
    private String defaultFrom;

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    public EmailServiceImpl(
            JavaMailSender javaMailSender,
            TemplateEngine templateEngine,
            @Value("${spring.mail.host}") String mailHost,
            @Value("${spring.mail.port}") String mailPort,
            @Value("${spring.mail.username}") String mailUsername,
            @Value("${spring.mail.password}") String mailPassword) {
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public void sendConfirmPurchase(String to, String username, String message) {
        sendEmail(to, "Confirmación de pedido - Insumotronics",
                "confirmation-purchase-email", username, message);
    }

    @Override
    public void sendCancelPurchase(String to, String username, String message) {
        sendEmail(to, "Cancelación de pedido - Insumotronics",
                "cancelation-purchase-email", username, message);
    }

    /**
     * Método genérico para evitar código duplicado
     */
    private void sendEmail(String to, String subject, String templateName,
                           String username, String message) {
        try {
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("message", message);

            String htmlContent = templateEngine.process(templateName, context);

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(defaultFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);

            log.info("Correo enviado correctamente a: {}", to);

        } catch (MessagingException e) {
            log.error("Error al enviar correo a: {} | Asunto: {}", to, subject, e);
            throw new RuntimeException("No se pudo enviar el correo: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error inesperado enviando correo a: {}", to, e);
            throw new RuntimeException("Error inesperado enviando correo", e);
        }
    }
}