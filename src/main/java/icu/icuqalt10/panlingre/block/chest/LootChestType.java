package icu.icuqalt10.panlingre.block.chest;

import net.minecraft.util.StringRepresentable;

public enum LootChestType implements StringRepresentable {
    GOLDEN("golden"),
    SILVER("silver"),
    COPPER("copper");

    private final String name;

    LootChestType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static LootChestType byName(String name) {
        for (LootChestType type : values()) {
            if (type.name.equals(name)) return type;
        }
        return GOLDEN;
    }
}
