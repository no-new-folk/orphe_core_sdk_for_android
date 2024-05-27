package io.orphe.orphecoresdkforandroid;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class DeviceInfoValue {

  public DeviceInfoValue(
          final OrpheBatteryStatus batteryStatus,
    final OrpheSidePosition sidePosition,
    final OrpheLogRecordingMode logRecordingMode,
    final boolean autoRecording,
    final OrpheLedBrightness ledBrightness,
    final int logDuration,
    final OrpheAccRange accRange,
    final OrpheGyroRange gyroRange
                     ){
      this.batteryStatus = batteryStatus;
      this.sidePosition = sidePosition;
      this.logRecordingMode = logRecordingMode;
      this.autoRecording = autoRecording;
      this.ledBrightness = ledBrightness;
      this.logDuration = logDuration;
      this.accRange = accRange;
      this.gyroRange = gyroRange;
  }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public static DeviceInfoValue fromBytes(
            byte[] bytes) throws Exception {
        return new DeviceInfoValue(
                OrpheBatteryStatus.fromValue(getUint8(bytes, 0)),
                OrpheSidePosition.fromValue(getUint8(bytes, 1)),
                OrpheLogRecordingMode.fromValue(getUint8(bytes, 2)),
                getUint8(bytes, 3) == 1,
                OrpheLedBrightness.fromValue(getUint8(bytes, 4)),
                getUint16(bytes, 6),
                OrpheAccRange.fromValue(getUint8(bytes, 8)),
                OrpheGyroRange.fromValue(getUint8(bytes, 9))
        );
    }

    final OrpheBatteryStatus batteryStatus;
    final OrpheSidePosition sidePosition;
    final OrpheLogRecordingMode logRecordingMode;
    final boolean autoRecording;
    final OrpheLedBrightness ledBrightness;
    final int logDuration;
    final OrpheAccRange accRange;
    final OrpheGyroRange gyroRange;


    private static int parseInt(@NonNull byte[] bytes,  int index) {
        return (int)((getInt8(bytes, index) << 8) + getInt8(bytes, index + 1));
    }

    private static short getUint16(@NonNull byte[] data, int index) {
        return (short) (((data[index] & 0xFF) << 8) | (data[index + 1] & 0xFF));
    }

    private static byte getUint8(@NonNull byte[] data, int index) {
        return (byte) (data[index] & 0xFF);
    }
    private static byte getInt8(@NonNull byte[] data, int index) {
        return (byte) data[index];
    }
}
