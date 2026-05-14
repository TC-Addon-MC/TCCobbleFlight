package com.toancao.flyingspawn;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import me.rufia.fightorflight.utils.PokemonUtils;

public class FightOrFlightCompat {

    /**
     * Trả về true nếu Pokemon đang trong trạng thái "Fight" (đánh) hoặc "Flight" (chạy).
     */
    public static boolean isEngaged(PokemonEntity pokemon) {
        // 1. Đã khóa mục tiêu (chuẩn bị cắn)
        if (PokemonUtils.getTarget(pokemon) != null) return true;

        // 2. Tức giận và quyết định đánh trả (Fight)
        if (PokemonUtils.shouldFightTarget(pokemon)) return true;


        return false;
    }
}