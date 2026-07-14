package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;

import java.util.Map;

/**
 * ORPHE INSOLEの6点の圧力センサーに適用する補正設定。
 */
public final class OrpheInsolePressureCalibration {
    /**
     * 全6点へdemo010と同じ既定補正値を適用する設定。
     */
    public static final OrpheInsolePressureCalibration DEFAULT =
            new OrpheInsolePressureCalibration(
                    OrpheInsolePressureCoefficient.DEFAULT,
                    OrpheInsolePressureCoefficient.DEFAULT,
                    OrpheInsolePressureCoefficient.DEFAULT,
                    OrpheInsolePressureCoefficient.DEFAULT,
                    OrpheInsolePressureCoefficient.DEFAULT,
                    OrpheInsolePressureCoefficient.DEFAULT
            );

    public OrpheInsolePressureCalibration(
            @NonNull final OrpheInsolePressureCoefficient toeInside,
            @NonNull final OrpheInsolePressureCoefficient midInside,
            @NonNull final OrpheInsolePressureCoefficient toeOutside,
            @NonNull final OrpheInsolePressureCoefficient center,
            @NonNull final OrpheInsolePressureCoefficient midOutside,
            @NonNull final OrpheInsolePressureCoefficient heel
    ) {
        this.toeInside = requireCoefficient(toeInside, "toeInside");
        this.midInside = requireCoefficient(midInside, "midInside");
        this.toeOutside = requireCoefficient(toeOutside, "toeOutside");
        this.center = requireCoefficient(center, "center");
        this.midOutside = requireCoefficient(midOutside, "midOutside");
        this.heel = requireCoefficient(heel, "heel");
    }

    @NonNull public final OrpheInsolePressureCoefficient toeInside;
    @NonNull public final OrpheInsolePressureCoefficient midInside;
    @NonNull public final OrpheInsolePressureCoefficient toeOutside;
    @NonNull public final OrpheInsolePressureCoefficient center;
    @NonNull public final OrpheInsolePressureCoefficient midOutside;
    @NonNull public final OrpheInsolePressureCoefficient heel;

    @NonNull
    OrpheInsolePressureCoefficient coefficientFor(
            @NonNull final OrpheInsoleSensorPosition sensorPosition
    ) {
        switch (sensorPosition) {
            case toeInside:
                return toeInside;
            case midInside:
                return midInside;
            case toeOutside:
                return toeOutside;
            case center:
                return center;
            case midOutside:
                return midOutside;
            case heel:
                return heel;
            default:
                throw new IllegalArgumentException("Unsupported pressure sensor position: " + sensorPosition);
        }
    }

    @NonNull
    OrpheInsolePressureCalibration withCoefficient(
            @NonNull final OrpheInsoleSensorPosition sensorPosition,
            @NonNull final OrpheInsoleCoefficient coefficient,
            final double value
    ) {
        final OrpheInsolePressureCoefficient updated =
                coefficientFor(sensorPosition).withCoefficient(coefficient, value);
        return new OrpheInsolePressureCalibration(
                sensorPosition == OrpheInsoleSensorPosition.toeInside ? updated : toeInside,
                sensorPosition == OrpheInsoleSensorPosition.midInside ? updated : midInside,
                sensorPosition == OrpheInsoleSensorPosition.toeOutside ? updated : toeOutside,
                sensorPosition == OrpheInsoleSensorPosition.center ? updated : center,
                sensorPosition == OrpheInsoleSensorPosition.midOutside ? updated : midOutside,
                sensorPosition == OrpheInsoleSensorPosition.heel ? updated : heel
        );
    }

    @NonNull
    static OrpheInsolePressureCalibration fromMap(
            final Map<OrpheInsoleSensorPosition, Map<OrpheInsoleCoefficient, Double>> coefficientMap
    ) {
        if (coefficientMap == null) {
            return DEFAULT;
        }
        return new OrpheInsolePressureCalibration(
                coefficientFromMap(coefficientMap, OrpheInsoleSensorPosition.toeInside),
                coefficientFromMap(coefficientMap, OrpheInsoleSensorPosition.midInside),
                coefficientFromMap(coefficientMap, OrpheInsoleSensorPosition.toeOutside),
                coefficientFromMap(coefficientMap, OrpheInsoleSensorPosition.center),
                coefficientFromMap(coefficientMap, OrpheInsoleSensorPosition.midOutside),
                coefficientFromMap(coefficientMap, OrpheInsoleSensorPosition.heel)
        );
    }

    @NonNull
    private static OrpheInsolePressureCoefficient coefficientFromMap(
            @NonNull final Map<OrpheInsoleSensorPosition, Map<OrpheInsoleCoefficient, Double>> coefficientMap,
            @NonNull final OrpheInsoleSensorPosition sensorPosition
    ) {
        final Map<OrpheInsoleCoefficient, Double> values = coefficientMap.get(sensorPosition);
        if (values == null) {
            return OrpheInsolePressureCoefficient.DEFAULT;
        }
        final Double coefficient1 = values.get(OrpheInsoleCoefficient.coefficient1);
        final Double coefficient3 = values.get(OrpheInsoleCoefficient.coefficient3);
        return new OrpheInsolePressureCoefficient(
                coefficient1 == null
                        ? OrpheInsolePressureCoefficient.DEFAULT.coefficient1
                        : coefficient1,
                coefficient3 == null
                        ? OrpheInsolePressureCoefficient.DEFAULT.coefficient3
                        : coefficient3
        );
    }

    @NonNull
    private static OrpheInsolePressureCoefficient requireCoefficient(
            final OrpheInsolePressureCoefficient coefficient,
            @NonNull final String sensorPosition
    ) {
        if (coefficient == null) {
            throw new IllegalArgumentException(sensorPosition + " pressure coefficient must not be null.");
        }
        return coefficient;
    }
}
