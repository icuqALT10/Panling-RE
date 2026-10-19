# 盘灵刷怪笼

方块、物品和方块实体 ID 均为 `panlingre:spawner`，使用提供的刷怪笼材质。

## JSON 的位置与修改

如需随模组发布默认数据，可放在：

- `src/main/resources/data/panlingre/spawner/block/<路径>.json`：刷怪笼参数。
- `src/main/resources/data/panlingre/spawner/monster/<路径>.json`：加权生物列表。

当前主地图使用的可编辑数据位于：

- `run/saves/world/datapacks/盘灵：RE数据包/data/panlingre/spawner/block/`
- `run/saves/world/datapacks/盘灵：RE数据包/data/panlingre/spawner/monster/`

**日常调数值请修改地图数据包的 JSON，然后在游戏执行 `/reload`。** 如果模组也包含同名默认文件，地图数据包文件优先；修改源码资源需重新构建，且不会覆盖地图数据包里的同名配置。允许任意层级的子文件夹。

引用推荐写 `panlingre:east/common/zombie`，分别对应 block 或 monster 目录下的 `east/common/zombie.json`。也接受 `panlingre:spawner/block/east/common/zombie.json`、`panlingre:spawner/monster/east/common/zombie.json` 这样的完整资源路径。省略命名空间时默认 `panlingre`。

每次加载数据包时解析 JSON 并缓存；方块加载时解析引用，`/reload` 后已加载方块在下一服务端 tick 更新缓存。普通 tick 不访问文件、不重复解析配置。

## 配置示例

`block/east/common/zombie.json`：

```json
{
  "spawn_count": 1,
  "spawn_speed": { "min": 200, "max": 800 },
  "spawn_monster": "panlingre:east/common/zombie",
  "spawn_max_count": { "limit": 10, "distance": 12, "tags": ["monster"] }
}
```

`monster/east/common/zombie.json` 是一个数组，例如：

```json
[
  { "id": "minecraft:zombie", "nbt": { "Tags": ["monster"] }, "weight": 3 },
  { "id": "minecraft:skeleton", "nbt": { "Tags": ["monster"] }, "weight": 1 }
]
```

- `spawn_count`：非负整数或 `{ "min": 1, "max": 2 }`，默认 1。每轮抽取一次数量；范围包含两端。0 表示本轮不生成。
- `spawn_speed`：正整数或相同形式的范围，单位 tick，默认 20。每轮仅抽取一次间隔并保存；20 tick 约为正常运行时的一秒。
- `spawn_monster`：monster JSON 引用，也可以直接内嵌上面的数组。每个待生成生物独立按 `weight` 抽取；默认权重为 1，0 不被抽中，非空数组的总权重必须大于 0。
- `spawn_max_count`：`{ "limit": 10, "distance": 12, "tags": ["monster"] }`。以每只生物最终选定的生成位置为中心，统计球形半径内存活且同时具有所有指定标签的生物；数量达到 `limit` 就阻止生成。距离按实体位置计算，包含半径边界，不计 marker、掉落物等非生物实体。`tags:["1","2"]` 要求两个标签同时存在，空列表匹配所有存活生物。`limit` 为非负整数（0 始终禁止生成），`distance` 为非负数，可带小数。整个键省略时兼容旧值：上限 9、半径 10、标签 `monster`；主地图现有 39 份配置均已显式设为上限 10、半径 12、标签 `monster`。
- `nbt` 是完整的实体 NBT 对象，省略时为 `{}`。JSON 数值不加 SNBT 后缀；装备掉率、旋转、位置、速度与 UUID 会转为 Minecraft 对应的 NBT 数值类型。直接内嵌在命令里的 SNBT 仍使用原版类型，例如 `ArmorDropChances:[0f,0f,0f,0f]`。
- 缺失或空的 `spawn_monster` 不刷怪；错误配置会记录位置/文件名并暂停该刷怪笼，修复后 `/reload` 恢复。

## 放置与直接配置

引用 JSON：

```mcfunction
give @s panlingre:spawner[minecraft:block_entity_data={id:"panlingre:spawner",spawner_data:"panlingre:east/common/zombie"}]
setblock ~ ~ ~ panlingre:spawner{spawner_data:"panlingre:east/common/zombie"}
```

直接内嵌参数和生物列表：

```mcfunction
setblock ~ ~ ~ panlingre:spawner{spawner_data:{spawn_count:{min:1,max:2},spawn_speed:100,spawn_max_count:{limit:10,distance:12,tags:["monster"]},spawn_monster:[{id:"minecraft:zombie",nbt:{Tags:["monster"]},weight:3},{id:"minecraft:skeleton",nbt:{Tags:["monster"]},weight:1}]}}
```

修改已放置方块的引用：

```mcfunction
data merge block ~ ~ ~ {spawner_data:"panlingre:west/common/zombie"}
data get block ~ ~ ~ LastSpawnTick
```

`LastSpawnTick` 为根标签下的 long，使用世界 `gameTime`；`/time set` 不会改变它。`SpawnDelay` 保存当前轮抽到的间隔，避免区块重新加载时重抽。首次放置从当前时间开始计时；区块卸载后，已过去的世界 tick 计入间隔，回来最多触发一轮，不补刷所有错过的轮次。成功生成一轮（或抽到数量 0）后更新时间；缺少玩家、没有空地或达到怪物上限时不更新时间。`/reload` 修改间隔时按新间隔重新抽取，保留上次生成时间。

## 保留的地图规则

- 刷怪笼中心 18 格内有存活且非旁观模式玩家时生成。
- 每个待生成生物先独立按权重抽取并加载 NBT，从刷怪笼上方第 2 格起，沿所在列逐格向上寻找可以容纳完整碰撞箱的位置，生成在该格底部中心。紧贴刷怪笼上方的一格检修空间会被跳过；幼年状态、`minecraft:generic.scale` 和骑乘乘客的实际占用空间均参与判断。
- 搜索起点由 `MonsterSpawnerBlockEntity.SPAWN_SEARCH_START_OFFSET` 控制，当前为 `2`。这是针对地图中一格高检修间的规则，不会自动识别任意高度的检修间；如果检修间加高，可将该常量调到顶板所在高度或更高，重新构建后生效。上方其他正常的一格高空间仍允许容得下的生物生成。
- 以方块的实际碰撞形状判断地形，不再使用固定的两格高度或可穿过方块标签。检查宽度和高度，并要求完整身体处于建筑高度、世界边界和已加载区块内；找不到则跳过本次生成。仍不额外要求脚下有实体方块，不改变原来的液体允许规则，也不因已有生物占位而向上堆叠生成。
- 每只生物在找到最终生成点后检查 `spawn_max_count`；同一批次已经生成的生物也会计入，附近其他刷怪笼生成的符合标签的生物同样计入。整轮未能生成时，20 tick 后再检查，不更新 `LastSpawnTick`。
- 直接加载实体 NBT，行为与带 NBT 的 `summon` 一致，不调用随机装备初始化，不受自然生成的亮度等条件限制。
- 旧 marker 函数保留以兼容尚存的 marker；新刷怪笼完全不生成 marker，因此不会重复触发旧逻辑。

## 副手物品：催生仪

```mcfunction
give @s panlingre:spawner_boost
```

放在副手时，以持有者位置为中心，对半径内的模组刷怪笼提供临时增强。主手、背包、死亡或旁观模式不生效。移出副手或离开范围后恢复基础倍率，不修改 JSON、`spawner_data`、`LastSpawnTick` 或保存的基础 `SpawnDelay`。物品不消耗、不可堆叠，无需右键。

三个静态字段均在 `src/main/java/icu/icuqalt10/panlingre/item/other/SpawnerBoostItem.java`：

```java
public static final int RADIUS = 16;
public static final double SPAWN_FREQUENCY_MULTIPLIER = 2.0;
public static final double SPAWN_COUNT_MULTIPLIER = 2.0;
```

频率 ×2 表示实际间隔为基础间隔的一半，向上取整且至少 1 tick；数量 ×2 表示本轮抽到的基础数量乘 2，小数结果向下取整。静态字段修改后需重新编译并重启游戏。同一刷怪笼附近有多个持有者时，频率和数量分别取适用倍率的最大值，不累乘；物品堆叠数量也不影响倍率。原有玩家激活距离和附近怪物上限仍然生效。

复制以下代码到 `item/other` 包即可定义另一套倍率；继承 `SpawnerBoostItem` 后，刷怪笼会自动识别，无需修改刷怪逻辑：

```java
package icu.icuqalt10.panlingre.item.other;

public class StrongSpawnerBoostItem extends SpawnerBoostItem {
    public static final int RADIUS = 24;
    public static final double SPAWN_FREQUENCY_MULTIPLIER = 3.0;
    public static final double SPAWN_COUNT_MULTIPLIER = 2.0;

    public StrongSpawnerBoostItem() {
        super(RADIUS, SPAWN_FREQUENCY_MULTIPLIER, SPAWN_COUNT_MULTIPLIER);
    }
}
```

随后在 `ModItems` 注册新 ID，并为新物品添加模型、语言名称和创造物品栏入口即可。

## 本次地图迁移与验收

38 份 monster 定义，39 份 block 定义；其中 `east/common/spider_1_200_400` 保留单独调整过的 200–400 tick 间隔。

替换四方 `spawn_entity` 逻辑的 534 个刷怪笼，涉及 12 个主世界区域文件；其余 744 个旧刷怪笼保持原样。四方 `spawner.mcfunction` 领取命令同步改为新方块。

`(466, 48, 26)` 原刷怪笼设置了 `SpawnRange:1`，迁移后与新刷怪笼统一采用所在列向上查找的规则。数量和间隔保留，其他目标原本都是 `SpawnRange:0`。

迁移器先复制完整受影响区域和领取函数到 `tmp/spawner-backup-<时间>/`，保存 SHA-256、每个刷怪笼的旧 NBT 与坐标，并检查世界未被 Minecraft 锁定。对每个修改区块逐方块校验，仅目标位置变成新方块，其他区块数据、方块实体、实体和世界信息不变。存档中原 `Delay` 转为同等剩余等待时间。

复核工具：`scripts/migrate_spawners.py`。不带参数只扫描；`--export` 导出源码 JSON；`--apply` 执行备份、迁移和地图 JSON 安装。已有地图数值调整后不要重新导出覆盖手工编辑。恢复时关闭游戏，将备份的区域文件和领取函数复制回主地图；`manifest.json` 的 `added_files` 列出此次新增的数据包 JSON。

自动测试命令：

```powershell
./gradlew.bat -I scripts/spawner-tests.gradle runGameTestServer --offline
```

测试使用 `build/spawner-test-run` 独立世界，不打开主地图。

原生物数据中已有 `generic.minecraft:attack_damage` 这个无效属性 ID；本次按原数据保留，Minecraft 会忽略该属性。若要修复原有攻击力配置，可另行改为 `minecraft:generic.attack_damage`。
