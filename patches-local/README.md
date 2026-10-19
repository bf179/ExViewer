# patches-local — 我的自有功能补丁

本目录只放**我自己的功能**，ujui 的 4 个直连补丁保持在 `../patches/` 原样不动（保证基线与 ujui 逐字节一致）。

## 目录分层

```
patches-local/
├── feat/        两轨共用（直连版 + 彩E 代理版都用）
├── net/         仅直连轨（v2/ujui-direct）：直连相关调整
└── cai-only/    仅彩E 轨（v2/cai-proxy）：代理版特有调整
```

## 命名

```
1xxx-feat-<模块>.patch     例：1002-feat-hide-list.patch
2xxx-net-<模块>.patch      例：2001-net-drop-doh.patch
3xxx-cai-<模块>.patch      例：3001-cai-no-ech.patch
```

## 规则（重要）

1. **只允许新增或修改，不允许删除 ujui 的文件**（否则上游 merge 会炸）。
2. 每批补丁必须能单独 `git apply --check` 通过。
3. 补丁内的路径以 submodule 根为基准（`app/...`、`core/...`）。
4. 功能补丁不得依赖 `net/` 里的符号（保证彩E 轨也能编译）。
5. 每次上游 bump 后，先跑 `scripts/verify-track.sh direct` 确认冲突面。

## 数据层补丁顺序（不可乱）

```
1001-db-v24-sync-outbox.patch
1002-db-v25-batch-fav.patch
1003-db-v26-pq-hide-type.patch
```

因为 Room 的 `version` 必须单调递增，三条必须按序应用。
