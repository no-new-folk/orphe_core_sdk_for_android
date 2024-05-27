package io.orphe.orphecoresdk;

public enum OrphePosition {
    /// 足底
    plantar,

    /// 足背
    instep,

    /// 両方
    both;

    /// 逆の位置を取得します。
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

    /// 位置を元に[OrpheSidePosition]を返します。
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
