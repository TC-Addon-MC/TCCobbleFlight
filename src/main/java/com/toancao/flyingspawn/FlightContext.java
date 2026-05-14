package com.toancao.flyingspawn;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.fabricmc.loader.api.FabricLoader;

public class FlightContext {

    private static final boolean HAS_FIGHT_OR_FLIGHT = FabricLoader.getInstance().isModLoaded("fightorflight");

    public static boolean isEligible(PokemonEntity pokemon) {
        if (pokemon == null || !pokemon.isAlive() || pokemon.isRemoved()) return false;

        Pokemon pkmn = pokemon.getPokemon();
        if (pkmn.getOwnerUUID() != null) return false;
        if (pokemon.getBattleId() != null) return false;
        if (pokemon.isSleeping()) return false;
        if (pokemon.isBusy()) return false;
        if (pokemon.isVehicle()) return false;
        // TÍCH HỢP FIGHT OR FLIGHT CHUẨN
        if (HAS_FIGHT_OR_FLIGHT) {
            // Nếu nó đang Tức giận (Fight) hoặc Hoảng sợ (Flight) -> Tắt bay, nhường quyền
            if (FightOrFlightCompat.isEngaged(pokemon)) {
                return false;
            }
        }

        if (pokemon.level() == null) return false;

        return true;
    }

    public static boolean shouldTick(PokemonEntity pokemon) {
        return isEligible(pokemon);
    }
}