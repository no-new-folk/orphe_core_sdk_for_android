package io.orphe.orphecoresdk;

import java.util.UUID;

public class GattUUIDDefine {
    public static final UUID UUID_DESC_CLIENT_CHAR_CONFIG = uuidFromShortString("2902");

    public static final UUID UUID_SERVICE_ORPHE_INFORMATION
            = UUID.fromString("01a9d6b5-ff6e-444a-b266-0be75e85c064");
    public static final UUID UUID_CHAR_ORPHE_DEVICE_INFORMATION
            = UUID.fromString("24354f22-1c46-430e-a4ab-a1eeabbcdfc0");

    public static final UUID UUID_SERVICE_ORPHE_OTHER_SERVICE
            = UUID.fromString("db1b7aca-cda5-4453-a49b-33a53d3f0833");
    public static final UUID UUID_CHAR_ORPHE_SENSOR_VALUES
            = UUID.fromString("f3f9c7ce-46ee-4205-89ac-abe64e626c0f");
    public static final UUID UUID_CHAR_ORPHE_REALTIME_ANALYSIS
            = UUID.fromString("adb7eb5a-ac8a-4f95-907b-45db4a71b45a");
    public static final UUID UUID_CHAR_ORPHE_STEP_ANALYSIS
            = UUID.fromString("4eb776dc-cf99-4af7-b2d3-ad0f791a79dd");

    private static UUID uuidFromShortString(String uuid) {
        return UUID.fromString(String.format("0000%s-0000-1000-8000-00805f9b34fb", uuid));
    }
}
