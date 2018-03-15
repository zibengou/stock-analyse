package com.stock.analyseservice.task;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.stock.analyseservice.dao.domain.StockComment;
import com.stock.analyseservice.dao.domain.StockHistoryOriginal;
import com.stock.analyseservice.dao.domain.StockRealtime;
import com.stock.analyseservice.dao.repository.StockCommentRepository;
import com.stock.analyseservice.dao.repository.StockHistoryOriginalRepository;
import com.stock.analyseservice.dao.repository.StockRealtimeRepository;
import com.stock.analyseservice.service.DataService;
import com.stock.analyseservice.util.JsonUtil;
import com.stock.analyseservice.util.net.HttpAsyncClient;
import com.stock.analyseservice.util.net.IHttpAsyncClient;
import com.stock.analyseservice.util.net.Response;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.channels.CompletionHandler;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class Crawler {

    Logger logger = LoggerFactory.getLogger(Crawler.class);

    private static IHttpAsyncClient client = new HttpAsyncClient(10, 5000);

    public static DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static DateTimeFormatter todayFormat = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static ExecutorService executorService = Executors.newFixedThreadPool(8);

    @Autowired
    StockHistoryOriginalRepository stockHistoryOriginalRepository;

    @Autowired
    StockCommentRepository stockCommentRepository;

    @Autowired
    StockRealtimeRepository realtimeRepository;

    @Autowired
    DataService dataService;

    @Scheduled(cron = "0 0 17 ? * MON-FRI")
    public void StockComment() {
        for (String code : DataService.codes) {
            StockComment(code);
        }
    }
    @Scheduled(cron = "0 30 17 ? * MON-FRI")
    public void updateData() {
        String todayTime = LocalDate.now().atStartOfDay().format(timeFormat);
        dataService.update(DataService.codes, true, todayTime, todayTime);
    }

    public void StockComment(String code) {
        parseGubaComment(code);
        parseSinaComment(code);
    }

    private void parseGubaComment(String code) {
        for (int page = 1; page < 2; page++) {
            String url = "http://m.guba.eastmoney.com/getdata/articlelist?code=" + code + "&count=200&thispage=" + page;
            client.handleGet(url, new CompletionHandler<Response, String>() {
                @Override
                public void completed(Response result, String attachment) {
                    JsonNode node = JsonUtil.toJsonNode(result.getBody());
                    for (JsonNode commentNode : node.get("re")) {
                        try {
                            String content = commentNode.get("post_content").toString().replace("\"", "");
                            if (StringUtils.isEmpty(content.trim())) {
                                continue;
                            }
                            LocalDateTime time = LocalDateTime.parse(commentNode.get("post_publish_time").toString().replace("\"", ""), timeFormat);
                            Integer commentCount = commentNode.get("post_comment_count").intValue();
                            StockComment data = new StockComment(time, code, content, commentCount, "guba");
                            executorService.execute(() -> {
                                try {
                                    stockCommentRepository.save(data);
                                } catch (Exception e) {
                                    logger.error("stock {} comment insert error date:{} source:guba", code, time);
                                }
                                logger.info("stock {} comment insert success date:{} source:guba", code, time);
                            });
                        } catch (Exception e) {
                            logger.error("stock {} comment insert error source:guba", code);
                        }
                    }
                }

                @Override
                public void failed(Throwable exc, String attachment) {

                }
            });
        }
    }

    private void parseSinaComment(String code) {
        String url = "http://guba.sina.cn/api/?s=h5bar&num=50&bname=" + parseCode(code) + "";
        client.handleGet(url, new CompletionHandler<Response, String>() {
            @Override
            public void completed(Response result, String attachment) {
                JsonNode node = JsonUtil.toJsonNode(result.getBody());
                for (JsonNode commentNode : node.get("data").get("threads")) {
                    try {
                        String content = commentNode.get("content").toString().replace("\"", "");
                        if (StringUtils.isEmpty(content.trim())) {
                            continue;
                        }
                        LocalDateTime time = LocalDateTime.parse(commentNode.get("lastctime").toString().replace("\"", ""), timeFormat);
                        String replyCountStr = commentNode.get("reply").toString().replace("\"", "");
                        Integer commentCount = "".equals(replyCountStr) ? 0 : Integer.parseInt(replyCountStr);
                        StockComment data = new StockComment(time, code, content, commentCount, "sina");
                        executorService.execute(() -> {
                            try {
                                stockCommentRepository.save(data);
                            } catch (Exception e) {
                                logger.error("stock {} comment insert error date:{} source:sina", code, time);
                            }
                            logger.info("stock {} comment insert success date:{} source:sina", code, time);
                        });
                    } catch (Exception e) {
                        logger.error("stock {} comment insert error source:sina", code);
                    }
                }
            }

            @Override
            public void failed(Throwable exc, String attachment) {

            }
        });
    }

    /***
     * 每十分钟获取一次实时数据
     */
    @Scheduled(cron = "0 0/1 9,10,11,13,14,21 ? * MON-FRI")
    public void StockRealTimeData() {
        String url = "http://hq.sinajs.cn/list=";
        List<String> codeList = new ArrayList<>();
        for (int i = 0; i < DataService.codes.length; i++) {
            codeList.add("sh" + DataService.codes[i]);
            if (i % 100 == 0) {
                String request = url + StringUtils.join(codeList, ",");
                getRealTimeData(request);
                codeList = new ArrayList<>();
            }
        }
        String request = url + StringUtils.join(codeList, ",") + ",sh000001";
        getRealTimeData(request);
        logger.info("init all realtime data success");
    }

    private void getRealTimeData(String url) {
        client.handleGet(url, new CompletionHandler<Response, String>() {
            @Override
            public void completed(Response result, String attachment) {
                String[] stockDataStrings = result.getBody().split(";");
                for (String stockDataString : stockDataStrings) {
                    String[] valueStrings = stockDataString.split("\"");
                    if (valueStrings.length > 1) {
                        String code = valueStrings[0].substring(13, 19);
                        String[] values = valueStrings[1].split(",");
                        if (values.length > 32) {
                            StockRealtime realtime = new StockRealtime(code, values);
                            if (null == realtimeRepository.findByStockCodeAndTime(realtime.getStockCode(), realtime.getTime())) {
                                realtimeRepository.save(realtime);
                            }
                        }
                    }
                }
                logger.info("init code:{} success...", url);
            }

            @Override
            public void failed(Throwable exc, String attachment) {

            }
        });
    }


    @Scheduled(cron = "0 0 16 ? * MON-FRI")
    public void StockHistoryData() {
        String today = LocalDate.now().format(todayFormat);
        StockHistoryData(today, today, false);
        StockHistoryData(today, today, "000001", true);
    }

    public void StockHistoryData(String start, String end, Boolean isIndex) {
        for (String code : DataService.codes) {
            StockHistoryData(start, end, code, isIndex);
        }
    }

    public void StockHistoryData(String start, String end, String code, Boolean isIndex) {
        TypeReference<List<Map<String, Object>>> typeReference = new TypeReference<List<Map<String, Object>>>() {
        };
        String url = "http://q.stock.sohu.com/hisHq?code=" + (isIndex ? "zs" : "cn") + "_" + code + "&start=" + start + "&end=" + end + "&period=d";
        client.handleGet(url, new CompletionHandler<Response, String>() {
            @Override
            public void completed(Response result, String attachment) {
                JsonNode node = JsonUtil.toJsonNode(result.getBody());
                for (JsonNode stock : node.get(0).get("hq")) {
                    try {
                        LocalDate time = LocalDate.parse(stock.get(0).toString().replace("\"", ""));
                        Float start = Float.parseFloat(stock.get(1).toString().replace("\"", ""));
                        Float end = Float.parseFloat(stock.get(2).toString().replace("\"", ""));
                        Float rate = Float.parseFloat(stock.get(4).toString().replace("\"", "").replace("%", ""));
                        Float low = Float.parseFloat(stock.get(5).toString().replace("\"", ""));
                        Float high = Float.parseFloat(stock.get(6).toString().replace("\"", ""));
                        Float volume = Float.parseFloat(stock.get(7).toString().replace("\"", ""));
                        Float volumePrice = Float.parseFloat(stock.get(8).toString().replace("\"", ""));
                        Float turnover = 0f;
                        if (!isIndex) {
                            turnover = Float.parseFloat(stock.get(9).toString().replace("%", "").replace("\"", ""));
                        }
                        StockHistoryOriginal data = new StockHistoryOriginal(time, code, start, end, low, high, rate, volume, volumePrice, turnover);
                        stockHistoryOriginalRepository.save(data);
                        logger.info("stock {} insert success date : {}", code, time);
                    } catch (Exception e) {
                        logger.info("stock {} insert error date : {}", code);
                    }

                }
            }

            @Override
            public void failed(Throwable exc, String attachment) {

            }
        });
    }

    private String parseCode(String code) {
        if (code.startsWith("6")) {
            return "sh" + code;
        } else {
            return "sz" + code;
        }
    }
}
