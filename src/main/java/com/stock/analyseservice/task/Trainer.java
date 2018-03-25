package com.stock.analyseservice.task;

import com.stock.analyseservice.algo.ClassifyStock;
import com.stock.analyseservice.algo.nlp.CommentClassifier;
import com.stock.analyseservice.controller.PredictController;
import com.stock.analyseservice.controller.TrainController;
import com.stock.analyseservice.dao.domain.StockComment;
import com.stock.analyseservice.dao.domain.StockUser;
import com.stock.analyseservice.dao.repository.StockCommentRepository;
import com.stock.analyseservice.dao.repository.StockUserRepository;
import com.stock.analyseservice.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class Trainer {

    Logger log = LoggerFactory.getLogger(Trainer.class);

    private CommentClassifier commentClassifier = CommentClassifier.newInstance();

    @Autowired
    private StockCommentRepository commentRepository;

    @Autowired
    private MailService mailService;

    @Autowired
    private TrainController trainController;

    @Autowired
    private PredictController predictController;

    @Autowired
    private StockUserRepository userRepository;


    /***
     * 每天更新一次评论测评模型
     */
//    @Scheduled
    public void CommentTrainer() {
        List<StockComment> comments = commentRepository.findByEditedIsTrueAndLabelIn(Arrays.asList("up", "down"));
        Map<String, List<String>> contentWithLables = new LinkedHashMap<>();
        for (StockComment stockComment : comments) {
            contentWithLables.put(stockComment.getContent(), Collections.singletonList(stockComment.getLabel()));
        }
        if (contentWithLables.size() > 0) {
            commentClassifier.train(contentWithLables);
        }
    }

    /***
     * 每天更新一次数据预测模型
     */
    @Scheduled(cron = "0 00 19 ? * MON-FRI")
    public void HistoryDataTrainer() throws InterruptedException {
        trainController.trainStockHistoryRegression("2018-01-01",
                LocalDate.now().format(Crawler.todayFormat2),
                "500-RELU,50-RELU",
                false, true, true, false,
                "avestart,aveend,avelow,avehigh,avevolume,rate,turnover,indexrate",
                "avevolume,rate,turnover,indexrate",
                "up_5",
                20,
                300,
                1,
                "default",
                0.001f);
    }

    @Scheduled(cron = "0 00 20 ? * MON-FRI")
    public void HistoryDataPredictor() throws InterruptedException {
        String today = LocalDate.now().plusDays(1).format(Crawler.todayFormat2);
        String netType = "default";
        Map<Integer, List<String>> result = predictController.predictRegression(today,
                20,
                true,
                false,
                "avevolume,rate,turnover,indexrate",
                "up_5",
                "default", null);
        String mailContent = "看涨>5% : " + String.join(",", result.get(1));
        String mailTitle = today + "涨幅大于5%预测结果(" + netType + ")";
        List<String> mailList = userRepository.findAll().stream().map(StockUser::getMail).collect(Collectors.toList());
        mailService.send(mailTitle, mailContent, mailList);
    }
}
