package io.orphe.orphecoresdk;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OrpheByteParserTest {
    @Test
    public void parsesBigEndianSignedInt16WithoutSigningTheLowByte() {
        assertEquals(255, OrpheByteParser.getInt16BigEndian(
                new byte[]{0x00, (byte) 0xFF}, 0));
        assertEquals(-256, OrpheByteParser.getInt16BigEndian(
                new byte[]{(byte) 0xFF, 0x00}, 0));
        assertEquals(32767, OrpheByteParser.getInt16BigEndian(
                new byte[]{0x7F, (byte) 0xFF}, 0));
        assertEquals(-32768, OrpheByteParser.getInt16BigEndian(
                new byte[]{(byte) 0x80, 0x00}, 0));
    }
}
