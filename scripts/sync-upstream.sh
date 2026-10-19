#!/usr/bin/env bash
# 同步 ujui 上游：拉取新提交 → 合入 → 检查我的补丁是否仍能干净应用
#
# 设计要点：
#   本仓库的提交只存在于 patches-local/、scripts/、.github/ 三处，
#   不修改 submodule 内任何源码。所以 `git merge ujui/main` 理论上永不冲突；
#   真正的风险是「ujui 改了上游 → 我的补丁打不上」，由 --check 检出。
#
# 用法：
#   bash scripts/sync-upstream.sh            # 只看上游有没有新提交 + 预演补丁
#   bash scripts/sync-upstream.sh --merge    # 合入 ujui/main
#   bash scripts/sync-upstream.sh --merge --build
set -uo pipefail

TRACK="${TRACK:-direct}"
DO_MERGE=0
DO_BUILD=0
for a in "$@"; do
  case "$a" in
    --merge) DO_MERGE=1 ;;
    --build) DO_BUILD=1 ;;
    *) echo "unknown arg: $a"; exit 2 ;;
  esac
done

ROOT_DIR="$(dirname "${BASH_SOURCE[0]}")/.."
ROOT="$(cd "$ROOT_DIR" && pwd -W 2>/dev/null)"
[ -n "$ROOT" ] || ROOT="$(cd "$ROOT_DIR" && pwd)"
cd "$ROOT" || exit 1

if ! git remote | grep -qx ujui; then
  git remote add ujui https://github.com/UjuiUjuMandan/EhViewer.git
fi

echo "[1/4] fetch ujui ..."
git fetch --prune ujui main || { echo "!! fetch 失败"; exit 3; }

echo "[2/4] 上游差异"
git rev-list --left-right --count ujui/main..."$(git rev-parse --abbrev-ref HEAD)" |
  awk '{printf "    ujui 新增: %s 个提交    本地独有: %s 个提交\n",$1,$2}'

if [ "$DO_MERGE" -eq 1 ]; then
  echo "[3/4] merge ujui/main"
  if git merge --no-ff --no-edit ujui/main; then
    echo "    ✔ 合并干净（预期如此：本地提交不触碰 ujui 的文件）"
  else
    echo "    ✘ merge 冲突（异常，说明本地改动进了 ujui 的文件）"
    git diff --name-only --diff-filter=U | sed 's/^/      /'
    exit 4
  fi
else
  echo "[3/4] 跳过 merge（加 --merge 才真正合入）"
fi

echo "[4/4] 检查我的补丁能否干净应用（track=$TRACK）"
bash "$ROOT/scripts/prepare-build.sh" "$TRACK" --check || {
  echo "!! 补丁与上游冲突，需要修 patches-local/"
  exit 5
}

if [ "$DO_BUILD" -eq 1 ]; then
  echo "[build] 准备构建树并编译"
  bash "$ROOT/scripts/prepare-build.sh" "$TRACK" || exit 6
  cd "$ROOT/EhViewer" && ./gradlew :app:assembleRelease --no-daemon || exit 7
  echo "产物: EhViewer/app/build/outputs/apk/release/"
fi
