package com.fund.collect.eastmoney.entity.history;

import lombok.Data;

import java.util.List;

@Data
public class EastmoneyHistoryData {
    /**
     * 数据体
     */
    private List<EastmoneyHistoryLSJZ> LSJZList;
    /**
     * 001 开放,场内
     * 005 货币类基金
     * 007 QDII
     */
    private String FundType;
    /**
     *
     */
    private String SYType;
    /**
     *
     */
    private String isNewType;
    /**
     * 特征
     */
    private String Feature;
}
