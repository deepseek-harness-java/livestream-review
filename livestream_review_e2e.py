#!/usr/bin/env python3
"""livestream-review E2E：通过业务应用 SSE 代理调用 DSH Agent，验证工具全链路。"""
import json, subprocess, sys

AGENT = "live-copilot"
URL = "http://127.0.0.1:18091/api/assistant/stream"

CASES = [
    ("T1 场次列表", "最近几场直播的场次列表有哪些？简洁回答", ["中秋礼盒专场", "林薇"]),
    ("T2 昨日复盘", "昨天那场直播复盘数据怎么样？GMV 多少？简洁回答", ["GMV", "8,214"]),
    ("T3 商品漏斗", "9.23 双节美妆宠粉夜那场直播的商品转化漏斗怎么样？简洁回答", ["曝光", "转化"]),
    ("T4 弹幕分析", "9.23 那场直播的弹幕观众都在关注什么？简洁回答", ["弹幕"]),
    ("T5 直播中监控", "现在直播中的那场情况怎么样？简洁回答", ["中秋", "苏晴"]),
]

def ask(message, timeout=170):
    payload = json.dumps({"message": message}, ensure_ascii=False)
    try:
        out = subprocess.run(
            ["curl", "-s", "--noproxy", "*", "-N", "-X", "POST", URL,
             "-H", "Content-Type: application/json", "-d", payload,
             "--max-time", str(timeout)],
            capture_output=True, text=True, timeout=timeout + 10).stdout
    except Exception as e:
        return "", f"curl 异常: {e}"
    text = []
    ev = ""
    for line in out.splitlines():
        line = line.rstrip("\r")
        if line.startswith("event:"):
            ev = line[6:].strip()
        elif line.startswith("data:"):
            s = line[5:].strip()
            if not s or s == "[DONE]" or ev != "chunk":
                continue
            try:
                j = json.loads(s)
                c = j.get("content", "")
                if c:
                    text.append(c)
            except Exception:
                pass
            ev = ""
    return "".join(text), out

def main():
    only = sys.argv[1] if len(sys.argv) > 1 else None
    cases = CASES if not only else [c for c in CASES if c[0].startswith(only)]
    passed, failed = 0, []
    for name, q, keys in cases:
        reply, raw = ask(q)
        ok = all(k in reply for k in keys)
        print(f"[{'PASS' if ok else 'FAIL'}] {name}\n  Q: {q}\n  A: {reply[:200]}")
        if ok:
            passed += 1
        else:
            failed.append(name)
            if not reply:
                print(f"  raw 首行: {raw.splitlines()[:3] if raw else '(空)'}")
    print(f"\n===== livestream-review E2E: {passed}/{len(cases)} PASS =====")

if __name__ == "__main__":
    main()
