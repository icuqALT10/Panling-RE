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

    /**
     * Every oriented part on the client within {@code radius} of {@code origin}.
     *
     * <p>Client code has no {@code getEntityOrPart}: that helper exists on {@code ServerLevel}
     * only, and {@code ClientLevel#getEntity(int)} has no part fallback. Parts are reached
     * through the world's part-entity collection, which is what this walks.
     */
    public static List<OrientedPart> orientedPartsNear(Level level, Vec3 origin, double radius) {
        List<OrientedPart> found = new ArrayList<>();
        if (level == null) return found;
        double limit = radius * radius;
        for (var part : level.getPartEntities()) {
            if (!(part instanceof OrientedPart oriented)) continue;
            var box = oriented.getOrientedBox();
            if (box == null) continue;
            if (box.distanceToSqr(origin) <= limit) found.add(oriented);
        }
        return found;
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

    /**
     * World-space points that represent where this root's body actually sits.
     *
     * <p>A multipart root's logical {@link #position()} is a bookkeeping anchor;
     * the body can extend tens of blocks away from it (and need not contain it at
     * all). Region tests that sample a single point therefore have to sample the
     * part hitboxes instead, or a boss whose only wing or tail segment is inside
     * the region is silently dropped.
     *
     * <p>Returns just the logical position for a root exposing no parts, so
     * callers can use this unconditionally.
     */
    public List<Vec3> multipartBodySamples() {
        Entity[] parts = multipartParts();
        if (parts.length == 0) return List.of(position());

        List<Vec3> samples = new ArrayList<>(parts.length * 9 + 1);
        samples.add(position());
        for (Entity part : parts) {
            if (part == null || part.isRemoved()) continue;
            AABB box = part.getBoundingBox();
            if (box == null) continue;
            // A single centre is a poor proxy for a long thin part such as a wing
            // or a tail segment, so every corner is sampled as well. For an
            // oriented part these are its envelope corners, which errs towards
            // "reachable" — the right direction for a hit test.
            samples.add(box.getCenter());
            samples.add(new Vec3(box.minX, box.minY, box.minZ));
            samples.add(new Vec3(box.maxX, box.minY, box.minZ));
            samples.add(new Vec3(box.minX, box.maxY, box.minZ));
            samples.add(new Vec3(box.minX, box.minY, box.maxZ));
            samples.add(new Vec3(box.maxX, box.maxY, box.minZ));
            samples.add(new Vec3(box.maxX, box.minY, box.maxZ));
            samples.add(new Vec3(box.minX, box.maxY, box.maxZ));
            samples.add(new Vec3(box.maxX, box.maxY, box.maxZ));
        }
        return samples;
    }
}
