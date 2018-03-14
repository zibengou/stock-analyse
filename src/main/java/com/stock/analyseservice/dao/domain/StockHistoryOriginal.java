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
import java.time.ZoneId;
import java.util.Date;


@Data
@Entity
@DynamicUpdate
@DynamicInsert
public class StockHistoryOriginal {
    public StockHistoryOriginal() {
    }

    public StockHistoryOriginal(LocalDate time, String stockCode, Float start, Float end, Float low, Float high, Float rate, Float volume, Float volumePrice, Float turnover) {
        this.time = Timestamp.valueOf(time.atStartOfDay());
        this.stockCode = stockCode;
        this.start = start;
        this.end = end;
        this.low = low;
        this.high = high;
        this.rate = rate;
        this.volume = volume;
        this.volumePrice = volumePrice;
        this.turnover = turnover;
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
}
