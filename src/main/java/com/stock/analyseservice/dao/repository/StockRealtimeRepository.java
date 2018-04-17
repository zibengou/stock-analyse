package com.stock.analyseservice.dao.repository;

import com.stock.analyseservice.dao.domain.StockRealtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;


@Repository
public interface StockRealtimeRepository extends JpaRepository<StockRealtime, Integer> {

    StockRealtime findByStockCodeAndTime(String code, Date time);

    List<StockRealtime> findByTimeBetweenAndStockCodeIn(Date start, Date end, List<String> codes);
}

