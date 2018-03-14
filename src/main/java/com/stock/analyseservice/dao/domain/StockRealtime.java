package com.stock.analyseservice.dao.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stock.analyseservice.task.Crawler;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.joda.time.DateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;


@Data
@Entity
@DynamicUpdate
@DynamicInsert
public class StockRealtime {
    public StockRealtime() {
    }

    public StockRealtime(String code, String[] values) {
        this.stockCode = code;
//        this.stockName = values[0];
        this.start = Float.valueOf(values[1]);
        this.lastStart = Float.valueOf(values[2]);
        this.now = Float.valueOf(values[3]);
        this.high = Float.valueOf(values[4]);
        this.low = Float.valueOf(values[5]);
        this.buy = Float.valueOf(values[6]);
        this.sell = Float.valueOf(values[7]);
        this.volume = Float.valueOf(values[8]) / 100;
        this.price = Float.valueOf(values[9]) / 10000;
        this.b1Volume = Float.valueOf(values[10]);
        this.b1 = Float.valueOf(values[11]);
        this.b2Volume = Float.valueOf(values[12]);
        this.b2 = Float.valueOf(values[13]);
        this.b3Volume = Float.valueOf(values[14]);
        this.b3 = Float.valueOf(values[15]);
        this.b4Volume = Float.valueOf(values[16]);
        this.b4 = Float.valueOf(values[17]);
        this.b5Volume = Float.valueOf(values[18]);
        this.b5 = Float.valueOf(values[19]);
        this.s1Volume = Float.valueOf(values[20]);
        this.s1 = Float.valueOf(values[21]);
        this.s2Volume = Float.valueOf(values[22]);
        this.s2 = Float.valueOf(values[23]);
        this.s3Volume = Float.valueOf(values[24]);
        this.s3 = Float.valueOf(values[25]);
        this.s4Volume = Float.valueOf(values[26]);
        this.s4 = Float.valueOf(values[27]);
        this.s5Volume = Float.valueOf(values[28]);
        this.s5 = Float.valueOf(values[29]);
        String date = values[30];
        String time = values[31];
        LocalDateTime t = LocalDateTime.parse(date + " " + time, Crawler.timeFormat);
        this.time = Timestamp.valueOf(t);

    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    private Integer id;

    private Date time;

    private String stockCode;
    private String stockName;

    private Float start;
    private Float lastStart;
    private Float now;
    private Float high;
    private Float low;
    private Float buy;
    private Float sell;
    private Float volume;
    private Float price;

    private Float b1Volume;
    private Float b1;
    private Float b2Volume;
    private Float b2;
    private Float b3Volume;
    private Float b3;
    private Float b4Volume;
    private Float b4;
    private Float b5Volume;
    private Float b5;
    private Float s1Volume;
    private Float s1;
    private Float s2Volume;
    private Float s2;
    private Float s3Volume;
    private Float s3;
    private Float s4Volume;
    private Float s4;
    private Float s5Volume;
    private Float s5;

    private Boolean deleted;
    private Date createTime;
}
