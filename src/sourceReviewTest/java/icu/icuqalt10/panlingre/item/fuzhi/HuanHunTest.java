package icu.icuqalt10.panlingre.item.fuzhi;

import com.mojang.authlib.GameProfile;
import icu.icuqalt10.panlingre.attachment.ItemUnlockData;
import icu.icuqalt10.panlingre.component.FuZhiBagContents;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.init.ModItems;
import icu.icuqalt10.panlingre.world.inventory.zftMenu;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;

@GameTestHolder("panlingre")
@PrefixGameTestTemplate(false)
public class HuanHunTest {
    @GameTest(template = "empty")
    public static void ordinaryInventoryAndUnselectedHotbarNeverActivate(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        unlock(player);
        for (int slot : new int[]{1, 12}) {
            player.getInventory().setItem(slot, new ItemStack(ModItems.huan_hun_fu.get(), 2));
            helper.assertTrue(!huan_hun_fu.tryProtect(player, player.damageSources().generic()),
                    "Loose talisman activated from inventory slot " + slot);
            ItemStack storedBag = bag(3);
            player.getInventory().setItem(slot, storedBag);
            helper.assertTrue(!huan_hun_fu.tryProtect(player, player.damageSources().generic()),
                    "Stored bag activated from inventory slot " + slot);
            helper.assertTrue(FuZhiBagItem.getContents(storedBag).count(0) == 3,
                    "Inactive bag consumed a talisman");
            helper.assertTrue(player.getData(ModAttachments.HUAN_HUN_COOLDOWN) == 0,
                    "Inactive bag started a cooldown");
            player.getInventory().setItem(slot, ItemStack.EMPTY);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lockedItemsCannotProtectOrCraftEvenWithRecipeBookEntry(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var recipe = helper.getLevel().getRecipeManager().byKey(ModItems.huan_hun_fu.getId().withPrefix("zft/")).orElseThrow();
        player.awardRecipes(java.util.List.of(recipe));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.huan_hun_fu.get(), 2));
        helper.assertTrue(!huan_hun_fu.tryProtect(player, player.damageSources().generic()), "Locked talisman protected player");
        helper.assertTrue(player.getMainHandItem().getCount() == 2, "Locked item was consumed");
        var menu = filledMenu(player);
        helper.assertTrue(menu.getSlot(7).getItem().isEmpty(), "Locked recipe produced output");
        helper.assertTrue(menu.quickMoveStack(player, 7).isEmpty(), "Shift craft bypassed lock");
        helper.assertTrue(menu.getSlot(0).getItem().getCount() == 4, "Rejected craft consumed ingredients");
        helper.assertTrue(!new ResultContainer().setRecipeUsed(player.level(), player, recipe), "Vanilla crafting gate bypassed lock");
        helper.assertTrue(ItemUnlockData.canUse(player, ModItems.ju_li_fu.get()), "Existing talisman unexpectedly locked");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void commandUnlocksOnlyExecutorAndAllowsShiftCraft(GameTestHelper helper) throws Exception {
        ServerPlayer player = player(helper);
        var server = helper.getLevel().getServer();
        var rule = helper.getLevel().getGameRules().getRule(GameRules.RULE_LIMITED_CRAFTING);
        boolean old = rule.get();
        try {
            rule.set(true, server);
            int result = server.getCommands().getDispatcher().execute("plre unlock item panlingre:huan_hun_fu on",
                    player.createCommandSourceStack().withPermission(2));
            helper.assertTrue(result == 1 && ItemUnlockData.canUse(player, ModItems.huan_hun_fu.get()), "Command failed to unlock");
            helper.assertTrue(!ItemUnlockData.canUse(player(helper), ModItems.huan_hun_fu.get()), "Unlock leaked to another player");
            var menu = filledMenu(player);
            helper.assertTrue(menu.getSlot(7).getItem().is(ModItems.huan_hun_fu.get()), "Unlocked recipe unavailable under limited crafting");
            helper.assertTrue(menu.quickMoveStack(player, 7).is(ModItems.huan_hun_fu.get()), "Unlocked shift craft failed");
            helper.assertTrue(menu.getSlot(0).getItem().isEmpty() && menu.getSlot(2).getItem().isEmpty(), "Craft failed to consume ingredients");
            helper.assertTrue(player.getInventory().countItem(ModItems.huan_hun_fu.get()) == 1, "Craft duplicated output");
        } finally {
            rule.set(old, server);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void commandOffAndQueryUpdatePermissionsAndOpenCraftingResults(GameTestHelper helper) throws Exception {
        ServerPlayer player = player(helper);
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        var source = player.createCommandSourceStack().withPermission(2);
        String command = "plre unlock item panlingre:huan_hun_fu ";
        var menu = filledMenu(player);
        player.containerMenu = menu;
        long expiry = System.currentTimeMillis() + 1200000;
        player.setData(ModAttachments.HUAN_HUN_COOLDOWN, expiry);
        helper.assertTrue(dispatcher.execute(command + "query", source) == 0, "Locked query must return zero");
        helper.assertTrue(!ItemUnlockData.canUse(player, ModItems.huan_hun_fu.get()), "Query changed unlock state");
        helper.assertTrue(dispatcher.execute(command + "on", source) == 1, "On command failed");
        helper.assertTrue(dispatcher.execute(command + "query", source) == 1, "Unlocked query must return one");
        helper.assertTrue(menu.getSlot(7).getItem().is(ModItems.huan_hun_fu.get()), "On did not refresh open crafting result");
        helper.assertTrue(dispatcher.execute(command + "off", source) == 1, "Off command failed");
        helper.assertTrue(dispatcher.execute(command + "query", source) == 0, "Off did not revoke permission");
        helper.assertTrue(menu.getSlot(7).getItem().isEmpty() && menu.quickMoveStack(player, 7).isEmpty(),
                "Off left a craftable stale output");
        helper.assertTrue(menu.getSlot(0).getItem().getCount() == 4, "Off consumed crafting inputs");
        var recipe = helper.getLevel().getRecipeManager().byKey(ModItems.huan_hun_fu.getId().withPrefix("zft/")).orElseThrow();
        helper.assertTrue(!player.getRecipeBook().contains(recipe), "Off left the recipe book entry unlocked");
        helper.assertTrue(player.getData(ModAttachments.HUAN_HUN_COOLDOWN) == expiry, "Changing permission reset cooldown");
        player.setData(ModAttachments.HUAN_HUN_COOLDOWN, 0L);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.huan_hun_fu.get()));
        helper.assertTrue(!huan_hun_fu.tryProtect(player, player.damageSources().generic()), "Off still allowed passive protection");
        helper.assertTrue(dispatcher.execute("plre unlock item minecraft:stick on", source) == 0, "Ordinary item accepted unlock command");
        boolean missingActionRejected = false;
        try {
            dispatcher.execute(command.trim(), source);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) {
            missingActionRejected = true;
        }
        helper.assertTrue(missingActionRejected, "Missing on/off/query was accepted");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lethalDamageUsesEitherHandAndAppliesExactPercentages(GameTestHelper helper) {
        for (InteractionHand hand : InteractionHand.values()) {
            ServerPlayer player = player(helper);
            unlock(player);
            player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(102);
            player.setHealth(102);
            player.setItemInHand(hand, new ItemStack(ModItems.huan_hun_fu.get(), 2));
            player.hurt(player.damageSources().generic(), 10000);
            helper.assertTrue(player.isAlive(), "Lethal damage was not intercepted for " + hand);
            helper.assertTrue(Math.abs(player.getHealth() - 15.3F) < 0.001F, "Health was not 15 percent");
            helper.assertTrue(Math.abs(player.getAbsorptionAmount() - 51) < 0.001F, "Absorption was rounded instead of exact 50 percent");
            var regen = player.getEffect(MobEffects.REGENERATION);
            helper.assertTrue(regen != null && regen.getDuration() == 600 && regen.getAmplifier() == 1, "Wrong regeneration");
            helper.assertTrue(player.getEffect(MobEffects.ABSORPTION).getDuration() == 600, "Wrong absorption duration");
            helper.assertTrue(!player.hasEffect(MobEffects.FIRE_RESISTANCE), "Unexpected fire resistance");
            helper.assertTrue(player.getItemInHand(hand).getCount() == 1, "Must consume exactly one talisman");
            long remaining = player.getData(ModAttachments.HUAN_HUN_COOLDOWN) - System.currentTimeMillis();
            long baseCooldown = huan_hun_fu.COOLDOWN_TICKS * 50L;
            helper.assertTrue(remaining > baseCooldown - 5000 && remaining <= baseCooldown, "Wrong configured base cooldown");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bagsConsumeOneAndShareReducedCooldownWithHands(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        unlock(player);
        player.getAttribute(ModAttributes.COOLDOWN_REMOVE).setBaseValue(0.25);
        ItemStack first = bag(3);
        ItemStack second = bag(4);
        player.setItemInHand(InteractionHand.MAIN_HAND, first);
        player.setItemInHand(InteractionHand.OFF_HAND, second);
        helper.assertTrue(huan_hun_fu.tryProtect(player, player.damageSources().generic()), "Main-hand bag did not protect");
        helper.assertTrue(FuZhiBagItem.getContents(first).count(0) == 2 && FuZhiBagItem.getContents(second).count(0) == 4,
                "Multiple bags consumed more than one talisman");
        long expiry = player.getData(ModAttachments.HUAN_HUN_COOLDOWN);
        long reducedCooldown = huan_hun_fu.COOLDOWN_TICKS * 50L * 3 / 4;
        helper.assertTrue(expiry - System.currentTimeMillis() > reducedCooldown - 5000
                        && expiry - System.currentTimeMillis() <= reducedCooldown,
                "25 percent reduction must retain 75 percent of the configured cooldown");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.huan_hun_fu.get()));
        player.getCooldowns().removeCooldown(ModItems.huan_hun_fu.get());
        helper.assertTrue(!huan_hun_fu.tryProtect(player, player.damageSources().generic()), "Switching to hand bypassed saved cooldown");
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "Cooldown consumed a second item");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void equippedAndOffhandBagsWorkButLooseInventoryDoesNot(GameTestHelper helper) {
        ServerPlayer loose = player(helper);
        unlock(loose);
        loose.getInventory().setItem(12, new ItemStack(ModItems.huan_hun_fu.get()));
        helper.assertTrue(!huan_hun_fu.tryProtect(loose, loose.damageSources().generic()), "Loose inventory item should not activate");
        ServerPlayer offhand = player(helper);
        unlock(offhand);
        offhand.setItemInHand(InteractionHand.OFF_HAND, bag(1));
        helper.assertTrue(huan_hun_fu.tryProtect(offhand, offhand.damageSources().generic()), "Offhand bag did not activate");
        ServerPlayer equipped = player(helper);
        unlock(equipped);
        var handler = CuriosApi.getCuriosInventory(equipped).orElseThrow();
        var slot = handler.getStacksHandler(FuZhiBagItem.CURIO_SLOT).orElseThrow();
        slot.addTransientModifier(new AttributeModifier(ModItems.huan_hun_fu.getId(), 1, AttributeModifier.Operation.ADD_VALUE));
        slot.update();
        slot.getStacks().setStackInSlot(0, bag(1));
        helper.assertTrue(huan_hun_fu.tryProtect(equipped, equipped.damageSources().generic()), "Equipped bag did not activate");
        helper.assertTrue(FuZhiBagItem.getContents(slot.getStacks().getStackInSlot(0)).entries().isEmpty(), "Last bag talisman was not removed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vanillaTotemWinsAndBypassDamageCannotActivate(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        unlock(player);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.huan_hun_fu.get()));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        player.hurt(player.damageSources().generic(), 10000);
        helper.assertTrue(player.isAlive() && player.getOffhandItem().isEmpty(), "Vanilla totem did not activate");
        helper.assertTrue(player.getMainHandItem().getCount() == 1 && player.getData(ModAttachments.HUAN_HUN_COOLDOWN) == 0,
                "Talisman activated together with vanilla totem");
        helper.assertTrue(!huan_hun_fu.tryProtect(player, player.damageSources().fellOutOfWorld()), "Void bypass was ignored");
        helper.assertTrue(!huan_hun_fu.tryProtect(player, player.damageSources().genericKill()), "Kill bypass was ignored");
        helper.assertTrue(((huan_hun_fu) ModItems.huan_hun_fu.get()).getSkillCount() == 0, "Passive item exposed active skill");
        helper.assertTrue(!((huan_hun_fu) ModItems.huan_hun_fu.get()).skill_use(player.level(), player, player.getMainHandItem(), 0),
                "Passive item allowed active use");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unlockAndCooldownSurviveSavingAndDeathCopy(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        unlock(player);
        long expiry = System.currentTimeMillis() + 1200000;
        player.setData(ModAttachments.HUAN_HUN_COOLDOWN, expiry);
        CompoundTag saved = player.saveWithoutId(new CompoundTag());
        ServerPlayer loaded = player(helper);
        loaded.load(saved);
        helper.assertTrue(ItemUnlockData.canUse(loaded, ModItems.huan_hun_fu.get()), "Unlock lost on load");
        helper.assertTrue(loaded.getData(ModAttachments.HUAN_HUN_COOLDOWN) == expiry, "Cooldown lost on load");
        ServerPlayer clone = player(helper);
        clone.copyAttachmentsFrom(player, true);
        helper.assertTrue(ItemUnlockData.canUse(clone, ModItems.huan_hun_fu.get()), "Unlock lost on death");
        helper.assertTrue(clone.getData(ModAttachments.HUAN_HUN_COOLDOWN) == expiry, "Cooldown lost on death");
        helper.succeed();
    }

    private static ItemStack bag(int count) {
        ItemStack bag = new ItemStack(ModItems.fu_zhi_bao.get());
        FuZhiBagItem.setContents(bag, FuZhiBagContents.EMPTY.add(0, ModItems.huan_hun_fu.getId(), count));
        return bag;
    }

    private static void unlock(ServerPlayer player) {
        player.setData(ModAttachments.ITEM_UNLOCKS, ItemUnlockData.EMPTY.unlock(ModItems.huan_hun_fu.getId()));
    }

    private static zftMenu filledMenu(ServerPlayer player) {
        var menu = new zftMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(0).set(new ItemStack(ModItems.zhu_sha_4.get(), 4));
        menu.getSlot(1).set(new ItemStack(ModItems.fu_zhi.get()));
        menu.getSlot(2).set(new ItemStack(ModItems.ys1_jin.get(), 50));
        menu.getSlot(3).set(new ItemStack(ModItems.ys1_mu.get(), 50));
        menu.getSlot(4).set(new ItemStack(ModItems.ys1_shui.get(), 50));
        menu.getSlot(5).set(new ItemStack(ModItems.ys1_huo.get(), 50));
        menu.getSlot(6).set(new ItemStack(ModItems.ys1_tu.get(), 50));
        return menu;
    }

    private static ServerPlayer player(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "huan-hun-test"), false);
        var player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        player.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player, cookie) {
            @Override public void send(Packet<?> packet) {}
        };
        // Fresh server players have 60 ticks of spawn protection before ordinary damage can land.
        for (int tick = 0; tick < 60; tick++) player.tick();
        return player;
    }
}
