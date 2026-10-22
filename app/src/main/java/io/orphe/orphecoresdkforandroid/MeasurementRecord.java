package io.orphe.orphecoresdkforandroid;

import org.json.JSONException;
import org.json.JSONObject;

public class MeasurementRecord {
    public final String id;
    public final long startTimeMillis;
    public final long endTimeMillis;
    public final String memo;
    public final String leftCsvFileName;
    public final String rightCsvFileName;
    public final int leftCount;
    public final int rightCount;
    public final String samplingRate;
    public final String receiveMode;
    public final long createdAtMillis;
    /** 左右のデバイス種別（INSOLE / CORE）。データのない側はnull。 */
    public final String leftDeviceType;
    public final String rightDeviceType;

    public MeasurementRecord(
            String id,
            long startTimeMillis,
            long endTimeMillis,
            String memo,
            String leftCsvFileName,
            String rightCsvFileName,
            int leftCount,
            int rightCount,
            String samplingRate,
            String receiveMode,
            long createdAtMillis,
            String leftDeviceType,
            String rightDeviceType
    ) {
        this.id = id;
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.memo = memo;
        this.leftCsvFileName = leftCsvFileName;
        this.rightCsvFileName = rightCsvFileName;
        this.leftCount = leftCount;
        this.rightCount = rightCount;
        this.samplingRate = samplingRate;
        this.receiveMode = receiveMode;
        this.createdAtMillis = createdAtMillis;
        this.leftDeviceType = leftDeviceType;
        this.rightDeviceType = rightDeviceType;
    }

    public long durationMillis() {
        return Math.max(0L, endTimeMillis - startTimeMillis);
    }

    /** 計測したデバイス種別の表示名。COREを含まない旧履歴はINSOLEとして扱う。 */
    public String deviceTypeLabel() {
        final String left = leftCount > 0 ? deviceTypeOrInsole(leftDeviceType) : null;
        final String right = rightCount > 0 ? deviceTypeOrInsole(rightDeviceType) : null;
        if (left != null && right != null && !left.equals(right)) {
            return "L:" + left + " R:" + right;
        }
        return left != null ? left : (right != null ? right : "INSOLE");
    }

    private static String deviceTypeOrInsole(String deviceType) {
        return deviceType == null || deviceType.isEmpty()
                ? SensorSample.DeviceType.insole.label
                : deviceType;
    }

    public int totalCount() {
        return leftCount + rightCount;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("startTimeMillis", startTimeMillis);
        object.put("endTimeMillis", endTimeMillis);
        object.put("memo", memo);
        object.put("leftCsvFileName", leftCsvFileName);
        object.put("rightCsvFileName", rightCsvFileName);
        object.put("leftCount", leftCount);
        object.put("rightCount", rightCount);
        object.put("samplingRate", samplingRate);
        object.put("receiveMode", receiveMode);
        object.put("createdAtMillis", createdAtMillis);
        object.put("leftDeviceType", leftDeviceType);
        object.put("rightDeviceType", rightDeviceType);
        return object;
    }

    public static MeasurementRecord fromJson(JSONObject object) {
        return new MeasurementRecord(
                object.optString("id"),
                object.optLong("startTimeMillis"),
                object.optLong("endTimeMillis"),
                object.optString("memo"),
                object.optString("leftCsvFileName", null),
                object.optString("rightCsvFileName", null),
                object.optInt("leftCount"),
                object.optInt("rightCount"),
                object.optString("samplingRate"),
                object.optString("receiveMode"),
                object.optLong("createdAtMillis"),
                object.has("leftDeviceType") ? object.optString("leftDeviceType") : null,
                object.has("rightDeviceType") ? object.optString("rightDeviceType") : null
        );
    }
}
