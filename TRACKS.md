# 双轨说明

本仓库同时维护两个版本，**共用同一套功能补丁**，只有「网络层」不同。

| 分支 | 基线 | 直连 | 适用场景 | 构建 |
|---|---|---|---|---|
| `v2/ujui-direct` | UjuiUjuMandan/EhViewer@e1381380 → submodule `257bfe8`（2026-07-26） | ✅ hosts + SNI=IP（ujui 的 4 个补丁） | 国内免代理（exhentai 主站可用） | `build-direct.yml` |
| `v2/cai-proxy` | Codeberg 彩E `main`（滚动最新） | ❌ 无 | 需要最新修复；走代理/VPN | `build-cai.yml`（每周自动构建） |

## 为什么不把两个版本做成一个

- 直连补丁（`patches/0001-anti-dpi.patch` 等）会改写 `EhDns` / `EhSSLSocketFactory` / `ktor` 层；
- 彩E 上游明确不做这类改动，且其架构（多模块）与补丁接缝点会持续变化；
- 拆成两条分支后：直连轨跟随 ujui（他维护补丁），彩E 轨跟随官方（拿最新修复），互不拖累。

## 同步上游

```bash
# 直连轨：拉 ujui 新提交（他通常只是 bump submodule 指针）
git checkout v2/ujui-direct
bash scripts/sync-upstream.sh --merge      # 合并 + 预演我的补丁是否仍适用

# 彩E 轨：拉彩E 最新（submodule 指针）
cd EhViewer && git fetch origin main && git checkout FETCH_HEAD && cd ..
git add EhViewer && git commit -m "chore: bump 彩E 到 $(git -C EhViewer log -1 --format=%h)"
```

## 只推自己的仓库

- `origin` = `github.com/bf179/ExViewer`（唯一可写）
- `ujui` / `upstream`(Codeberg) = **只读**，`git fetch` 可以，`git push` 禁止
