package com.stock.analyseservice.controller;

import com.stock.analyseservice.algo.nlp.CommentClassifier;
import com.stock.analyseservice.dao.domain.StockComment;
import com.stock.analyseservice.dao.domain.StockHistory;
import com.stock.analyseservice.dao.domain.StockHistoryOriginal;
import com.stock.analyseservice.dao.repository.StockCommentRepository;
import com.stock.analyseservice.dao.repository.StockHistoryOriginalRepository;
import com.stock.analyseservice.dao.repository.StockHistoryRepository;
import com.stock.analyseservice.service.DataService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/data")
public class DataController {

    private static final Logger log = LoggerFactory.getLogger(CommentClassifier.class);

    private static String[] CODES = new String[]{"600037", "600088", "600128", "600386", "600551", "600633", "600637", "600662", "600716", "600757", "600811", "600825", "600831", "600880", "601098", "601801", "601999", "000504", "000665", "000793"};

    private static AtomicInteger codeIndex = new AtomicInteger(0);

    @Autowired
    private StockCommentRepository commentRepository;

    @Autowired
    private StockHistoryOriginalRepository historyOriginalRepository;

    @Autowired
    private StockHistoryRepository historyRepository;

    @Autowired
    DataService dataService;


//    @RequestMapping(value = "/comment_list", method = RequestMethod.GET)
//    public List<StockComment> getCommentList(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
//                                             @RequestParam(defaultValue = "0") int count) {
//        Sort sort = new Sort(Sort.Direction.DESC, "time");
//        PageRequest pageRequest = new PageRequest(page, size, sort);
////        return commentRepository.findAllByEditedIsFalseAndCommentCountGreaterThan(count, pageRequest);
//        Integer index = codeIndex.getAndIncrement();
//        if (index >= CODES.length - 1) {
//            codeIndex.set(0);
//            index = 0;
//        }
//        String code = CODES[index];
//        log.info("get stock:{},size:{},page:{}", code, size, page);
//        return commentRepository.findAllByStockCodeIsAndEditedIsFalseAndCommentCountGreaterThan(code, count, pageRequest);
//    }
//
//    @RequestMapping(value = "/comment/{id}", method = RequestMethod.PUT)
//    public StockComment getCommentList(@PathVariable(value = "id") int id, @RequestBody StockComment stockComment) throws Exception {
//        StockComment oldComment = commentRepository.findOne(id);
//        if (oldComment == null) {
//            throw new Exception("ID不存在");
//        }
//        stockComment.setId(id);
//        if (StringUtils.isNoneEmpty(stockComment.getContent(), stockComment.getLabel(), stockComment.getCommentSource())) {
//            return commentRepository.save(stockComment);
//        } else {
//            throw new Exception("请求非法");
//        }
//    }

    @RequestMapping(value = "/update_history", method = RequestMethod.GET)
    public String updateHistoryData(@RequestParam String start, @RequestParam String end) throws Exception {
        return dataService.update(DataService.codes, true, start, end);
    }


//    @RequestMapping(value = "/update_history", method = RequestMethod.POST)
//    public String updateMoreHistoryData(@RequestParam String start, @RequestParam String end, @RequestParam(defaultValue = "false") Boolean withComment, @RequestBody String stockCodes) throws Exception {
//        return dataService.update(stockCodes.split(","), withComment, start, end);
//    }

//    @RequestMapping(value = "/init_train_data", method = RequestMethod.GET)
//    public void initTrainData(@RequestParam(defaultValue = "600198") String stockCodes, @RequestParam(defaultValue = "5") Integer size, String start, String end) throws Exception {
//        initTrain(stockCodes, true, size, start, end);
//    }
//
//    @RequestMapping(value = "/init_train_data", method = RequestMethod.POST)
//    public void initMoreTrainData(@RequestBody String stockCodes,
//                                  @RequestParam(defaultValue = "5") Integer size,
//                                  @RequestParam String start,
//                                  @RequestParam String end,
//                                  @RequestParam Boolean withComment) throws Exception {
//        initTrain(stockCodes, withComment, size, start, end);
//    }

    @Test
    public void initTrain(String stockCodes, Boolean withComment, Integer size, String start, String end) throws IOException {
        Date st = Timestamp.valueOf(LocalDate.parse(start).atStartOfDay());
        Date et = Timestamp.valueOf(LocalDate.parse(end).atStartOfDay());
        List<String> testLines = new ArrayList<>();
        List<String> trainLines = new ArrayList<>();
        Integer index = 0;
        List<StockHistoryOriginal> indexHistoryList = historyOriginalRepository.findAllByStockCodeInAndTimeBetween(Collections.singletonList("000001"), st, et);
        Map<Long, StockHistoryOriginal> indexHistoryMap = new LinkedHashMap<>();
        for (StockHistoryOriginal history : indexHistoryList) {
            Long t = history.getTime().getTime();
            indexHistoryMap.put(t, history);
        }
        for (String stockCode : stockCodes.split(",")) {
            List<StockHistory> historyList = historyRepository.findByStockCodeAndTimeBetweenOrderByTimeDesc(stockCode, st, et);
            for (int i = 0; i < historyList.size() - size - 1; i++) {
                StockHistory data = historyList.get(i);
                if (!withComment || data.getAllCount() > 0) {
                    List<StockHistory> features = historyList.subList(i + 1, i + size + 1);
                    Float aveVolume = 1f;
                    Float aveCount = 1f;
                    Float avePrice = 1f;
                    Boolean noEnoughComment = false;
                    for (StockHistory fea : features) {
                        aveVolume += fea.getVolume();
                        aveCount += fea.getAllCount();
                        avePrice += fea.getStart();
                        if (withComment && fea.getAllCount() < 1) {
                            noEnoughComment = true;
                        }
                    }
                    aveVolume = aveVolume / features.size();
                    aveCount = aveCount / features.size();
                    avePrice = avePrice / features.size();
                    if (!noEnoughComment) {
                        StringBuilder sb = new StringBuilder();
                        if (data.getRate() > 2) {
                            sb.append("1");
                        } else {
                            sb.append("0");
                        }

                        for (StockHistory feature : features) {
                            // 暴涨 涨 跌 暴跌
                            Integer[] status = new Integer[]{0, 0, 0, 0};
                            if (feature.getRate() > 2) {
                                status[0] = 1;
                            } else if (feature.getRate() > 0) {
                                status[1] = 1;
                            } else if (feature.getRate() > -2) {
                                status[2] = 1;
                            } else {
                                status[3] = 1;
                            }
                            StockHistoryOriginal indexHistory = indexHistoryMap.get(feature.getTime().getTime());
//                            sb.append(",").append(status[0])
//                                    .append(",").append(status[1])
//                                    .append(",").append(status[2])
//                                    .append(",").append(status[3])
//                            sb.append(",").append(feature.getStart() / avePrice)
                            sb.append(",").append(feature.getRate())
//                                    .append(",").append(feature.getLowRate())
//                                    .append(",").append(feature.getHighRate())
                                    .append(",").append(feature.getVolume() / aveVolume)
                                    .append(",").append(feature.getTurnover());
//                                    .append(",").append(indexHistory.getRate());
                            if (withComment) {
                                sb.append(",").append(feature.getAllCount() / aveCount);
                            }
                        }
                        // 最后两天为测试数据
                        if (index % 10 == 0) {
                            testLines.add(sb.toString());
                            log.info("init test data success code:{} date:{}", stockCode, data.getTime());
                        } else {
                            trainLines.add(sb.toString());
                            log.info("init train data success code:{} date:{}", stockCode, data.getTime());
                        }
                        index++;
                    } else {
                        log.warn("train data don't have enough comment data code:{} date:{}", stockCode, data.getTime());
                    }
                }
            }
        }
        File trainFile = new File("train_data");
        File testFile = new File("test_data");
        FileUtils.writeLines(trainFile, "utf-8", trainLines);
        FileUtils.writeLines(testFile, "utf-8", testLines);
    }
}
