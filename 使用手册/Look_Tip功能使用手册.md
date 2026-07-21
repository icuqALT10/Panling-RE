# Look Tip 功能使用手册

## 概述

Look Tip 是一个类似 Jade mod 的功能，当玩家看向特定的实体或方块时，会在屏幕中央偏右下方显示提示文本。

## 配置文件位置

配置文件放在 `data/panlingre/look_tip/` 目录下，文件名任意，扩展名为 `.json`。

## 基本格式

```json
{
    "title": {
        "text": "显示的文本\\n支持换行",
        "color": "gold"
    },
    "entities": [
        {
            "type": "entity 或 block",
            "name": "实体或方块ID",
            "匹配条件（可选）"
        }
    ]
}
```

## 字段说明

### title（必填）

要显示的文本内容，使用 Minecraft 文本组件格式。

**简单文本**：
```json
"title": "简单文本"
```

**带颜色和格式**：
```json
"title": {
    "text": "彩色文本",
    "color": "gold",
    "bold": true
}
```

**支持换行**：使用 `\\n`
```json
"title": {
    "text": "第一行\\n第二行\\n第三行"
}
```

### entities（必填）

匹配条件数组。当玩家看向的实体/方块满足其中任意一个条件时，就会显示文本。

#### type（必填）
- `"entity"` - 匹配实体
- `"block"` - 匹配方块

#### name（必填）
实体或方块的 ID。可以是单个字符串或字符串数组。

**单个 ID**：
```json
"name": "minecraft:villager"
```

**多个 ID**：
```json
"name": ["minecraft:villager", "minecraft:zombie", "panlingre:custom_mob"]
```

**注意**：ID 必须包含命名空间（如 `minecraft:` 或 `panlingre:`）

#### pos（可选）

位置匹配条件。用于匹配特定坐标或坐标范围内的实体/方块。

**固定坐标**（直接写整数）：
```json
"pos": {
    "x": 100,
    "y": 64,
    "z": -200
}
```

**范围匹配**：
```json
"pos": {
    "x": {"min": 10, "max": 20},
    "y": {"min": 60, "max": 70},
    "z": {"min": -100, "max": 100}
}
```

**混合使用**：
```json
"pos": {
    "x": 100,
    "y": {"min": 60, "max": 70},
    "z": {"max": 0}
}
```

**说明**：
- 直接写整数表示精确匹配该坐标
- 使用 `{"min": 值, "max": 值}` 定义范围（包含边界）
- 可以只指定 `min` 或只指定 `max`
- 三个轴（x、y、z）都必须指定

#### nbt（可选）

实体或方块实体的 NBT 数据字符串（SNBT 格式）。

**检测特定血量**：
```json
"nbt": "{Health:100.0f}"
```

**检测自定义名称**：
```json
"nbt": "{CustomName:'{\"text\":\"Boss Name\"}'}"
```

**注意**：
- **不支持 Tags 字段**（客户端无法获取实体 Tags）
- 支持的字段：Health、CustomName、Motion、Pos、Rotation 等
- 方块实体的 NBT 不包含坐标信息，请使用 `pos` 字段匹配坐标

#### block_state（可选，仅限 type: "block"）

方块的状态属性。

**检测方块状态**：
```json
"block_state": {
    "has_ore": "true"
}
```

**检测朝向**：
```json
"block_state": {
    "facing": "north"
}
```

**注意**：block_state 的值必须是字符串（如 `"true"` 而不是 `true`）

## 完整示例

### 示例 1：显示特定坐标的箱子
```json
{
    "title": {
        "text": "§e宝藏箱子\\n§7位于特殊坐标",
        "color": "yellow"
    },
    "entities": [
        {
            "type": "block",
            "name": "minecraft:chest",
            "pos": {
                "x": 89,
                "y": 48,
                "z": 12
            }
        }
    ]
}
```

### 示例 2：显示区域内的村民
```json
{
    "title": {
        "text": "§6安全区域内的村民",
        "color": "gold"
    },
    "entities": [
        {
            "type": "entity",
            "name": "minecraft:villager",
            "pos": {
                "x": {"min": 3115, "max": 3116},
                "y": {"min": 128, "max": 131},
                "z": {"min": -2228, "max": -2227}
            }
        }
    ]
}
```

### 示例 3：显示特定血量的怪物
```json
{
    "title": {
        "text": "§c§l精英怪物\\n§7血量: 100",
        "color": "red"
    },
    "entities": [
        {
            "type": "entity",
            "name": ["minecraft:zombie", "minecraft:skeleton"],
            "nbt": "{Health:100.0f}"
        }
    ]
}
```

### 示例 4：显示特定状态的矿石
```json
{
    "title": {
        "text": "§e朱砂矿石\\n§a可以挖掘",
        "color": "yellow"
    },
    "entities": [
        {
            "type": "block",
            "name": "panlingre:zhu_sha_ore",
            "block_state": {
                "has_ore": "true"
            }
        }
    ]
}
```

### 示例 5：多条件组合
```json
{
    "title": {
        "text": "§6特殊区域的村民\\n§7满足多个条件",
        "color": "gold"
    },
    "entities": [
        {
            "type": "entity",
            "name": "minecraft:villager",
            "pos": {
                "x": {"min": 0, "max": 100},
                "y": 64,
                "z": {"min": -50, "max": 50}
            },
            "nbt": "{Health:20.0f}"
        }
    ]
}
```

## 显示效果

- **位置**：屏幕中央偏右下方
- **文字**：白色带阴影
- **背景**：透明（无背景框）
- **换行**：支持 `\\n` 换行

## 技术说明

- **服务端匹配**：NBT 和位置匹配在服务端进行，确保数据准确
- **网络优化**：只在看向的目标变化时才发送请求，减少网络流量
- **自动更新**：资源包重载（F3+T）时自动更新配置

## 已知限制

- **方块 NBT 不含坐标**：方块实体的 NBT 不包含 x、y、z 字段，请使用 `pos` 字段匹配坐标

## 注意事项

1. JSON 文件必须使用 UTF-8 编码
2. 实体/方块 ID 必须包含命名空间（`minecraft:` 或 `panlingre:`）
3. block_state 的值必须是字符串格式（`"true"` 而不是 `true`）
4. 换行使用 `\\n`（两个反斜杠 + n）
5. 可以使用 Minecraft 的格式代码（如 `§c` 表示红色）
6. 配置文件在资源包重载时自动更新（F3+T）
