package io.orphe.orphecoresdkforandroid;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import io.orphe.orphecoresdk.OrpheInsolePressureCalibration;
import io.orphe.orphecoresdk.OrpheInsolePressureCoefficient;

/**
 * サンプルアプリでデバイスごとの圧力補正値を永続化します。
 */
final class PressureCalibrationStorage {
    private static final String PREFERENCES_NAME = "pressure_calibrations";
    private static final String CONFIGURED = "configured";
    private static final String TOE_INSIDE_C1 = "toe_inside.c1";
    private static final String TOE_INSIDE_C3 = "toe_inside.c3";
    private static final String MID_INSIDE_C1 = "mid_inside.c1";
    private static final String MID_INSIDE_C3 = "mid_inside.c3";
    private static final String TOE_OUTSIDE_C1 = "toe_outside.c1";
    private static final String TOE_OUTSIDE_C3 = "toe_outside.c3";
    private static final String CENTER_C1 = "center.c1";
    private static final String CENTER_C3 = "center.c3";
    private static final String MID_OUTSIDE_C1 = "mid_outside.c1";
    private static final String MID_OUTSIDE_C3 = "mid_outside.c3";
    private static final String HEEL_C1 = "heel.c1";
    private static final String HEEL_C3 = "heel.c3";

    private static final String[] VALUE_SUFFIXES = {
            TOE_INSIDE_C1, TOE_INSIDE_C3,
            MID_INSIDE_C1, MID_INSIDE_C3,
            TOE_OUTSIDE_C1, TOE_OUTSIDE_C3,
            CENTER_C1, CENTER_C3,
            MID_OUTSIDE_C1, MID_OUTSIDE_C3,
            HEEL_C1, HEEL_C3
    };

    private final SharedPreferences preferences;

    PressureCalibrationStorage(@NonNull Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
        );
    }

    @NonNull
    OrpheInsolePressureCalibration load(String deviceId) {
        if (!isValidDeviceId(deviceId)
                || !preferences.getBoolean(key(deviceId, CONFIGURED), false)) {
            return OrpheInsolePressureCalibration.DEFAULT;
        }
        final OrpheInsolePressureCalibration defaults = OrpheInsolePressureCalibration.DEFAULT;
        return new OrpheInsolePressureCalibration(
                loadCoefficient(deviceId, TOE_INSIDE_C1, TOE_INSIDE_C3, defaults.toeInside),
                loadCoefficient(deviceId, MID_INSIDE_C1, MID_INSIDE_C3, defaults.midInside),
                loadCoefficient(deviceId, TOE_OUTSIDE_C1, TOE_OUTSIDE_C3, defaults.toeOutside),
                loadCoefficient(deviceId, CENTER_C1, CENTER_C3, defaults.center),
                loadCoefficient(deviceId, MID_OUTSIDE_C1, MID_OUTSIDE_C3, defaults.midOutside),
                loadCoefficient(deviceId, HEEL_C1, HEEL_C3, defaults.heel)
        );
    }

    void save(String deviceId, @NonNull OrpheInsolePressureCalibration calibration) {
        if (!isValidDeviceId(deviceId)) {
            throw new IllegalArgumentException("Device ID must not be empty.");
        }
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key(deviceId, CONFIGURED), true);
        saveCoefficient(editor, deviceId, TOE_INSIDE_C1, TOE_INSIDE_C3, calibration.toeInside);
        saveCoefficient(editor, deviceId, MID_INSIDE_C1, MID_INSIDE_C3, calibration.midInside);
        saveCoefficient(editor, deviceId, TOE_OUTSIDE_C1, TOE_OUTSIDE_C3, calibration.toeOutside);
        saveCoefficient(editor, deviceId, CENTER_C1, CENTER_C3, calibration.center);
        saveCoefficient(editor, deviceId, MID_OUTSIDE_C1, MID_OUTSIDE_C3, calibration.midOutside);
        saveCoefficient(editor, deviceId, HEEL_C1, HEEL_C3, calibration.heel);
        editor.apply();
    }

    void clear(String deviceId) {
        if (!isValidDeviceId(deviceId)) {
            return;
        }
        final SharedPreferences.Editor editor = preferences.edit();
        editor.remove(key(deviceId, CONFIGURED));
        for (String suffix : VALUE_SUFFIXES) {
            editor.remove(key(deviceId, suffix));
        }
        editor.apply();
    }

    @NonNull
    private OrpheInsolePressureCoefficient loadCoefficient(
            String deviceId,
            String coefficient1Suffix,
            String coefficient3Suffix,
            @NonNull OrpheInsolePressureCoefficient defaults
    ) {
        return new OrpheInsolePressureCoefficient(
                loadDouble(deviceId, coefficient1Suffix, defaults.coefficient1),
                loadDouble(deviceId, coefficient3Suffix, defaults.coefficient3)
        );
    }

    private double loadDouble(String deviceId, String suffix, double defaultValue) {
        final long bits = preferences.getLong(
                key(deviceId, suffix),
                Double.doubleToLongBits(defaultValue)
        );
        final double value = Double.longBitsToDouble(bits);
        return Double.isNaN(value) || Double.isInfinite(value) ? defaultValue : value;
    }

    private void saveCoefficient(
            @NonNull SharedPreferences.Editor editor,
            String deviceId,
            String coefficient1Suffix,
            String coefficient3Suffix,
            @NonNull OrpheInsolePressureCoefficient coefficient
    ) {
        editor.putLong(
                key(deviceId, coefficient1Suffix),
                Double.doubleToLongBits(coefficient.coefficient1)
        );
        editor.putLong(
                key(deviceId, coefficient3Suffix),
                Double.doubleToLongBits(coefficient.coefficient3)
        );
    }

    private String key(String deviceId, String suffix) {
        return "device." + deviceId + "." + suffix;
    }

    private boolean isValidDeviceId(String deviceId) {
        return deviceId != null && !deviceId.trim().isEmpty();
    }
}
