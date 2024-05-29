package io.orphe.orphecoresdk;

/**
 * 左右の取り付け位置
 */
public enum OrpheSide {

    /// 左
    left,

    /// 右
    right,

    /// 両方
    both;

    /**
     * 逆のサイドを取得します。
     * @return 逆のサイド
     */
    OrpheSide other() {
        switch (this) {
            case left:
                return OrpheSide.right;
            case right:
                return OrpheSide.left;
            case both:
                return OrpheSide.both;
        }
        return null;
    }

    /**
     * [OrphePosition]を渡して[OrpheSidePosition]を取得します。
     * @param position 取り付け位置
     * @return OrpheSidePosition
     */
    /// 位置を元に[OrpheSidePosition]を返します。
    OrpheSidePosition position(OrphePosition position) {
        switch (position) {
            case plantar:
                switch (this) {
                    case left:
                        return OrpheSidePosition.leftPlantar;
                    case right:
                        return OrpheSidePosition.rightPlantar;
                    case both:
                        return OrpheSidePosition.both;
                }
            case instep:
                switch (this) {
                    case left:
                        return OrpheSidePosition.leftInstep;
                    case right:
                        return OrpheSidePosition.rightInstep;
                    case both:
                        return OrpheSidePosition.both;
                }
            case both:
                return OrpheSidePosition.both;
        }
        return null;
    }
}
