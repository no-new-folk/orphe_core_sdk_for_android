package io.orphe.orphecoresdk;

/**
 * orphe_insoleのMadgwick 6軸姿勢フィルタをJavaへ移植したSDK内部実装。
 *
 * <p>加速度は内部で正規化するため任意の一貫した単位、ジャイロはrad/s、
 * クオータニオンは[w, x, y, z]順です。</p>
 */
final class OrpheMadgwickFilter {
    static final double DEFAULT_BETA = 0.1;
    static final double SAMPLE_INTERVAL_SECONDS = 0.005;
    private static final double DEGREES_TO_RADIANS = Math.PI / 180.0;

    private double q0;
    private double q1;
    private double q2;
    private double q3;
    private final double beta;

    OrpheMadgwickFilter() {
        this(DEFAULT_BETA);
    }

    OrpheMadgwickFilter(final double beta) {
        if (!Double.isFinite(beta) || beta < 0.0) {
            throw new IllegalArgumentException("beta must be finite and non-negative.");
        }
        this.beta = beta;
        reset();
    }

    void reset() {
        q0 = 1.0;
        q1 = 0.0;
        q2 = 0.0;
        q3 = 0.0;
    }

    void restore(final State state) {
        if (state == null) {
            reset();
            return;
        }
        q0 = state.q0;
        q1 = state.q1;
        q2 = state.q2;
        q3 = state.q3;
    }

    State snapshot() {
        return new State(q0, q1, q2, q3);
    }

    OrpheQuaternion updateDps(
            final double ax,
            final double ay,
            final double az,
            final double gxDps,
            final double gyDps,
            final double gzDps
    ) {
        return update(
                ax,
                ay,
                az,
                gxDps * DEGREES_TO_RADIANS,
                gyDps * DEGREES_TO_RADIANS,
                gzDps * DEGREES_TO_RADIANS,
                SAMPLE_INTERVAL_SECONDS
        );
    }

    OrpheQuaternion update(
            double ax,
            double ay,
            double az,
            final double gx,
            final double gy,
            final double gz,
            final double dt
    ) {
        if (!allFinite(ax, ay, az, gx, gy, gz, dt) || dt <= 0.0) {
            return current();
        }

        double nextQ0 = q0;
        double nextQ1 = q1;
        double nextQ2 = q2;
        double nextQ3 = q3;

        double qDot0 = 0.5 * (-nextQ1 * gx - nextQ2 * gy - nextQ3 * gz);
        double qDot1 = 0.5 * (nextQ0 * gx + nextQ2 * gz - nextQ3 * gy);
        double qDot2 = 0.5 * (nextQ0 * gy - nextQ1 * gz + nextQ3 * gx);
        double qDot3 = 0.5 * (nextQ0 * gz + nextQ1 * gy - nextQ2 * gx);

        final double accelerationNorm = Math.sqrt(ax * ax + ay * ay + az * az);
        if (accelerationNorm > 0.0 && Double.isFinite(accelerationNorm)) {
            final double reciprocal = 1.0 / accelerationNorm;
            ax *= reciprocal;
            ay *= reciprocal;
            az *= reciprocal;

            final double twoQ0 = 2.0 * nextQ0;
            final double twoQ1 = 2.0 * nextQ1;
            final double twoQ2 = 2.0 * nextQ2;
            final double twoQ3 = 2.0 * nextQ3;
            final double fourQ0 = 4.0 * nextQ0;
            final double fourQ1 = 4.0 * nextQ1;
            final double fourQ2 = 4.0 * nextQ2;
            final double eightQ1 = 8.0 * nextQ1;
            final double eightQ2 = 8.0 * nextQ2;
            final double q0q0 = nextQ0 * nextQ0;
            final double q1q1 = nextQ1 * nextQ1;
            final double q2q2 = nextQ2 * nextQ2;
            final double q3q3 = nextQ3 * nextQ3;

            double s0 = fourQ0 * q2q2 + twoQ2 * ax + fourQ0 * q1q1 - twoQ1 * ay;
            double s1 = fourQ1 * q3q3 - twoQ3 * ax + 4.0 * q0q0 * nextQ1
                    - twoQ0 * ay - fourQ1 + eightQ1 * q1q1 + eightQ1 * q2q2
                    + fourQ1 * az;
            double s2 = 4.0 * q0q0 * nextQ2 + twoQ0 * ax + fourQ2 * q3q3
                    - twoQ3 * ay - fourQ2 + eightQ2 * q1q1 + eightQ2 * q2q2
                    + fourQ2 * az;
            double s3 = 4.0 * q1q1 * nextQ3 - twoQ1 * ax
                    + 4.0 * q2q2 * nextQ3 - twoQ2 * ay;

            final double stepNorm = Math.sqrt(s0 * s0 + s1 * s1 + s2 * s2 + s3 * s3);
            if (stepNorm > 0.0 && Double.isFinite(stepNorm)) {
                final double stepReciprocal = 1.0 / stepNorm;
                s0 *= stepReciprocal;
                s1 *= stepReciprocal;
                s2 *= stepReciprocal;
                s3 *= stepReciprocal;

                qDot0 -= beta * s0;
                qDot1 -= beta * s1;
                qDot2 -= beta * s2;
                qDot3 -= beta * s3;
            }
        }

        nextQ0 += qDot0 * dt;
        nextQ1 += qDot1 * dt;
        nextQ2 += qDot2 * dt;
        nextQ3 += qDot3 * dt;

        final double quaternionNorm = Math.sqrt(
                nextQ0 * nextQ0 + nextQ1 * nextQ1 + nextQ2 * nextQ2 + nextQ3 * nextQ3
        );
        if (quaternionNorm > 0.0 && Double.isFinite(quaternionNorm)) {
            final double reciprocal = 1.0 / quaternionNorm;
            q0 = nextQ0 * reciprocal;
            q1 = nextQ1 * reciprocal;
            q2 = nextQ2 * reciprocal;
            q3 = nextQ3 * reciprocal;
        }
        return current();
    }

    private OrpheQuaternion current() {
        return new OrpheQuaternion(q0, q1, q2, q3);
    }

    private static boolean allFinite(final double... values) {
        for (double value : values) {
            if (!Double.isFinite(value)) {
                return false;
            }
        }
        return true;
    }

    static final class State {
        final double q0;
        final double q1;
        final double q2;
        final double q3;

        State(final double q0, final double q1, final double q2, final double q3) {
            this.q0 = q0;
            this.q1 = q1;
            this.q2 = q2;
            this.q3 = q3;
        }
    }
}
