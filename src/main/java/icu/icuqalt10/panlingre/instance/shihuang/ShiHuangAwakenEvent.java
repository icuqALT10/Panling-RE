package icu.icuqalt10.panlingre.instance.shihuang;

import icu.icuqalt10.panlingre.instance.InstanceSession;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;

/** Server-side entrance hooks. A future emperor listener spawns and binds its entity here. */
public final class ShiHuangAwakenEvent extends Event {
    public enum Phase { REVEAL, COMBAT_READY }
    public final InstanceSession session;
    public final Phase phase;
    public final boolean dragonEnhanced;
    public final Vec3 coffinPosition, combatPosition;
    public ShiHuangAwakenEvent(InstanceSession session, Phase phase, boolean dragonEnhanced, Vec3 coffinPosition, Vec3 combatPosition) {
        this.session=session; this.phase=phase; this.dragonEnhanced=dragonEnhanced;
        this.coffinPosition=coffinPosition; this.combatPosition=combatPosition;
    }
}
