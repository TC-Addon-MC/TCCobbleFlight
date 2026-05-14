package com.toancao.flyingspawn;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

/** Lọc whitelist/blacklist xem hệ bay có được dùng cơ chế bay này không. */
public class FlyingCapabilityChecker {

    public static boolean canFly(PokemonEntity pokemon) {
        FlyingSpawnConfig cfg = FlyingSpawnConfig.get();
        String speciesName = pokemon.getPokemon().getSpecies().getName().toLowerCase();

        if (cfg.forceFly.contains(speciesName)) return true;
        if (cfg.cannotFly.contains(speciesName)) return false;

        return pokemon.getBehaviour().getMoving().getFly().getCanFly();
    }
}