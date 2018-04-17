package com.stock.analyseservice.controller;

import com.stock.analyseservice.algo.ClassifyStock;
import com.stock.analyseservice.algo.RegressionStock;
import com.stock.analyseservice.algo.nlp.CommentClassifier;
import com.stock.analyseservice.service.DataService;
import com.stock.analyseservice.task.Crawler;
import com.stock.analyseservice.task.Trainer;
import org.nd4j.linalg.primitives.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.ws.http.HTTPException;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/train")
public class TrainController {

    private static final Logger log = LoggerFactory.getLogger(CommentClassifier.class);

    @Autowired
    Trainer trainer;

    @Autowired
    DataService dataService;

    private static ClassifyStock classifyStock = new ClassifyStock();

    private static RegressionStock regressionStock = new RegressionStock();

    CommentClassifier commentClassifier = CommentClassifier.newInstance();

//    @RequestMapping(value = "/comment", method = RequestMethod.GET)
//    public void trainStockComment() {
//        trainer.CommentTrainer();
//    }
//
//    @RequestMapping(value = "/history", method = RequestMethod.GET)
//    public void trainStockHistory(@RequestParam(defaultValue = "20") int dimension,
//                                  @RequestParam(defaultValue = "1000") int hiddenNum,
//                                  @RequestParam(defaultValue = "200") int nEpochs) throws IOException, InterruptedException {
//        classifyStock.train(dimension, hiddenNum, nEpochs);
//    }

    @RequestMapping(value = "/history/regression", method = RequestMethod.GET)
    public long trainStockHistoryRegression(@RequestParam String start,
                                            @RequestParam String end,
                                            @RequestParam(defaultValue = "500-RELU,50-RELU") String hidden,
                                            @RequestParam(defaultValue = "false") Boolean dataUpdate,
                                            @RequestParam(defaultValue = "true") Boolean modelUpdate,
                                            @RequestParam(defaultValue = "true") Boolean isClassify,
                                            @RequestParam(defaultValue = "false") Boolean hasTomorrow,
                                            @RequestParam(defaultValue = "avestart,aveend,avelow,avehigh,avevolume,rate,turnover,indexrate") String properties,
                                            @RequestParam(defaultValue = "avevolume,rate,turnover,indexrate") String inputs,
                                            @RequestParam(defaultValue = "up_5") String outputs,
                                            @RequestParam(defaultValue = "20") Integer dayNum,
                                            @RequestParam(defaultValue = "200") int nEpochs,
                                            @RequestParam(defaultValue = "1") int seed,
                                            @RequestParam(defaultValue = "default") String netTyle,
                                            @RequestParam(defaultValue = "0.005") Float learningRate) {
        Date st = Timestamp.valueOf(LocalDate.parse(start).atStartOfDay());
        Date et = Timestamp.valueOf(LocalDate.parse(end).atStartOfDay());
        List<String> ps = Arrays.asList(properties.split(","));
        List<String> columns = new ArrayList<>(Arrays.asList(inputs.split(",")));
        List<String> out = new ArrayList<>(Arrays.asList(outputs.split(",")));
        if (isClassify && out.size() > 1) {
            log.error("classification cannot have more than 2 outputs");
            throw new HTTPException(405);
        }
        List<String> propertyList = new ArrayList<>(ps);
        List<String> pathList = new ArrayList<>();
        pathList.add(start);
        pathList.add(end);
        pathList.add(String.valueOf(dayNum));
        pathList.addAll(propertyList);
        Collections.sort(pathList);
        String path = String.join("_", pathList) + (hasTomorrow ? "_tomorrow_" : "");
        File trainFile = new File(path);
        List<Map<String, Float>> data = dataService.getTrainData(st, et, dayNum, propertyList, trainFile, dataUpdate, hasTomorrow);
        List<Pair<Integer, String>> hiddens = parseHiddens(hidden);
        regressionStock.setSeed(seed);
        Thread regressionThread = new Thread(() -> regressionStock.train(trainFile, hiddens, learningRate, nEpochs, dayNum, columns, out, modelUpdate, isClassify, netTyle));
        return runThread(regressionThread);
    }

    @RequestMapping(value = "/realtime/classify", method = RequestMethod.GET)
    public long trainRealtime(@RequestParam String start,
                              @RequestParam String end,
                              @RequestParam(required = false) String codes,
                              @RequestParam(defaultValue = "500-RELU,50-RELU") String hidden,
                              @RequestParam(defaultValue = "false") Boolean dataUpdate,
                              @RequestParam(defaultValue = "now,b1,b2,b3,s1,s2,s3") String inputs,
                              @RequestParam(defaultValue = "up_2") String output,
                              @RequestParam(defaultValue = "10") Integer timeRange,
                              @RequestParam(defaultValue = "200") int nEpochs,
                              @RequestParam(defaultValue = "1") int seed,
                              @RequestParam(defaultValue = "default") String netStyle,
                              @RequestParam(defaultValue = "0.005") Float learningRate) throws InterruptedException, IOException {
        Date st = Timestamp.valueOf(LocalDate.parse(start).atStartOfDay());
        Date et = Timestamp.valueOf(LocalDate.parse(end).atStartOfDay());
        List<String> codeList = codes == null ? Arrays.asList(DataService.codes) : Arrays.asList(codes.split(","));
        String filePath = String.join("_", start, end, timeRange.toString());
        File trainData = new File(filePath);
        if (dataUpdate || !trainData.exists()) {
            dataService.initRealTimeData(st, et, codeList, trainData, timeRange);
            log.info("init train data success: {}", filePath);
        } else {
            log.info("train data:{} already exists ", filePath);
        }
        regressionStock.setSeed(seed);
        List<Pair<Integer, String>> hiddens = parseHiddens(hidden);
        List<String> columns = new ArrayList<>(Arrays.asList(inputs.split(",")));
        columns.add(output);
        Collections.sort(columns);
        String modelPath = String.join("_", columns) + "_" + netStyle;
        File modelFile = new File(modelPath);
        List<Map<String, Float>> dataList = RegressionStock.readTrainData(trainData, columns);
        Thread thread = new Thread(() -> regressionStock.train(dataList, hiddens, learningRate, nEpochs, Arrays.asList(output), modelFile, true, netStyle));
        return runThread(thread);
    }

    @RequestMapping(value = "/stop", method = RequestMethod.GET)
    public String classify(long threadId) {
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.getId() == threadId) {
                try {
                    thread.stop();
                    return "thread stop success...";
                } catch (Exception e) {
                    return "thread stop failure " + e.getMessage();
                }
            }
        }
        return "thread not exists..";
    }


    private List<Pair<Integer, String>> parseHiddens(String hidden) {
        List<Pair<Integer, String>> hiddens = new ArrayList<>();
        for (String h : hidden.split(",")) {
            Pair<Integer, String> pair = new Pair<>();
            String active = "TANH";
            Integer num = null;
            try {
                if (h.contains("-")) {
                    String[] hh = h.split("-");
                    num = Integer.valueOf(hh[0]);
                    active = hh[1].toUpperCase();
                } else {
                    num = Integer.valueOf(h);
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            if (num != null) {
                pair.setKey(num);
                pair.setValue(active);
                hiddens.add(pair);
            }
        }
        return hiddens;
    }

    private Long runThread(Thread thread) {
        thread.run();
        return thread.getId();
    }

}
