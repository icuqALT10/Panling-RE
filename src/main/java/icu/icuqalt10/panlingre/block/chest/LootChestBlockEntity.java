package icu.icuqalt10.panlingre.block.chest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import icu.icuqalt10.panlingre.init.ModBlockEntities;
import icu.icuqalt10.panlingre.init.ModComponents;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class LootChestBlockEntity extends BlockEntity {

    private String chestType = "golden";
    private String chestId = "";
    private String lootTableId = "";
    private int openTick = -1;
    private String openerName = "";
    private boolean currentIsSpecial;

    private transient ItemEntity displayEntity;
    private transient List<LootChestLoader.LootEntry> availableEntries;

    private static final Map<String, List<LootChestLoader.LootEntry>> lootCache = new HashMap<>();

    public LootChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOOT_CHEST_BE.get(), pos, state);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        chestType = tag.getString("chestType");
        if (chestType.isEmpty()) chestType = "golden";
        chestId = tag.getString("chestId");
        lootTableId = tag.getString("lootTableId");
        openTick = tag.getInt("openTick");
        openerName = tag.getString("openerName");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("chestType", chestType);
        tag.putString("chestId", chestId);
        tag.putString("lootTableId", lootTableId);
        tag.putInt("openTick", openTick);
        tag.putString("openerName", openerName);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public String getChestType() { return chestType; }
    public String getChestId() { return chestId; }
    public String getLootTableId() { return lootTableId; }
    public void setChestType(String v) { this.chestType = v; setChanged(); }
    public void setChestId(String v) { this.chestId = v; setChanged(); }
    public void setLootTableId(String v) { this.lootTableId = v; setChanged(); }
    public boolean isOpening() { return openTick >= 0; }
    public int getOpenTick() { return openTick; }

    // ==================== Logic ====================

    public boolean tryOpenWith(ItemStack key, String playerName) {
        if (isOpening()) return false;
        String keyType = key.getOrDefault(ModComponents.KEY_TYPE.get(), "");
        String keyId = key.getOrDefault(ModComponents.KEY_ID.get(), "");
        if (!keyType.equals(chestType) || !keyId.equals(chestId) || keyId.isEmpty()) return false;
        if (!(level instanceof ServerLevel serverLevel)) return false;

        List<LootChestLoader.LootEntry> raw = getLootEntries(serverLevel);
        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayerByName(playerName);
        availableEntries = filterByCommand(raw, serverLevel, player);
        if (availableEntries.isEmpty()) return false;

        // 第一次抽取并生成展示实体
        LootChestLoader.LootEntry first = rollEntry(availableEntries, serverLevel.random);
        if (first == null) return false;

        Vec3 c = worldPosition.getCenter();
        displayEntity = new ItemEntity(serverLevel, c.x, worldPosition.getY() + 0.95, c.z, first.item().copy());
        displayEntity.setPickUpDelay(32767);
        displayEntity.setDeltaMovement(Vec3.ZERO);
        displayEntity.setCustomName(first.item().getHoverName());
        displayEntity.setCustomNameVisible(true);
        serverLevel.addFreshEntity(displayEntity);
        currentIsSpecial = first.specialItem();

        openTick = 0;
        openerName = playerName;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        return true;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LootChestBlockEntity be) {
        if (!be.isOpening()) return;
        be.openTick++;

        if (be.openTick % 10 == 0 && be.openTick <= 100) {
            if (level instanceof ServerLevel serverLevel) {
                if (be.displayEntity == null || !be.displayEntity.isAlive()) {
                    Vec3 c = pos.getCenter();
                    be.displayEntity = new ItemEntity(serverLevel, c.x, pos.getY() + 0.95, c.z, ItemStack.EMPTY);
                    be.displayEntity.setPickUpDelay(32767);
                    be.displayEntity.setDeltaMovement(Vec3.ZERO);
                    serverLevel.addFreshEntity(be.displayEntity);
                }

                LootChestLoader.LootEntry entry = be.rollEntry(be.availableEntries, serverLevel.random);
                if (entry != null) {
                    ItemStack newItem = entry.item().copy();
                    be.displayEntity.setItem(newItem);
                    be.displayEntity.setCustomName(newItem.getHoverName());
                    be.displayEntity.setCustomNameVisible(true);
                    be.currentIsSpecial = entry.specialItem();
                }

                if (be.openTick < 100) {
                    be.playSound(serverLevel, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.4f);
                }
            }

            if (be.openTick == 100 && level instanceof ServerLevel serverLevel) {
                be.displayEntity.setPickUpDelay(20);

                if (be.currentIsSpecial) {
                    be.broadcast(serverLevel, be.displayEntity.getItem());
                    be.playSound(serverLevel, SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR, 1.0f);
                    be.spawnFirework(serverLevel);
                } else {
                    be.playSound(serverLevel, SoundEvents.PLAYER_LEVELUP, 1.0f);
                }

                be.openTick = -1;
                be.displayEntity = null;
                be.availableEntries = null;
                be.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
            }
        } else if (be.openTick > 110) {
            be.openTick = -1;
            if (be.displayEntity != null && be.displayEntity.isAlive()) {
                be.displayEntity.discard();
            }
            be.displayEntity = null;
            be.availableEntries = null;
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    private List<LootChestLoader.LootEntry> filterByCommand(List<LootChestLoader.LootEntry> raw, ServerLevel level, ServerPlayer player) {
        List<LootChestLoader.LootEntry> filtered = new ArrayList<>();
        for (var e : raw) {
            if (e.command().isEmpty()) {
                filtered.add(e);
            } else if (player != null) {
                CommandSourceStack src = player.createCommandSourceStack()
                        .withPosition(worldPosition.getCenter())
                        .withSuppressedOutput()
                        .withPermission(4);
                try {
                    int result = level.getServer().getCommands().getDispatcher().execute(e.command(), src);
                    if (result > 0) {
                        filtered.add(e);
                    }
                } catch (CommandSyntaxException ignored) {
                }
            }
        }
        return filtered;
    }

    private void spawnFirework(ServerLevel serverLevel) {
        Vec3 c = worldPosition.getCenter();
        FireworkExplosion explosion = new FireworkExplosion(
                FireworkExplosion.Shape.SMALL_BALL,
                new IntArrayList(new int[]{65280}),
                new IntArrayList(),
                false, true);
        Fireworks fireworks = new Fireworks(0, List.of(explosion));
        ItemStack fwStack = new ItemStack(Items.FIREWORK_ROCKET);
        fwStack.set(DataComponents.FIREWORKS, fireworks);
        FireworkRocketEntity fw = new FireworkRocketEntity(serverLevel, c.x, c.y + 1.5, c.z, fwStack);
        try {
            var f = FireworkRocketEntity.class.getDeclaredField("lifetime");
            f.setAccessible(true);
            f.setInt(fw, 1);
        } catch (Exception ignored) {}
        serverLevel.addFreshEntity(fw);
    }

    private void playSound(ServerLevel serverLevel, net.minecraft.sounds.SoundEvent sound, float volume) {
        serverLevel.playSound(null, worldPosition, sound, SoundSource.BLOCKS, volume, 1.0f);
    }

    private void broadcast(ServerLevel serverLevel, ItemStack drop) {
        String dungeonKey = "plre.loot_chest.instance_name." + chestId;
        Component msg = Component.translatable("plre.loot_chest.broadcast",
                Component.literal(openerName).withStyle(ChatFormatting.YELLOW),
                Component.translatable(dungeonKey),
                drop.getDisplayName().copy().withStyle(ChatFormatting.GOLD));
        serverLevel.getServer().getPlayerList().broadcastSystemMessage(msg, false);
    }

    private List<LootChestLoader.LootEntry> getLootEntries(ServerLevel serverLevel) {
        return lootCache.computeIfAbsent(lootTableId, id -> LootChestLoader.load(id, serverLevel));
    }

    private LootChestLoader.LootEntry rollEntry(List<LootChestLoader.LootEntry> entries, net.minecraft.util.RandomSource random) {
        if (entries == null || entries.isEmpty()) return null;
        int totalWeight = 0;
        for (var e : entries) totalWeight += e.weight();
        if (totalWeight <= 0) return entries.get(random.nextInt(entries.size()));
        int roll = random.nextInt(totalWeight);
        for (var e : entries) {
            roll -= e.weight();
            if (roll < 0) return e;
        }
        return entries.get(entries.size() - 1);
    }
}
