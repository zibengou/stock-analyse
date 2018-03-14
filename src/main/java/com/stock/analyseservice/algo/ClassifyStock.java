package com.stock.analyseservice.algo;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.datavec.api.util.ClassPathResource;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.junit.Test;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.io.IOException;

public class ClassifyStock {

    int seed = 1;
    double learningRate = 0.005;
    int batchSize = 1000;

    @Test
    public void tainTest() {
        try {
            train(20, 1000, 300);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void train(int size, int numHiddenNodes, int nEpochs) throws IOException, InterruptedException {
        //Load the training data
        RecordReader rr = new CSVRecordReader();
//        rr.initialize(new FileSplit(new File("src/main/resources/classification/linear_data_train.csv")));
        rr.initialize(new FileSplit(new File("train_data")));
        DataSetIterator trainIter = new RecordReaderDataSetIterator(rr, batchSize, 0, size);

        //Load the test/evaluation data:
        RecordReader rrTest = new CSVRecordReader();
        rrTest.initialize(new FileSplit(new File("test_data")));
        DataSetIterator testIter = new RecordReaderDataSetIterator(rrTest, batchSize, 0, size);

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .iterations(1)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .learningRate(learningRate)
                .updater(Updater.NESTEROVS)     //To configure: .updater(new Nesterovs(0.9))
                .list()
                .layer(0, new DenseLayer.Builder().nIn(size).nOut(numHiddenNodes)
                        .weightInit(WeightInit.XAVIER)
                        .activation(Activation.RELU)
                        .build())
                .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .weightInit(WeightInit.XAVIER)
                        .activation(Activation.SOFTMAX).weightInit(WeightInit.XAVIER)
                        .nIn(numHiddenNodes).nOut(size).build())
                .pretrain(false).backprop(true).build();


        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();
        model.setListeners(new ScoreIterationListener(100));  //Print score every 10 parameter updates


        for (int n = 0; n < nEpochs; n++) {
            model.fit(trainIter);
            if (n % 50 == 0) {
                System.out.println("train finished epoch num : " + (n + 1));
            }
        }

        System.out.println("Evaluate model....");
        testModel(model, testIter, size);
        File locationToSave = new File("model.zip");      //Where to save the network. Note: the file is in .zip format - can be opened externally
        ModelSerializer.writeModel(model, locationToSave, true);
    }

    public void predict(int size) throws IOException, InterruptedException {
        RecordReader rrTest = new CSVRecordReader();
        rrTest.initialize(new FileSplit(new File("test_data")));
        DataSetIterator testIter = new RecordReaderDataSetIterator(rrTest, 1, 0, size);
        File locationToSave = new File("model.zip");
        MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(locationToSave);
        testModel(model, testIter, size);
    }

    private void testModel(MultiLayerNetwork model, DataSetIterator testIter, int size) {
        Evaluation eval = new Evaluation(size);
        int down2Down = 0;
        int down2up = 0;
        int up2Up = 0;
        int up2Down = 0;
        while (testIter.hasNext()) {
            DataSet t = testIter.next();
            int real = (int) t.getLabels().getDouble(1);
            INDArray features = t.getFeatureMatrix();
            INDArray lables = t.getLabels();
            INDArray predicted = model.output(features);
            int[] res = model.predict(features);
            eval.eval(lables, predicted);
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
        }
        System.out.println("up-up:" + up2Up + "  down-down:" + down2Down + "  up-down:" + up2Down + "  down-up:" + down2up);
        System.out.println(eval.stats());
    }
}
