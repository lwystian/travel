package org.example.springboot.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TourDestinationSchemaInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TourDestinationSchemaInitializer.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        try {
            jdbcTemplate.update("UPDATE `tour` SET `destination` = 'sanxia' WHERE `destination` = 'sanyan'");
            normalizeTourLocationCodes();
        } catch (Exception e) {
            LOGGER.warn("修正行程目的地/出发地历史数据失败", e);
        }
    }

    private void normalizeTourLocationCodes() {
        updateCode("city", "\u95B2\u5DB6\u7C21", "chongqing");
        updateCode("city", "重庆", "chongqing");
        updateCode("city", "\u93B4\u6130\u5158", "chengdu");
        updateCode("city", "成都", "chengdu");
        updateCode("city", "\u7490\u7538\u69FC", "guiyang");
        updateCode("city", "贵阳", "guiyang");
        updateCode("city", "\u93C4\u55D8\u69D1", "kunming");
        updateCode("city", "昆明", "kunming");
        updateCode("city", "\u7039\u6EC4\u69CD", "yichang");
        updateCode("city", "宜昌", "yichang");
        updateCode("city", "\u59DD\uFE3D\u773D", "wuhan");
        updateCode("city", "武汉", "wuhan");
        updateCode("city", "\u95C0\u630E\u77D9", "changsha");
        updateCode("city", "长沙", "changsha");
        updateCode("city", "\u9A9E\u57AE\u7A9E", "guangzhou");
        updateCode("city", "广州", "guangzhou");
        updateCode("city", "\u5A23\u535E\u6E77", "shenzhen");
        updateCode("city", "深圳", "shenzhen");
        updateCode("city", "\u6D93\u5A43\u6363", "shanghai");
        updateCode("city", "上海", "shanghai");
        updateCode("city", "\u93C9\uE15E\u7A9E", "hangzhou");
        updateCode("city", "杭州", "hangzhou");
        updateCode("city", "\u9357\u693E\u542B", "nanjing");
        updateCode("city", "南京", "nanjing");
        updateCode("city", "\u7457\u57AE\u7568", "xian");
        updateCode("city", "西安", "xian");
        updateCode("city", "\u9356\u693E\u542B", "beijing");
        updateCode("city", "北京", "beijing");
        updateCode("city", "\u6D93\u5909\u7C39", "sanya");
        updateCode("city", "三亚", "sanya");
        updateCode("city", "\u5A34\u5CF0\u5F5B", "haikou");
        updateCode("city", "海口", "haikou");

        updateCode("destination", "\u7457\u632E\u77D9\u7F07\u3085\u77DD", "xisha");
        updateCode("destination", "西沙群岛", "xisha");
        updateCode("destination", "\u6D93\u592F\u573A", "sanxia");
        updateCode("destination", "三峡", "sanxia");
        updateCode("destination", "\u95B2\u5DB6\u7C21", "chongqing");
        updateCode("destination", "重庆", "chongqing");
        updateCode("destination", "\u93B4\u6130\u5158", "chengdu");
        updateCode("destination", "成都", "chengdu");
        updateCode("destination", "\u7490\u7538\u69FC", "guiyang");
        updateCode("destination", "贵阳", "guiyang");
        updateCode("destination", "\u93C4\u55D8\u69D1", "kunming");
        updateCode("destination", "昆明", "kunming");
        updateCode("destination", "\u6D93\u5909\u7C39", "sanya");
        updateCode("destination", "三亚", "sanya");

        updateCode("tour_type", "\u935B\u3128\u7ADF\u5A13?", "around");
        updateCode("tour_type", "周边游", "around");
        updateCode("tour_type", "\u95C0\u8DE8\u568E\u5A13?", "long");
        updateCode("tour_type", "长线游", "long");
        updateCode("tour_type", "\u74BA\u719A\u6D1F\u5A13?", "team");
        updateCode("tour_type", "跟团游", "team");
        updateCode("tour_type", "自由行", "free");
        updateCode("tour_type", "私家团", "private");
        updateCode("tour_type", "定制游", "custom");
        updateCode("tour_type", "当地参团", "local");
        updateCode("tour_type", "当地游", "local");
        updateCode("tour_type", "自驾游", "selfdrive");
        updateCode("tour_type", "自驾", "selfdrive");
        updateCode("tour_type", "亲子游", "parent_child");
        updateCode("tour_type", "亲子", "parent_child");
        updateCode("tour_type", "研学游", "study");
        updateCode("tour_type", "研学", "study");
        updateCode("tour_type", "摄影游", "photography");
        updateCode("tour_type", "摄影", "photography");
        updateCode("tour_type", "户外徒步", "outdoor");
        updateCode("tour_type", "徒步", "outdoor");
        updateCode("tour_type", "\u95AD\uE1C6\u8F75\u934F\u51FA\uE511", "cruise");
        updateCode("tour_type", "邮轮出行", "cruise");
        updateCode("tour_type", "康养度假", "wellness");
        updateCode("tour_type", "康养", "wellness");
        updateCode("tour_type", "其它", "other");
        updateCode("tour_type", "其他", "other");
    }

    private void updateCode(String column, String oldValue, String newValue) {
        jdbcTemplate.update("UPDATE `tour` SET `" + column + "` = ? WHERE `" + column + "` = ?", newValue, oldValue);
    }
}
