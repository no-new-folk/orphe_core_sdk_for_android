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
            long createdAtMillis
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
    }

    public long durationMillis() {
        return Math.max(0L, endTimeMillis - startTimeMillis);
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
                object.optLong("createdAtMillis")
        );
    }
}
