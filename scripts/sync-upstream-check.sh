#!/usr/bin/env bash
# B8：上游同步检查（由 .github/workflows/sync-upstream.yml 调用）
# 在 EhViewer/ 目录下 fetch 上游，预演本仓库补丁，按结果开 issue（不自动合并）。
set -uo pipefail

UPSTREAM_URL="${UPSTREAM_URL:-https://codeberg.org/EhViewer/EhViewer.git}"
UPSTREAM_BRANCH="${UPSTREAM_BRANCH:-main}"
PATCH_DIR="${PATCH_DIR:-../patches-local/feat}"

cd "$(dirname "${BASH_SOURCE[0]}")/.." || exit 1
cd EhViewer || { echo "no EhViewer submodule"; exit 1; }

git remote add upstream "$UPSTREAM_URL" 2>/dev/null || true
git fetch --depth=50 upstream "$UPSTREAM_BRANCH" || { echo "fetch upstream failed"; exit 1; }

UP=$(git rev-parse "upstream/$UPSTREAM_BRANCH")
CUR=$(git rev-parse HEAD)
echo "current=$CUR"
echo "upstream=$UP"

if [ "$UP" = "$CUR" ]; then
  echo "upstream unchanged"
  exit 0
fi

LOG=$(git log --oneline "$CUR..$UP" | head -30)
git checkout -q "$UP" || exit 1

FAILED=""
for p in "$PATCH_DIR"/*.patch; do
  [ -e "$p" ] || continue
  if ! git apply --check "$p" 2>/dev/null; then
    FAILED="$FAILED $(basename "$p")"
  fi
done

SHORT_UP=$(printf '%s' "$UP" | cut -c1-10)
SHORT_CUR=$(printf '%s' "$CUR" | cut -c1-10)

if [ -z "$FAILED" ]; then
  TITLE="上游可升级：$SHORT_UP"
  BODY="自动检查：上游有新提交，且全部功能补丁可干净应用。

- 当前: \`$SHORT_CUR\`
- 上游: \`$SHORT_UP\`

\`\`\`
$LOG
\`\`\`

升级步骤：
\`\`\`bash
cd EhViewer
git fetch upstream $UPSTREAM_BRANCH
git checkout $UP
cd ..
bash scripts/prepare-build.sh direct
git add EhViewer && git commit -m \"chore: bump EhViewer to $SHORT_UP\"
\`\`\`"
else
  TITLE="上游冲突：$SHORT_UP"
  BODY="上游有新提交，但以下补丁无法直接应用：

\`\`\`
$FAILED
\`\`\`

- 当前: \`$SHORT_CUR\`
- 上游: \`$SHORT_UP\`

复现：
\`\`\`bash
cd EhViewer && git fetch upstream $UPSTREAM_BRANCH && git checkout $UP
for p in ../patches-local/feat/*.patch; do git apply --check \"\$p\" || echo \"FAIL \$p\"; done
\`\`\`"
fi

if command -v gh >/dev/null 2>&1; then
  EXISTING=$(gh issue list --search "$TITLE in:title" --state open --json number --jq '.[0].number' 2>/dev/null || true)
  if [ -n "${EXISTING:-}" ]; then
    printf '%s\n' "$BODY" | gh issue comment "$EXISTING" --body-file -
  else
    printf '%s\n' "$BODY" | gh issue create --title "$TITLE" --body-file -
  fi
else
  echo "gh not available; title=$TITLE"
  printf '%s\n' "$BODY"
fi
