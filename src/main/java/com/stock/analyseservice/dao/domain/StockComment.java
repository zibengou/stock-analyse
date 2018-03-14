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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;


@Data
@Entity
@DynamicUpdate
@DynamicInsert
public class StockComment {
    public StockComment() {
    }

    public StockComment(LocalDateTime time, String stockCode, String content, Integer commentCount, String commentSource) {
        this.time = Timestamp.valueOf(time);
        this.stockCode = stockCode;
        this.content = content;
        this.commentCount = commentCount;
        this.commentSource = commentSource;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    private Date time;
    private String stockCode;

    private String content;
    private Integer commentCount;
    private String commentSource;

    private String label;
    private String machineLabel;
    private Boolean edited;
    private Boolean machineEdited;
    private Boolean deleted;

    private Date createTime;
    private Date updateTime;
}
