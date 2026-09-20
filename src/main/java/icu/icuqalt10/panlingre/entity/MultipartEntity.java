package icu.icuqalt10.panlingre.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;

/** Base class for bosses with parent-owned interaction parts. */
public abstract class MultipartEntity extends Monster {
    private static final Set<MultipartEntity> LIVE_ROOTS =
            Collections.newSetFromMap(new WeakHashMap<>());

    protected MultipartEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        LIVE_ROOTS.add(this);
    }

    public Entity getMultipartRoot() { return this; }

    /**
     * Damage arriving at the logical root is routed through the same part
     * selection used by area effects and projectiles.  This is important for
     * effects which hit the root directly (rather than the visible part).
     */
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        int part = selectPartIndex(source.getDirectEntity() == null
                ? position() : source.getDirectEntity().position());
        return hurtSelectedPart(part, source, amount);
    }

    /** Multipart roots must remain valid targets even when their logical box is tiny. */
    @Override
    public boolean isAttackable() { return true; }

    /** Override in a multipart boss to expose its hitbox list. */
    protected Entity[] multipartParts() { return new Entity[0]; }

    /** Select by distance first, then by damage multiplier (higher wins). */
    protected int selectPartIndex(Vec3 point) {
        Entity[] parts = multipartParts();
        double bestDistance = Double.POSITIVE_INFINITY;
        float bestMultiplier = -Float.MAX_VALUE;
        int best = -1;
        for (int i = 0; i < parts.length; i++) {
            Entity part = parts[i];
            if (part == null || part.isRemoved()) continue;
            double distance = part instanceof OrientedPart oriented
                    ? oriented.getOrientedBox().distanceToSqr(point)
                    : part.getBoundingBox().distanceToSqr(point);
            float multiplier = damageMultiplierForPart(i);
            if (distance < bestDistance - 1.0E-7D
                    || (Math.abs(distance - bestDistance) <= 1.0E-7D && multiplier > bestMultiplier)) {
                bestDistance = distance;
                bestMultiplier = multiplier;
                best = i;
            }
        }
        return best;
    }

    protected float damageMultiplierForPart(int partIndex) { return 1.0F; }

    protected boolean hurtSelectedPart(int partIndex, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return super.hurt(source, amount * damageMultiplierForPart(partIndex));
    }

    public static MultipartEntity rootOf(Entity entity) {
        if (entity instanceof MultipartPart part && part.getMultipartRoot() instanceof MultipartEntity root) return root;
        return entity instanceof MultipartEntity root ? root : null;
    }

    public interface MultipartPart {
        Entity getMultipartRoot();
        int getPartIndex();
    }

    public interface OrientedPart {
        OrientedBoundingBox getOrientedBox();
    }

    public static List<LivingEntity> collectTargets(Level level, AABB area, LivingEntity source) {
        Map<UUID, LivingEntity> result = new LinkedHashMap<>();
        // The logical root deliberately has a tiny box. Query a generous
        // neighbourhood first, then keep only roots whose real part boxes
        // intersect the requested area. This avoids making the root itself a
        // giant collision box while still allowing skill area scans to find it.
        AABB search = area.inflate(128.0D);
        for (Entity e : level.getEntities(source, search)) {
            Entity root = e instanceof MultipartPart p ? p.getMultipartRoot() : e;
            if (!(root instanceof LivingEntity living) || living == source || !living.isAlive()) continue;
            if (root instanceof MultipartEntity multipart) {
                if (!multipart.intersectsMultipartArea(area)) continue;
            } else if (!e.getBoundingBox().intersects(area)) {
                // The enlarged query is only an index lookup. Ordinary
                // entities must still obey the caller's exact area.
                continue;
            }
            result.putIfAbsent(living.getUUID(), living);
        }
        // A root can be outside the normal entity query because its logical
        // box is intentionally tiny; explicitly inspect nearby multipart roots.
        // Native parts are not LivingEntity instances, so typed area queries need
        // to resolve their OBBs to the root explicitly. Ignore roots not yet spawned.
        for (MultipartEntity multipart : LIVE_ROOTS) {
            if (multipart.level() == level && multipart.isAddedToLevel() && multipart != source && multipart.isAlive()
                    && multipart.intersectsMultipartArea(area))
                result.putIfAbsent(multipart.getUUID(), multipart);
        }
        return new ArrayList<>(result.values());
    }

    protected boolean intersectsMultipartArea(AABB area) {
        for (Entity part : multipartParts()) {
            if (part != null && (part instanceof OrientedPart oriented
                    ? oriented.getOrientedBox().intersects(area)
                    : part.getBoundingBox().intersects(area))) return true;
        }
        return multipartParts().length == 0 && getBoundingBox().intersects(area);
    }
}
