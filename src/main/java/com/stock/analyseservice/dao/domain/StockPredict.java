package com.stock.analyseservice.dao.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

@Data
@Entity
@DynamicUpdate
@DynamicInsert
public class StockPredict {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    private Integer id;

    private String inputs;
    private String output;
    private String netType;

    private Date time;
    private String codes;

    public StockPredict() {
    }

    public StockPredict(String inputs, String output, String netType, Date time, String codes) {
        this.inputs = inputs;
        this.output = output;
        this.netType = netType;
        this.time = time;
        this.codes = codes;
    }

    @CreationTimestamp
    private Date creatTime;

    @UpdateTimestamp
    private Date updateTime;
}
