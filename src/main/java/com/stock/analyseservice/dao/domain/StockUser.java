package com.stock.analyseservice.dao.domain;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

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
public class StockUser {
    public StockUser() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    private String name;
    private String mail;

    @CreatedDate
    private Date createTime;

    @LastModifiedDate
    private Date updateTime;
}
