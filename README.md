# PackagedAuto Wireless（无线封包供应器）

Minecraft 1.12.2 Forge 附属模组，依赖 [PackagedAuto](https://www.curseforge.com/minecraft/mc-mods/packagedauto) 与 [PackagingProvider](https://www.curseforge.com/minecraft/mc-mods/packagingprovider)。

在 ME 封包供应器基础上，新增 **ME 无线封包供应器**，通过 **封包连接器** 物品远程驱动封包合成机，实现 **1 对 N** 服务。

---

## 新增内容

| 名称 | 类型 | 说明 |
|------|------|------|
| ME 无线封包供应器 | 方块 | 接入 ME 网络，提供样板；向已绑定的远程目标推送合成任务 |
| 封包连接器 | 物品 | 绑定远程封包方块 / 查看与清除连接列表 |

---

## 使用方法

### ME 无线封包供应器

1. 放置在 ME 线缆旁（与原版 ME 封包供应器相同，占用频道）。
2. 放入封包配方储存器。
3. 在 GUI 中配置径直 / 封包 / 解包供应模式（与 PackagingProvider 相同）。
4. 用封包连接器绑定远程目标（见下）。

### 封包连接器

1. **右键封包方块**（如高级封包合成器）— 记录坐标与点击面。
2. **再右键 ME 无线封包供应器** — 将该目标加入连接列表。
3. 可重复绑定多台机器（1 个供应器 → N 个目标，轮询分配）。
4. **未绑定**的连接器 **右键供应器** — 在聊天栏显示已连接坐标列表。
5. **Shift + 右键供应器** — 清除该供应器上的**全部**连接。
6. **Shift + 右键空气** — 清除连接器物品上的临时绑定。
---


# English

Minecraft 1.12.2 Forge addon for [PackagedAuto](https://www.curseforge.com/minecraft/mc-mods/packagedauto) + [PackagingProvider](https://www.curseforge.com/minecraft/mc-mods/packagingprovider).

Adds an **ME Wireless Packaging Provider** that can drive remote package machines (1→N) via a **Package Connector** item.

## Dependencies

- PackagedAuto `1.12.2-1.0.24+`
- PackagingProvider
- Applied Energistics 2 (AE2 Unofficial Extended Life)
- MixinBooter (required by PackagingProvider)

## Usage

### ME Wireless Packaging Provider

1. Place next to an ME cable (uses a channel like the normal ME Packaging Provider).
2. Insert a Package Recipe Holder.
3. Configure direct / packaging / unpackaging modes in the GUI (same as PackagingProvider).
4. Link remote targets with the Package Connector (below).

### Package Connector

1. Right-click a package machine (e.g. Advanced Package Crafter) — stores position + face.
2. Right-click the ME Wireless Packaging Provider — adds that target to its link list.
3. Repeat for more machines (1 provider → N targets, round-robin).
4. Right-click the provider with an **unbound** connector to list linked coordinates in chat.
5. **Shift + right-click** the wireless provider with the connector to clear **all** links.
6. Shift + right-click air to clear the temporary bind on the connector item.
