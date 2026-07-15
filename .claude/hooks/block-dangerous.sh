#!/bin/sh
# PreToolUse(Bash) 훅: 위험 명령을 세션 안에서 조기 차단한다.
# stdin으로 훅 JSON을 받아 tool_input.command를 검사한다.
exec python3 -c '
import json, re, sys

try:
    data = json.load(sys.stdin)
except Exception:
    sys.exit(0)

cmd = (data.get("tool_input") or {}).get("command") or ""

BLOCKED_PATTERNS = [
    r"rm\s+-rf",
    r"git\s+reset\s+--hard",
    r"git\s+checkout\s+--",
    r"git\s+clean\s+-fd",
    r"git\s+push(?:\s+\S+)*\s+--force(?:\S+)?",
    r"git\s+push(?:\s+\S+)*\s+-[A-Za-z]*f[A-Za-z]*",
    r"DROP\s+TABLE",
    r"TRUNCATE\s+TABLE",
]

for pattern in BLOCKED_PATTERNS:
    if re.search(pattern, cmd, re.IGNORECASE):
        print(json.dumps({
            "hookSpecificOutput": {
                "hookEventName": "PreToolUse",
                "permissionDecision": "deny",
                "permissionDecisionReason": "위험 명령 차단 (pattern: " + pattern + "). 꼭 필요하면 사용자에게 직접 실행을 요청하세요.",
            }
        }, ensure_ascii=False))
        sys.exit(0)

sys.exit(0)
'
