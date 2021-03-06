package com.stock.analyseservice.service;

import com.stock.analyseservice.algo.nlp.CommentClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Arrays;
import java.util.List;

@Component
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    @Autowired
    private JavaMailSender sender;

    public static final String from = "18317014390@139.com";
    public static final String to = "zibengou@outlook.com";

    public void send(String title, String content) {
        send(title, content, Arrays.asList(to));
    }

    public void send(String title, List<String> content) throws MessagingException {
        send(title, content, Arrays.asList(to));
    }

    public void send(String title, String content, List<String> to) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from); //发送者
        message.setTo(to.toArray(new String[to.size()])); //接受者
        message.setSubject(title); //发送标题
        message.setText(content);  //发送内容
        try {
            sender.send(message);
            log.error("send mail success:{}", message);
        } catch (Exception e) {
            log.error("send mail error:{}", e.getMessage());
        }
    }

    public void send(String title, List<String> contents, List<String> to) throws MessagingException {
        MimeMessage msg = sender.createMimeMessage();
        MimeMessageHelper message = new MimeMessageHelper(msg, true);
        message.setFrom(from); //发送者
        message.setTo(to.toArray(new String[to.size()])); //接受者
        message.setSubject(title); //发送标题
        StringBuilder sb = new StringBuilder();
        sb.append("<html>").append("<body>");
        for (String line : contents) {
            sb.append(line).append("<br />");
        }
        sb.append("</body>").append("</html>");
        message.setText(sb.toString(), true);  //发送内容
        try {
            sender.send(msg);
            log.error("send mail success:{}", msg);
        } catch (Exception e) {
            log.error("send mail error:{}", e.getMessage());
        }
    }

}
