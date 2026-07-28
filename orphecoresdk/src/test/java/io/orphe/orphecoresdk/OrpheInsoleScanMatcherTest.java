package io.orphe.orphecoresdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OrpheInsoleScanMatcherTest {
    private static final String ADDRESS = "AA:BB:CC:DD:EE:FF";

    /** 旧世代のmanufacturer data。[0]=1,[5]=1がシグネチャ、[1-4]がID、[6]が左右、[14]が充電ステータス。 */
    private static byte[] legacyManufacturerData(final int side, final int chargeStatus) {
        final byte[] data = new byte[15];
        data[0] = 1;
        data[1] = 0x12;
        data[2] = 0x34;
        data[3] = 0x56;
        data[4] = 0x78;
        data[5] = 1;
        data[6] = (byte) side;
        data[14] = (byte) chargeStatus;
        return data;
    }

    @Test
    public void matchesLegacyManufacturerDataForLeft() {
        final OrpheInsoleScanMatcher.Match match =
                OrpheInsoleScanMatcher.match(legacyManufacturerData(0, 32), null, ADDRESS);
        assertNotNull(match);
        assertEquals(OrpheInsoleScanMatcher.Source.manufacturerData, match.source);
        assertEquals("IN12345678L", match.deviceId);
        assertEquals(OrpheSide.left, match.side);
        assertEquals(OrpheInsoleChargeStatus.wired, match.chargeStatus);
        assertFalse(match.isCore);
    }

    @Test
    public void matchesLegacyManufacturerDataForRight() {
        final OrpheInsoleScanMatcher.Match match =
                OrpheInsoleScanMatcher.match(legacyManufacturerData(1, 16), null, ADDRESS);
        assertNotNull(match);
        assertEquals("IN12345678R", match.deviceId);
        assertEquals(OrpheSide.right, match.side);
        assertEquals(OrpheInsoleChargeStatus.wireless, match.chargeStatus);
    }

    @Test
    public void matchesInsoleByAdvertisedNameWithoutManufacturerData() {
        final OrpheInsoleScanMatcher.Match match =
                OrpheInsoleScanMatcher.match(null, "INS0A1B2C3D", ADDRESS);
        assertNotNull(match);
        assertEquals(OrpheInsoleScanMatcher.Source.deviceName, match.source);
        assertEquals("INS0A1B2C3D", match.deviceId);
        assertNull(match.side);
        assertNull(match.chargeStatus);
        assertFalse(match.isCore);
    }

    /** Company IDが異なりレイアウトも一致しない場合でも、アドバタイズ名で判定できること。 */
    @Test
    public void matchesInsoleByAdvertisedNameWhenManufacturerDataIsUnexpected() {
        final byte[] unexpected = new byte[]{(byte) 0x99, 0x01, 0x02};
        final OrpheInsoleScanMatcher.Match match =
                OrpheInsoleScanMatcher.match(unexpected, "INS0A1B2C3D", ADDRESS);
        assertNotNull(match);
        assertEquals(OrpheInsoleScanMatcher.Source.deviceName, match.source);
        assertEquals("INS0A1B2C3D", match.deviceId);
        assertNull(match.side);
    }

    /** manufacturer dataが15バイト未満でもシグネチャが一致すれば判定できること。 */
    @Test
    public void matchesShortManufacturerDataWithoutChargeStatus() {
        final byte[] data = new byte[]{1, 0x12, 0x34, 0x56, 0x78, 1, 1};
        final OrpheInsoleScanMatcher.Match match =
                OrpheInsoleScanMatcher.match(data, null, ADDRESS);
        assertNotNull(match);
        assertEquals(OrpheInsoleScanMatcher.Source.manufacturerData, match.source);
        assertEquals("IN12345678R", match.deviceId);
        assertEquals(OrpheSide.right, match.side);
        assertNull(match.chargeStatus);
    }

    @Test
    public void parsesSideFromAdvertisedNameSuffix() {
        final OrpheInsoleScanMatcher.Match left =
                OrpheInsoleScanMatcher.match(null, "INS0A1B2C3L", ADDRESS);
        assertNotNull(left);
        assertEquals(OrpheSide.left, left.side);

        final OrpheInsoleScanMatcher.Match right =
                OrpheInsoleScanMatcher.match(null, "INS0A1B2C3R", ADDRESS);
        assertNotNull(right);
        assertEquals(OrpheSide.right, right.side);
    }

    /** manufacturer dataが取れる場合はそちらのIDを優先し、圧力補正の保存キーを揺らさないこと。 */
    @Test
    public void prefersManufacturerDataDeviceIdWhenNameAlsoMatches() {
        final OrpheInsoleScanMatcher.Match match = OrpheInsoleScanMatcher.match(
                legacyManufacturerData(0, 0), "INS0A1B2C3D", ADDRESS);
        assertNotNull(match);
        assertEquals("IN12345678L", match.deviceId);
        assertEquals(OrpheSide.left, match.side);
    }

    @Test
    public void matchesCoreByDeviceName() {
        final OrpheInsoleScanMatcher.Match match =
                OrpheInsoleScanMatcher.match(null, "CR-1234", ADDRESS);
        assertNotNull(match);
        assertTrue(match.isCore);
        assertEquals("CR-1234", match.deviceId);
        assertNull(match.side);
    }

    @Test
    public void doesNotMatchWhenNeitherNameNorManufacturerDataMatches() {
        assertNull(OrpheInsoleScanMatcher.match(null, null, ADDRESS));
        assertNull(OrpheInsoleScanMatcher.match(
                new byte[]{0x00, 0x01, 0x02}, "OtherDevice", ADDRESS));
    }

    /** containsではなくstartsWithで判定するため、名前の途中に"INS"を含むだけでは一致しないこと。 */
    @Test
    public void doesNotMatchWhenInsolePrefixIsNotAtTheHead() {
        assertNull(OrpheInsoleScanMatcher.match(null, "MYINSOLE", ADDRESS));
    }

    @Test
    public void skipsInsoleNameMatchWhenDisabled() {
        assertNull(OrpheInsoleScanMatcher.match(null, "INS0A1B2C3D", ADDRESS, false));
        // 名前判定を無効にしてもmanufacturer data経路とCORE判定は維持されること。
        assertNotNull(OrpheInsoleScanMatcher.match(
                legacyManufacturerData(0, 0), "INS0A1B2C3D", ADDRESS, false));
        assertNotNull(OrpheInsoleScanMatcher.match(null, "CR-1234", ADDRESS, false));
    }

    @Test
    public void fallsBackToAddressWhenAdvertisedNameIsEmpty() {
        // 空文字はstartsWithに一致しないため、名前が空の機体はmanufacturer dataでのみ判定されます。
        assertNull(OrpheInsoleScanMatcher.match(null, "", ADDRESS));
    }
}
