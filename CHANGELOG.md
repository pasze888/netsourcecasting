# 更新日志

本文件格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。

## [Unreleased]

### Changed

- 发布产物改名为 `<mod_id>-<mc_version>-<loader>-<mod_version>`（如 `netsourcecasting-1.21.1-neoforge-1.0.3.jar`），与 release tag 同名，下载到的文件能直接对上版本

## [1.0.2] - 2026-10-18

### Added

- 支持放在 Curios 饰品槽里的 AE2 无线终端，此前只识别主背包

## [1.0.1] - 2026-09-30

### Added

- 用法写进 Ars Nouveau 的 Worn Notebook，游戏内即可查阅，不必翻仓库

### Changed

- NeoForge 最低要求从 21.1.250 降到 21.1.219，本模组只用到长期稳定的 API

### Fixed

- 施法失败时不再叠加模组自己的动作栏提示，统一沿用原版的「魔力不足。」

## [1.0.0] - 2026-09-29

### Added

- 施法时优先从存储网络扣除魔源，网络不足的部分才回落玩家自身魔力（魔源由 Ars Énergistique 提供）
- 支持超越维度的维度网络作为第二条魔源来源

### Changed

- 模组定名 Network Source Casting（原 mesourcecasting）：只装超越维度的玩家不会误以为装错了
- ME 网络那一路改为可选，只装超越维度也能用；两条通道至少装一条，都没装时静默放行
