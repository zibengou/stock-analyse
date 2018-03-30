package com.stock.analyseservice.task;

import com.stock.analyseservice.algo.ClassifyStock;
import com.stock.analyseservice.algo.nlp.CommentClassifier;
import com.stock.analyseservice.controller.PredictController;
import com.stock.analyseservice.controller.TrainController;
import com.stock.analyseservice.dao.domain.StockComment;
import com.stock.analyseservice.dao.domain.StockHistory;
import com.stock.analyseservice.dao.domain.StockPredict;
import com.stock.analyseservice.dao.domain.StockUser;
import com.stock.analyseservice.dao.repository.StockCommentRepository;
import com.stock.analyseservice.dao.repository.StockHistoryRepository;
import com.stock.analyseservice.dao.repository.StockPredictRepository;
import com.stock.analyseservice.dao.repository.StockUserRepository;
import com.stock.analyseservice.service.DataService;
import com.stock.analyseservice.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.stock.analyseservice.service.DataService.codes;

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

    @Autowired
    private StockPredictRepository predictRepository;

    @Autowired
    private DataService dataService;


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

    private static Map<Long, Map<String, List<String>>> predickStockMap = new LinkedHashMap<>();

    @Scheduled(cron = "0 00 20 ? * MON-FRI")
    public void HistoryDataPredictor() {
        LocalDate tomorrowDate = LocalDate.now().plusDays(1);
        HistoryDataPredictor(tomorrowDate);
    }

    public void HistoryDataPredictor(LocalDate tomorrowDate) {
        String tomorrow = tomorrowDate.format(Crawler.todayFormat2);
        String netType = "default";
        String inputs = sortInputs("avevolume,rate,turnover,indexrate");
        String output = "up_5";
        Map<Integer, List<String>> result = predictController.predictRegression(tomorrow,
                20,
                true,
                false,
                inputs,
                output,
                netType, null);
        List<String> codeList = result.get(1);
//        List<String> codeList = Arrays.asList("600198", "600078");
        try {
            sendPredictResult(tomorrowDate, inputs, output, netType, codeList);
        } catch (MessagingException e) {
            log.error("send mail error {}", e);
        }
        try {
            savePredictCodes(tomorrowDate, inputs, output, netType, codeList);
        } catch (Exception e) {
            log.error("save predict data error:{}", e);
        }
    }

    private void sendPredictResult(LocalDate date, String inputs, String output, String netType, List<String> codeList) throws MessagingException {
        String d = date.format(Crawler.todayFormat2);
        String mailTitle = d + "涨幅大于5%预测结果(" + netType + ")";
        List<String> text = new ArrayList<>();
        text.add("看涨(" + output + ") : " + String.join(",", codeList));
        text.add("===========================================");
        text.add("昨日预测结果: ");
        text.addAll(getTodayResult(date, inputs, output, netType));
        List<String> mailList = userRepository.findByEnabledIsTrue().stream().map(StockUser::getMail).collect(Collectors.toList());
        mailService.send(mailTitle, text, mailList);

    }


    private void savePredictCodes(LocalDate date, String inputs, String output, String netType, List<String> codeList) {
        Date time = Timestamp.valueOf(date.atStartOfDay());
        String codes = String.join(",", codeList);
        StockPredict stockPredict = new StockPredict(inputs, output, netType, time, codes);
        predictRepository.save(stockPredict);
        log.info("save predict codes success : {}", stockPredict);
    }

    private List<String> getTodayResult(LocalDate date, String inputs, String output, String netType) {
        Date today = Timestamp.valueOf(date.minusDays(1).atStartOfDay());
        List<String> resultList = new ArrayList<>();
        StockPredict predict = predictRepository.findByTimeAndInputsAndOutputAndNetType(today, inputs, output, netType);
        if (predict != null) {
            List<String> codeList = Arrays.asList(predict.getCodes().split(","));
            Map<String, StockHistory> historyMap = dataService.getHistoryDataByCodeAndTime(today, codeList);
            for (String code : codeList) {
                if (historyMap.containsKey(code)) {
                    StockHistory data = historyMap.get(code);
                    String content = String.format("%s: 最高价->%s,最低价->%s,开盘价->%s,收盘价->%s,涨幅->%s", data.getStockCode(), data.getHigh(), data.getLow(), data.getStart(), data.getEnd(), data.getRate() + "%");
                    if (data.getEnd() > data.getStart()) {
                        content = "<font style=\"color: red\">" + content + "</font>";
                    } else {
                        content = "<font style=\"color: green\">" + content + "</font>";
                    }
                    resultList.add(content);
                }
            }
        }
        return resultList;
    }

    private String sortInputs(String inputs) {
        List<String> inputList = Arrays.asList(inputs.split(","));
        Collections.sort(inputList);
        return String.join(",", inputList);
    }

}
