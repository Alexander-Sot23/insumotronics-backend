package com.alexander.springboot.insumotronics.service.email.impl;

import com.alexander.springboot.insumotronics.service.email.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Override
    public void sendConfirmPurchase(String to, String username, String message) throws MessagingException {
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("message", message);

        String htmlContent = templateEngine.process("confirmation-purchase-email", context);

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject("Confirmación de pedido - Insumotronics");
        helper.setText(htmlContent, true);

        javaMailSender.send(mimeMessage);
    }

    @Override
    public void sendCancelPurchase(String to, String username, String message) throws MessagingException {
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("message", message);


        String htmlContent = templateEngine.process("cancelation-purchase-email", context);

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject("Cancelación de pedido - Insumotronics");
        helper.setText(htmlContent, true);

        javaMailSender.send(mimeMessage);
    }
}

