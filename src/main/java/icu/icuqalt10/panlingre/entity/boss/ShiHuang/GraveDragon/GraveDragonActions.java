package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

import java.util.List;
import java.util.Map;

/** Server combat timing in seconds. Animation lengths come from the exported resource. */
public final class GraveDragonActions {
    public record Window(double from, double to, String part, float damage, int repeatTicks) {
        boolean active(double previous, double now) { return previous < to && now >= from; }
    }

    public record Action(String name, int recovery,
                  List<Window> windows, double... events) {
        double duration() { return GraveDragonPose.duration(name); }
    }

    private static Window hit(double from, double to, String part, float damage) {
        return new Window(from, to, part, damage, 0);
    }
    private static Window repeat(double from, double to, String part, float damage, int ticks) {
        return new Window(from, to, part, damage, ticks);
    }
    private static Action action(String name, int recovery,
                                 List<Window> windows, double... events) {
        return new Action(name, recovery, windows, events);
    }

    static final Map<String, Action> ALL = Map.ofEntries(
            Map.entry("ground_double_bite", action("ground_double_bite", 20,
                    List.of(repeat(1.2, 1.35, "bite", 20, 1), repeat(2, 2.15, "bite", 20, 1)), .8, 1.2, 2)),
            Map.entry("ground_tail_stab", action("ground_tail_stab", 30,
                    List.of(hit(1.5, 1.55, "tail_slam", 30)), .75, 1.5)),
            Map.entry("ground_rift_fan", action("ground_rift_fan", 30,
                    List.of(hit(2.1, 2.15, "rift", 20)), 1.25, 2.1)),
            Map.entry("ground_flank_press_l", action("ground_flank_press_l", 30,
                    List.of(hit(2.15, 2.2, "flank_l", 5)), 1.2, 2.15)),
            Map.entry("ground_flank_press_r", action("ground_flank_press_r", 30,
                    List.of(hit(2.15, 2.2, "flank_r", 5)), 1.2, 2.15)),
            Map.entry("ground_fan_breath", action("ground_fan_breath", 40,
                    List.of(repeat(1.75, 5.25, "fan_breath", 3, 1)), 1.1, 1.75, 5.25)),
            Map.entry("air_tail_pierce", action("air_tail_pierce", 30,
                    List.of(repeat(1.85, 2.15, "tail_tip", 35, 1), repeat(3.35, 3.65, "tail_tip", 35, 1)), 1.25, 1.85, 2.75, 3.35)),
            Map.entry("air_open_fire_ring", action("air_open_fire_ring", 30,
                    List.of(repeat(1.75, 3.75, "fire_ring", 5, 1)), 1.1, 1.5, 1.75, 3.75)),
            Map.entry("air_thunder_trail", action("air_thunder_trail", 40,
                    List.of(hit(2, 2.05, "thunder_trail", 30), hit(5.2, 5.25, "thunder_trail", 30),
                            hit(8.4, 8.45, "thunder_trail", 30)), 1, 2, 4.2, 5.2, 7.4, 8.4)),
            Map.entry("takeoff", action("takeoff", 0, List.of())),
            Map.entry("air_dive_slam", action("air_dive_slam", 40,
                    List.of(hit(3.9, 3.95, "landing", 50)), 2.5, 3.5, 3.9)),
            Map.entry("air_coil_lightning", action("air_coil_lightning", 40,
                    List.of(hit(3.2, 3.25, "lightning", 30)), 1.8, 2.6, 3.2)),
            Map.entry("air_fireball3", action("air_fireball3", 30,
                    List.of(hit(.7, .9, "fireball", 20), hit(2.2, 2.4, "fireball", 20),
                            hit(3.7, 3.9, "fireball", 20)), .5, .7, 2.0, 2.2, 3.5, 3.7)),
            Map.entry("phase50_firefield", action("phase50_firefield", 40,
                    List.of(repeat(3.7, 13.7, "firefield", 3, 1)), 3.7, 13.7)),
            Map.entry("roar_air", action("roar_air", 32,
                    List.of(hit(.9, 2.9, "roar", 0)), .9, 2.9)),
            Map.entry("dying_thrash", action("dying_thrash", 0,
                    List.of(), 1.4, 7.1)),
            Map.entry("ground_charge", action("ground_charge", 30,
                    List.of(repeat(2, 6.5, "charge", 15, 1)), 2)),
            Map.entry("ground_charge_wall", action("ground_charge_wall", 0,
                    List.of(), 0)),
            Map.entry("ground_charge_nowall", action("ground_charge_nowall", 0,
                    List.of())),
            Map.entry("ground_claw_front_l", action("ground_claw_front_l", 20,
                    List.of(repeat(1, 1.15, "front_l", 10, 1)), 1, 1.15)),
            Map.entry("ground_claw_front_r", action("ground_claw_front_r", 20,
                    List.of(repeat(1, 1.15, "front_r", 10, 1)), 1, 1.15)),
            Map.entry("ground_claw_back_l", action("ground_claw_back_l", 20,
                    List.of(repeat(1, 1.15, "hind_l", 8, 1)), 1, 1.15)),
            Map.entry("ground_claw_back_r", action("ground_claw_back_r", 20,
                    List.of(repeat(1, 1.15, "hind_r", 8, 1)), 1, 1.15)),
            Map.entry("ground_turn_sweep", action("ground_turn_sweep", 30,
                    List.of(repeat(.5, 1.25, "breath", 10, 1)), .5, 1.25)),
            Map.entry("ground_turn_breath", action("ground_turn_breath", 40,
                    List.of(repeat(1, 5, "breath", 5, 1)), 1, 5)),
            Map.entry("ground_coil", action("ground_coil", 40,
                    List.of(repeat(3, 9, "coil", 30, 1)), 3, 9)),
            Map.entry("ground_tailroll", action("ground_tailroll", 30,
                    List.of(repeat(.65, .85, "tail", 15, 1), hit(1.9, 1.95, "landing", 30)), .65, .85, 1.4, 1.9)),
            Map.entry("ground_firezone", action("ground_firezone", 40,
                    List.of(hit(.7, .9, "firezone", 15), hit(2.2, 2.4, "firezone", 15),
                            hit(3.7, 3.9, "firezone", 15), hit(5.2, 5.4, "firezone", 15),
                            hit(6.7, 6.9, "firezone", 15)), .7, 2.2, 3.7, 5.2, 6.7)),
            Map.entry("ground_slam", action("ground_slam", 40,
                    List.of(hit(2.05, 2.1, "landing", 30)), 1, 1.75, 2.05)),
            Map.entry("ground_tailsweep", action("ground_tailsweep", 30,
                    List.of(repeat(1.1, 1.5, "tail", 15, 1)), 1.1, 1.5)),
            Map.entry("ground_sweep_breath", action("ground_sweep_breath", 40,
                    List.of(repeat(2.5, 6.5, "breath", 5, 1)), 1.5, 2.5, 6.5)),
            Map.entry("roar", action("roar", 32,
                    List.of(hit(.9, 2.9, "roar", 0)), .9, 2.9))
    );

    static boolean damagingPart(String group, int index) {
        return switch (group) {
            case "front_l" -> index >= 22 && index <= 34;
            case "front_r" -> index >= 35 && index <= 47;
            case "hind_l" -> index >= 48 && index <= 60;
            case "hind_r" -> index >= 61 && index <= 73;
            case "tail" -> index >= 13 && index <= 20;
            case "bite" -> index == 11 || index == 12 || index == 78;
            case "tail_tip" -> index == 19 || index == 20;
            case "coil", "charge", "all" -> true;
            default -> false;
        };
    }

    static boolean crossed(double previous, double now, double event) {
        return previous < event && now >= event;
    }

    public static Action get(String name) { return ALL.get(name); }
    public static double event(String name, int index) { return ALL.get(name).events()[index]; }

    private GraveDragonActions() { }
}
