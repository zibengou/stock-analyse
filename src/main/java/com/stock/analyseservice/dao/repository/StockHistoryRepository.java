package com.stock.analyseservice.dao.repository;

import com.stock.analyseservice.dao.domain.StockHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface StockHistoryRepository extends JpaRepository<StockHistory, Integer> {

    List<StockHistory> findByStockCode(String code);

    StockHistory findByStockCodeAndTime(String stockCode, Date time);

    List<StockHistory> findByStockCodeInAndTime(List<String> stockCodes, Date time);

    List<StockHistory> findByStockCodeAndTimeBetween(String stockCode, Date start, Date end);

    List<StockHistory> findByStockCodeAndTimeBetweenOrderByTimeDesc(String stockCode, Date start, Date end);
}

