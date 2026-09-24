package cn.xiaofuge.live.app;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 直播复盘 REST API */
@RestController
public class LiveController {

    private final LiveStore store;

    public LiveController(LiveStore store) { this.store = store; }

    /** 场次列表 */
    @GetMapping("/api/sessions")
    public Map<String, Object> sessions(@RequestParam(required = false) String status) {
        return Map.of("code", 0, "data", store.sessionList(status));
    }

    /** 场次详情 */
    @GetMapping("/api/session")
    public Map<String, Object> session(@RequestParam String id) {
        LiveStore.Session s = store.session(id);
        return s == null ? Map.of("code", 404, "message", "场次不存在")
                : Map.of("code", 0, "data", s);
    }

    /** 场次复盘指标 */
    @GetMapping("/api/review")
    public Map<String, Object> review(@RequestParam String id) {
        LiveStore.Session s = store.session(id);
        return s == null ? Map.of("code", 404, "message", "场次不存在")
                : Map.of("code", 0, "data", store.review(s));
    }

    /** 商品漏斗 */
    @GetMapping("/api/funnel")
    public Map<String, Object> funnel(@RequestParam String id) {
        return Map.of("code", 0, "data", store.funnelOf(id));
    }

    /** 小时趋势 */
    @GetMapping("/api/timeline")
    public Map<String, Object> timeline(@RequestParam String id) {
        return Map.of("code", 0, "data", store.timelineOf(id));
    }

    /** 主播绩效 */
    @GetMapping("/api/anchors")
    public Map<String, Object> anchors() {
        return Map.of("code", 0, "data", store.anchorList());
    }

    /** 概览 */
    @GetMapping("/api/overview")
    public Map<String, Object> overview() {
        return Map.of("code", 0, "data", store.overview());
    }
}
