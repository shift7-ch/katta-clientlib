/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto.uvf;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WordEncoderTest {

    @Test
    void encodePaddedRequiresMultipleOfThree() {
        assertThrows(IllegalArgumentException.class, () -> new WordEncoder().encodePadded(new byte[]{0x00, 0x01}));
    }

    @Test
    void encodeDecode() {
        final WordEncoder encoder = new WordEncoder();
        final byte[] input = "Encode me to words".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(input, encoder.decode(encoder.encodePadded(input)));
    }

    @Test
    void encodeZeroes() {
        final WordEncoder encoder = new WordEncoder();
        // First word of the dictionary for every 12 bits set to zero
        assertEquals("ad ad ad ad", encoder.encodePadded(new byte[6]));
        assertArrayEquals(new byte[6], encoder.decode("ad ad ad ad"));
    }

    @Test
    void decodeUnknownWord() {
        assertThrows(IllegalArgumentException.class, () -> new WordEncoder().decode("hallo bonjour"));
    }

    @Test
    void decodeOddNumberOfWords() {
        assertThrows(IllegalArgumentException.class, () -> new WordEncoder().decode("ad ad ad"));
    }

    @Test
    void decodeIgnoresSuperfluousWhitespace() {
        final WordEncoder encoder = new WordEncoder();
        assertArrayEquals(encoder.decode("ad ad ad ad"), encoder.decode("  ad  ad\nad\tad "));
    }

    @Test
    void dictionaryMustBeComplete() {
        assertThrows(IllegalArgumentException.class, () -> new WordEncoder(Arrays.asList("ad", "ah")));
    }
}
