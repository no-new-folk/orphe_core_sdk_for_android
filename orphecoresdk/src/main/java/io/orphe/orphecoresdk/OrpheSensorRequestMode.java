package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;

/**
 * デバイスへ書き込む低レベルのセンサー送信モード。
 * SDK利用者が受信戦略を選択する場合は{@link OrpheSensorReceiveMode}を使用します。
 */
public enum OrpheSensorRequestMode {
        /// リアルタイム
        realtime(1),

        /// リクエスト形式
        request(2),

        /// インソール向けリアルタイム
        realtimeForInsole(3),

        /// インソール向けリアルタイム（クオータニオン付き）
        realtimeForInsoleWithQuaternion(4);

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
