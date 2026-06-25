#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEMO_WORKSPACE="${DEMO_WORKSPACE:-/tmp/sac-agent4j-toy-demo}"
JAR="$ROOT_DIR/target/sac-agent4j-0.1.0-SNAPSHOT.jar"

cd "$ROOT_DIR"
mvn -q package

rm -rf "$DEMO_WORKSPACE"
mkdir -p "$DEMO_WORKSPACE"
cp -R "$ROOT_DIR/examples/toy-java-bug/." "$DEMO_WORKSPACE/"
chmod +x "$DEMO_WORKSPACE/test.sh"

echo "== Baseline failing test =="
if (cd "$DEMO_WORKSPACE" && ./test.sh); then
  echo "Expected baseline test to fail, but it passed" >&2
  exit 1
else
  echo "Baseline failed as expected"
fi

PATCH=$(cat <<'PATCH_EOF'
diff --git a/src/Calculator.java b/src/Calculator.java
--- a/src/Calculator.java
+++ b/src/Calculator.java
@@ -2,6 +2,6 @@ public final class Calculator {
     private Calculator() {}
 
     public static int add(int left, int right) {
-        return left - right;
+        return left + right;
     }
 }
PATCH_EOF
)

ACTIONS=$(PATCH="$PATCH" python3 - <<'PY'
import json
import os
patch = os.environ["PATCH"] + "\n"
for action in [
    {"type": "set_plan", "items": ["reproduce failing test", "inspect Calculator implementation", "apply minimal fix", "rerun tests"]},
    {"type": "update_todo", "id": 1, "status": "in_progress"},
    {"type": "shell", "command": "./test.sh"},
    {"type": "update_todo", "id": 1, "status": "completed"},
    {"type": "update_todo", "id": 2, "status": "in_progress"},
    {"type": "read_file", "path": "src/Calculator.java"},
    {"type": "update_todo", "id": 2, "status": "completed"},
    {"type": "update_todo", "id": 3, "status": "in_progress"},
    {"type": "apply_patch", "patch": patch},
    {"type": "update_todo", "id": 3, "status": "completed"},
    {"type": "update_todo", "id": 4, "status": "in_progress"},
    {"type": "run_tests"},
    {"type": "update_todo", "id": 4, "status": "completed"},
    {"type": "finish", "summary": "fixed Calculator.add and verified ./test.sh passes"},
]:
    print(json.dumps(action))
PY
)

echo
echo "== Running sac-agent4j scripted JSON-line demo =="
printf '%s\n' "$ACTIONS" | java -jar "$JAR" \
  --workspace "$DEMO_WORKSPACE" \
  --test-command "./test.sh" \
  --max-steps 16 \
  "Fix the failing Calculator.add test"

echo
echo "== Final test =="
(cd "$DEMO_WORKSPACE" && ./test.sh)

echo
echo "== Latest trajectory =="
latest_log=$(ls -t "$DEMO_WORKSPACE"/.sac-agent4j/runs/*.jsonl | head -1)
echo "$latest_log"
sed -n '1,20p' "$latest_log"
