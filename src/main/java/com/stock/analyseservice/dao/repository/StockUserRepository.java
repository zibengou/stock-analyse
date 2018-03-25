package com.stock.analyseservice.dao.repository;

import com.stock.analyseservice.dao.domain.StockUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface StockUserRepository extends JpaRepository<StockUser, Integer> {

}

