package com.stock.analyseservice.service;

import com.stock.analyseservice.algo.nlp.CommentClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    @Autowired
    private JavaMailSender sender;

    public static final String from = "18317014390@139.com";
    public static final String to = "zibengou@outlook.com";

    public void send(String title, String content) {
        send(title, content, to);
    }

    public void send(String title, String content, String to) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from); //发送者
        message.setTo(to); //接受者
        message.setSubject(title); //发送标题
        message.setText(content);  //发送内容
        try {
            sender.send(message);
            log.error("send mail success:{}", message);
        } catch (Exception e) {
            log.error("send mail error:{}", message);
        }
    }

}
