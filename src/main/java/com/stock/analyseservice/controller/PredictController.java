package com.stock.analyseservice.controller;

import com.stock.analyseservice.algo.RegressionStock;
import com.stock.analyseservice.algo.nlp.CommentClassifier;
import com.stock.analyseservice.dao.domain.StockHistory;
import com.stock.analyseservice.dao.domain.StockHistoryOriginal;
import com.stock.analyseservice.dao.repository.StockHistoryOriginalRepository;
import com.stock.analyseservice.dao.repository.StockHistoryRepository;
import com.stock.analyseservice.service.DataService;
import com.stock.analyseservice.service.MailService;
import com.stock.analyseservice.task.Crawler;
import com.stock.analyseservice.task.Trainer;
import org.apache.commons.lang3.StringUtils;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/predict")
public class PredictController {

    private static final Logger log = LoggerFactory.getLogger(CommentClassifier.class);

    private static final RegressionStock regression = new RegressionStock();

    private MultiLayerNetwork model;

    @Autowired
    StockHistoryRepository historyRepository;

    @Autowired
    StockHistoryOriginalRepository originalRepository;

    @Autowired
    Trainer trainer;

    @Autowired
    DataService dataService;

    @Autowired
    MailService mailService;

    public PredictController() {
        refreshModel();
    }

    @RequestMapping(value = "/data", method = RequestMethod.POST)
    public Map predictStock(@RequestBody String stockCodes, @RequestParam String date, @RequestParam(defaultValue = "20") Integer size,
                            @RequestParam(defaultValue = "false") Boolean withComment,
                            @RequestParam(defaultValue = "false") Boolean isEval) {
        LocalDate end = LocalDate.parse(date).minusDays(1);
        LocalDate start = end.minusDays(size + 20);
        Integer up2Down = 0;
        Integer up2Up = 0;
        Integer down2Down = 0;
        Integer down2up = 0;
        Integer length = 3;
        if (withComment) {
            length = 4;
        }
        List<StockHistoryOriginal> originalList = originalRepository.findAllByStockCodeInAndTimeBetween(Collections.singletonList("000001"), Timestamp.valueOf(start.atStartOfDay()), Timestamp.valueOf(end.atStartOfDay()));
        Map<Long, StockHistoryOriginal> indexHistoryMap = new LinkedHashMap<>();
        for (StockHistoryOriginal history : originalList) {
            Long t = history.getTime().getTime();
            indexHistoryMap.put(t, history);
        }
        Evaluation eval = new Evaluation(length * size);
        Map<String, Integer> resMap = new LinkedHashMap<>();
        for (String stockCode : stockCodes.split(",")) {
            float[] datas = new float[length * size];
            List<StockHistory> historyList = historyRepository.findByStockCodeAndTimeBetweenOrderByTimeDesc(stockCode, Timestamp.valueOf(start.atStartOfDay()), Timestamp.valueOf(end.atStartOfDay()));
            if (historyList.size() < size) {
                continue;
            }
            historyList = historyList.subList(0, size);
            Float aveVolume = 0f;
            Float aveCount = 0f;
            for (StockHistory h : historyList) {
                aveVolume += h.getVolume();
                if (withComment) {
                    aveCount += h.getAllCount();
                }
            }
            aveVolume = aveVolume / historyList.size();
            aveCount = aveCount / historyList.size();
            for (int i = 0; i < size; i++) {
                StockHistory history = historyList.get(i);
                StockHistoryOriginal indexHistory = indexHistoryMap.get(history.getTime().getTime());
//                if (history.getRate() > 2) {
//                    datas[i * length] = 1;
//                } else if (history.getRate() > 0) {
//                    datas[i * length + 1] = 1;
//                } else if (history.getRate() > -2) {
//                    datas[i * length + 2] = 1;
//                } else {
//                    datas[i * length + 3] = 1;
//                }
                datas[i * length] = history.getRate();
                datas[i * length + 1] = history.getVolume() / aveVolume;
                datas[i * length + 2] = history.getTurnover();
//                datas[i * length + 6] = indexHistory.getRate();
                if (withComment) {
                    datas[i * length + 3] = history.getAllCount() / aveCount;
                }
            }
            INDArray predictData = Nd4j.create(datas);
            predictData.setShape(1, datas.length);
            int[] res = model.predict(predictData);
            if (isEval) {
                StockHistory h = historyRepository.findByStockCodeAndTime(stockCode, Timestamp.valueOf(end.plusDays(1).atStartOfDay()));
                if (h != null) {
                    int real = h.getRate() >= 0 ? 1 : 0;
                    if (real == 0) {
                        if (real == res[0]) {
                            down2Down++;
                        } else {
                            down2up++;
                        }
                    } else {
                        if (real == res[0]) {
                            up2Up++;
                        } else {
                            up2Down++;
                        }
                    }
//                    log.debug("real data:{}  predict data:{}", h.getRate(), res[0]);
                }
            }
            if (res[0] > 0) {
                resMap.put(stockCode, res[0]);
            }
//            log.info("result:{} stock:{}", res, stockCode);
        }
        if (isEval) {
            log.info("up-up:{}  down-down:{}  up-down:{}  down-up:{}", up2Up, down2Down, up2Down, down2up);
        }
        return resMap;
    }

    @RequestMapping(value = "/refresh", method = RequestMethod.GET)
    public void refreshModelAPI() {
        refreshModel();
    }

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public void refreshModelAPI(String date) {
        LocalDate d = LocalDate.parse(date);
        trainer.HistoryDataPredictor(d);
    }

    @RequestMapping(value = "/mail", method = RequestMethod.GET)
    public void refreshModelAPI(String title, String contents) throws MessagingException {
        mailService.send(title, Arrays.asList(contents.split(",")));
    }

    @RequestMapping(value = "/regression", method = RequestMethod.GET)
    public Map<Integer, List<String>> predictRegression(@RequestParam String date,
                                                        @RequestParam(defaultValue = "20") Integer dayNum,
                                                        @RequestParam(defaultValue = "true") Boolean isClassify,
                                                        @RequestParam(defaultValue = "false") Boolean hasTomorrow,
                                                        @RequestParam(defaultValue = "avevolume,rate,turnover,indexrate") String inputs,
                                                        @RequestParam(defaultValue = "up_5") String output,
                                                        @RequestParam(defaultValue = "default") String netType,
                                                        @RequestParam(required = false) String codes) {
        List<String> columns = new ArrayList<>(Arrays.asList(inputs.split(",")));
        List<String> codeList = Arrays.asList((StringUtils.isEmpty(codes) ? DataService.codes : codes.split(",")));
        Map<String, Map<String, Float>> codeData = dataService.getPredictData(codeList, date, dayNum, columns, hasTomorrow);
        for (Map.Entry<String, Map<String, Float>> entry : codeData.entrySet()) {
            String code = entry.getKey();
            Map<String, Float> todayData = new HashMap<>();
            todayData.put("avestart", entry.getValue().get("avestart"));
            todayData.put("avecomment", entry.getValue().get("avecomment"));
            todayData.put("avevolume", entry.getValue().get("avevolume"));
            entry.getValue().remove("avestart");
            entry.getValue().remove("avecomment");
            entry.getValue().remove("avevolume");
        }
        Map<String, Integer> result = regression.predict(codeData, new ArrayList<>(Arrays.asList(output.split(","))), columns, dayNum, isClassify, netType);
        Map<Integer, List<String>> resMap = new LinkedHashMap<>();
        resMap.put(0, new ArrayList<>());
        resMap.put(1, new ArrayList<>());
        for (Map.Entry<String, Integer> entry : result.entrySet()) {
            String code = entry.getKey();
            Integer value = entry.getValue();
            resMap.get(value).add(code);
        }
//        String mailContent = String.join(",", resMap.get(1));
//        String mailTitle = LocalDateTime.now().format(Crawler.timeFormat);
//        mailService.send(mailTitle, mailContent);
        return resMap;

    }

    private void refreshModel() {
        try {
            model = ModelSerializer.restoreMultiLayerNetwork(new File("model.zip"));
        } catch (IOException e) {
            log.error("init model error :{}", e.getMessage());
        }
    }

}
