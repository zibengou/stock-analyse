package com.stock.analyseservice.dao.repository;

import com.stock.analyseservice.dao.domain.StockHistoryOriginal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface StockHistoryOriginalRepository extends JpaRepository<StockHistoryOriginal, Integer> {
    List<StockHistoryOriginal> findAllByStockCode(String stockCode);

    List<StockHistoryOriginal> findAllByStockCodeInAndTimeBetween(List<String> stockCodes, Date start, Date end);

    StockHistoryOriginal findByStockCodeAndTime(String code, Date time);

}

