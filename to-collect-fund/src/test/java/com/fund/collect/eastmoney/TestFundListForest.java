package com.fund.collect.eastmoney;

import com.fund.collect.ToCollectFundApplication;
import com.fund.collect.eastmoney.entity.list.EastMoneyListResult;
import com.fund.collect.eastmoney.forest.FundListForest;
import com.fund.entity.dto.EastMoneyListDataDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
@SpringBootTest(classes = ToCollectFundApplication.class)
@Slf4j
public class TestFundListForest {
    @Autowired
    FundListForest fundListForest;

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Test
    public void testFundList() {
        LocalDate nowDate = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        EastMoneyListResult fundList = fundListForest.getFundList();
        List<EastMoneyListDataDto> list = fundList.getData().stream().map(v -> {
            EastMoneyListDataDto e = new EastMoneyListDataDto();
            e.setFundCode(v.get(0));
            e.setFundName(v.get(2));
            e.setFundType(v.get(3));
            e.setCreateDate(nowDate);
            e.setCreateTime(nowTime);
            return e;
        }).collect(Collectors.toList());
        List<EastMoneyListDataDto> splitList = new ArrayList<>();
        for (int i = 1; i <= list.size(); i++) {
            if (i % 500 == 0) {
                kafkaTemplate.send(EastMoneyListDataDto.TOPIC, splitList);
                splitList.clear();
            }
            splitList.add(list.get(i - 1));
        }
        if (!splitList.isEmpty()) {
            kafkaTemplate.send(EastMoneyListDataDto.TOPIC, splitList);
        }
    }

}
