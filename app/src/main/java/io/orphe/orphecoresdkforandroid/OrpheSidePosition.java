package io.orphe.orphecoresdkforandroid;

import android.content.Context;

import androidx.annotation.NonNull;

public enum OrpheSidePosition {
        /// 左の足底
        leftPlantar(OrpheSide.left, OrphePosition.plantar),

        /// 右の足底
        rightPlantar( OrpheSide.right, OrphePosition.plantar),

        /// 左の足背
        leftInstep( OrpheSide.left,  OrphePosition.instep),

        /// 右の足背
        rightInstep( OrpheSide.right,  OrphePosition.instep),

        /// すべて
        both( OrpheSide.both,  OrphePosition.both);

        /// Orpheの左右と足底足背の位置を管理します。
        OrpheSidePosition(
                @NonNull final OrpheSide side, @NonNull final OrphePosition position
        ){
                this.side = side;
                this.position = position;
        }

        /// 左右
        final OrpheSide side;

        /// 足底足背
        final OrphePosition position;

        /// 10進数を元に[OrpheSidePosition]を返します。
        static OrpheSidePosition fromValue(int value) {
                OrpheSidePosition[] values = OrpheSidePosition.values();
                return values[value];
        }

        /// 逆側の[OrpheSidePosition]を返します。
        OrpheSidePosition otherSide() {
                switch (this) {
                        case leftPlantar:
                                return OrpheSidePosition.rightPlantar;
                        case rightPlantar:
                                return OrpheSidePosition.leftPlantar;
                        case leftInstep:
                                return OrpheSidePosition.rightInstep;
                        case rightInstep:
                                return OrpheSidePosition.leftInstep;
                        case both:
                                return OrpheSidePosition.both;
                }
                return null;
        }

        /// 逆側の[OrpheSidePosition]を返します。
        OrpheSidePosition  otherPosition() {
                switch (this) {
                        case leftPlantar:
                                return OrpheSidePosition.leftInstep;
                        case rightPlantar:
                                return OrpheSidePosition.rightInstep;
                        case leftInstep:
                                return OrpheSidePosition.leftPlantar;
                        case rightInstep:
                                return OrpheSidePosition.rightPlantar;
                        case both:
                                return OrpheSidePosition.both;
                }
                return null;
        }
}
