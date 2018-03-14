package com.stock.analyseservice.dao.repository;

import com.stock.analyseservice.dao.domain.StockComment;
import com.stock.analyseservice.dao.domain.StockHistoryOriginal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface StockCommentRepository extends JpaRepository<StockComment, Integer> {
    List<StockComment> findByEditedIsTrue();

    List<StockComment> findByEditedIsTrueAndLabelIn(List<String> labels);

    List<StockComment> findByStockCodeAndTimeBetween(String stockCode, Date start, Date end);

    List<StockComment> findAllByEditedIsFalse();

    List<StockComment> findAllByStockCodeIsAndEditedIsFalseAndCommentCountGreaterThan(String stockCode, Integer count, Pageable commentCount);

    List<StockComment> findAllByStockCodeIsAndEditedIsTrue(String stockCode);

    List<StockComment> findAllByEditedIsFalseAndCommentCountGreaterThan(Integer count, Pageable commentCount);

}

