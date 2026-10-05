package icu.icuqalt10.panlingre.review;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.authlib.GameProfile;
import icu.icuqalt10.panlingre.looktip.LookTipData;
import icu.icuqalt10.panlingre.looktip.LookTipMatcher;
import icu.icuqalt10.panlingre.looktip.LookTipLoader;
import icu.icuqalt10.panlingre.looktip.LookTipNetworkHandler;
import icu.icuqalt10.panlingre.looktip.LookTipRequestPayload;
import icu.icuqalt10.panlingre.entity.FireTrailTracker;
import icu.icuqalt10.panlingre.entity.FireTornadoEntity;
import icu.icuqalt10.panlingre.entity.MultipartEntity;
import icu.icuqalt10.panlingre.entity.OrientedBoundingBox;
import icu.icuqalt10.panlingre.entity.OrientedHitbox;
import icu.icuqalt10.panlingre.init.ModEntities;
import icu.icuqalt10.panlingre.recipe.LdlRecipe;
import icu.icuqalt10.panlingre.recipe.dztRecipe;
import icu.icuqalt10.panlingre.recipe.zftRecipe;
import icu.icuqalt10.panlingre.world.inventory.dztMenu;
import icu.icuqalt10.panlingre.world.inventory.ldlMenu;
import icu.icuqalt10.panlingre.world.inventory.zftMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder("panlingre")
@PrefixGameTestTemplate(false)
public class SourceReviewTest {
    @GameTest(template = "empty")
    public static void shiftCraftConsumesExactlyOneBatch(GameTestHelper helper) {
        for (int kind = 0; kind < 3; kind++) checkShiftCraft(helper, kind, 32, 4, 0);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shiftCraftPreservesPartialOutput(GameTestHelper helper) {
        for (int kind = 0; kind < 3; kind++) checkShiftCraft(helper, kind, 48, 4, 16);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fullInventoryDoesNotConsumeIngredients(GameTestHelper helper) {
        for (int kind = 0; kind < 3; kind++) checkShiftCraft(helper, kind, 64, 5, 0);
        helper.succeed();
    }

    private static void checkShiftCraft(GameTestHelper helper, int kind, int stored,
                                       int remaining, int dropped) {
        var manager = helper.getLevel().getRecipeManager();
        var originalRecipes = List.copyOf(manager.getRecipes());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
        SizedIngredient ingredient = new SizedIngredient(Ingredient.of(Items.DIRT), 1);
        ItemStack output = new ItemStack(Items.DIAMOND, 32);
        Recipe<?> recipe = switch (kind) {
            case 0 -> new LdlRecipe(List.of(new LdlRecipe.SlotIngredient(ingredient, 0)), output);
            case 1 -> new zftRecipe(List.of(new zftRecipe.SlotIngredient(ingredient, 0)), output);
            default -> new dztRecipe(List.of(new dztRecipe.SlotIngredient(ingredient, 0)), output);
        };
        List<ItemEntity> drops = new ArrayList<>();
        try {
            manager.replaceRecipes(List.of(new RecipeHolder<>(
                    ResourceLocation.fromNamespaceAndPath("panlingre", "review_crafting"), recipe)));
            for (int index = 0; index < 36; index++) {
                player.getInventory().setItem(index, new ItemStack(Items.STONE, 64));
            }
            player.getInventory().setItem(8, new ItemStack(Items.DIAMOND, stored));
            AbstractContainerMenu menu = switch (kind) {
                case 0 -> new ldlMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
                case 1 -> new zftMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
                default -> new dztMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
            };
            menu.getSlot(0).set(new ItemStack(Items.DIRT, 5));
            int resultSlot = kind == 0 ? 5 : kind == 1 ? 7 : 3;
            ItemStack moved = menu.quickMoveStack(player, resultSlot);
            drops.addAll(helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    player.getBoundingBox().inflate(3), entity -> entity.getItem().is(Items.DIAMOND)));
            helper.assertTrue(menu.getSlot(0).getItem().getCount() == remaining,
                    "Wrong ingredient consumption for menu " + kind);
            helper.assertTrue(player.getInventory().getItem(8).getCount() == 64,
                    "Wrong inventory output for menu " + kind);
            helper.assertTrue(drops.stream().mapToInt(entity -> entity.getItem().getCount()).sum() == dropped,
                    "Lost partial output for menu " + kind);
            helper.assertTrue(moved.isEmpty() == (stored == 64),
                    "Wrong shift-click result for menu " + kind);
        } finally {
            drops.forEach(ItemEntity::discard);
            manager.replaceRecipes(originalRecipes);
        }
    }

    @GameTest(template = "empty")
    public static void invalidRecipesCannotProduceFreeItems(GameTestHelper helper) {
        SizedIngredient ingredient = new SizedIngredient(Ingredient.of(Items.DIRT), 1);
        List<Recipe<RecipeInput>> invalid = List.of(
                new LdlRecipe(List.of(), new ItemStack(Items.DIAMOND)),
                new zftRecipe(List.of(), new ItemStack(Items.DIAMOND)),
                new dztRecipe(List.of(), new ItemStack(Items.DIAMOND)),
                new LdlRecipe(List.of(new LdlRecipe.SlotIngredient(ingredient, 8)), new ItemStack(Items.DIAMOND)),
                new zftRecipe(List.of(new zftRecipe.SlotIngredient(ingredient, 8)), new ItemStack(Items.DIAMOND)),
                new dztRecipe(List.of(new dztRecipe.SlotIngredient(ingredient, 8)), new ItemStack(Items.DIAMOND)),
                new LdlRecipe(List.of(new LdlRecipe.SlotIngredient(ingredient, 0),
                        new LdlRecipe.SlotIngredient(ingredient, 0)), new ItemStack(Items.DIAMOND)),
                new zftRecipe(List.of(new zftRecipe.SlotIngredient(ingredient, 0),
                        new zftRecipe.SlotIngredient(ingredient, 0)), new ItemStack(Items.DIAMOND)),
                new dztRecipe(List.of(new dztRecipe.SlotIngredient(ingredient, 0),
                        new dztRecipe.SlotIngredient(ingredient, 0)), new ItemStack(Items.DIAMOND))
        );
        for (int test = 0; test < invalid.size(); test++) {
            boolean duplicate = test >= 6;
            RecipeInput input = new RecipeInput() {
                @Override public ItemStack getItem(int index) {
                    return duplicate && index == 0 ? new ItemStack(Items.DIRT) : ItemStack.EMPTY;
                }
                @Override public int size() { return 7; }
            };
            Recipe<RecipeInput> recipe = invalid.get(test);
            helper.assertTrue(!recipe.matches(input, helper.getLevel()), "Invalid recipe matched: " + recipe);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blockPositionAppliesWithoutBlockEntity(GameTestHelper helper) {
        var fixed = new LookTipData.AxisCondition(Optional.of(1), Optional.empty(), Optional.empty());
        var condition = new LookTipData.EntityCondition("block", List.of("minecraft:stone"),
                Optional.empty(), Optional.empty(), Optional.of(new LookTipData.PosCondition(fixed, fixed, fixed)));
        helper.assertTrue(!LookTipMatcher.matchesBlock(Blocks.STONE.defaultBlockState(), BlockPos.ZERO, null, condition),
                "Server ignored block position when no block entity was present");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void trailsAreIsolatedByDimensionAndExpire(GameTestHelper helper) {
        var level = helper.getLevel();
        var nether = level.getServer().getLevel(Level.NETHER);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(42, 64, 42);
        FireTrailTracker.clear();
        try {
            FireTrailTracker.addTrail(level, pos, Blocks.STONE.defaultBlockState(), 2);
            pos.set(43, 64, 43);
            BlockPos original = new BlockPos(42, 64, 42);
            helper.assertTrue(FireTrailTracker.isPositionInTrail(level, original), "Mutable key moved the trail");
            helper.assertTrue(!FireTrailTracker.isPositionInTrail(nether, original), "Trail leaked into nether");
            FireTrailTracker.tick();
            helper.assertTrue(FireTrailTracker.getTrailRemainingTicks(level, original) == 1, "Wrong trail age");
            FireTrailTracker.tick();
            helper.assertTrue(!FireTrailTracker.isPositionInTrail(level, original), "Expired trail remained");
            helper.succeed();
        } finally {
            FireTrailTracker.clear();
        }
    }

    @GameTest(template = "empty")
    public static void tornadoDamageSurvivesSaveAndLoad(GameTestHelper helper) {
        var tornado = new FireTornadoEntity(ModEntities.FIRE_TORNADO.get(), helper.getLevel(),
                Vec3.ZERO, new Vec3(5, 0, 5), 60, 37.5F);
        CompoundTag saved = new CompoundTag();
        tornado.addAdditionalSaveData(saved);
        var loaded = new FireTornadoEntity(ModEntities.FIRE_TORNADO.get(), helper.getLevel());
        loaded.readAdditionalSaveData(saved);
        CompoundTag reSaved = new CompoundTag();
        loaded.addAdditionalSaveData(reSaved);
        helper.assertTrue(reSaved.getFloat("Damage") == 37.5F, "Tornado damage reset after save/load");
        saved.remove("Damage");
        var legacy = new FireTornadoEntity(ModEntities.FIRE_TORNADO.get(), helper.getLevel());
        legacy.readAdditionalSaveData(saved);
        CompoundTag legacySaved = new CompoundTag();
        legacy.addAdditionalSaveData(legacySaved);
        helper.assertTrue(legacySaved.getFloat("Damage") == 15.0F, "Legacy default damage changed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void multipartSearchFindsDistantRootsAndRejectsEmptyCorners(GameTestHelper helper) {
        Vec3 center = helper.absolutePos(new BlockPos(1, 2, 1)).getCenter();
        var root = new ReviewMultipart(helper.getLevel());
        root.setPos(center.add(500, 0, 0));
        root.onAddedToLevel();
        double diagonal = Math.sqrt(0.5);
        root.part.box = new OrientedBoundingBox(center,
                new Vec3(diagonal, 0, diagonal), new Vec3(0, 1, 0),
                new Vec3(-diagonal, 0, diagonal), new Vec3(4, 0.5, 0.25));
        root.part.setPos(center);
        try {
            AABB contact = new AABB(center, center).inflate(0.1);
            helper.assertTrue(MultipartEntity.collectTargets(helper.getLevel(), contact, null).contains(root),
                    "Root outside entity query was missed");
            Vec3 corner = center.add(2.6, 0, -2.6);
            AABB emptyCorner = new AABB(corner, corner).inflate(0.05);
            helper.assertTrue(root.part.getBoundingBox().minX < emptyCorner.minX,
                    "Test corner is outside broad-phase envelope");
            helper.assertTrue(!MultipartEntity.collectTargets(helper.getLevel(), emptyCorner, null).contains(root),
                    "Empty OBB envelope corner was treated as a hit");
            root.part.discard();
            helper.assertTrue(!MultipartEntity.collectTargets(helper.getLevel(), contact, null).contains(root),
                    "Removed part remained a target");
            helper.succeed();
        } finally {
            root.discard();
            root.onRemovedFromLevel();
        }
    }

    private static final class ReviewMultipart extends MultipartEntity {
        final ReviewPart part = new ReviewPart(this);

        ReviewMultipart(Level level) { super(EntityType.ZOMBIE, level); }
        @Override protected Entity[] multipartParts() { return new Entity[]{part}; }
    }

    @GameTest(template = "empty")
    public static void lookTipReloadUpdatesCompiledNbt(GameTestHelper helper) {
        Map<ResourceLocation, JsonElement> original = new HashMap<>();
        LookTipLoader.getLookTips().forEach((id, tip) ->
                original.put(id, LookTipData.CODEC.encodeStart(JsonOps.INSTANCE, tip).getOrThrow()));
        var loader = new ReviewLookTipLoader();
        var id = ResourceLocation.fromNamespaceAndPath("panlingre", "review_nbt");
        var pig = EntityType.PIG.create(helper.getLevel());
        pig.setNoAi(true);
        try {
            loader.reload(Map.of(id, JsonParser.parseString("""
                    {"title":"review", "entries":[{"type":"entity", "name":"minecraft:pig", "nbt":"{NoAI:1b}"}]}
                    """)), helper);
            var condition = LookTipLoader.getLookTips().get(id).entries().getFirst();
            helper.assertTrue(LookTipMatcher.matchesEntity(pig, condition), "Compiled NBT did not match");
            pig.setNoAi(false);
            helper.assertTrue(!LookTipMatcher.matchesEntity(pig, condition), "Entity NBT change was ignored");
            pig.addTag("review");
            loader.reload(Map.of(id, JsonParser.parseString("""
                    {"title":"review", "entries":[{"type":"entity", "name":"minecraft:pig", "nbt":"{Tags:[\\"review\\"]}"}]}
                    """)), helper);
            condition = LookTipLoader.getLookTips().get(id).entries().getFirst();
            helper.assertTrue(LookTipMatcher.matchesEntity(pig, condition), "Reload retained stale NBT");
            helper.assertTrue(LookTipLoader.getRequiredNbt("{NoAI:1b}") == null, "Old NBT remained after reload");
            helper.succeed();
        } finally {
            loader.reload(original, helper);
        }
    }

    @GameTest(template = "empty")
    public static void lookTipRequestsCannotLoadDistantChunks(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "source-review"), false);
        var player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        int[] responses = {0};
        player.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player, cookie) {
            @Override public void send(Packet<?> packet) { responses[0]++; }
        };
        player.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
        var context = (IPayloadContext) Proxy.newProxyInstance(IPayloadContext.class.getClassLoader(),
                new Class<?>[]{IPayloadContext.class}, (proxy, method, args) -> {
                    if (method.getName().equals("player")) return player;
                    throw new UnsupportedOperationException(method.getName());
                });
        BlockPos distant = player.blockPosition().offset(100000, 0, 100000);
        try {
            helper.assertTrue(!helper.getLevel().hasChunkAt(distant), "Test chunk was already loaded");
            LookTipNetworkHandler.handleRequest(LookTipRequestPayload.create(
                    LookTipRequestPayload.TargetType.BLOCK, new UUID(0, 0), distant), context);
            helper.assertTrue(!helper.getLevel().hasChunkAt(distant), "Look Tip request loaded a distant chunk");
            helper.assertTrue(responses[0] == 1, "Request did not receive an empty response");
            helper.assertTrue(new LookTipRequestPayload(-1, new UUID(0, 0), BlockPos.ZERO).getType()
                    == LookTipRequestPayload.TargetType.NONE, "Negative target type was accepted");
            helper.assertTrue(new LookTipRequestPayload(Integer.MAX_VALUE, new UUID(0, 0), BlockPos.ZERO).getType()
                    == LookTipRequestPayload.TargetType.NONE, "Out-of-range target type was accepted");
            helper.succeed();
        } finally {
            player.getTextFilter().leave();
            player.discard();
        }
    }

    private static final class ReviewLookTipLoader extends LookTipLoader {
        void reload(Map<ResourceLocation, JsonElement> tips, GameTestHelper helper) {
            apply(tips, helper.getLevel().getServer().getResourceManager(), InactiveProfiler.INSTANCE);
        }
    }

    private static final class ReviewPart extends PartEntity<ReviewMultipart>
            implements MultipartEntity.OrientedPart {
        OrientedBoundingBox box;

        ReviewPart(ReviewMultipart parent) { super(parent); }
        @Override public OrientedBoundingBox getOrientedBox() { return box; }
        @Override protected AABB makeBoundingBox() { return box == null ? super.makeBoundingBox() : new OrientedHitbox(box); }
        @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
        @Override protected void readAdditionalSaveData(CompoundTag tag) { }
        @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    }
}
