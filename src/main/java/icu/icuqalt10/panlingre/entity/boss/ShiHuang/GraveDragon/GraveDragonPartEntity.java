package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

import icu.icuqalt10.panlingre.entity.MultipartEntity;
import icu.icuqalt10.panlingre.entity.OrientedBoundingBox;
import icu.icuqalt10.panlingre.entity.OrientedHitbox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

/**
 * A parent-owned interaction handle, with no independent health, tick or spawn packet.
 *
 * <p>The entity only exists so vanilla can report "the player is looking at this
 * dragon" and so projectiles have something to collide with. Melee hit resolution is
 * deliberately decided on the server by {@link GraveDragonEntity#pickPartAlongViewRay},
 * not by whichever part the client happened to name.
 */
public final class GraveDragonPartEntity extends PartEntity<GraveDragonEntity>
        implements MultipartEntity.MultipartPart, MultipartEntity.OrientedPart {
    private final int partIndex;
    private OrientedBoundingBox orientedBox;

    public GraveDragonPartEntity(GraveDragonEntity parent, int index) {
        super(parent);
        partIndex = index;
        noPhysics = true;
    }

    @Override public Entity getMultipartRoot() { return getParent(); }
    @Override public int getPartIndex() { return partIndex; }
    @Override public OrientedBoundingBox getOrientedBox() { return orientedBox; }

    /**
     * Extra interaction range granted to boss parts, in blocks.
     *
     * <p>Melee reach is validated against the server's copy of the player's position, and a
     * moving player is always a little stale there, so clicks land just past the vanilla
     * limit. Measured rejections were 0.17 to 1.6 blocks over, so a small, part-only margin
     * removes the silent discards without changing how anything else in the world behaves.
     */
    public static final double REACH_MARGIN = 2.0;

    /** Whether an interaction box belongs to one of our parts, for the reach mixin. */
    public static boolean isPartHitbox(AABB box) {
        return box instanceof OrientedHitbox;
    }

    public void setOrientedBox(OrientedBoundingBox box) {
        xo = xOld = getX(); yo = yOld = getY(); zo = zOld = getZ();
        orientedBox = box;
        setPos(box.center);
    }

    @Override protected AABB makeBoundingBox() {
        return orientedBox == null ? super.makeBoundingBox() : new OrientedHitbox(orientedBox);
    }

    @Override public boolean isPickable() { return orientedBox != null && getParent().isAlive(); }
    @Override public boolean isAlive() { return getParent().isAlive(); }
    @Override public boolean is(Entity entity) { return this == entity || getParent() == entity; }
    @Override public boolean shouldBeSaved() { return false; }
    @Override public Component getDisplayName() { return getParent().getDisplayName(); }
    @Override public ItemStack getPickResult() { return getParent().getPickResult(); }

    @Override public boolean hurt(DamageSource source, float amount) {
        if (!isPickable() || isInvulnerableTo(source)) return false;

        if (source.getDirectEntity() instanceof Player player) {
            // Melee: one call into the dragon's resolver, one reach gate, one verdict. This
            // method contributes no policy of its own. Damage reporting happens centrally in
            // the dragon's hurtSelectedPart, so every attack type is covered by one path.
            if (GraveDragonDamageDebug.enabled()) {
                GraveDragonDamageDebug.log("part.hurt named=" + partIndex
                        + " (" + GraveDragonEntity.PART_LABELS[partIndex] + ")"
                        + " local=" + (player.level().isClientSide ? "client" : "server")
                        + " side=" + (level().isClientSide ? "client" : "server"));
            }
            int struck = getParent().resolveMeleeStrike(player, partIndex);
            if (struck < 0) return false;
            return getParent().hurtPart(struck, source, amount);
        }

        if (source.getDirectEntity() instanceof Projectile projectile) {
            Vec3 start = projectile.position(), delta = projectile.getDeltaMovement();
            // Arrow/snowball utility uses a 0.3-block collision margin. Check both
            // sides of the current position because projectile implementations
            // differ in whether they move before invoking onHit.
            if (orientedBox.inflate(0.3, 0.3, 0.3).clip(start.subtract(delta), start.add(delta)).isEmpty()) return false;
        }
        return getParent().hurtPart(partIndex, source, amount);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
}
