package com.stock.analyseservice.task;

import com.stock.analyseservice.algo.ClassifyStock;
import com.stock.analyseservice.algo.nlp.CommentClassifier;
import com.stock.analyseservice.dao.domain.StockComment;
import com.stock.analyseservice.dao.repository.StockCommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class Trainer {

    private CommentClassifier commentClassifier = CommentClassifier.newInstance();

    @Autowired
    private StockCommentRepository commentRepository;


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
//    @Scheduled
    public void HistoryDataTrainer() throws IOException, InterruptedException {
//        classifyStock.train();
    }
}
