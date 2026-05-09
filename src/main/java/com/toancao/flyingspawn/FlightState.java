package com.toancao.flyingspawn;

/**
 * Các trạng thái trong vòng đời hành vi bay của Pokemon hệ FLYING.
 *
 * Sơ đồ chuyển trạng thái:
 *
 *   PERCHING ──(observe done)──▶ GROUNDED ──(takeoff)──▶ TAKING_OFF ──(done)──▶ FLYING
 *      ▲                            ▲                                               │
 *      └────────────────────────────└──────(smooth land)── LANDING ◀──(land)───────┘
 *
 *   Spawn luôn bắt đầu ở PERCHING — đứng yên quan sát vài giây trước khi hành động.
 */
public enum FlightState {

    /**
     * Vừa spawn — đứng yên quan sát, chưa có hành vi tự chủ nào.
     * Chuyển sang GROUNDED sau khi hết observe delay.
     */
    PERCHING,

    /**
     * Đứng / đi bộ / nhìn xung quanh trên mặt đất.
     */
    GROUNDED,

    /**
     * Đang cất cánh theo quỹ đạo cong (arc), chưa vào chế độ bay hoàn toàn.
     * Trạng thái chuyển tiếp ngắn (~1-2 giây).
     */
    TAKING_OFF,

    /**
     * Bay liên tục trên không, có hướng chính và dao động sin.
     */
    FLYING,

    /**
     * Đang hạ cánh từ từ xuống đất, giảm tốc và độ cao dần.
     * Trạng thái chuyển tiếp ngắn (~1-2 giây).
     */
    LANDING
}