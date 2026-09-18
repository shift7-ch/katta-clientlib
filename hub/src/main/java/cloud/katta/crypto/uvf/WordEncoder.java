/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto.uvf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Encodes binary data as a sequence of words from a fixed dictionary of {@value #WORD_COUNT} words. Every
 * word represents 12 bits, hence three bytes of input are encoded as two words.
 *
 * @see <a href="https://github.com/shift7-ch/katta-server/blob/develop/frontend/src/common/util.ts">Reference implementation in katta-server</a>
 */
public final class WordEncoder {

    /**
     * Number of words in the dictionary. Every word encodes 12 bits.
     */
    public static final int WORD_COUNT = 4096;

    /**
     * Separator between words in the encoded representation.
     */
    public static final String DELIMITER = " ";

    /**
     * Dictionary shared with the web frontend and Cryptomator.
     */
    private static final String WORD_FILE = "4096words_en.txt";

    private final List<String> words;
    private final Map<String, Integer> indices;

    public WordEncoder() {
        this(WordEncoder.read());
    }

    public WordEncoder(final List<String> words) {
        if(words.size() != WORD_COUNT) {
            throw new IllegalArgumentException(String.format("Dictionary must contain %d words", WORD_COUNT));
        }
        this.words = words;
        this.indices = new HashMap<>(words.size());
        for(int i = 0; i < words.size(); i++) {
            indices.put(words.get(i), i);
        }
    }

    private static List<String> read() {
        try (InputStream in = WordEncoder.class.getResourceAsStream(WORD_FILE)) {
            if(null == in) {
                throw new UncheckedIOException(new IOException(String.format("Missing resource %s", WORD_FILE)));
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines().filter(line -> !line.isEmpty()).collect(Collectors.toList());
            }
        }
        catch(IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Encodes the input as a sequence of words. Three bytes of input are mapped to two words of 12 bits each.
     *
     * @param input Data padded to a multiple of three bytes
     * @return Words separated by {@value #DELIMITER}
     * @throws IllegalArgumentException If the length of the input is not a multiple of three
     */
    public String encodePadded(final byte[] input) {
        if(input.length % 3 != 0) {
            throw new IllegalArgumentException("Input needs to be padded to a multiple of three");
        }
        final StringJoiner encoded = new StringJoiner(DELIMITER);
        for(int i = 0; i < input.length; i += 3) {
            final int b1 = Byte.toUnsignedInt(input[i]);
            final int b2 = Byte.toUnsignedInt(input[i + 1]);
            final int b3 = Byte.toUnsignedInt(input[i + 2]);
            final int firstWordIndex = (0xff0 & (b1 << 4)) + (0x00f & (b2 >> 4)); // 0xFFF000
            final int secondWordIndex = (0xf00 & (b2 << 8)) + (0x0ff & b3); // 0x000FFF
            encoded.add(words.get(firstWordIndex));
            encoded.add(words.get(secondWordIndex));
        }
        return encoded.toString();
    }

    /**
     * Decodes a sequence of words previously encoded with {@link #encodePadded(byte[])}. The returned data still
     * contains the padding added prior to encoding.
     *
     * @param encoded Words separated by whitespace
     * @return Padded data
     * @throws IllegalArgumentException If the number of words is odd or a word is not found in the dictionary
     */
    public byte[] decode(final String encoded) {
        final String[] split = encoded.trim().split("\\s+");
        if(split.length % 2 != 0) {
            throw new IllegalArgumentException(String.format("Input needs to be a multiple of two words: \"%s\"", encoded));
        }
        final byte[] decoded = new byte[split.length / 2 * 3];
        for(int i = 0; i < split.length; i += 2) {
            final Integer firstWordIndex = indices.get(split[i]);
            final Integer secondWordIndex = indices.get(split[i + 1]);
            if(null == firstWordIndex || null == secondWordIndex) {
                throw new IllegalArgumentException(String.format("Can't decode \"%s %s\". Word not in dictionary", split[i], split[i + 1]));
            }
            decoded[i / 2 * 3] = (byte) (0xff & (firstWordIndex >> 4));
            decoded[i / 2 * 3 + 1] = (byte) ((0xf0 & (firstWordIndex << 4)) + (0x0f & (secondWordIndex >> 8)));
            decoded[i / 2 * 3 + 2] = (byte) (0xff & secondWordIndex);
        }
        return decoded;
    }
}
