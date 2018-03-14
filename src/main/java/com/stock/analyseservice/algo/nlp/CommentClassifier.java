package com.stock.analyseservice.algo.nlp;

import com.google.common.io.Files;
import com.stock.analyseservice.algo.nlp.tools.LabelSeeker;
import com.stock.analyseservice.algo.nlp.tools.MeansBuilder;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.paragraphvectors.ParagraphVectors;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.text.documentiterator.FileLabelAwareIterator;
import org.deeplearning4j.text.documentiterator.LabelAwareIterator;
import org.deeplearning4j.text.documentiterator.LabelledDocument;
import org.deeplearning4j.text.documentiterator.SimpleLabelAwareIterator;
import org.deeplearning4j.text.tokenization.tokenizerFactory.ChineseTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.primitives.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

public class CommentClassifier {
    private static CommentClassifier instance = new CommentClassifier();

    private static final Logger log = LoggerFactory.getLogger(CommentClassifier.class);
    private static final String PATH = "comment_model";
    private static final String STOPWORDS = "stopwords.txt";
    private ParagraphVectors paragraphVectors;
    private TokenizerFactory tokenizerFactory;

    public static CommentClassifier newInstance() {
        return instance;
    }

    private CommentClassifier() {
        File modelFile = new File(PATH);
        if (modelFile.exists()) {
            try {
                paragraphVectors = WordVectorSerializer.readParagraphVectors(modelFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        tokenizerFactory = new ChineseTokenizerFactory();
    }

    /***
     * 训练评论数据
     * @param data key-评论内容,value-标签组
     */
    public void train(Map<String, List<String>> data) {
        File modelFile = new File(PATH);
        File stopFile = new File(STOPWORDS);
        List<LabelledDocument> documents = new ArrayList<>();
        data.forEach((content, labels) -> {
            LabelledDocument document = new LabelledDocument();
            document.setContent(content);
            document.setLabels(labels);
            document.setId(String.valueOf(content.hashCode()));
            documents.add(document);
        });
        LabelAwareIterator iterator = new SimpleLabelAwareIterator(documents);
        List<String> stopWordList = new ArrayList<>();
        if (stopFile.exists()) {
            try {
                stopWordList = Files.readLines(stopFile, Charset.defaultCharset());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        paragraphVectors = new ParagraphVectors.Builder()
                .learningRate(0.025)
                .minLearningRate(0.001)
                .batchSize(100)
                .epochs(20)
                .iterate(iterator)
                .trainWordVectors(true)
                .labelsSource(iterator.getLabelsSource())
                .tokenizerFactory(tokenizerFactory)
                .stopWords(stopWordList)
                .build();
        paragraphVectors.fit();
        if (modelFile.exists()) {
            modelFile.delete();
        }
        WordVectorSerializer.writeParagraphVectors(paragraphVectors, modelFile);
    }

    public List<Pair<String, Double>> classify(String content, List<String> labels) {
        MeansBuilder meansBuilder = new MeansBuilder(
                (InMemoryLookupTable<VocabWord>) paragraphVectors.getLookupTable(),
                tokenizerFactory);
        // paragraphVectors 不支持序列化labelsource
        LabelSeeker seeker = new LabelSeeker(labels,
                (InMemoryLookupTable<VocabWord>) paragraphVectors.getLookupTable());

        INDArray documentAsCentroid = meansBuilder.documentAsVector(content);
        return seeker.getScores(documentAsCentroid);
    }
}
