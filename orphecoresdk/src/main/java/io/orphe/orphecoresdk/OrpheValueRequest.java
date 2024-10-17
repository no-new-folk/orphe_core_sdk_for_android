package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;

/**
 * ORPHEの生データ取得のためのリクエスト用のクラス。
 */
public class OrpheValueRequest {
    /**
     * ORPHEの生データ取得のためのリクエスト用のクラス。
     */
    public OrpheValueRequest(
            /// シリアルナンバーの開始値
            @NonNull final int startSerialNumber,
            /// 要求件数
            @NonNull final int length


    ) {
        this.startSerialNumber = startSerialNumber;
        this.length = length;

    }

    /**
     * シリアルナンバーの開始値
     */
    @NonNull
    public final int startSerialNumber;

    /**
     * 要求件数
     */
    @NonNull
    public final int length;

    /**
     * 文字列に変換します。
     *
     * @return 文字列
     */
    public String toString(){
        final StringBuilder builder = new StringBuilder();
        builder.append("request:(");
        builder.append(String.format("%d", startSerialNumber));
        builder.append(",");
        builder.append(String.format("%d", length));
        builder.append(")\n");
        return builder.toString();
    }
}
