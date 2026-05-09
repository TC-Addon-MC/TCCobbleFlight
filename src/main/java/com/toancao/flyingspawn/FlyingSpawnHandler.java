package com.toancao.flyingspawn;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Lắng nghe entity spawn và set flight state ban đầu. */
public class FlyingSpawnHandler {

    private static final Logger LOGGER = LogManager.getLogger("FlyingSpawn");

    public static void register() {
        LOGGER.info("FlyingSpawnHandler: Đã đăng ký event ENTITY_LOAD");

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof PokemonEntity pokemon)) return;
            if (!FlyingCapabilityChecker.canFly(pokemon)) return;

            world.getServer().execute(() -> {
                if (!FlightContext.isEligible(pokemon)) return;

                // Spawn luôn bắt đầu PERCHING — đứng yên quan sát trước
                FlightTickManager.register(pokemon, FlightState.PERCHING, (profile) -> {
                    // Set legendary flag và anchor
                    profile.isLegendary = pokemon.getPokemon().isLegendary();
                    profile.anchorX = pokemon.getX();
                    profile.anchorY = pokemon.getY();
                    profile.anchorZ = pokemon.getZ();
                });
            });
        });
    }
}