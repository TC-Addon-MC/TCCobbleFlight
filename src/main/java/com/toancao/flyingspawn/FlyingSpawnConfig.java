package com.toancao.flyingspawn;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Config(name = "flyingspawn")
public class FlyingSpawnConfig implements ConfigData {

    // ── Flight speed ──────────────────────────────────────────────
    public double flightSpeedMin = 0.05;
    public double flightSpeedMax = 0.50;

    // ── Preferred altitude ────────────────────────────────────────
    public double preferredHeightMin = 4.0;
    public double preferredHeightMax = 30.0;

    // ── Takeoff ───────────────────────────────────────────────────
    @ConfigEntry.Gui.CollapsibleObject
    public TakeoffConfig takeoff = new TakeoffConfig();

    public static class TakeoffConfig {
        public double chanceMin       = 0.05;
        public double chanceMax       = 0.18;
        public double bonusNearPlayer = 0.25;
        public double bonusOpenSpace  = 0.08;
        public double bonusIdleLong   = 0.10;
        public double velocityCap     = 0.20;
        public double accelFactor     = 0.6;
        public double hSpeedFactor    = 0.4;
        public double doneRatio       = 0.95;
    }

    // ── Landing ───────────────────────────────────────────────────
    @ConfigEntry.Gui.CollapsibleObject
    public LandingConfig landing = new LandingConfig();

    public static class LandingConfig {
        public double chanceMin        = 0.01;
        public double chanceMax        = 0.05;
        public double bonusNearGround  = 0.12;
        public double bonusFlyingLong  = 0.08;
        public double velocityFloor    = -0.15;
        public double hSpeedFactor     = 0.3;
        public double groundOffset     = 0.5;
    }

    // ── Environment & Spawn ───────────────────────────────────────
    public double playerAlertRadius       = 8.0;
    public int    openSpaceThreshold      = 50;
    public int    spawnSolidCountMax      = 20;
    public double baseGroundedChance      = 0.40;
    public double openGroundedChance      = 0.20;
    public double treeGroundedChance      = 0.05;
    public int    treeSpawnScanDepth      = 3;
    public int    spawnAreaRadius         = 2;
    public int    spawnAreaHeight         = 8;

    // ── State machine intervals ───────────────────────────────────
    public int transitionCheckInterval = 60;
    public int flyingLongThreshold     = 400;
    public int idleLongThreshold1      = 200;
    public int idleLongThreshold2      = 400;
    public int walkDirectionChange     = 60;
    public int idleDurationMin         = 40;
    public int idleDurationMax         = 120;
    public int logInterval             = 20;

    // ── Flying behavior ───────────────────────────────────────────
    public double openSpaceRadius         = 12.0;
    public double verticalSwayMin         = 0.008;
    public double verticalSwayRange       = 0.017;
    public int    directionChangeBase     = 80;
    public int    directionChangeRange    = 120;
    public double takeoffAccelBase        = 0.04;
    public double takeoffAccelRange       = 0.06;
    public double landingDecelBase        = 0.03;
    public double landingDecelRange       = 0.04;
    public double directionNudgeDegrees   = 30.0;
    public double heightCorrectionRate    = 0.04;
    public double heightCorrectionMax     = 0.30;
    public double heightCorrectionBase    = 0.03;
    public double heightCorrectionScale   = 0.15;
    public double hopVelocityY            = 0.25;

    public int    groundScanDepth         = 50;

    // ── Spawn observe (PERCHING delay) ────────────────────────────
    /** Số tick đứng yên sau spawn trong điều kiện bình thường (~3 giây = 60 tick) */
    public int    spawnObserveTicksNormal  = 60;
    /** Số tick tối thiểu nếu bị giật mình (agitation cao) (~1 giây = 20 tick) */
    public int    spawnObserveTicksMin     = 20;

    // ── Agitation system ──────────────────────────────────────────
    /** Agitation tăng mỗi tick khi bị hit */
    public double agitationHitIncrease        = 0.40;
    /** Agitation tăng mỗi tick khi đang trong battle */
    public double agitationBattleIncrease     = 0.05;
    /** Agitation tăng mỗi tick khi player trong alertRadius */
    public double agitationProximityIncrease  = 0.003;
    /** Agitation giảm mỗi tick (calm down) */
    public double agitationDecayPerTick       = 0.001;
    /** Ngưỡng agitation để coi là "bị giật mình" → rút ngắn perch time */
    public double agitationStartleThreshold   = 0.5;
    /** Agitation tối thiểu để được phép cất cánh (trừ khi đã idle lâu) */
    public double agitationMinToFly           = 0.05;
    /** Bonus takeoff chance theo agitation (nhân với agitation 0-1) */
    public double agitationTakeoffBonus       = 0.30;

    // ── Proximity fly gate ────────────────────────────────────────
    /** Bán kính tối đa để kích hoạt hành vi bay (chim chỉ bay khi player trong vòng này) */
    public double flyActivationRadius         = 40.0;

    // ── Legendary patrol ─────────────────────────────────────────
    /** Bán kính patrol tối đa của legendary (tính từ anchor spawn) */
    public double legendaryPatrolRadius       = 30.0;
    public int    initialFlightScanUp     = 30;
    public int    nearGroundBlockDist     = 4;

    // ── Multiplier Clamp & Weight ─────────────────────────────────
    public double speedMultMin    = 0.70;
    public double speedMultMax    = 1.30;
    public double staminaMultMin  = 0.70;
    public double staminaMultMax  = 1.50;
    public double weightMultMin   = 0.70;
    public double weightMultMax   = 1.20;
    public double heightMultMin   = 0.40;
    public double heightMultMax   = 1.40;
    public double groundMultMin   = 0.20;
    public double groundMultMax   = 2.50;

    public double weightNormKg    = 40.0;
    public double weightDivisor   = 200.0;

    // ── Lists & Maps (Type Specific) ──────────────────────────────
    public List<String> cannotFly = new ArrayList<>(List.of(
            "torchic", "combusken", "doduo", "dodrio", "emolga", "noibat", "flygon", "archeops", "rufflet"
    ));
    public List<String> forceFly = new ArrayList<>();

    public Map<String, Double> typeSpeed = new HashMap<>(Map.ofEntries(
            Map.entry("flying", 1.15), Map.entry("electric", 1.20), Map.entry("psychic", 1.10),
            Map.entry("ghost", 1.08), Map.entry("normal", 1.05), Map.entry("grass", 1.00),
            Map.entry("bug", 1.02), Map.entry("rock", 0.75), Map.entry("steel", 0.70),
            Map.entry("ground", 0.85), Map.entry("dragon", 0.90), Map.entry("water", 0.95),
            Map.entry("ice", 0.92), Map.entry("fire", 1.05), Map.entry("dark", 1.03),
            Map.entry("fairy", 1.07), Map.entry("poison", 0.98)
    ));

    public Map<String, Double> typeStamina = new HashMap<>(Map.ofEntries(
            Map.entry("dragon", 1.40), Map.entry("steel", 1.30), Map.entry("rock", 1.25),
            Map.entry("ground", 1.15), Map.entry("water", 1.10), Map.entry("flying", 1.05),
            Map.entry("electric", 0.95), Map.entry("psychic", 0.90), Map.entry("normal", 0.90),
            Map.entry("ghost", 0.85), Map.entry("fairy", 0.92)
    ));

    public Map<String, Double> typeGrounded = new HashMap<>(Map.ofEntries(
            Map.entry("bug", 1.50), Map.entry("grass", 1.50), Map.entry("rock", 1.50),
            Map.entry("ground", 1.50), Map.entry("steel", 1.50), Map.entry("flying", 0.50),
            Map.entry("dragon", 0.50), Map.entry("psychic", 0.50), Map.entry("ghost", 0.50)
    ));

    public Map<String, Double> typeHeight = new HashMap<>(Map.ofEntries(
            Map.entry("flying", 1.40), Map.entry("dragon", 1.40), Map.entry("psychic", 1.20),
            Map.entry("ghost", 1.20), Map.entry("fairy", 1.20), Map.entry("electric", 1.05),
            Map.entry("fire", 1.05), Map.entry("dark", 1.05), Map.entry("normal", 1.00),
            Map.entry("water", 1.00), Map.entry("grass", 0.75), Map.entry("bug", 0.75),
            Map.entry("ice", 0.75), Map.entry("poison", 0.75), Map.entry("rock", 0.55),
            Map.entry("steel", 0.55), Map.entry("ground", 0.55), Map.entry("fighting", 0.55)
    ));

    @Override
    public void validatePostLoad() {
        if (takeoff == null) takeoff = new TakeoffConfig();
        if (landing == null) landing = new LandingConfig();

        flightSpeedMin     = Math.max(0.01, flightSpeedMin);
        flightSpeedMax     = Math.max(flightSpeedMin, flightSpeedMax);
        preferredHeightMin = Math.max(1.0, preferredHeightMin);
        preferredHeightMax = Math.max(preferredHeightMin + 1.0, preferredHeightMax);

        transitionCheckInterval = Math.max(1, transitionCheckInterval);
        flyingLongThreshold     = Math.max(1, flyingLongThreshold);
        groundScanDepth         = Math.max(5, groundScanDepth);
        initialFlightScanUp     = Math.max(1, initialFlightScanUp);
        try {
            AutoConfig.getConfigHolder(FlyingSpawnConfig.class).save();
        } catch (Exception ignored) {}
    }

    public static void register() {
        AutoConfig.register(FlyingSpawnConfig.class, GsonConfigSerializer::new);
        AutoConfig.getConfigHolder(FlyingSpawnConfig.class).save();
    }

    public static FlyingSpawnConfig get() {
        try {
            return AutoConfig.getConfigHolder(FlyingSpawnConfig.class).getConfig();
        } catch (Exception e) {
            return new FlyingSpawnConfig();
        }
    }
}