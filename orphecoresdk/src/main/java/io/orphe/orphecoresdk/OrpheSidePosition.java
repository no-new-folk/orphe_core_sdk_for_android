package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;

/**
 * 左右と取り付け位置を含めた取り付け位置
 */
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

        /**
         * 左右と取り付け位置を含めた取り付け位置
         */
        OrpheSidePosition(
                @NonNull final OrpheSide side, @NonNull final OrphePosition position
        ){
                this.side = side;
                this.position = position;
        }

        /**
         * 左右の取り付け位置
         */
        final OrpheSide side;

        /**
         * 足底足背の取り付け位置
         */
        final OrphePosition position;

        /**
         * OrpheSidePosition。
         *
         * @param value 数字
         * @return 対応するOrpheSidePosition。
         */
        static OrpheSidePosition fromValue(int value) {
                OrpheSidePosition[] values = OrpheSidePosition.values();
                return values[value];
        }

        /**
         * 左右逆側の[OrpheSidePosition]を返します。
         *
         * @return 左右逆側の[OrpheSidePosition]
         */
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

        /**
         * 足背足底逆側の[OrpheSidePosition]を返します。
         *
         * @return 足背足底逆側の[OrpheSidePosition]
         */
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
