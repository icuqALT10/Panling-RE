# 需要解锁的物品模板

## 阴阳还魂符

- 物品 ID：`panlingre:huan_hun_fu`。
- 指令：`/plre unlock item <ItemID> <on/off/query>`，需要 2 级命令权限，作用于指令执行者。
- `on` 解锁合成与使用；`off` 取消解锁并移除对应配方书条目；`query` 显示并返回 `1`（已解锁）或 `0`（未解锁），不改变状态。切换权限会立即刷新已打开的合成界面，不重置物品冷却。
- 例如：`/plre unlock item panlingre:huan_hun_fu on`。
- 控制台、命令方块或任务函数为指定玩家解锁：`execute as <玩家> run plre unlock item panlingre:huan_hun_fu on`。
- 解锁前不能合成，也不能触发保命效果；`/give` 或原版 `/recipe give` 不会赋予使用权限。
- 致命伤触发：清除原版图腾可清除的状态后，恢复至当前最大生命值的 15%，给予当前最大生命值 50% 的伤害吸收（30 秒），并给予 30 秒生命恢复 II。不附加抗火。
- 每次消耗一张，基础冷却由 `huan_hun_fu.COOLDOWN_TICKS` 决定（当前 1800 秒），调用现有 `cooldown_remove` 算法。冷却按玩家共享，换符、换手、换包不能重置。
- 解锁和冷却保存到玩家 attachment，死亡与读档保留。冷却以实际时间计算，离线期间也会经过。
- 生效位置：主手、副手持有的还魂符；主副手持有的符纸包；`fabao` 栏有效饰品槽中佩戴的符纸包。普通背包和未选中快捷栏中的还魂符或符纸包均不触发。
- 同时存在多个来源时只消耗一张。手持原版图腾优先；虚空、`/kill` 等绕过图腾的伤害仍不能抵挡。
- 此物品是被动符纸，右键和技能轮盘不能主动消耗。
- 触发时弹出还魂符自己的物品模型，并在玩家周围发出黑白两色的图腾形状粒子；旁观者也能看到粒子。原版不死图腾的模型和粒子不受影响。
- 未解锁时隐藏 JEI 物品列表条目，以及产出该物品的制符台、炼丹炉、锻造台和原版合成配方。`on/off` 同步到客户端后自动更新，无需重进游戏。直接查看背包中已有物品的配方也受此限制。

主要文件：

- `src/main/java/icu/icuqalt10/panlingre/item/fuzhi/huan_hun_fu.java`：效果、触发位置、消耗与冷却。
- `src/main/resources/data/panlingre/recipe/zft/huan_hun_fu.json`：临时制符台配方，可直接修改。
- `src/main/resources/assets/panlingre/models/item/huan_hun_fu.json`：使用已有的 `item/fuzhi/huan_hun_fu.png` 材质。

当前配方以 `data/panlingre/recipe/zft/huan_hun_fu.json` 为准，产出 1 张还魂符。

## 后续添加主动符纸

继承现有 `FuZhiItem`，加上 `implements RecipeLockedItem`，其余沿用普通符纸的注册、模型和配方流程：

```java
public class ExampleFu extends FuZhiItem implements RecipeLockedItem {
    public ExampleFu() {
        // 冷却 tick、灵气消耗、施法 tick、符纸包法力值加成、说明翻译 ID
        super(200, 5.0F, 20, 0.0D, "example_fu");
    }

    @Override
    protected void applyEffect(Level level, Player player) {
        // 在这里编写实际效果；基类会在执行前检查解锁，处理手持消耗。
    }
}
```

在 `ModItems` 注册后，这个 ID 会自动出现在 `/plre unlock item` 的补全列表中。加入 `data/panlingre/tags/item/fuzhi.json` 后可放入符纸包。解锁指令会同时授予该物品现有配方，兼容 `doLimitedCrafting=true`。

实现 `RecipeLockedItem` 的后续物品也会自动接入 JEI 隐藏逻辑。客户端只在玩家或解锁数据变化时更新，JEI 重载／退出世界时清除旧运行时状态；未安装 JEI 时不加载这段兼容代码。

## 其他物品及被动效果

其他 `Item` 子类同样加上 `implements RecipeLockedItem`。公共代码已接入以下入口：

- `ItemStack` 的右键、对方块使用、对生物使用。
- `SkillWheelPayload` 的技能请求，以及 `FuZhiItem`／`FuZhiBagItem` 的符纸执行入口。
- 制符台、炼丹炉、锻造台的产物预览和取出，以及原版 2×2／3×3 合成格。

事件驱动的被动效果、库存 tick、自定义属性加成、其他容器的委托技能等不经过这些公共入口，必须在发放效果前调用同一个检查：

```java
if (!ItemUnlockData.canUse(player, stack)) return;
// 解锁后才执行效果、发放属性或消耗物品。
```

不要只检查配方书：配方书负责显示配方，`ItemUnlockData` 才是玩家权限来源。不要把解锁状态写到物品堆栈上，否则玩家间转移物品就可能转移权限。

有玩家的新增合成容器应在预览、取出、Shift 合成前调用 `ItemUnlockData.canUse(player, result)`；熔炉、自动合成器等没有玩家上下文的加工方式不在此模板接入范围内，不要直接为受限物品添加这类配方。

## 验证

```powershell
./gradlew.bat -I scripts/item-unlock-tests.gradle runGameTestServer --offline --console=plain
```

测试运行目录为 `build/item-unlock-test-run`，不会使用 `run/saves` 中的游戏存档。覆盖解锁指令、配方书绕过、Shift 合成、双手致命伤、精确百分比效果、符纸包、原版图腾优先级、冷却缩减、保存和死亡复制。

客户端集成测试（先运行上述服务端测试，以生成隔离世界的 `level.dat`）：

```powershell
New-Item -ItemType Directory -Force -Path build/item-unlock-client-run/saves/huan-hun-test
Copy-Item build/item-unlock-test-run/world/level.dat build/item-unlock-client-run/saves/huan-hun-test/level.dat
./gradlew.bat -I scripts/item-unlock-client-tests.gradle runItemUnlockClientTest --offline --console=plain
```

客户端自动验证 JEI 的未解锁／on／off／on 状态、真实致命伤后的弹出物品模型和黑白粒子颜色，完成后退出。结果位于 `build/item-unlock-client-run/test-result.txt`，截图位于同目录的 `screenshots` 文件夹。测试源码仅由测试脚本加载，不会打包进发布模组。
