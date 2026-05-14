package com.toancao.flyingspawn;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/** Xử lý di chuyển, vận tốc dựa trên type và thông số cân nặng */
public class FlyingBehavior {

    private static final Random RNG = new Random();
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("FlyingSpawn");

    private static double getTypeSpeedMultiplier(PokemonEntity pokemon, FlyingSpawnConfig cfg) {
        var types = pokemon.getPokemon().getTypes();
        double multiplier = 1.0;
        int i = 0;

        for (var type : types) {
            String name = type.getName().toLowerCase();
            double weight = (i == 0) ? 1.0 : 0.5;
            double value = cfg.typeSpeed.getOrDefault(name, 1.0);
            multiplier *= (1.0 + (value - 1.0) * weight);
            i++;
        }
        return Math.max(cfg.speedMultMin, Math.min(cfg.speedMultMax, multiplier));
    }

    private static double getTypeStaminaMultiplier(PokemonEntity pokemon, FlyingSpawnConfig cfg) {
        var types = pokemon.getPokemon().getTypes();
        double multiplier = 1.0;

        for (var type : types) {
            String name = type.getName().toLowerCase();
            multiplier *= cfg.typeStamina.getOrDefault(name, 1.0);
        }
        return Math.max(cfg.staminaMultMin, Math.min(cfg.staminaMultMax, multiplier));
    }

    private static double getWeightSpeedMultiplier(PokemonEntity pokemon, FlyingSpawnConfig cfg) {
        float weightKg = pokemon.getPokemon().getForm().getWeight();
        double multiplier = 1.0 - (weightKg - cfg.weightNormKg) / cfg.weightDivisor;
        return Math.max(cfg.weightMultMin, Math.min(cfg.weightMultMax, multiplier));
    }

    public static void tick(PokemonEntity pokemon, PokemonFlightProfile profile, int tick) {
        pokemon.setNoGravity(true);
        disableAI(pokemon);
        FlyingSpawnConfig cfg = FlyingSpawnConfig.get();

        if (tick % profile.directionChangeInterval == 0) {
            applyDirectionChange(profile, cfg);
        }

        double sinSway = Math.sin(tick * 0.02 + pokemon.getId() * 0.3) * profile.verticalSway;
        double heightCorrection = computeHeightCorrection(pokemon, profile, cfg);

        double radians = Math.toRadians(profile.currentYaw);
        double speedMult = getTypeSpeedMultiplier(pokemon, cfg) * getWeightSpeedMultiplier(pokemon, cfg);
        double hX = -Math.sin(radians) * (profile.flightSpeed + profile.speedBonus) * speedMult;
        double hZ =  Math.cos(radians) * (profile.flightSpeed + profile.speedBonus) * speedMult;
        Vec3 velocity = new Vec3(hX, sinSway + heightCorrection, hZ);
        pokemon.setDeltaMovement(velocity);
        syncRotationFromVelocity(pokemon, velocity);
    }

    public static boolean tickTakingOff(PokemonEntity pokemon, PokemonFlightProfile profile, int tick) {
        pokemon.setNoGravity(true);
        disableAI(pokemon);
        FlyingSpawnConfig cfg = FlyingSpawnConfig.get();

        if (tick % cfg.logInterval == 0) {
            double currentAltitude = pokemon.getY() - estimateGroundY(pokemon, cfg);
         }

        double weightMult = getWeightSpeedMultiplier(pokemon, cfg);
        double accel = profile.takeoffAcceleration * weightMult * cfg.takeoff.accelFactor;

        profile.verticalVelocity = Math.min(profile.verticalVelocity + accel, cfg.takeoff.velocityCap);

        double radians = Math.toRadians(profile.currentYaw);
        double speedMult = getTypeSpeedMultiplier(pokemon, cfg) * weightMult;
        double hSpeed = profile.flightSpeed * cfg.takeoff.hSpeedFactor * speedMult;

        Vec3 velocity = new Vec3(-Math.sin(radians) * hSpeed, profile.verticalVelocity, Math.cos(radians) * hSpeed);
        pokemon.setDeltaMovement(velocity);
        syncRotationFromVelocity(pokemon, velocity);

        return (pokemon.getY() - estimateGroundY(pokemon, cfg)) >= profile.preferredHeight * cfg.takeoff.doneRatio;
    }

    public static boolean tickLanding(PokemonEntity pokemon, PokemonFlightProfile profile, int tick) {
        pokemon.setNoGravity(true);
        disableAI(pokemon);
        FlyingSpawnConfig cfg = FlyingSpawnConfig.get();

        profile.verticalVelocity = Math.max(profile.verticalVelocity - profile.landingDeceleration, cfg.landing.velocityFloor);

        double radians = Math.toRadians(profile.currentYaw);
        double speedMult = getTypeSpeedMultiplier(pokemon, cfg) * getWeightSpeedMultiplier(pokemon, cfg);
        double hSpeed = profile.flightSpeed * cfg.landing.hSpeedFactor * speedMult;

        Vec3 velocity = new Vec3(-Math.sin(radians) * hSpeed, profile.verticalVelocity, Math.cos(radians) * hSpeed);
        pokemon.setDeltaMovement(velocity);
        syncRotationFromVelocity(pokemon, velocity);

        if (pokemon.onGround() || pokemon.getY() <= estimateGroundY(pokemon, cfg) + cfg.landing.groundOffset) {
            pokemon.setNoGravity(false);
            pokemon.setDeltaMovement(Vec3.ZERO);
            profile.verticalVelocity = 0.0;
            restoreAI(pokemon);
            return true;
        }
        return false;
    }

    private static void applyDirectionChange(PokemonFlightProfile profile, FlyingSpawnConfig cfg) {
        double nudge = (RNG.nextDouble() - 0.5) * cfg.directionNudgeDegrees;
        profile.currentYaw = (profile.currentYaw + nudge) % 360.0;
        if (profile.currentYaw < 0) profile.currentYaw += 360.0;
    }

    private static double computeHeightCorrection(PokemonEntity pokemon, PokemonFlightProfile profile, FlyingSpawnConfig cfg) {
        double diff = profile.preferredHeight - (pokemon.getY() - estimateGroundY(pokemon, cfg));
        double maxCorrection = Math.min(cfg.heightCorrectionMax, Math.abs(diff) * cfg.heightCorrectionScale + cfg.heightCorrectionBase);
        return Math.max(-maxCorrection, Math.min(maxCorrection, diff * cfg.heightCorrectionRate));
    }

    private static double estimateGroundY(PokemonEntity pokemon, FlyingSpawnConfig cfg) {
        if (pokemon.level() == null) return pokemon.getY() - 10.0;
        double checkY = pokemon.getY();
        for (int i = 0; i < cfg.groundScanDepth; i++) {
            checkY -= 1.0;
            var pos = new net.minecraft.core.BlockPos((int) pokemon.getX(), (int) checkY, (int) pokemon.getZ());
            if (!pokemon.level().getBlockState(pos).isAir()) return checkY + 1.0;
        }
        return pokemon.getY() - 10.0;
    }

    private static void syncRotationFromVelocity(PokemonEntity pokemon, Vec3 velocity) {
        if (Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z) < 0.001) return;
        float yaw = (float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z));
        pokemon.setYRot(yaw); pokemon.setYHeadRot(yaw);
        pokemon.yBodyRot = yaw; pokemon.yBodyRotO = yaw;
        pokemon.yHeadRot = yaw; pokemon.yHeadRotO = yaw;
    }

    private static void disableAI(PokemonEntity pokemon) {
        if (pokemon.getNavigation() != null) pokemon.getNavigation().stop();
    }

    private static void restoreAI(PokemonEntity pokemon) {}
}