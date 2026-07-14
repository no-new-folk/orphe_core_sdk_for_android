package io.orphe.orphecoresdk;

/**
 * ORPHE INSOLEの1つの圧力センサーに適用する補正値。
 */
public final class OrpheInsolePressureCoefficient {
    /**
     * demo010と同じ既定補正値。
     */
    public static final OrpheInsolePressureCoefficient DEFAULT =
            new OrpheInsolePressureCoefficient(2.77942, 0.00235, 4.14411, 240.0);

    /**
     * coefficient2とthresholdにdemo010と同じ既定値を使用します。
     *
     * @param coefficient1 指数項へ乗算する係数
     * @param coefficient3 計算結果へ加算する係数
     */
    public OrpheInsolePressureCoefficient(
            final double coefficient1,
            final double coefficient3
    ) {
        this(
                coefficient1,
                DEFAULT.coefficient2,
                coefficient3,
                DEFAULT.threshold
        );
    }

    public OrpheInsolePressureCoefficient(
            final double coefficient1,
            final double coefficient2,
            final double coefficient3,
            final double threshold
    ) {
        this.coefficient1 = coefficient1;
        this.coefficient2 = coefficient2;
        this.coefficient3 = coefficient3;
        this.threshold = threshold;
    }

    /**
     * 指数項へ乗算する係数。
     */
    public final double coefficient1;

    /**
     * 圧力値へ乗算する指数係数。
     */
    public final double coefficient2;

    /**
     * 計算結果へ加算する係数。
     */
    public final double coefficient3;

    /**
     * 圧力を0Nとして扱う上限電圧（mV）。
     */
    public final double threshold;

    OrpheInsolePressureCoefficient withCoefficient(
            final OrpheInsoleCoefficient coefficient,
            final double value
    ) {
        switch (coefficient) {
            case coefficient1:
                return new OrpheInsolePressureCoefficient(
                        value, coefficient2, coefficient3, threshold);
            case coefficient2:
                return new OrpheInsolePressureCoefficient(
                        coefficient1, value, coefficient3, threshold);
            case coefficient3:
                return new OrpheInsolePressureCoefficient(
                        coefficient1, coefficient2, value, threshold);
            case threshold:
                return new OrpheInsolePressureCoefficient(
                        coefficient1, coefficient2, coefficient3, value);
            default:
                throw new IllegalArgumentException("Unsupported pressure coefficient: " + coefficient);
        }
    }
}
