package com.stock.analyseservice.controller;

import com.stock.analyseservice.task.Crawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/crawl")
public class CrawlerController {
    @Autowired
    Crawler crawler;

    @RequestMapping(value = "/history", method = RequestMethod.GET)
    public void saveStockHistory(@RequestParam(value = "start") String start,
                                 @RequestParam(value = "end") String end,
                                 @RequestParam(value = "stock", required = false) String stock,
                                 @RequestParam(value = "isIndex", required = false) Boolean isIndex) {
        if (StringUtils.isEmpty(stock)) {
            crawler.StockHistoryData(start, end, isIndex);
        } else {
            for (String code : stock.split(",")) {
                crawler.StockHistoryData(start, end, code, isIndex);
            }
        }
        crawler.StockHistoryData(start, end, "000001", true);

    }

    @RequestMapping(value = "/realtime", method = RequestMethod.GET)
    public void saveStockHistory() {
        crawler.StockRealTimeData();
    }

    @RequestMapping(value = "/history", method = RequestMethod.POST)
    public void saveMoreStockHistory(@RequestParam(value = "start") String start,
                                     @RequestParam(value = "end") String end,
                                     @RequestParam(value = "isIndex", required = false) Boolean isIndex,
                                     @RequestBody(required = false) String stock) {
        if (StringUtils.isEmpty(stock)) {
            crawler.StockHistoryData(start, end, isIndex);
        } else {
            for (String code : stock.split(",")) {
                crawler.StockHistoryData(start, end, code, isIndex);
            }
        }
        crawler.StockHistoryData(start, end, "000001", true);
    }

    @RequestMapping(value = "/comment", method = RequestMethod.POST)
    public void saveMoreStockComment(@RequestBody String stock) {
        if (StringUtils.isEmpty(stock)) {
            crawler.StockComment();
        } else {
            for (String code : stock.split(",")) {
                crawler.StockComment(code);
            }
        }

    }

    @RequestMapping(value = "/comment", method = RequestMethod.GET)
    public void saveStockComment(@RequestParam(value = "stock", required = false) String stock) {
        if (StringUtils.isEmpty(stock)) {
            crawler.StockComment();
        } else {
            for (String code : stock.split(",")) {
                crawler.StockComment(code);
            }
        }

    }
}
