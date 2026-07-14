package io.orphe.orphecoresdk;

/**
 * ORPHE INSOLEの1つの圧力センサーに適用する補正値。
 */
public final class OrpheInsolePressureCoefficient {
    /**
     * demo010と同じ既定補正値。
     */
    public static final OrpheInsolePressureCoefficient DEFAULT =
            new OrpheInsolePressureCoefficient(2.77942, 4.14411);

    public OrpheInsolePressureCoefficient(
            final double coefficient1,
            final double coefficient3
    ) {
        this.coefficient1 = coefficient1;
        this.coefficient3 = coefficient3;
    }

    /**
     * 指数項へ乗算する係数。
     */
    public final double coefficient1;

    /**
     * 計算結果へ加算する係数。
     */
    public final double coefficient3;

    OrpheInsolePressureCoefficient withCoefficient(
            final OrpheInsoleCoefficient coefficient,
            final double value
    ) {
        switch (coefficient) {
            case coefficient1:
                return new OrpheInsolePressureCoefficient(value, coefficient3);
            case coefficient3:
                return new OrpheInsolePressureCoefficient(coefficient1, value);
            default:
                throw new IllegalArgumentException("Unsupported pressure coefficient: " + coefficient);
        }
    }
}
