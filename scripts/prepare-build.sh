#!/usr/bin/env bash
# 准备构建树：重置 submodule → 打 ujui 补丁（直连轨）→ 打我的功能补丁
#
# 用法：
#   bash scripts/prepare-build.sh direct     # 直连版（ujui 基线）
#   bash scripts/prepare-build.sh cai        # 原版彩E（无直连，需代理）
#   bash scripts/prepare-build.sh direct --check   # 只检查能否干净应用，不改动
set -uo pipefail

TRACK="${1:-direct}"
MODE="${2:-}"

ROOT_DIR="$(dirname "${BASH_SOURCE[0]}")/.."
ROOT="$(cd "$ROOT_DIR" && pwd -W 2>/dev/null)"
[ -n "$ROOT" ] || ROOT="$(cd "$ROOT_DIR" && pwd)"
SUB="$ROOT/EhViewer"

if [ ! -d "$SUB/.git" ] && [ ! -f "$SUB/.git" ]; then
  echo "[1/5] 初始化 submodule"
  git -C "$ROOT" submodule update --init --recursive || exit 1
else
  echo "[1/5] 重置 submodule 到指针版本（丢弃上次打补丁的脏改动）"
  git -C "$SUB" checkout -q -- . 2>/dev/null
  git -C "$SUB" clean -qfd 2>/dev/null
fi

cd "$SUB" || exit 1

if [ "$TRACK" = "direct" ]; then
  echo "[2/5] 应用 ujui 直连补丁（patches/0001~0004）"
  if [ "$MODE" = "--check" ]; then
    git apply --check "$ROOT/patches/0001-anti-dpi.patch" || { echo "!! ujui 补丁冲突（上游已改）"; exit 2; }
  else
    bash "$ROOT/patches/apply.sh" || { echo "!! ujui 补丁应用失败"; exit 2; }
  fi
else
  echo "[2/5] 彩E 轨：跳过 ujui 直连补丁（本版本无直连、需代理）"
fi

echo "[3/5] 应用功能补丁（feat/ 共用）"
for p in "$ROOT"/patches-local/feat/*.patch; do
  [ -e "$p" ] || continue
  if [ "$MODE" = "--check" ]; then
    git apply --check "$p" || { echo "!! 冲突: $(basename "$p")"; exit 3; }
    echo "    ok  $(basename "$p")"
  else
    git apply "$p" || { echo "!! 失败: $(basename "$p")"; exit 3; }
    echo "    applied $(basename "$p")"
  fi
done

if [ "$TRACK" = "direct" ]; then
  echo "[4/5] 应用直连轨补丁（net/）"
  for p in "$ROOT"/patches-local/net/*.patch; do
    [ -e "$p" ] || continue
    git apply "$p" || { echo "!! 失败: $(basename "$p")"; exit 4; }
    echo "    applied $(basename "$p")"
  done
else
  echo "[4/5] 应用彩E 轨补丁（cai-only/）"
  for p in "$ROOT"/patches-local/cai-only/*.patch; do
    [ -e "$p" ] || continue
    git apply "$p" || { echo "!! 失败: $(basename "$p")"; exit 4; }
    echo "    applied $(basename "$p")"
  done
fi

echo "[5/5] 完成（track=$TRACK）。构建： cd EhViewer && ./gradlew :app:assembleRelease"
