package com.toancao.flyingspawn;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import java.util.function.Consumer;

/**
 * Tick loop trung tâm — chạy tất cả FlightStateMachine mỗi server tick.
 *
 * Tự động dọn dẹp state machine của Pokemon đã chết / bị remove.
 */
public class FlightTickManager {

    private static final Logger LOGGER = LogManager.getLogger("FlyingSpawn");

    /** Map UUID → StateMachine. UUID làm key để tránh giữ hard reference entity. */
    private static final Map<UUID, FlightStateMachine> machines = new ConcurrentHashMap<>();

    /** Hard reference ngắn hạn cần thiết để tick (dọn khi isAlive = false). */
    private static final Map<UUID, PokemonEntity> entities = new ConcurrentHashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
        LOGGER.info("✅ FlightTickManager: Đã đăng ký END_SERVER_TICK");
    }

    public static void register(PokemonEntity pokemon, FlightState initialState) {
        register(pokemon, initialState, null);
    }

    /**
     * Đăng ký Pokemon mới, với callback để khởi tạo thêm profile (ví dụ: set legendary anchor).
     */
    public static void register(PokemonEntity pokemon, FlightState initialState, java.util.function.Consumer<PokemonFlightProfile> profileInit) {
        UUID id = pokemon.getUUID();
        if (machines.containsKey(id)) return;

        FlightStateMachine machine = new FlightStateMachine(pokemon, initialState);
        if (profileInit != null) machine.initProfile(profileInit);
        machines.put(id, machine);
        entities.put(id, pokemon);

        LOGGER.debug("📋 FlightTickManager: Tracked {} (initial={})",
                pokemon.getPokemon().getSpecies().getName(), initialState);
    }

    /**
     * Gỡ Pokemon khỏi hệ thống (khi bắt / despawn thủ công).
     */
    public static void unregister(PokemonEntity pokemon) {
        UUID id = pokemon.getUUID();
        machines.remove(id);
        entities.remove(id);
    }

    /** Số Pokemon đang được quản lý (debug). */
    public static int getTrackedCount() {
        return machines.size();
    }

    // ─────────────────────────────────────────────────────────────
    //  TICK
    // ─────────────────────────────────────────────────────────────

    private static void tick() {
        if (machines.isEmpty()) return;

        Iterator<Map.Entry<UUID, FlightStateMachine>> iterator = machines.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            UUID id = entry.getKey();
            FlightStateMachine machine = entry.getValue();
            PokemonEntity pokemon = entities.get(id);

            // 1. Kiểm tra sự sống
            if (!machine.isAlive()) {
                iterator.remove();
                entities.remove(id);
                continue;
            }

            // 2. TÍCH HỢP: Kiểm tra ngữ cảnh (Wild, No Battle,...)
            // Nếu không thỏa mãn, phải dọn dẹp (reset trọng lực) trước khi xóa
            if (!FlightContext.shouldTick(pokemon)) {
                machine.deactivate(); // Trả lại trạng thái mặc định
                iterator.remove();
                entities.remove(id);
                continue;
            }

            try {
                machine.tick();
            } catch (Exception e) {
                machine.deactivate();
                iterator.remove();
                entities.remove(id);
            }
        }
    }
}