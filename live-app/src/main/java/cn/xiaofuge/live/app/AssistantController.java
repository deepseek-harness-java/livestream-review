package cn.xiaofuge.live.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/** AI 直播复盘助手入口：页面消息代理到 DSH（SSE 透传） */
@RestController
public class AssistantController {

    @Value("${live.assistant.harness-base-url:http://127.0.0.1:8090}")
    private String harnessBaseUrl;

    @Value("${live.assistant.agent-id:live-copilot}")
    private String agentId;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @PostMapping(value = "/api/assistant/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> stream(@RequestBody Map<String, Object> body) {
        String message = body.get("message") == null ? "" : String.valueOf(body.get("message"));
        String payload = "{\"agentId\":\"" + agentId + "\",\"approvalMode\":\"FULL_OPEN\",\"message\":\""
                + message.replace("\\", "\\\\").replace("\"", "\\\"")
                        .replace("\n", "\\n").replace("\r", "\\r") + "\"}";

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(harnessBaseUrl + "/api/agent/stream"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(170))
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(out -> out.write(("event: error\ndata: {\"message\":\"代理地址不可用: " + e.getMessage() + "\"}\n\n")
                            .getBytes(StandardCharsets.UTF_8)));
        }

        HttpResponse<InputStream> upstream;
        try {
            upstream = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (Exception e) {
            return ResponseEntity.status(502)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(out -> out.write(("event: error\ndata: {\"message\":\"AI 服务连接失败: " + e.getMessage() + "\"}\n\n")
                            .getBytes(StandardCharsets.UTF_8)));
        }

        StreamingResponseBody stream = out -> {
            try (InputStream in = upstream.body()) {
                in.transferTo(out);
                out.flush();
            }
        };
        return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(stream);
    }
}
