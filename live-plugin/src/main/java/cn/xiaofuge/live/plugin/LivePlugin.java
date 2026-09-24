package cn.xiaofuge.live.plugin;

import cn.xiaofuge.deepseek.harness.domain.model.entity.AbstractTool;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolDefinition;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolExecutionResult;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolRunContext;
import cn.xiaofuge.deepseek.harness.domain.spi.AbstractHarnessPlugin;
import cn.xiaofuge.deepseek.harness.domain.spi.PluginContext;
import cn.xiaofuge.deepseek.harness.domain.spi.PluginHookResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** 直播复盘助手插件：把 live-app REST API 注册为 DSH Agent 工具 */
public class LivePlugin extends AbstractHarnessPlugin {

    public static final String PLUGIN_ID = "live-copilot";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build();

    public LivePlugin() { super(PLUGIN_ID); }

    @Override
    public List<ToolDefinition> tools() {
        return List.of(
                new SessionListTool(),
                new SessionReviewTool(),
                new GoodsFunnelTool(),
                new TimelineTool(),
                new AnchorPerfTool());
    }

    @Override
    public void configure(PluginContext context) {
        super.configure(context);
        context.registerSystemPrompt("live-capabilities", 20, """
                ## 直播复盘助手（电商直播运营 · 2026-09-24）
                - 用户问"有哪些场次/最近直播情况" → session_list（可按 status：直播中/已结束）
                - 用户问"XX 场怎么样/复盘一下/数据如何" → session_review（给 GMV、转化率、客单、UV 价值、退款率与结论）
                - 用户问"哪个商品卖得好/商品漏斗/为什么 XX 没卖动" → goods_funnel（曝光→点击→下单逐层分析，找断点）
                - 用户问"流量节奏/几点是峰值/在线走势" → hour_trend
                - 用户问"主播表现/谁该培养/绩效对比" → anchor_perf（结合 strength/weakness 给培养建议）
                - 复盘报告结构：核心数据 → 商品漏斗断点 → 流量节奏 → 改进动作（3 条以内，可执行）
                - 回答要求：数字说话（转化率、客单价、UV 价值、退款率），断点分析给具体商品与环节；数据来自工具返回，禁止编造
                """);
        context.registerHook("PRE_TOOL_USE", (toolName, payloadJson) -> {
            if (toolName != null && toolName.startsWith("plugin__" + PLUGIN_ID + "__")) {
                return PluginHookResult.context("audit: live tool call.");
            }
            return null;
        });
    }

    private String get(String path, Map<String, Object> args) {
        return send(HttpRequest.newBuilder(URI.create(baseUrl(args) + path)).GET().build());
    }

    private String baseUrl(Map<String, Object> args) {
        Object override = args == null ? null : args.get("appBaseUrl");
        return override == null || String.valueOf(override).isBlank()
                ? System.getenv().getOrDefault("LIVE_APP_BASE_URL", "http://127.0.0.1:18091")
                : String.valueOf(override);
    }

    private String send(HttpRequest request) {
        try {
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) return "{\"error\":true,\"status\":" + resp.statusCode() + "}";
            return resp.body();
        } catch (Exception e) {
            return "{\"error\":true,\"message\":\"" + String.valueOf(e.getMessage()).replace("\"", "'") + "\"}";
        }
    }

    private String str(Map<String, Object> args, String key) {
        Object v = args == null ? null : args.get(key);
        return v == null ? "" : String.valueOf(v);
    }

    private class SessionListTool extends AbstractTool {
        @Override public String name() { return "session_list"; }
        @Override public String description() {
            return "直播场次列表：标题/主播/日期/时段/场观峰值/GMV/订单/转化率/状态。"
                    + "何时必须调用：查场次、找 sessionId。";
        }
        @Override public Map<String, Object> parameters() {
            return objectSchema().prop("status", stringSchema("可选过滤：直播中 / 已结束")).build();
        }
        @Override public boolean isConcurrencySafe(Object args) { return true; }
        @Override protected CompletableFuture<ToolExecutionResult> run(Map<String, Object> args, ToolRunContext ctx) {
            String s = str(args, "status");
            return ok(get("/api/sessions" + (s.isBlank() ? "" : "?status=" + s), args));
        }
    }

    private class SessionReviewTool extends AbstractTool {
        @Override public String name() { return "session_review"; }
        @Override public String description() {
            return "单场复盘指标：GMV/订单/转化率/客单价/UV 价值/退款率 + 自动结论。"
                    + "何时必须调用：复盘某场直播、评估效果。";
        }
        @Override public Map<String, Object> parameters() {
            return objectSchema().prop("sessionId", stringSchema("场次 ID，如 s0923")).required("sessionId").build();
        }
        @Override public boolean isConcurrencySafe(Object args) { return true; }
        @Override protected CompletableFuture<ToolExecutionResult> run(Map<String, Object> args, ToolRunContext ctx) {
            return ok(get("/api/review?id=" + str(args, "sessionId"), args));
        }
    }

    private class GoodsFunnelTool extends AbstractTool {
        @Override public String name() { return "goods_funnel"; }
        @Override public String description() {
            return "商品成交漏斗：每个商品的曝光→点击→下单，含 CTR/转化率/退款率与备注。"
                    + "何时必须调用：分析商品为什么卖得好/不好、找转化断点。";
        }
        @Override public Map<String, Object> parameters() {
            return objectSchema().prop("sessionId", stringSchema("场次 ID，如 s0923")).required("sessionId").build();
        }
        @Override public boolean isConcurrencySafe(Object args) { return true; }
        @Override protected CompletableFuture<ToolExecutionResult> run(Map<String, Object> args, ToolRunContext ctx) {
            return ok(get("/api/funnel?id=" + str(args, "sessionId"), args));
        }
    }

    private class TimelineTool extends AbstractTool {
        @Override public String name() { return "hour_trend"; }
        @Override public String description() {
            return "场次小时趋势：每小时的在线人数、订单、GMV 与运营事件（秒杀/福袋/返场）。"
                    + "何时必须调用：分析流量节奏、峰值归因、排品时段。";
        }
        @Override public Map<String, Object> parameters() {
            return objectSchema().prop("sessionId", stringSchema("场次 ID，如 s0923")).required("sessionId").build();
        }
        @Override public boolean isConcurrencySafe(Object args) { return true; }
        @Override protected CompletableFuture<ToolExecutionResult> run(Map<String, Object> args, ToolRunContext ctx) {
            return ok(get("/api/timeline?id=" + str(args, "sessionId"), args));
        }
    }

    private class AnchorPerfTool extends AbstractTool {
        @Override public String name() { return "anchor_perf"; }
        @Override public String description() {
            return "主播绩效：累计 GMV/场次/平均转化率/长项与短板。"
                    + "何时必须调用：评估主播、排班、培养建议。";
        }
        @Override public Map<String, Object> parameters() { return objectSchema().build(); }
        @Override public boolean isConcurrencySafe(Object args) { return true; }
        @Override protected CompletableFuture<ToolExecutionResult> run(Map<String, Object> args, ToolRunContext ctx) {
            return ok(get("/api/anchors", args));
        }
    }
}
