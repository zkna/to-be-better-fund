package com.collect_fund.eastmoney.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.collect_fund.eastmoney.entity.all_rank.EastmoneyAllRankListData;
import com.collect_fund.eastmoney.entity.all_rank.EastmoneyAllRankListResult;
import com.collect_fund.eastmoney.forest.FundAllRankForest;
import com.dtflys.forest.Forest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.awt.print.Book;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @Author: tbb
 * @Date: 2022/2/14 0:56
 * @Description: 保存基金排名数据
 */
@RestController
@RequestMapping("/eastmoney/company")
@Slf4j
public class ToFundRankController {

    final static ExecutorService executorPool = new ThreadPoolExecutor(15, 15,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    private FundAllRankForest fundAllRankForest;

    CountDownLatch cd = null;

    @GetMapping(value = "/save")
    public String list() throws InterruptedException {

        EastmoneyAllRankListResult allRankListResult = fundAllRankForest.getFundAllRank("2022-02-12", "2022-02-12", 1, 50);
        cd = new CountDownLatch(allRankListResult.getAllPages() - 1);
        List<EastmoneyAllRankListData> pojoResultList = allRankListResult.getData().stream().map(v -> {
            EastmoneyAllRankListData allRankListData = new EastmoneyAllRankListData();
            String[] r = v.split(",", -1);
            allRankListData.setFundCode(r[0]);
            allRankListData.setFundShortName(r[1]);
            allRankListData.setFundShortCode(r[2]);
            allRankListData.setFundDate(StrUtil.isEmpty(r[3]) ? null : DateUtil.parseDate(r[3]));
            allRankListData.setUnitNet(toDouble(r[4]));
            allRankListData.setSumNet(toDouble(r[5]));
            allRankListData.setDayGrowRate(toDouble(r[6]));
            allRankListData.setLastWeek(toDouble(r[7]));
            allRankListData.setLast1Month(toDouble(r[8]));
            allRankListData.setLast3Month(toDouble(r[9]));
            allRankListData.setLast6Month(toDouble(r[10]));
            allRankListData.setLast1Year(toDouble(r[11]));
            allRankListData.setLast2Year(toDouble(r[12]));
            allRankListData.setLast3Year(toDouble(r[13]));
            allRankListData.setThisYear(toDouble(r[14]));
            allRankListData.setSinceEstablish(toDouble(r[15]));
            allRankListData.setFee(toDouble(r[20]));
            allRankListData.setBuyStatus(r[17]);
            return allRankListData;
        }).collect(Collectors.toList());
        ResponseEntity<String> responseEntity = restTemplate.postForEntity("http://to-store-fund/eastmoney/fund_rank/save", pojoResultList, String.class);
        log.info("returnBody:" + responseEntity.getBody());
        for (int i = 2; i <= allRankListResult.getAllPages(); i++) {
            executorPool.execute(new FundAllRankThread("2022-02-12", "2022-02-12", i, 50));
        }
        cd.await();
        return "true";
    }

    class FundAllRankThread extends Thread {
        private String startDate;
        private String endDate;
        private Integer pageIndex;
        private Integer pageSize;

        public FundAllRankThread(String startDate, String endDate, Integer pageIndex, Integer pageSize) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.pageIndex = pageIndex;
            this.pageSize = pageSize;
        }

        @SneakyThrows
        @Override
        public void run() {
            EastmoneyAllRankListResult allRankListResult = null;
            try {
                allRankListResult = fundAllRankForest.getFundAllRank(startDate, endDate, pageIndex, pageSize);
                List<EastmoneyAllRankListData> pojoResultList = allRankListResult.getData().stream().map(v -> {
                    EastmoneyAllRankListData allRankListData = new EastmoneyAllRankListData();
                    String[] r = v.split(",", -1);
                    allRankListData.setFundCode(r[0]);
                    allRankListData.setFundShortName(r[1]);
                    allRankListData.setFundShortCode(r[2]);
                    allRankListData.setFundDate(StrUtil.isEmpty(r[3]) ? null : DateUtil.parseDate(r[3]));
                    allRankListData.setUnitNet(toDouble(r[4]));
                    allRankListData.setSumNet(toDouble(r[5]));
                    allRankListData.setDayGrowRate(toDouble(r[6]));
                    allRankListData.setLastWeek(toDouble(r[7]));
                    allRankListData.setLast1Month(toDouble(r[8]));
                    allRankListData.setLast3Month(toDouble(r[9]));
                    allRankListData.setLast6Month(toDouble(r[10]));
                    allRankListData.setLast1Year(toDouble(r[11]));
                    allRankListData.setLast2Year(toDouble(r[12]));
                    allRankListData.setLast3Year(toDouble(r[13]));
                    allRankListData.setThisYear(toDouble(r[14]));
                    allRankListData.setSinceEstablish(toDouble(r[15]));
                    allRankListData.setFee(toDouble(r[20]));
                    allRankListData.setBuyStatus(r[17]);
                    return allRankListData;
                }).collect(Collectors.toList());
                ResponseEntity<String> responseEntity = restTemplate.postForEntity("http://to-store-fund/eastmoney/fund_rank/save", pojoResultList, String.class);
                log.info("Thread Id[" + Thread.currentThread().getId() + "]-End,Page Index: " + pageIndex + "]returnBody:" + responseEntity.getBody());
            } catch (Exception e) {
                log.error("Error:" + e.getMessage() + ",,,PageIndex:" + pageIndex + "||| " + JSON.toJSONString(allRankListResult));
            }
            cd.countDown();
        }
    }

    public static void main(String[] args) {

        String a = "{\"allNum\":13494,\"allPages\":270,\"allRecords\":13494,\"bbNum\":0,\"data\":[\"014417,泰康研究精选股票发起C,TKYJJXGPFQC,2022-02-11,0.8954,0.8954,,-4.07,-7.78,,,,,,-11.68,-10.46,2021-12-28,6,-10.46,,0.00%,,,,\",\"009619,博时女性消费主题混合A,BSNXXFZTHHA,2022-02-11,0.8534,0.9113,-2.01,-1.89,-5.44,-14.60,-15.99,-30.35,,,-11.30,-10.47,2020-06-30,1,-10.47149,1.80%,0.18%,1,0.18%,1,\",\"501209,银华富久食品饮料精选混合(LOF)A,YHFJSPYLJXHHLOFA,2022-02-11,0.8952,0.8952,-0.35,1,-3.65,-9.06,,,,,-10.50,-10.48,2021-09-03,1,-10.48,1.50%,0.15%,1,0.15%,1,\",\"010210,国泰中证计算机主题ETF联接C,GTZZJSJZTETFLJC,2022-02-11,0.8439,0.8439,-1.55,-1.41,-6.60,-10.22,-13.31,-11.75,,,-10.27,-10.48,2020-12-03,,-10.480535,,0.00%,,,,\",\"013050,兴业能源革新股票C,XYNYGXGPC,2022-02-11,0.8950,0.8950,-1.51,-2.79,-5.91,-7.99,,,,,-14.38,-10.50,2021-08-31,1,-10.5,,0.00%,,,,\",\"005347,诺德量化优选6个月持有期混合,NDLHYX6GYCYQHH,2022-02-11,0.8948,0.8948,-1.28,-2.67,-9.10,-14.96,-19.55,-25.79,,,-14.17,-10.52,2020-08-27,1,-10.52,1.50%,0.15%,1,0.15%,1,\",\"011523,前海联合产业趋势混合A,QHLHCYQSHHA,2022-02-11,0.8948,0.8948,-0.92,0.40,-6.75,-11.02,,,,,-8.65,-10.52,2021-08-17,1,-10.52,1.50%,0.15%,1,0.15%,1,\",\"012805,广发恒生科技指数(QDII)C,GFHSKJZSQDIIC,2022-02-11,0.8947,0.8947,-1.27,5.41,-0.51,-13.61,-10.53,,,,-1.52,-10.53,2021-08-11,1,-10.53,,0.00%,,,,\",\"014505,中银收益混合C,ZYSYHHC,2022-02-11,2.0027,2.0027,-1.73,-0.32,-6.42,,,,,,-11.76,-10.57,2021-12-29,1,-10.565802,,0.00%,,,,\",\"008702,华夏黄金ETF联接C,HXHJETFLJC,2022-02-11,0.8941,0.8941,-0.47,1.21,0.38,-2.51,2.11,-3.63,,,-0.17,-10.59,2020-07-16,1,-10.59,,0.00%,,,,\",\"012082,博时数字经济18个月封闭A,BSSZJJ18GYFBA,2022-02-11,0.8941,0.8941,,-3.78,-10,-17.64,-18.27,,,,-16.11,-10.59,2021-06-18,5,-10.59,1.50%,0.15%,1,0.15%,1,\",\"011334,鹏华品质优选混合C,PHPZYXHHC,2022-02-11,0.8940,0.8940,0.52,4.70,1.14,1.15,-0.42,-10.60,,,3.16,-10.60,2021-02-09,1,-10.6,,0.00%,,,,\",\"010877,浙商智选先锋一年持有混合C,ZSZXXFYNCYHHC,2022-02-11,0.8939,0.8939,-1.91,-0.48,-7.17,-10.75,-10.61,,,,-13.48,-10.61,2021-08-10,3,-10.61,,0.00%,,,,\",\"010308,东财信息产业精选C,DCXXCYJXC,2022-02-11,0.8938,0.8938,-1.17,-3.34,-7.84,-13.16,-15.39,-16.75,,,-13.78,-10.62,2020-10-30,1,-10.62,,0.00%,,,,\",\"013270,前海开源聚利一年持有混合A,QHKYJLYNCYHHA,2022-02-11,0.8937,0.8937,-1.69,-1.26,-3.90,-10.61,,,,,-9.94,-10.63,2021-10-26,3,-10.63,1.50%,0.15%,1,0.15%,1,\",\"013177,华宝深证创新100ETF发起联接A,HBSZCX100ETFFQLJA,2022-02-11,0.8937,0.8937,-1.85,-2.95,-7.98,-14.32,,,,,-12.68,-10.63,2021-09-07,1,-10.63,1.00%,0.10%,1,0.10%,1,\",\"013171,华夏恒生互联网科技业ETF联接(QDII)A,HXHSHLWKJYETFLJQDIIA,2022-02-11,0.8935,0.8935,-1.13,6.58,2.18,-12.70,,,,,1.25,-10.65,2021-09-14,1,-10.65,1.20%,0.12%,1,0.12%,1,\",\"012004,招商价值成长混合C,ZSJZCZHHC,2022-02-11,0.8934,0.8934,-1.25,-0.26,-3.50,-5.84,,,,,-7.03,-10.66,2021-08-20,1,-10.66,,0.00%,,,,\",\"013271,前海开源聚利一年持有混合C,QHKYJLYNCYHHC,2022-02-11,0.8934,0.8934,-1.69,-1.26,-3.91,-10.64,,,,,-9.95,-10.66,2021-10-26,3,-10.66,,0.00%,,,,\",\"013597,招商中证全指证券公司指数(LOF)C,ZSZZQZZQGSZSLOFC,2022-02-11,1.1757,1.1757,-0.01,2.99,-2.38,-2.54,,,,,-5.27,-10.66,2021-09-13,1,-10.661094,,0.00%,,,,\",\"014902,长城新能源股票发起式A,CCXNYGPFQSA,2022-02-11,0.8933,0.8933,-2.48,-10.21,,,,,,,,-10.67,2022-01-19,1,-10.67,1.50%,0.15%,1,0.15%,1,\",\"013402,华夏恒生科技ETF发起式联接(QDII)A,HXHSKJETFFQSLJQDIIA,2022-02-11,0.8932,0.8932,-1.21,4.97,-0.65,-13.24,,,,,-1.68,-10.68,2021-09-28,1,-10.68,1.20%,0.12%,1,0.12%,1,\",\"001730,兴银大健康,XYDJK,2022-02-11,0.8930,0.8930,-3.15,-0.89,-13.05,-18.15,-23.35,-29.24,-2.30,22.33,-18.22,-10.70,2015-08-27,1,-10.7,1.50%,0.15%,1,0.15%,1,1.13\",\"011722,前海开源深圳特区精选股票A,QHKYSZTQJXGPA,2022-02-11,0.8930,0.8930,-0.49,2.02,-1.63,-5,-5.62,,,,-3.72,-10.70,2021-05-07,1,-10.7,1.50%,0.15%,1,0.15%,1,\",\"011524,前海联合产业趋势混合C,QHLHCYQSHHC,2022-02-11,0.8930,0.8930,-0.92,0.39,-6.78,-11.11,,,,,-8.69,-10.70,2021-08-17,1,-10.7,,0.00%,,,,\",\"014903,长城新能源股票发起式C,CCXNYGPFQSC,2022-02-11,0.8930,0.8930,-2.48,-10.23,,,,,,,,-10.70,2022-01-19,1,-10.7,,0.00%,,,,\",\"014642,上投摩根新兴动力混合C,STMGXXDLHHC,2022-02-11,6.4838,6.4838,-0.57,-1.31,-7.10,,,,,,-13.04,-10.71,2021-12-24,,-10.709908,,0.00%,,,,\",\"011820,兴业兴智一年持有期混合A,XYXZYNCYQHHA,2022-02-11,0.8929,0.8929,-1.04,-0.02,-4.10,-8.81,-10.11,,,,-9.48,-10.71,2021-05-10,3,-10.71,1.50%,0.15%,1,0.15%,1,\",\"012096,鑫元鑫动力混合A,XYXDLHHA,2022-02-11,0.8929,0.8929,-1.62,-5.61,-7.39,-14.98,-12.89,,,,-13.31,-10.71,2021-07-13,1,-10.71,1.20%,0.12%,1,0.12%,1,\",\"011273,泰信景气驱动12个月持有混合A,TXJQQD12GYCYHHA,2022-02-11,0.8926,0.8926,-0.59,-0.93,-4.78,-13.52,-18.22,,,,-10.75,-10.74,2021-04-08,3,-10.74,1.50%,0.15%,1,0.15%,1,\",\"013178,华宝深证创新100ETF发起联接C,HBSZCX100ETFFQLJC,2022-02-11,0.8925,0.8925,-1.85,-2.97,-8.01,-14.39,,,,,-12.71,-10.75,2021-09-07,1,-10.75,,0.00%,,,,\",\"011723,前海开源深圳特区精选股票C,QHKYSZTQJXGPC,2022-02-11,0.8924,0.8924,-0.48,2.02,-1.63,-5.01,-5.66,,,,-3.73,-10.76,2021-05-07,1,-10.76,,0.00%,,,,\",\"010559,汇安鑫利优选混合C,HAXLYXHHC,2022-02-11,0.8924,0.8924,-1.52,-1.71,-7.91,-9.75,-6.28,,,,-11.48,-10.76,2021-04-02,1,-10.76,,0.00%,,,,\",\"013172,华夏恒生互联网科技业ETF联接(QDII)C,HXHSHLWKJYETFLJQDIIC,2022-02-11,0.8924,0.8924,-1.13,6.57,2.16,-12.77,,,,,1.21,-10.76,2021-09-14,1,-10.76,,0.00%,,,,\",\"013209,金鹰大视野混合A,JYDSYHHA,2022-02-11,0.8923,0.8923,-1.98,-3.90,-7.71,-10.79,,,,,-12.96,-10.77,2021-08-25,1,-10.77,1.50%,0.15%,1,0.15%,1,\",\"970113,兴证资管金麒麟兴睿优选一年持有期混合B,XZZGJQLXRYXYNCYQHHB,2022-02-11,0.9508,2.0478,-0.78,0.37,-4.51,,,,,,-9.61,-10.77,2021-12-13,4,-10.773273,,,,,,\",\"008838,德邦量化对冲混合A,DBLHDCHHA,2022-02-11,0.8922,0.8922,-0.25,0.18,-0.08,-6.55,-10.19,-8.60,,,-2.81,-10.78,2020-04-29,1,-10.78,1.50%,0.15%,1,0.15%,1,\",\"870005,广发资管核心精选一年持有混合A,GFZGHXJXYNCYHHA,2022-02-11,0.8922,1.3127,-0.23,0.43,-4.58,-7.07,-7.15,,,,-8.58,-10.78,2021-06-02,1,-10.78,1.00%,1.00%,99,1.00%,99,\",\"013417,博时核心资产精选混合A,BSHXZCJXHHA,2022-02-11,0.8922,0.8922,-1.43,-3.57,-5.30,-10.96,,,,,-12.94,-10.78,2021-09-17,1,-10.78,1.50%,0.15%,1,0.15%,1,\",\"012559,天弘中证沪港深科技龙头指数A,THZZHGSKJLTZSA,2022-02-11,0.8922,0.8922,-1.98,-0.86,-6.32,-12.73,,,,,-10.02,-10.78,2021-10-26,1,-10.78,1.00%,0.10%,1,0.10%,1,\",\"013403,华夏恒生科技ETF发起式联接(QDII)C,HXHSKJETFFQSLJQDIIC,2022-02-11,0.8922,0.8922,-1.22,4.96,-0.67,-13.30,,,,,-1.72,-10.78,2021-09-28,1,-10.78,,0.00%,,,,\",\"013885,交银阿尔法核心混合C,JYAEFHXHHC,2022-02-11,3.7055,3.7055,-2.31,-4.55,-10.01,,,,,,-13.73,-10.79,2021-11-12,1,-10.786084,,0.00%,,,,\",\"013084,信诚中证智能家居指数(LOF)C,XCZZZNJJZSLOFC,2022-02-11,0.8715,0.8715,-1.68,-1.45,-9.58,-11.73,,,,,-12.75,-10.79,2021-08-26,1,-10.789231,,0.00%,,,,\",\"013027,银华富久食品饮料精选混合(LOF)C,YHFJSPYLJXHHLOFC,2022-02-11,0.8921,0.8921,-0.35,0.97,-3.70,-9.24,,,,,-10.58,-10.79,2021-09-03,1,-10.79,,0.00%,,,,\",\"970073,东证融汇成长优选混合A,DZRHCZYXHHA,2022-02-11,0.9940,1.3521,-1.69,-0.45,-6.60,-10.80,,,,,-10.64,-10.80,2021-11-08,1,-10.80402,1.20%,0.12%,1,0.12%,1,\",\"011313,东方红启华三年持有混合B,DFHQHSNCYHHB,2022-02-11,4.0468,4.0468,-1.93,-0.92,-3.68,-9.52,-11.31,,,,-8.59,-10.81,2021-07-26,3,-10.808428,1.50%,1.50%,99,1.50%,99,\",\"010409,富国消费精选30股票,FGXFJX30GP,2022-02-11,0.8919,0.8919,-0.88,-0.13,-3.83,-7.92,-4.22,-18.08,,,-8.42,-10.81,2020-11-05,1,-10.81,1.50%,0.15%,1,0.15%,1,\",\"012083,博时数字经济18个月封闭C,BSSZJJ18GYFBC,2022-02-11,0.8918,0.8918,,-3.79,-10.03,-17.73,-18.45,,,,-16.14,-10.82,2021-06-18,5,-10.82,,0.00%,,,,\",\"012560,天弘中证沪港深科技龙头指数C,THZZHGSKJLTZSC,2022-02-11,0.8917,0.8917,-1.98,-0.86,-6.34,-12.78,,,,,-10.03,-10.83,2021-10-26,1,-10.83,,0.00%,,,,\",\"009776,中欧阿尔法混合A,ZOAEFHHA,2022-02-11,0.8916,0.8916,-2.09,-8.26,-11.11,-26.28,-26.21,-32.60,,,-19.97,-10.84,2020-08-20,1,-10.84,1.50%,0.15%,1,0.15%,1,\"],\"etfNum\":0,\"fofNum\":338,\"gpNum\":2072,\"hhNum\":6738,\"lofNum\":388,\"pageIndex\":234,\"pageNum\":50,\"qdiiNum\":345,\"zqNum\":4339,\"zsNum\":1622}";
        String b = "{\"allNum\":13494,\"allPages\":270,\"allRecords\":13494,\"bbNum\":0,\"data\":[\"320013,诺安全球黄金,NAQQHJ,2022-02-10,1.0050,1.09,0.10,2.13,1.62,-2.05,2.76,-3.37,2.13,24.07,0.40,8.76,2011-01-13,1,,0.80%,0.08%,1,0.08%,1,24.69\",\"262001,景顺长城大中华混合(QDII)人民币,JSCCDZHHHQDIIRMB,2022-02-10,2.0340,2.4550,1.29,6.21,2.16,-5.83,-16.16,-28.98,33.86,65.74,1.09,161.71,2011-09-22,1,,1.60%,0.16%,1,0.16%,1,95.51\",\"050015,博时大中华亚太精选,BSDZHYTJX,2022-02-10,1.2390,1.3210,1.06,5.90,2.23,-6.42,-14.96,-29.36,0.65,13.88,-2.06,32.78,2010-07-27,1,,1.60%,0.16%,1,0.16%,1,8.59\",\"040046,华安纳斯达克100指数,HANSDK100ZS,2022-02-10,4.2010,4.2010,-2.39,1.50,-5.93,-8.51,-4.26,6.14,43.04,103.04,-10.16,320.10,2013-08-02,1,,1.20%,0.12%,1,0.12%,1,155.38\",\"118001,易方达亚洲精选股票,YFDYZJXGP,2022-02-10,1.1650,1.1650,1.04,7.08,5.53,-1.19,-4.35,-35.46,8.27,29.59,8.47,16.50,2010-01-21,1,,1.60%,0.16%,1,0.16%,1,47.84\",\"377016,上投摩根亚太优势混合,STMGYTYSHH,2022-02-10,0.9738,0.9738,0.71,4.44,0.89,-2.92,-7.02,-21.91,15.11,33.03,1.08,-2.62,2007-10-22,1,,1.80%,0.18%,1,0.18%,1,51.21\",\"378006,上投摩根全球新兴市场混合,STMGQQXXSCHH,2022-02-10,1.1372,1.2361,0.54,4.52,2.23,-0.24,-6.26,-21.16,12.97,29.52,2.18,23.82,2011-01-30,1,,1.60%,0.16%,1,0.16%,1,41.18\",\"202801,南方全球精选配置,NFQQJXPZ,2022-02-10,0.9890,1.13,0,5.66,-1.38,-11.56,-13.38,-16.47,2.17,25.65,-4.94,12.08,2007-09-19,1,,1.60%,0.16%,1,0.16%,1,38.20\",\"164701,汇添富黄金及贵金属,HTFHJJGJS,2022-02-10,0.7510,0.7510,-0.13,1.90,1.49,-2.21,2.46,-4.21,1.62,22.51,-0.13,-24.90,2011-08-31,1,,0.80%,0.08%,1,0.08%,1,21.92\",\"486001,工银全球股票(QDII)人民币,GYQQGPQDIIRMB,2022-02-10,1.4280,2.2510,-0.83,3.33,-1.24,-9.16,-11.03,-14.39,9.13,40.60,-5.43,165.79,2008-02-14,1,,1.60%,0.16%,1,0.16%,1,78\",\"486002,工银全球精选股票(QDII),GYQQJXGPQDII,2022-02-10,2.9670,2.9670,-1.40,2.24,-3.23,-9.29,-8.34,-0.77,9.65,41.62,-7.97,196.70,2010-05-25,1,,1.60%,0.16%,1,0.16%,1,75.46\",\"000044,嘉实美国成长股票美元现汇,JSMGCZGPMYXH,2022-02-10,2.9810,2.9810,-1.88,2.72,-3.71,-6.38,0.20,8.95,46.63,87.01,-7.85,198.10,2013-06-14,,,,1.50%,,,,126.35\",\"539001,建信纳斯达克100指数(QDII)人民币A,JXNSDK100ZSQDIIRMBA,2022-02-10,1.6539,1.6539,-2.36,1.52,-5.62,-8.10,-5.11,2.09,4.15,21.79,-9.53,65.39,2010-09-14,1,,1.20%,0.12%,1,0.12%,1,37.14\",\"050202,博时亚洲票息收益债券现汇,BSYZPXSYZQXH,2022-02-10,0.2106,0.2332,0.14,-0.14,-0.89,-0.80,-6.52,-9.61,-0.19,7.23,-1.63,50.87,2013-02-01,,,,0.80%,,,,13.23\",\"457001,国富亚洲机会股票(QDII),GFYZJHGPQDII,2022-02-10,1.4290,1.5960,1.35,6.88,-1.45,-7.75,-16.82,-26.49,26.29,68.58,-4.73,62.16,2012-02-22,1,,1.50%,0.15%,1,0.15%,1,81.80\",\"070012,嘉实海外中国股票混合,JSHWZGGPHH,2022-02-10,0.8520,0.8540,0.12,3.15,1.55,-8.19,-13.15,-37.54,-5.29,4.07,-0.93,-14.66,2007-10-12,1,,1.50%,0.15%,1,0.15%,1,20.36\",\"160719,嘉实黄金,JSHJ,2022-02-10,0.8760,0.8760,-0.11,1.98,1.51,-2.12,2.58,-3.63,1.15,22.01,0.11,-12.40,2011-08-04,1,,0.80%,0.08%,1,0.08%,1,22.52\",\"519696,交银环球精选混合(QDII),JYHQJXHHQDII,2022-02-10,2.3190,3.11,-0.69,2.98,-0.23,-6.44,-9.86,-9.09,21.85,48.44,-2.75,260.08,2008-08-22,1,,1.50%,0.15%,1,0.15%,1,68.52\",\"000274,广发亚太中高收益债(QDII)A,GFYTZGSYZQDIIA,2022-02-10,1.1623,1.2323,-0.04,-0.51,-0.80,1.36,-9.90,-11.68,-14.91,-7.24,-0.95,23.69,2013-11-28,1,,0.80%,0.08%,1,0.08%,1,-5.81\",\"519981,长信标普100等权重指数人民币,CXBP100DQZZSRMB,2022-02-10,1.6560,2.1730,-1.55,1.22,-2.82,-2.70,-1.37,10.84,24.23,45.86,-2.93,166.78,2011-03-30,1,,1.40%,0.14%,1,0.14%,1,58.36\",\"000341,嘉实新兴市场C2(QDII),JSXXSCC2QDII,2022-02-10,1.1630,1.2180,-0.17,-0.60,-1.77,-2.43,-7.26,-6.66,-7.48,2.11,-2.60,22.83,2013-11-26,,,,0.00%,,,,8.59\",\"000369,广发全球医疗保健(QDII),GFQQYLBJQDII,2022-02-10,1.9140,2.0240,-1.09,1.06,-1.90,-1.95,-1.34,9.56,10.19,33.94,-5.20,110.59,2013-12-10,1,,1.30%,0.13%,1,0.13%,1,58.84\",\"160213,国泰纳斯达克100指数,GTNSDK100ZS,2022-02-10,5.62,6.12,-2.40,1.46,-6.08,-8.78,-4.62,5.54,38.49,99.60,-10.30,576.15,2010-04-29,1,,1.50%,0.15%,1,0.15%,1,153.24\",\"000342,嘉实新兴市场A1(QDII),JSXXSCA1QDII,2022-02-10,1.1960,1.4640,-0.25,-0.83,-1.89,-2.92,-8.84,-7.36,-14.94,-1.64,-2.84,51.65,2013-11-26,1,,0.80%,0.08%,1,0.08%,1,2.93\",\"000834,大成纳斯达克100,DCNSDK100,2022-02-10,3.2080,3.2080,-2.40,1.52,-5.92,-8.58,-4.38,5.80,38.57,93.37,-10.11,220.80,2014-11-13,1,,1.20%,0.12%,1,0.12%,1,142.66\",\"000906,广发全球精选股票美元现汇,GFQQJXGPMYXH,2022-02-10,0.4280,0.4898,-1.09,8.27,-3.45,-19.12,-15.60,-31,69.57,110.01,-10.03,126.33,2015-01-16,,,,1.60%,,,,119.26\",\"320017,诺安全球收益不动产,NAQQSYBDC,2022-02-10,1.6080,1.6080,-2.13,-1.11,-7.80,-6.89,-4.85,11.05,-2.49,7.20,-11.50,60.80,2011-09-23,1,,1.50%,0.15%,1,0.15%,1,5.58\",\"160416,华安标普全球石油指数,HABPQQSYZS,2022-02-10,1.2330,1.2730,-0.24,3.96,7.87,9.41,25.43,41.08,29.79,24.17,14.38,28.14,2012-03-29,1,,1.20%,0.12%,1,0.12%,1,24.17\",\"001092,广发生物科技指数(QDII),GFSWKJZSQDII,2022-02-10,1.0330,1.0330,-2.46,2.08,-7.10,-14.70,-24.32,-23.93,-8.66,3.71,-12.38,3.30,2015-03-30,1,,1.30%,0.13%,1,0.13%,1,16.33\",\"001093,广发生物科技指数美元,GFSWKJZSMY,2022-02-10,0.1624,0.1624,-2.40,2.27,-7.04,-14.26,-22.85,-23,0.31,9.36,-12.17,-0.31,2015-03-30,,,,1.30%,,,,25.89\",\"000055,广发纳斯达克100美元现汇A,GFNSDK100MYXHA,2022-02-10,0.6529,0.6928,-2.39,2,-5.45,-7.35,-1.67,8.67,52.58,109.94,-9.48,244.80,2015-01-16,,,,1.30%,,,,174.44\",\"460010,华泰柏瑞亚洲领导企业混合,HTBRYZLDQYHH,2022-02-10,1.4580,1.4580,2.53,5.88,0.21,-2.21,-12.85,-21.82,39.66,67.20,1.18,45.80,2010-12-02,1,,1.60%,0.16%,1,0.16%,1,76.51\",\"070031,嘉实全球房地产(QDII),JSQQFDCQDII,2022-02-10,1.23,1.6040,-1.36,-0.24,-4.46,-3.95,0.07,16.32,0.30,22.43,-7.67,69.31,2012-07-24,1,,1.50%,0.15%,1,0.15%,1,27.84\",\"000370,广发全球医疗保健美元,GFQQYLBJMY,2022-02-10,0.3009,0.3178,-1.02,1.28,-1.83,-1.41,0.57,10.91,21.04,41.27,-4.99,103.14,2013-12-10,,,,1.30%,,,,71.84\",\"050025,博时标普500ETF联接A,BSBP500ETFLJA,2022-02-10,3.2202,3.2792,-1.79,1.35,-3.57,-3.42,-0.34,13.06,22.13,56.32,-5.54,238.08,2012-06-14,1,,1.20%,0.12%,1,0.12%,1,77.94\",\"001274,民生加银新动力混合D,MSJYXDLHHD,,,,,0,0,0,0,0,1.4199,,0,0,2015-05-18,,,,0.00%,,,,\",\"001785,民生加银岁岁增利债券D,MSJYSSZLZQD,,,,,0,0,1.2146,1.2146,0,-0.1996,,1.5228,0,2015-08-17,,,,0.00%,,,,\",\"050020,博时抗通胀增强回报,BSKTZZQHB,2022-02-10,0.3970,0.3970,-1,1.79,2.58,1.02,3.66,2.06,-26.07,-23.65,3.12,-60.30,2011-04-25,1,,1.50%,0.15%,1,0.15%,1,-26.07\",\"000103,国泰中国企业境外高收益债,GTZGQYJWGSYZ,2022-02-10,1.0767,1.0767,1.59,-0.85,-3.52,-2.83,-19.50,-20.14,-27.78,-20.36,-10.92,7.67,2013-04-26,1,,0.80%,0.08%,1,0.08%,1,-20.42\",\"000041,华夏全球股票(QDII),HXQQGPQDII,2022-02-10,1.0730,1.0730,-1.65,3.17,-4.28,-13.68,-12.19,-34.57,-1.65,14.88,-10.36,7.30,2007-10-09,1,,1.60%,0.16%,1,0.16%,1,28.04\",\"000180,广发美国房地产指数现汇,GFMGFDCZSXH,2022-02-10,0.1939,0.2660,-2.27,-1.12,-5.36,-3.39,2.21,20.51,10.84,25.99,-8.65,79.18,2013-08-09,,,,1.30%,,,,31.79\",\"001428,工银瑞信灵活配置混合B,GYRXLHPZHHB,,,,,0,-0.0886,-0.6168,2.5454,3.3913,,,2.5454,3.7718,2015-06-04,4,,,,,,,\",\"000990,嘉实全球互联网股票美元现钞,JSQQHLWGPMYXC,2022-02-10,1.9790,1.9790,-2.32,0.66,-7.70,-13.88,-3.70,-15.03,25.57,60.37,-12.74,97.90,2015-04-15,,,,1.50%,,,,100.10\",\"000614,华安德国(DAX)联接(QDII),HADGDAXLJQDII,2022-02-10,1.3280,1.3280,0.08,2.39,-2.57,-6.87,-8.29,-0.52,9.84,26.60,-3.42,32.80,2014-08-12,1,,1.20%,0.12%,1,0.12%,1,22.40\",\"001691,南方香港成长(QDII),NFXGCZQDII,2022-02-10,1.6110,1.6110,0.75,4.27,-1.23,-14.26,-24.68,-44.14,31.94,81.42,-4.96,61.10,2015-09-30,1,,1.60%,0.16%,1,0.16%,1,70.66\",\"040048,华安纳斯达克100指数现汇,HANSDK100ZSXH,2022-02-10,0.6605,0.6605,-2.32,1.72,-5.86,-8.02,-2.39,7.45,57.11,114.17,-9.94,308.22,2013-08-02,,,,1.20%,,,,176.36\",\"000989,嘉实全球互联网股票美元现汇,JSQQHLWGPMYXH,2022-02-10,1.9790,1.9790,-2.32,0.66,-7.70,-13.88,-3.70,-15.03,25.57,60.37,-12.74,97.90,2015-04-15,,,,1.50%,,,,100.10\",\"162411,华宝标普油气上游股票人民币A,HBBPYQSYGPRMBA,2022-02-10,0.5263,0.5263,-0.09,3.32,5.24,3.40,29.12,45.79,57.81,1.80,13.52,-47.37,2011-09-29,1,,1.50%,0.15%,1,0.15%,1,-23.72\",\"165510,信诚四国配置,XCSGPZ,2022-02-10,0.7270,0.7270,-0.55,3.41,0.69,-5.09,-9.12,-17.67,-5.83,0.69,0.55,-27.30,2010-12-17,1,,1.60%,0.16%,1,0.16%,1,-3.07\",\"001481,华宝标普油气上游股票美元A,HBBPYQSYGPMYA,2022-02-10,0.0828,0.0828,0,3.63,5.34,4.02,31.64,47.59,73.58,7.39,13.89,-31,2015-06-18,,,,1.50%,,,,-17.45\"],\"etfNum\":0,\"fofNum\":338,\"gpNum\":2072,\"hhNum\":6738,\"lofNum\":388,\"pageIndex\":257,\"pageNum\":50,\"qdiiNum\":345,\"zqNum\":4339,\"zsNum\":1622}";
        EastmoneyAllRankListResult allRankListResult = JSONObject.parseObject(b, EastmoneyAllRankListResult.class);
        List<EastmoneyAllRankListData> pojoResultList = allRankListResult.getData().stream().map(v -> {
            EastmoneyAllRankListData allRankListData = new EastmoneyAllRankListData();
            String[] r = v.split(",", -1);
            allRankListData.setFundCode(r[0]);
            allRankListData.setFundShortName(r[1]);
            allRankListData.setFundShortCode(r[2]);
            allRankListData.setFundDate(StrUtil.isEmpty(r[3]) ? null : DateUtil.parseDate(r[3]));
            allRankListData.setUnitNet(toDouble(r[4]));
            allRankListData.setSumNet(toDouble(r[5]));
            allRankListData.setDayGrowRate(toDouble(r[6]));
            allRankListData.setLastWeek(toDouble(r[7]));
            allRankListData.setLast1Month(toDouble(r[8]));
            allRankListData.setLast3Month(toDouble(r[9]));
            allRankListData.setLast6Month(toDouble(r[10]));
            allRankListData.setLast1Year(toDouble(r[11]));
            allRankListData.setLast2Year(toDouble(r[12]));
            allRankListData.setLast3Year(toDouble(r[13]));
            allRankListData.setThisYear(toDouble(r[14]));
            allRankListData.setSinceEstablish(toDouble(r[15]));
            allRankListData.setFee(toDouble(r[20]));
            allRankListData.setBuyStatus(r[17]);
            return allRankListData;
        }).collect(Collectors.toList());
    }

    public static Double toDouble(String value) {
        return StrUtil.isEmpty(value) ? 0.0d : Double.parseDouble(value.replace("%", ""));
    }
}
