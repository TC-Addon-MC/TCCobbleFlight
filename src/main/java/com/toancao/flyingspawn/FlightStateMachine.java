package com.toancao.flyingspawn;

import com.cobblemon.mod.common.entity.pokemon.PokemonBehaviourFlag;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Random;

/**
 * Điều khiển trạng thái bay của từng Pokemon riêng biệt.
 *
 * Spawn flow: PERCHING (quan sát) → GROUNDED → TAKING_OFF → FLYING ⇄ LANDING
 *
 * Agitation system: Pokemon calm không bay mạnh. Agitation tăng khi bị hit / combat / player lại gần.
 * Legendary: neo anchor tại spawn, chỉ patrol trong radius.
 * Proximity gate: chim chỉ bay khi player trong 20 block.
 */
public class FlightStateMachine {

    private static final Logger LOGGER = LogManager.getLogger("FlyingSpawn");
    private static final Random RNG = new Random();

    private final PokemonEntity pokemon;
    private final PokemonFlightProfile profile;
    private FlightState state;
    private int globalTick = 0;

    public FlightStateMachine(PokemonEntity pokemon, FlightState initialState) {
        this.pokemon = pokemon;
        this.profile = new PokemonFlightProfile(pokemon);
        this.state = initialState;

        if (initialState == FlightState.FLYING) {
            applyInitialFlight();
        }
    }

    /** Chạy logic mỗi tick tùy theo state hiện tại */
    public void tick() {
        globalTick++;
        profile.ticksInCurrentState++;

        tickAgitation();

        switch (state) {
            case PERCHING -> {
                pokemon.setBehaviourFlag(PokemonBehaviourFlag.FLYING, false);
                tickPerching();
            }
            case GROUNDED -> {
                pokemon.setBehaviourFlag(PokemonBehaviourFlag.FLYING, false);
                tickGrounded();
            }
            case TAKING_OFF -> {
                pokemon.setBehaviourFlag(PokemonBehaviourFlag.FLYING, true);
                tickTakingOff();
            }
            case FLYING -> {
                // Chỉ duy trì bay khi player trong fly radius
                if (!isPlayerWithinFlyRadius()) {
                    transitionTo(FlightState.LANDING);
                    return;
                }
                pokemon.setBehaviourFlag(PokemonBehaviourFlag.FLYING, true);
                tickFlying();
            }
            case LANDING -> {
                pokemon.setBehaviourFlag(PokemonBehaviourFlag.FLYING, true);
                tickLanding();
            }
        }
    }

    /**
     * Agitation system: tăng khi bị hit / combat / player gần; giảm dần theo thời gian.
     */
    private void tickAgitation() {
        FlyingSpawnConfig cfg = FlyingSpawnConfig.get();

        if (pokemon.hurtTime > 0) {
            profile.agitation = Math.min(1.0, profile.agitation + cfg.agitationHitIncrease);
        }
        if (pokemon.getBattleId() != null) {
            profile.agitation = Math.min(1.0, profile.agitation + cfg.agitationBattleIncrease);
        }
        if (isPlayerNearby(cfg)) {
            profile.agitation = Math.min(1.0, profile.agitation + cfg.agitationProximityIncrease);
        }
        profile.agitation = Math.max(0.0, profile.agitation - cfg.agitationDecayPerTick);
    }

    /**
     * PERCHING: đứng yên quan sát sau spawn.
     * Nếu bị giật mình (agitation cao), rút ngắn thời gian observe.
     */
    private void tickPerching() {
        profile.spawnObserveTicks++;
        FlyingSpawnConfig cfg = FlyingSpawnConfig.get();

        boolean startled = profile.agitation >= cfg.agitationStartleThreshold;
        int requiredTicks = startled ? cfg.spawnObserveTicksMin : cfg.spawnObserveTicksNormal;

        if (profile.spawnObserveTicks >= requiredTicks) {
            transitionTo(FlightState.GROUNDED);
        }
    }

    private void tickGrounded() {
        GroundedBehavior.tick(pokemon, profile, globalTick);
        FlyingSpawnConfig cfg = FlyingSpawnConfig.get();

        if (globalTick % cfg.transitionCheckInterval == 0) {
            // Chim chỉ cất cánh khi player trong fly radius (20 block)
            if (!isPlayerWithinFlyRadius()) return;

            boolean nearPlayer = isPlayerNearby(cfg);
            boolean openSpace = isOpenSpace(cfg);
            double takeoffChance = profile.computedTakeoffChance(nearPlayer, openSpace);

            // Calm pokemon (agitation thấp) không dễ cất cánh trừ khi đã idle lâu
            if (profile.agitation < cfg.agitationMinToFly && profile.idleTicks < cfg.idleLongThreshold1) return;

            if (RNG.nextDouble() < takeoffChance) {
                transitionTo(FlightState.TAKING_OFF);
            }
        }
    }

    private void tickTakingOff() {
        boolean done = FlyingBehavior.tickTakingOff(pokemon, profile, globalTick);
        if (done) transitionTo(FlightState.FLYING);
    }

    private void tickFlying() {
        profile.speedBonus = profile.isLegendary ? 0.0 : 0.25;
        FlyingBehavior.tick(pokemon, profile, globalTick);
        FlyingSpawnConfig cfg = FlyingSpawnConfig.get();

        // Legendary: kéo về anchor nếu bay quá xa
        if (profile.isLegendary) {
            double dx = pokemon.getX() - profile.anchorX;
            double dz = pokemon.getZ() - profile.anchorZ;
            double distSq = dx * dx + dz * dz;
            double maxRadius = cfg.legendaryPatrolRadius;
            if (distSq > maxRadius * maxRadius) {
                // Quay đầu về anchor
                double angleToAnchor = Math.toDegrees(Math.atan2(-dx, dz));
                profile.currentYaw = angleToAnchor;
            }
        }

        if (globalTick % cfg.transitionCheckInterval == 0) {
            boolean nearGround = isNearGround(cfg);
            boolean flyingLong = profile.ticksInCurrentState > cfg.flyingLongThreshold;
            double landingChance = profile.computedLandingChance(nearGround, flyingLong);

            if (RNG.nextDouble() < landingChance) {
                profile.verticalVelocity = 0.0;
                transitionTo(FlightState.LANDING);
            }
        }
    }

    private void tickLanding() {
        boolean done = FlyingBehavior.tickLanding(pokemon, profile, globalTick);
        if (done) transitionTo(FlightState.GROUNDED);
    }

    private void transitionTo(FlightState newState) {
        state = newState;
        profile.ticksInCurrentState = 0;

        if (newState == FlightState.TAKING_OFF) {
            profile.refreshPreferredHeight();
            profile.verticalVelocity = 0.0;
            profile.currentYaw = RNG.nextDouble() * 360.0;
            if (profile.isLegendary) {
                profile.verticalVelocity= profile.verticalVelocity + profile.verticalVelocity*0.25;
            }
        }
    }

    /** Kiểm tra có player trong playerAlertRadius (dùng cho agitation / nearPlayer check) */
    private boolean isPlayerNearby(FlyingSpawnConfig cfg) {
        if (pokemon.level() == null) return false;
        double r = cfg.playerAlertRadius;
        List<Player> players = pokemon.level().getEntitiesOfClass(
                Player.class,
                new AABB(pokemon.getX() - r, pokemon.getY() - r, pokemon.getZ() - r,
                        pokemon.getX() + r, pokemon.getY() + r, pokemon.getZ() + r)
        );
        return !players.isEmpty();
    }

    /**
     * Proximity gate: chim chỉ bay khi có player trong flyActivationRadius (mặc định 20 block).
     */
    private boolean isPlayerWithinFlyRadius() {
        if (pokemon.level() == null) return false;
        FlyingSpawnConfig cfg = FlyingSpawnConfig.get();
        double r = cfg.flyActivationRadius;
        List<Player> players = pokemon.level().getEntitiesOfClass(
                Player.class,
                new AABB(pokemon.getX() - r, pokemon.getY() - r, pokemon.getZ() - r,
                        pokemon.getX() + r, pokemon.getY() + r, pokemon.getZ() + r)
        );
        return !players.isEmpty();
    }

    private boolean isOpenSpace(FlyingSpawnConfig cfg) {
        if (pokemon.level() == null) return false;
        int airCount = 0;
        int px = (int) pokemon.getX(), py = (int) pokemon.getY(), pz = (int) pokemon.getZ();

        for (int dx = -3; dx <= 3; dx += 2) {
            for (int dy = 0; dy <= 6; dy += 2) {
                for (int dz = -3; dz <= 3; dz += 2) {
                    var pos = new net.minecraft.core.BlockPos(px + dx, py + dy, pz + dz);
                    if (pokemon.level().getBlockState(pos).isAir()) airCount++;
                }
            }
        }
        return airCount >= cfg.openSpaceThreshold;
    }

    private boolean isNearGround(FlyingSpawnConfig cfg) {
        return pokemon.onGround() || (pokemon.level() != null && isBlockBelow(cfg.nearGroundBlockDist));
    }

    private boolean isBlockBelow(int maxDist) {
        for (int i = 1; i <= maxDist; i++) {
            var pos = new net.minecraft.core.BlockPos((int) pokemon.getX(), (int) (pokemon.getY() - i), (int) pokemon.getZ());
            if (pokemon.level() != null && !pokemon.level().getBlockState(pos).isAir()) return true;
        }
        return false;
    }

    /** Tránh kẹt vào lá cây khi vừa sinh ra */
    private void applyInitialFlight() {
        pokemon.setNoGravity(true);
        FlyingSpawnConfig cfg = FlyingSpawnConfig.get();
        double startX = pokemon.getX(), startY = pokemon.getY(), startZ = pokemon.getZ();
        double highestSolidY = startY;

        if (pokemon.level() != null) {
            for (int dy = 1; dy <= cfg.initialFlightScanUp; dy++) {
                var pos = new net.minecraft.core.BlockPos((int)startX, (int)(startY + dy), (int)startZ);
                if (pokemon.level().getBlockState(pos).isSolid()) highestSolidY = startY + dy;
            }
        }

        double finalY = highestSolidY + profile.preferredHeight;

        pokemon.setDeltaMovement(0, 0, 0);
        pokemon.teleportTo(startX, finalY, startZ);
        pokemon.setOldPosAndRot();
        pokemon.hurtMarked = true;
    }

    /** Cho phép caller khởi tạo thêm thông tin vào profile sau khi tạo machine */
    public void initProfile(java.util.function.Consumer<PokemonFlightProfile> init) {
        init.accept(profile);
    }

    public FlightState getState() { return state; }

    public boolean isAlive() {
        return pokemon.isAlive() && !pokemon.isRemoved() && pokemon.level() != null;
    }

    public void deactivate() {
        pokemon.setNoGravity(false);
        pokemon.setBehaviourFlag(PokemonBehaviourFlag.FLYING, false);
        pokemon.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    }
}