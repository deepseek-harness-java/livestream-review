package cn.xiaofuge.live.app;

import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.util.*;
import java.util.stream.Collectors;

/** 直播复盘内存数据层：场次、商品漏斗、小时趋势、主播绩效 */
@Component
public class LiveStore {

    /** 一场直播 */
    public record Session(String id, String title, String anchor, String date,
                          String startEnd, int peakViewers, int avgViewers,
                          int gmv, int orders, int uv, double conversionRate,
                          int refunds, String status) {}

    /** 单商品成交漏斗 */
    public record Funnel(String goodsId, String goodsName, int exposures, int clicks,
                         int orders, int gmv, int refundCnt, String remark) {}

    /** 小时趋势 */
    public record HourPoint(String hour, int viewers, int orders, int gmv, String event) {}

    public record Anchor(String id, String name, String level, int totalGmv, int sessions,
                         double avgConversion, String strength, String weakness) {}

    public final Map<String, Session> sessions = new LinkedHashMap<>();
    public final Map<String, List<Funnel>> funnels = new LinkedHashMap<>();
    public final Map<String, List<HourPoint>> timeline = new LinkedHashMap<>();
    public final Map<String, Anchor> anchors = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        session(new Session("s0923", "9.23 双节美妆宠粉夜", "林薇", "2026-09-23",
                "19:00-23:00", 48200, 21600, 1286000, 8214, 156300, 5.3, 217, "已结束"));
        session(new Session("s0922", "9.22 家居焕新专场", "陈牧", "2026-09-22",
                "14:00-18:00", 21400, 9800, 542000, 3165, 88100, 3.6, 132, "已结束"));
        session(new Session("s0921", "9.21 数码爆品夜", "林薇", "2026-09-21",
                "19:00-23:00", 52700, 24100, 1642000, 6102, 171500, 3.6, 189, "已结束"));
        session(new Session("s0924", "9.24 中秋礼盒专场", "苏晴", "2026-09-24",
                "19:00-23:00", 0, 0, 0, 0, 0, 0, 0, "直播中"));

        anchor(new Anchor("a01", "林薇", "金牌主播", 2928000, 2, 4.45, "美妆品类转化率高，宠粉互动氛围强", "数码品类客单高但转化偏低，讲解节奏偏快"));
        anchor(new Anchor("a02", "陈牧", "认证主播", 542000, 1, 3.6, "家居场景讲解细致，白场直播稳", "场观与拉新能力弱，缺少爆品钩子"));
        anchor(new Anchor("a03", "苏晴", "新星主播", 0, 0, 0, "新人成长快，礼盒品类选品准", "直播经验不足，憋单节奏不稳"));

        // 9.23 美妆场商品漏斗
        funnel("s0923", List.of(
                new Funnel("f01", "水感粉底液", 96200, 31400, 3820, 687600, 96, "爆品，转化 12.2% 优秀"),
                new Funnel("f02", "口红礼盒三支装", 88400, 18900, 2140, 428000, 58, "礼盒客单高，点击率偏低"),
                new Funnel("f03", "卸妆水 500ml", 79200, 22100, 1650, 148500, 41, "引流款，转化 7.5% 正常"),
                new Funnel("f04", "美容仪", 51300, 4300, 310, 155000, 15, "高客单低转化，讲解时长不足"),
                new Funnel("f05", "面膜囤货装", 74600, 12600, 294, 66900, 7, "凑单款")));
        timeline("s0923", List.of(
                new HourPoint("19:00", 8200, 210, 32100, "开播预热+福袋"),
                new HourPoint("20:00", 21600, 1450, 228400, "粉底液上场，冲量"),
                new HourPoint("21:00", 48200, 3210, 512300, "口红礼盒+秒杀，峰值"),
                new HourPoint("22:00", 28400, 2430, 396700, "返场补货"),
                new HourPoint("23:00", 9800, 914, 116500, "收尾清库存")));

        // 9.22 家居场
        funnel("s0922", List.of(
                new Funnel("f11", "四件套纯棉", 52300, 12800, 1140, 342000, 51, "主力款"),
                new Funnel("f12", "香薰蜡烛", 48100, 8900, 920, 73600, 22, "低价凑单"),
                new Funnel("f13", "空气炸锅", 39800, 6100, 480, 105600, 34, "厨房电器，讲解一般"),
                new Funnel("f14", "乳胶枕", 35200, 5400, 425, 89250, 18, "利润款")));
        timeline("s0922", List.of(
                new HourPoint("14:00", 4100, 90, 21400, "开播"),
                new HourPoint("15:00", 9800, 720, 128000, "四件套讲解"),
                new HourPoint("16:00", 21400, 1380, 246000, "抽免单，峰值"),
                new HourPoint("17:00", 12600, 720, 116500, "返场"),
                new HourPoint("18:00", 5200, 255, 50100, "收尾")));

        // 9.21 数码场
        funnel("s0921", List.of(
                new Funnel("f21", "无线耳机 Pro", 98400, 21600, 1840, 1104000, 92, "爆品，占 GMV 67%"),
                new Funnel("f22", "智能手表", 72300, 9800, 760, 380000, 54, "高客单，转化 7.8%"),
                new Funnel("f23", "充电宝", 65100, 11200, 2100, 84000, 28, "引流款"),
                new Funnel("f24", "机械键盘", 44200, 4300, 502, 74000, 15, "小众")));
        timeline("s0921", List.of(
                new HourPoint("19:00", 9100, 180, 48200, "开播"),
                new HourPoint("20:00", 24300, 1120, 321000, "耳机讲解"),
                new HourPoint("21:00", 52700, 2640, 762400, "耳机秒杀，峰值"),
                new HourPoint("22:00", 26100, 1520, 391000, "手表+返场"),
                new HourPoint("23:00", 10400, 642, 119400, "收尾")));
    }

    private void session(Session s) { sessions.put(s.id(), s); }
    private void funnel(String sid, List<Funnel> list) { funnels.put(sid, list); }
    private void timeline(String sid, List<HourPoint> list) { timeline.put(sid, list); }
    private void anchor(Anchor a) { anchors.put(a.id(), a); }

    /** 场次列表 */
    public List<Session> sessionList(String status) {
        return sessions.values().stream()
                .filter(s -> status == null || status.isBlank() || s.status().equals(status))
                .collect(Collectors.toList());
    }

    public Session session(String id) { return sessions.get(id); }

    /** 复盘指标：GMV/UV 客单、转化、退款率、UV 价值 */
    public Map<String, Object> review(Session s) {
        Map<String, Object> m = new LinkedHashMap<>();
        double refundRate = s.orders() == 0 ? 0 : Math.round(s.refunds() * 1000.0 / s.orders()) / 10.0;
        int avgOrder = s.orders() == 0 ? 0 : (int) Math.round((double) s.gmv() / s.orders());
        double uvValue = s.uv() == 0 ? 0 : Math.round(s.gmv() * 100.0 / s.uv()) / 100.0;
        m.put("session", s);
        m.put("gmvPerUv", uvValue);
        m.put("avgOrderValue", avgOrder);
        m.put("refundRate", refundRate);
        String verdict;
        if (s.gmv() == 0) verdict = "直播中或未开始，暂无数据";
        else if (s.conversionRate() >= 5 && refundRate < 3) verdict = "优秀：转化与退款双达标，沉淀话术模板";
        else if (s.conversionRate() >= 5) verdict = "转化好但退款偏高，排查商品期望管理";
        else if (refundRate < 3) verdict = "转化偏低：优化选品钩子与憋单节奏";
        else verdict = "转化低且退款高，需全面复盘选品与讲解";
        m.put("verdict", verdict);
        return m;
    }

    /** 商品漏斗（曝光→点击→下单）+ 转化率标注 */
    public Map<String, Object> funnelOf(String sessionId) {
        List<Funnel> list = funnels.getOrDefault(sessionId, List.of());
        List<Map<String, Object>> rows = list.stream().map(f -> {
            double ctr = f.exposures() == 0 ? 0 : Math.round(f.clicks() * 1000.0 / f.exposures()) / 10.0;
            double cvr = f.clicks() == 0 ? 0 : Math.round(f.orders() * 1000.0 / f.clicks()) / 10.0;
            double refundRate = f.orders() == 0 ? 0 : Math.round(f.refundCnt() * 1000.0 / f.orders()) / 10.0;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("goodsId", f.goodsId()); m.put("goodsName", f.goodsName());
            m.put("exposures", f.exposures()); m.put("clicks", f.clicks());
            m.put("orders", f.orders()); m.put("gmv", f.gmv());
            m.put("ctr", ctr); m.put("cvr", cvr); m.put("refundRate", refundRate);
            m.put("remark", f.remark());
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> m = new HashMap<>();
        m.put("sessionId", sessionId);
        m.put("goods", rows);
        return m;
    }

    /** 小时趋势 */
    public Map<String, Object> timelineOf(String sessionId) {
        Map<String, Object> m = new HashMap<>();
        m.put("sessionId", sessionId);
        m.put("points", timeline.getOrDefault(sessionId, List.of()));
        return m;
    }

    /** 主播绩效 */
    public List<Anchor> anchorList() {
        return anchors.values().stream().collect(Collectors.toList());
    }

    /** 全站概览 */
    public Map<String, Object> overview() {
        List<Session> all = new ArrayList<>(sessions.values());
        int totalGmv = all.stream().mapToInt(Session::gmv).sum();
        int totalOrders = all.stream().mapToInt(Session::orders).sum();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("date", "2026-09-24");
        m.put("sessionCount", all.size());
        m.put("totalGmv", totalGmv);
        m.put("totalOrders", totalOrders);
        m.put("liveNow", all.stream().filter(s -> "直播中".equals(s.status())).map(Session::title).collect(Collectors.toList()));
        return m;
    }
}
