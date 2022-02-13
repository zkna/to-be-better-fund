package com.collect_fund.eastmoney;

import com.alibaba.fastjson.JSON;
import com.collect_fund.ToCollectFundApplication;
import com.collect_fund.eastmoney.entity.company_list.EastmoneyCompanyListResult;
import com.collect_fund.eastmoney.forest.FundCompanyForest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ToCollectFundApplication.class)
public class TestFundCompanyForest {

    @Autowired
    private FundCompanyForest companyClient;

    @Test
    void getCompanyList(){
        EastmoneyCompanyListResult eastmoneyCompanyListResult = companyClient.getCompanyList();
//        System.out.println(JSON.toJSONString(eastmoneyCompanyListResult));

        eastmoneyCompanyListResult.getData().forEach(v->{
            System.out.println(JSON.toJSONString(v));
        });
    }
}
