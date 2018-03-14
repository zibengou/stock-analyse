package com.stock.analyseservice.dao.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Date;


@Data
@Entity
@DynamicUpdate
@DynamicInsert
public class StockHistory {
    public StockHistory() {
    }

    public StockHistory(StockHistoryOriginal original) {
        this.setStockCode(original.getStockCode());
        this.setTime(original.getTime());
        this.setStart(original.getStart());
        this.setEnd(original.getEnd());
        this.setTurnover(original.getTurnover());
        this.setRate(original.getRate());
        this.setHigh(original.getHigh());
        this.setLow(original.getLow());
        this.setVolume(original.getVolume());
        this.setVolumePrice(original.getVolumePrice());

        this.setLowRate(low / start);
        this.setHighRate(high / start);
    }


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    private Integer id;

    private Date time;

    private String stockCode;

    private Float start;
    private Float end;
    private Float low;
    private Float high;
    private Float rate;
    private Float volume;
    private Float volumePrice;
    private Float turnover;

    private Float lowRate;
    private Float highRate;

    private Integer allCount;
    private Integer upCount;
    private Integer downCount;

    private Boolean deleted;
    private Date createTime;
}
