package io.orphe.orphecoresdkforandroid;

import android.content.Context;
import android.net.Uri;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import io.orphe.orphecoresdk.OrpheInsoleSamplingRate;
import io.orphe.orphecoresdk.OrpheInsoleValue;
import io.orphe.orphecoresdk.OrpheSensorReceiveMode;

public class MeasurementStorage {
    private static final String MEASUREMENTS_DIR = "measurements";
    private static final String INDEX_FILE = "index.json";
    private static final String LEFT_CSV = "sensor-left.csv";
    private static final String RIGHT_CSV = "sensor-right.csv";
    private static final String CSV_HEADER_PREFIX =
            "timestamp,accX,accY,accZ,gyroX,gyroY,gyroZ";
    private static final String CSV_QUATERNION_HEADER =
            ",quatW,quatX,quatY,quatZ";
    private static final String CSV_HEADER_SUFFIX =
            ",pressureToeOutside,pressureMidOutside,pressureToeInside,pressureCenter,"
                    + "pressureMidInside,pressureHeel,serialNumber,dataPosition,sidePosition,receivedAt";

    private final Context mContext;

    public MeasurementStorage(Context context) {
        mContext = context.getApplicationContext();
    }

    public List<MeasurementRecord> loadRecords() {
        JSONArray array = readIndex();
        List<MeasurementRecord> records = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            if (array.optJSONObject(i) != null) {
                records.add(MeasurementRecord.fromJson(array.optJSONObject(i)));
            }
        }
        Collections.sort(records, (left, right) -> Long.compare(right.createdAtMillis, left.createdAtMillis));
        return records;
    }

    public MeasurementRecord findRecord(String id) {
        for (MeasurementRecord record : loadRecords()) {
            if (record.id.equals(id)) {
                return record;
            }
        }
        return null;
    }

    public MeasurementRecord saveMeasurement(
            List<OrpheInsoleValue> leftValues,
            List<OrpheInsoleValue> rightValues,
            long startTimeMillis,
            long endTimeMillis,
            OrpheInsoleSamplingRate samplingRate,
            OrpheSensorReceiveMode receiveMode
    ) throws IOException, JSONException {
        String id = UUID.randomUUID().toString();
        File measurementDir = measurementDir(id);
        if (!measurementDir.exists() && !measurementDir.mkdirs()) {
            throw new IOException("Failed to create measurement directory: " + measurementDir);
        }

        String leftFileName = null;
        if (!leftValues.isEmpty()) {
            writeString(csvFile(id, LEFT_CSV), toCsv(leftValues, receiveMode, samplingRate));
            leftFileName = LEFT_CSV;
        }

        String rightFileName = null;
        if (!rightValues.isEmpty()) {
            writeString(csvFile(id, RIGHT_CSV), toCsv(rightValues, receiveMode, samplingRate));
            rightFileName = RIGHT_CSV;
        }

        MeasurementRecord record = new MeasurementRecord(
                id,
                startTimeMillis,
                endTimeMillis,
                "",
                leftFileName,
                rightFileName,
                leftValues.size(),
                rightValues.size(),
                samplingRate == OrpheInsoleSamplingRate.hz100 ? "100Hz" : "200Hz",
                receiveModeLabel(receiveMode),
                System.currentTimeMillis()
        );

        List<MeasurementRecord> records = loadRecords();
        records.add(record);
        writeIndex(records);
        return record;
    }

    public boolean deleteRecord(String id) {
        boolean removed = false;
        List<MeasurementRecord> records = loadRecords();
        List<MeasurementRecord> kept = new ArrayList<>();
        for (MeasurementRecord record : records) {
            if (record.id.equals(id)) {
                removed = true;
            } else {
                kept.add(record);
            }
        }
        try {
            writeIndex(kept);
        } catch (IOException | JSONException ignored) {
            return false;
        }
        deleteRecursively(measurementDir(id));
        return removed;
    }

    public File getCsvFile(MeasurementRecord record, boolean left) {
        String fileName = left ? record.leftCsvFileName : record.rightCsvFileName;
        if (fileName == null || fileName.length() == 0 || "null".equals(fileName)) {
            return null;
        }
        File file = csvFile(record.id, fileName);
        return file.exists() ? file : null;
    }

    public List<Uri> getCsvUris(MeasurementRecord record) {
        List<Uri> uris = new ArrayList<>();
        File leftFile = getCsvFile(record, true);
        File rightFile = getCsvFile(record, false);
        if (leftFile != null) {
            uris.add(toContentUri(leftFile));
        }
        if (rightFile != null) {
            uris.add(toContentUri(rightFile));
        }
        return uris;
    }

    public String readCsvPreview(MeasurementRecord record, boolean left, int maxLines) throws IOException {
        File file = getCsvFile(record, left);
        if (file == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < maxLines) {
                if (count > 0) {
                    builder.append('\n');
                }
                builder.append(line);
                count++;
            }
        }
        return builder.toString();
    }

    public void copyCsvTo(MeasurementRecord record, boolean left, OutputStream outputStream) throws IOException {
        File file = getCsvFile(record, left);
        if (file == null) {
            throw new IOException("CSV file is not found.");
        }
        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        }
    }

    private Uri toContentUri(File file) {
        return FileProvider.getUriForFile(
                mContext,
                mContext.getPackageName() + ".fileprovider",
                file
        );
    }

    static String toCsv(
            List<OrpheInsoleValue> values,
            OrpheSensorReceiveMode receiveMode,
            OrpheInsoleSamplingRate samplingRate
    ) {
        final boolean includesQuaternion = includesQuaternion(receiveMode, samplingRate);
        StringBuilder builder = new StringBuilder();
        builder.append(CSV_HEADER_PREFIX);
        if (includesQuaternion) {
            builder.append(CSV_QUATERNION_HEADER);
        }
        builder.append(CSV_HEADER_SUFFIX);
        for (OrpheInsoleValue value : values) {
            builder.append('\n');
            appendCsvRow(builder, value, includesQuaternion);
        }
        return builder.toString();
    }

    private static boolean includesQuaternion(
            OrpheSensorReceiveMode receiveMode,
            OrpheInsoleSamplingRate samplingRate
    ) {
        return receiveMode == OrpheSensorReceiveMode.realtime
                && samplingRate == OrpheInsoleSamplingRate.hz100;
    }

    private static String receiveModeLabel(OrpheSensorReceiveMode receiveMode) {
        switch (receiveMode) {
            case request:
                return "Request";
            case fifo:
                return "FIFO";
            case realtime:
            default:
                return "Realtime";
        }
    }

    private static void appendCsvRow(
            StringBuilder builder,
            OrpheInsoleValue value,
            boolean includesQuaternion
    ) {
        appendCell(builder, String.format(Locale.US, "%.3f", value.startTime / 1000.0));
        appendCell(builder, Double.toString(value.accX));
        appendCell(builder, Double.toString(value.accY));
        appendCell(builder, Double.toString(value.accZ));
        appendCell(builder, Double.toString(value.gyroX));
        appendCell(builder, Double.toString(value.gyroY));
        appendCell(builder, Double.toString(value.gyroZ));
        if (includesQuaternion) {
            appendCell(builder, Double.toString(value.quatW));
            appendCell(builder, Double.toString(value.quatX));
            appendCell(builder, Double.toString(value.quatY));
            appendCell(builder, Double.toString(value.quatZ));
        }
        appendCell(builder, Double.toString(value.pressureToeOutside));
        appendCell(builder, Double.toString(value.pressureMidOutside));
        appendCell(builder, Double.toString(value.pressureToeInside));
        appendCell(builder, Double.toString(value.pressureCenter));
        appendCell(builder, Double.toString(value.pressureMidInside));
        appendCell(builder, Double.toString(value.pressureHeel));
        appendCell(builder, Integer.toString(value.serialNumber));
        appendCell(builder, Integer.toString(value.dataPosition));
        appendCell(builder, value.sidePosition.name());
        builder.append(Long.toString(value.receivedAt));
    }

    private static void appendCell(StringBuilder builder, String value) {
        builder.append(escape(value));
        builder.append(',');
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private JSONArray readIndex() {
        File file = indexFile();
        if (!file.exists()) {
            return new JSONArray();
        }
        try {
            return new JSONArray(readString(file));
        } catch (IOException | JSONException ignored) {
            return new JSONArray();
        }
    }

    private void writeIndex(List<MeasurementRecord> records) throws IOException, JSONException {
        Collections.sort(records, Comparator.comparingLong(record -> record.createdAtMillis));
        JSONArray array = new JSONArray();
        for (MeasurementRecord record : records) {
            array.put(record.toJson());
        }
        writeString(indexFile(), array.toString(2));
    }

    private File measurementsRoot() {
        return new File(mContext.getFilesDir(), MEASUREMENTS_DIR);
    }

    private File measurementDir(String id) {
        return new File(measurementsRoot(), id);
    }

    private File csvFile(String id, String fileName) {
        return new File(measurementDir(id), fileName);
    }

    private File indexFile() {
        File root = measurementsRoot();
        if (!root.exists()) {
            root.mkdirs();
        }
        return new File(root, INDEX_FILE);
    }

    private String readString(File file) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString();
    }

    private void writeString(File file, String content) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create directory: " + parent);
        }
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
