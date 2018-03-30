package com.stock.analyseservice.dao.repository;

import com.stock.analyseservice.dao.domain.StockPredict;
import com.stock.analyseservice.dao.domain.StockRealtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;


@Repository
public interface StockPredictRepository extends JpaRepository<StockPredict, Integer> {
    StockPredict findByTimeAndInputsAndOutputAndNetType(Date time, String inputs, String output, String netType);

    StockPredict findByTime(Date time);
}

