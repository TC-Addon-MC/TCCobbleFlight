package com.toancao.flyingspawn;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import java.util.Random;

/** Hồ sơ cấu hình riêng cho mỗi entity pokemon. */
public class PokemonFlightProfile {

    public int fallDamageImmunityTicks = 0;

    // Agitation: 0.0 = calm, 1.0 = fully agitated. Tăng khi player lại gần / bị hit.
    public double agitation = 0.0;

    // Spawn observe: đứng yên quan sát sau khi spawn
    public int spawnObserveTicks = 0;

    // Legendary anchor — neo vị trí spawn, chỉ patrol trong radius
    public boolean isLegendary = false;
    public double anchorX = 0, anchorY = 0, anchorZ = 0;

    public final double flightSpeed;
    public double preferredHeight;
    public final double baseTakeoffChance;
    public final double baseLandingChance;
    public final double verticalSway;
    public final int    directionChangeInterval;
    public final double takeoffAcceleration;
    public final double landingDeceleration;

    public double currentYaw;
    public int    ticksInCurrentState;
    public int    idleTicks;
    public double verticalVelocity;

    public final double heightMultiplier;
    public double speedBonus = 0.25;
    public PokemonFlightProfile(PokemonEntity pokemon) {
        long seed = pokemon.getUUID().getLeastSignificantBits() ^ pokemon.getUUID().getMostSignificantBits();
        Random r = new Random(seed);
        FlyingSpawnConfig cfg = FlyingSpawnConfig.get();

        this.heightMultiplier = computeTypeHeightMultiplier(pokemon, cfg);
        this.flightSpeed = cfg.flightSpeedMin + r.nextDouble() * (cfg.flightSpeedMax - cfg.flightSpeedMin);

        double baseHeight = cfg.preferredHeightMin + r.nextDouble() * (cfg.preferredHeightMax - cfg.preferredHeightMin);
        this.preferredHeight = baseHeight * this.heightMultiplier;

        this.baseTakeoffChance = cfg.takeoff.chanceMin + r.nextDouble() * (cfg.takeoff.chanceMax - cfg.takeoff.chanceMin);
        this.baseLandingChance = cfg.landing.chanceMin + r.nextDouble() * (cfg.landing.chanceMax - cfg.landing.chanceMin);

        this.verticalSway = cfg.verticalSwayMin + r.nextDouble() * cfg.verticalSwayRange;
        this.directionChangeInterval = cfg.directionChangeBase + r.nextInt(cfg.directionChangeRange);
        this.takeoffAcceleration = cfg.takeoffAccelBase + r.nextDouble() * cfg.takeoffAccelRange;
        this.landingDeceleration = cfg.landingDecelBase + r.nextDouble() * cfg.landingDecelRange;

        this.currentYaw = r.nextDouble() * 360.0;
        this.ticksInCurrentState = 0;
        this.idleTicks = 0;
        this.verticalVelocity = 0.0;
    }

    public double computedTakeoffChance(boolean nearPlayer, boolean openSpace) {
        double chance = baseTakeoffChance;
        FlyingSpawnConfig cfg = FlyingSpawnConfig.get();
        if (nearPlayer) chance += cfg.takeoff.bonusNearPlayer;
        if (openSpace)  chance += cfg.takeoff.bonusOpenSpace;
        if (idleTicks > cfg.idleLongThreshold1) chance += cfg.takeoff.bonusIdleLong;
        if (idleTicks > cfg.idleLongThreshold2) chance += cfg.takeoff.bonusIdleLong;
        // Agitation mạnh mẽ thúc đẩy cất cánh
        chance += agitation * cfg.agitationTakeoffBonus;
        return Math.min(chance, 0.90);
    }

    public double computedLandingChance(boolean nearGround, boolean flyingLong) {
        double chance = baseLandingChance;
        FlyingSpawnConfig cfg = FlyingSpawnConfig.get();
        if (nearGround) chance += cfg.landing.bonusNearGround;
        if (flyingLong) chance += cfg.landing.bonusFlyingLong;
        return Math.min(chance, 0.80);
    }

    public void refreshPreferredHeight() {
        FlyingSpawnConfig cfg = FlyingSpawnConfig.get();
        Random r = new Random((long)(currentYaw * 1000) ^ ticksInCurrentState);
        this.preferredHeight = cfg.preferredHeightMin + r.nextDouble() * (cfg.preferredHeightMax - cfg.preferredHeightMin);
    }

    private double computeTypeHeightMultiplier(PokemonEntity pokemon, FlyingSpawnConfig cfg) {
        var types = pokemon.getPokemon().getTypes();
        double multiplier = 1.0;
        int i = 0;

        for (var type : types) {
            String name = type.getName().toLowerCase();
            double weight = (i == 0) ? 1.0 : 0.5;
            double value = cfg.typeHeight.getOrDefault(name, 1.0);
            multiplier *= (1.0 + (value - 1.0) * weight);
            i++;
        }
        return Math.max(cfg.heightMultMin, Math.min(cfg.heightMultMax, multiplier));
    }
}