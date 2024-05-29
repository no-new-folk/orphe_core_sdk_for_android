package io.orphe.orphecoresdk;

/**
 * ORPHE COREの取り付け位置
 */
public enum OrphePosition {
    /// 足底
    plantar,

    /// 足背
    instep,

    /// 両方
    both;

    /**
     * 逆の位置を取得します。
     *
     * @return ORPHE COREの取り付け位置
     */
    OrphePosition  other() {
        switch (this) {
            case plantar:
                return OrphePosition.instep;
            case instep:
                return OrphePosition.plantar;
            case both:
                return OrphePosition.both;
        }
        return null;
    }

    /**
     * [OrpheSide]を渡して[OrpheSidePosition]を返します。
     *
     * @param side 左右の位置
     * @return [OrpheSidePosition]を返します。
     * @throws Exception [both]が設定されている場合はエラー。
     */
    OrpheSidePosition side(OrpheSide side) throws Exception {
        switch (side) {
            case left:
                switch (this) {
                    case plantar:
                        return OrpheSidePosition.leftPlantar;
                    case instep:
                        return OrpheSidePosition.leftInstep;
                    case both:
                        throw new Exception("両方の場合は指定できません。");
                }
            case right:
                switch (this) {
                    case plantar:
                        return OrpheSidePosition.rightPlantar;
                    case instep:
                        return OrpheSidePosition.rightInstep;
                    case both:
                        throw new Exception("両方の場合は指定できません。");
                }
            case both:
                throw new Exception("両方の場合は指定できません。");
        }
        return null;
    }
}
