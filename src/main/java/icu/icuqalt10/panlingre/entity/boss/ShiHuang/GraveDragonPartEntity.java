package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

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
            // Melee: the client tells us which dragon it aimed at; the server decides which
            // part was actually struck. Re-casting the view ray against the real oriented
            // boxes fixes the wrong-part selections the client makes through AABB envelopes
            // (large parts steal small ones at their empty corners). The client's named part
            // stays as a fallback: reach was already validated by the attack packet, and
            // rejecting a legitimate click because the ray grazed a gap would be worse than
            // accepting the part the player was visibly aiming at. Either way the struck
            // part must itself be within range, so this cannot extend melee reach.
            int struck = getParent().pickPartAlongViewRay(player);
            if (struck < 0 || !getParent().canPlayerReachPart(player, struck)) struck = partIndex;
            if (!getParent().canPlayerReachPart(player, struck)) return false;
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
