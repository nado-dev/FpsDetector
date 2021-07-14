package com.aaronfang.fpsdetector;

/**
 * Created by AaFaa
 * on 2021/7/13
 * in package com.aaronfang.fpsdetector
 * with project FpsDetector
 */
public interface FPSConst {
    String KEY_SCENE = "scene";
    String SCROLL_TIME = "scroll_time";
    String SCROLL_VELOCITY = "scroll_velocity";
    String SCROLL_DISTANCE = "scroll_distance";
    String KEY_DROP_RATE = "drop_rate";
    long FACTOR_1000 = 1000L;

    long FACTOR_1000_POWER_3 = FACTOR_1000 * FACTOR_1000 * FACTOR_1000;
}
