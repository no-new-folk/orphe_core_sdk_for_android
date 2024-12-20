package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;

/**
 * センサーの取得モード
 */
public enum OrpheSensorRequestMode {
        /// リアルタイム
        realtime(1),

        /// リクエスト形式
        request(2),

        /// インソール向けリアルタイム
        realtimeForInsole(3);

        /// インソール向けリアルタイム（クオータニオン付き）
        // realtimeForInsoleWithQuaternion(4);

        /**
         * センサーの取得モード
         */
        OrpheSensorRequestMode(
                @NonNull final int value
        ){
                this.value = value;
        }

        /**
         * コアに渡す値
         */
        final int value;

        /**
         * OrpheSensorRequestModeを取得します。
         *
         * @param value 数字
         * @return 対応するOrpheSensorRequestMode。
         */
        static OrpheSensorRequestMode fromValue(int value) {
                OrpheSensorRequestMode[] values = OrpheSensorRequestMode.values();
                for(OrpheSensorRequestMode mode : values){
                        if(mode.value == value){
                                return mode;
                        }
                }
                return null;
        }
}
