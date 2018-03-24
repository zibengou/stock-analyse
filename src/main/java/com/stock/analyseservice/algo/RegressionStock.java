package com.stock.analyseservice.algo;

import com.stock.analyseservice.algo.nlp.CommentClassifier;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
import org.deeplearning4j.earlystopping.EarlyStoppingConfiguration;
import org.deeplearning4j.earlystopping.EarlyStoppingModelSaver;
import org.deeplearning4j.earlystopping.saver.LocalFileModelSaver;
import org.deeplearning4j.earlystopping.scorecalc.DataSetLossCalculator;
import org.deeplearning4j.earlystopping.termination.MaxEpochsTerminationCondition;
import org.deeplearning4j.earlystopping.termination.MaxTimeIterationTerminationCondition;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.FeedForwardLayer;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.deeplearning4j.util.StringUtils;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.primitives.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class RegressionStock {
    private static final Logger log = LoggerFactory.getLogger(CommentClassifier.class);

    private final static Integer batchSize = 100;
    private Integer seed = 1;
    private Random rng = new Random(seed);

    private static final File train_temp = new File("train_temp");
    private static final File test_temp = new File("test_temp");

    private static final String stopping_dir = "stop_dir";

    private static final String avestart = "avestart";
    private static final String avevolume = "avevolume";
    public static final String path = "2018-03-01_2018-03-09_5_avestart_aveend_avelow_avehigh_avevolume_rate_turnover";

    private static final DataNormalization normalizer = new NormalizerStandardize();

    private Integer numInputs = 1;

    public RegressionStock() {

    }

    public void setSeed(Integer seed) {
        this.seed = seed;
        this.rng = new Random(seed);
    }

    public Map<String, Integer> predict(Map<String, Map<String, Float>> codeDatas, List<String> outputs, List<String> cs, Integer dayNum, Boolean isClassify) {
        List<String> columns = new ArrayList<>(cs);
        columns.addAll(outputs);
        Collections.sort(columns);
        String path = String.join("_", columns) + "_" + dayNum;
        File model = new File(path);
        MultiLayerNetwork net = null;
        try {
            net = ModelSerializer.restoreMultiLayerNetwork(model);
        } catch (IOException e) {
            log.error("reload model data error:{}", e.getMessage());
            return null;
        }
        DataSetIterator input, input_2 = null, input_3 = null;
        try {
            input = getTestingData(new ArrayList<>(codeDatas.values()), outputs, isClassify);
            if (outputs.contains("up_2")) {
                input_2 = getTestingData(new ArrayList<>(codeDatas.values()), Collections.singletonList("up_0"), isClassify);
            }
            if (outputs.contains("up_5")) {
                input_2 = getTestingData(new ArrayList<>(codeDatas.values()), Collections.singletonList("up_0"), isClassify);
                input_3 = getTestingData(new ArrayList<>(codeDatas.values()), Collections.singletonList("up_2"), isClassify);
            }
        } catch (IOException | InterruptedException e) {
            log.error("load test iter error:{}", e.getMessage());
            return null;
        }
        if (isClassify) {
            Evaluation evaluation = net.evaluate(input);
            log.info("=========== output:{} ==========", outputs);
            log.info(evaluation.stats());
            if (input_2 != null) {
                Evaluation evaluation_2 = net.evaluate(input_2);
                log.info("=========== output:up_0 ==========");
                log.info(evaluation_2.stats());
            }
            if (input_3 != null) {
                Evaluation evaluation_3 = net.evaluate(input_3);
                log.info("=========== output:up_2 ==========");
                log.info(evaluation_3.stats());
            }
            input.reset();
            int[] result = net.predict(input.next().getFeatures());
            Object[] codes = codeDatas.keySet().toArray();
            Map<String, Integer> resultMap = new LinkedHashMap<>();
            for (int i = 0; i < codes.length; i++) {
                resultMap.put((String) codes[i], result[i]);
            }
            return resultMap;
        } else {
            //todo 完善regression 输出匹配
            return (Map<String, Integer>) net.output(input);
        }
    }

    public void train(File trainFile, List<Pair<Integer, String>> hiddens, Float learningRate, Integer nEpochs, Integer dayNum, List<String> inputs, List<String> outputs, Boolean update, Boolean isClassify, String netType) {
        try {
            List<String> columns = new ArrayList<>();
            for (int i = 0; i < dayNum; i++) {
                for (String c : inputs) {
                    String column = i + "_" + c;
                    columns.add(column);
                }
            }
            columns.addAll(outputs);
            inputs.addAll(outputs);
            Collections.sort(columns);
            Collections.sort(inputs);
            String path = String.join("_", inputs) + "_" + dayNum;
            File model = new File(path);
            if (!update && model.exists()) {
                log.info("net model:{} already exists ", model.getPath());
                return;
            }
            train(readTrainData(trainFile, columns), hiddens, learningRate, nEpochs, outputs, model, isClassify, netType);
        } catch (IOException e) {
            log.error("init train data error:{}", trainFile.getPath());
        }
    }

    private void train(List<Map<String, Float>> data, List<Pair<Integer, String>> hiddens, Float learningRate, Integer nEpochs, List<String> outputs, File model, Boolean isClassify, String netType) {
        List<DataSetIterator> iterators = null;
        try {
            iterators = getTrainingData(data, outputs, isClassify);
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
            return;
        }
        DataSetIterator trainIterator = iterators.get(1);
        DataSetIterator testIterator = iterators.get(0);
        NeuralNetConfiguration.ListBuilder builder = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .iterations(1)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .learningRate(learningRate)
//                .weightInit(WeightInit.XAVIER)
                .updater(Updater.NESTEROVS)     //To configure: .updater(new Nesterovs(0.9))
                .list()
                .layer(0, parseNet(netType, numInputs, hiddens.get(0).getKey(), hiddens.get(0).getValue()));
        for (int i = 1; i < hiddens.size(); i++) {
            builder.layer(i, parseNet(netType, hiddens.get(i - 1).getKey(), hiddens.get(i).getKey(), hiddens.get(i).getValue()));
        }
        builder.layer(hiddens.size(), new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                .weightInit(WeightInit.XAVIER)
                .activation(Activation.SOFTMAX).weightInit(WeightInit.XAVIER)
                .nIn(hiddens.get(hiddens.size() - 1).getKey()).nOut(isClassify ? outputs.size() * 2 : outputs.size()).build());
        MultiLayerConfiguration conf = builder.pretrain(false).backprop(true).build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        net.setListeners(new ScoreIterationListener(1000));

//        UIServer uiServer = UIServer.getInstance();
//
//        StatsStorage statsStorage = new InMemoryStatsStorage();             //Alternative: new FileStatsStorage(File) - see UIStorageExample
//        int listenerFrequency = 1;
//        net.setListeners(new StatsListener(statsStorage, listenerFrequency));
//
//        uiServer.attach(statsStorage);

        Evaluation evaluation = new Evaluation();
        double lastF1Score = 0;
        for (int i = 0; i < nEpochs; i++) {
//            trainIterator.reset();
            net.fit(trainIterator);
            if (i % 30 == 29) {
                log.info("iterator num:{} / {}", i + 1, nEpochs);
                evaluation = net.evaluate(testIterator);
                double f1 = evaluation.f1();
                log.info(evaluation.stats());
                if (f1 > lastF1Score) {
                    log.info("get higher f1 score this:{} last:{}", f1, lastF1Score);
                    try {
                        ModelSerializer.writeModel(net, model, true);
                        log.info("save net model data success:{}", model.getPath());
                    } catch (IOException e) {
                        log.error("save net model data error:{}", e.getMessage());
                    }
                } else {
                    log.info("not get higher f1 score this:{} last:{}", f1, lastF1Score);
                }
            }
        }
        try {
            ModelSerializer.writeModel(net, model, true);
            log.info("save net model data success:{}", model.getPath());
        } catch (IOException e) {
            log.error("save net model data error:{}", e.getMessage());
        }
    }

    private FeedForwardLayer parseNet(String netType, int inNums, int outNum, String activation) {
        switch (netType.toLowerCase()) {
            case "lstm":
                return new LSTM.Builder().nIn(inNums).nOut(outNum)
                        .weightInit(WeightInit.XAVIER)
                        .activation(Activation.fromString(activation))
                        .build();
            default:
                return new DenseLayer.Builder().nIn(inNums).nOut(outNum)
                        .weightInit(WeightInit.XAVIER)
                        .activation(Activation.fromString(activation))
                        .build();

        }
    }

    public static List<Map<String, Float>> readTrainData(File file, List<String> columns) throws IOException {
        List<Map<String, Float>> resList = new ArrayList<>();
        List<String> lines = Files.readAllLines(file.toPath());
        List<String> properties = Arrays.asList(lines.get(0).split(","));
        for (int i = 1; i < lines.size(); i++) {
            String[] line = lines.get(i).split(",");
            // 保证顺序
            Map<String, Float> data = new TreeMap<>();
            for (int j = 0; j < line.length; j++) {
                String key = properties.get(j);
                if (columns == null || columns.size() == 0) {
                    Float value = Float.valueOf(line[j]);
                    data.put(key, value);
                } else if (columns.contains(key)) {
                    Float value = Float.valueOf(line[j]);
                    data.put(key, value);
                }
            }
            resList.add(data);
        }
        return resList;
    }

    public static Boolean checkOutputValue(String key) {
        return "start".equals(key) || "code".equals(key) || "end".equals(key) || "volume".equals(key) || "rate".equals(key) || "up_0".equals(key) || "up_2".equals(key) || "up_5".equals(key) || "up_0_2".equals(key) || "up_0_3".equals(key);
    }


    private DataSetIterator getTestingData(List<Map<String, Float>> data, List<String> outputs, Boolean isClassify) throws IOException, InterruptedException {
        File file = new File("test_temp_" + StringUtils.join("_", outputs));
        List<String> lines = loadDataAsList(data, outputs, isClassify);
        Integer inputLength = data.get(0).size();
        Files.write(file.toPath(), lines);
        RecordReader testReader = new CSVRecordReader();
        testReader.initialize(new FileSplit(file));
        testReader.initialize(new FileSplit(file));
        DataSetIterator testIter;
        if (isClassify) {
            testIter = new RecordReaderDataSetIterator(testReader, data.size(), lines.get(0).split(",").length - outputs.size(), outputs.size() * 2);
        } else {
            testIter = new RecordReaderDataSetIterator(testReader, data.size(), inputLength, inputLength + outputs.size() - 1, true);
        }
        return testIter;
    }

    private List<DataSetIterator> getTrainingData(List<Map<String, Float>> data, List<String> outputs, Boolean isClassify) throws IOException, InterruptedException {
        List<String> lines = loadDataAsList(data, outputs, isClassify);
        Collections.shuffle(lines, rng);
        List<String> testLines = lines.subList(0, lines.size() / 10);
        List<String> trainLines = lines.subList(lines.size() / 10, lines.size());
        Files.write(test_temp.toPath(), testLines);
        Files.write(train_temp.toPath(), trainLines);
        RecordReader trainReader = new CSVRecordReader();
        RecordReader testReader = new CSVRecordReader();
        trainReader.initialize(new FileSplit(train_temp));
        testReader.initialize(new FileSplit(test_temp));
        DataSetIterator trainIter;
        DataSetIterator testIter;
        if (isClassify) {
            trainIter = new RecordReaderDataSetIterator(trainReader, batchSize, numInputs, outputs.size() * 2);
            testIter = new RecordReaderDataSetIterator(testReader, batchSize, numInputs, outputs.size() * 2);
        } else {
            trainIter = new RecordReaderDataSetIterator(trainReader, batchSize, numInputs, numInputs + outputs.size() - 1, true);
            testIter = new RecordReaderDataSetIterator(testReader, batchSize, numInputs, numInputs + outputs.size() - 1, true);
        }
        List<DataSetIterator> result = new ArrayList<>();
        result.add(testIter);
        result.add(trainIter);

//        List<INDArray> arrayList = new ArrayList<>();
//        for (float[] floats : inputMap.values()) {
//            arrayList.add(Nd4j.create(floats, new int[]{length, 1}));
//        }
//        INDArray input = Nd4j.hstack(arrayList);
//        INDArray output = Nd4j.create(outputs, new int[]{length, 1});
//        DataSet dataSet = new DataSet(input, output);
//        List<DataSet> listDs = dataSet.asList();
//        Collections.shuffle(listDs, rng);
//        int testSize = (listDs.size() / 10) / batchSize * batchSize;
//        int trainSize = listDs.size() / batchSize * batchSize;
//        List<DataSet> testData = listDs.subList(0, testSize);
//        List<DataSet> trainData = listDs.subList(testSize, trainSize);

//        result.add(new ListDataSetIterator(testData, batchSize));
//        result.add(new ListDataSetIterator(trainData, batchSize));
        return result;
    }

    private List<String> loadDataAsList(List<Map<String, Float>> data, List<String> outputs, Boolean isClassify) {
        final int length = data.size();
        Map<String, float[]> inputMap = new TreeMap<>();
        Map<String, float[]> outputMap = new TreeMap<>();
        List<Float> aveStarts = new ArrayList<>();
        List<Float> aveVolumes = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            Map<String, Float> d = data.get(i);
            int inputNums = 0;
            for (Map.Entry<String, Float> m : d.entrySet()) {
                if (outputs.contains(m.getKey())) {
                    if (!outputMap.containsKey(m.getKey())) {
                        float[] nF = new float[length];
                        outputMap.put(m.getKey(), nF);
                    }
                    outputMap.get(m.getKey())[i] = m.getValue();
                } else if (m.getKey().equals(avestart)) {
                    aveStarts.add(m.getValue());
                } else if (m.getKey().equals(avevolume)) {
                    aveVolumes.add(m.getValue());
                } else if (checkOutputValue(m.getKey())) {
                } else {
                    if (!inputMap.containsKey(m.getKey())) {
                        float[] newF = new float[data.size()];
                        inputMap.put(m.getKey(), newF);
                    }
                    inputNums++;
                    float[] inF = inputMap.get(m.getKey());
                    inF[i] = m.getValue();
                }
            }
            numInputs = inputNums;
        }
        List<float[]> values = new ArrayList<>(inputMap.values());
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            StringBuilder line = new StringBuilder();
            for (float[] vs : values) {
                line.append(vs[i]).append(",");
            }
            for (float[] os : outputMap.values()) {
                if (isClassify) {
                    line.append((int) os[i]).append(",");
                } else {
                    line.append(os[i]).append(",");
                }

            }
            line.deleteCharAt(line.length() - 1);
            lines.add(line.toString());
        }
        return lines;
    }
}
