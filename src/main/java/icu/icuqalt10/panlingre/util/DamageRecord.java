package icu.icuqalt10.panlingre.util;

public class DamageRecord {
    public int invulnerableTime;
    public float lastHurt;

    public DamageRecord(int invulnerableTime, float lastHurt) {
        this.invulnerableTime = invulnerableTime;
        this.lastHurt = lastHurt;
    }
}