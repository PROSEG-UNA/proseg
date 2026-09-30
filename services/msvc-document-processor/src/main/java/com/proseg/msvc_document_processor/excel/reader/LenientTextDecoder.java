package com.proseg.msvc_document_processor.excel.reader;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public final class LenientTextDecoder {

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final byte[] UTF16_LE_BOM = {(byte) 0xFF, (byte) 0xFE};
    private static final byte[] UTF16_BE_BOM = {(byte) 0xFE, (byte) 0xFF};

    private LenientTextDecoder() {
    }

    public static String decode(byte[] content) {
        if (startsWith(content, UTF8_BOM)) {
            return new String(content, UTF8_BOM.length, content.length - UTF8_BOM.length, StandardCharsets.UTF_8);
        }
        if (startsWith(content, UTF16_LE_BOM)) {
            return new String(content, UTF16_LE_BOM.length, content.length - UTF16_LE_BOM.length, StandardCharsets.UTF_16LE);
        }
        if (startsWith(content, UTF16_BE_BOM)) {
            return new String(content, UTF16_BE_BOM.length, content.length - UTF16_BE_BOM.length, StandardCharsets.UTF_16BE);
        }
        return decodeUtf8WithWindows1252Fallback(content);
    }

    static boolean startsWith(byte[] content, byte[] prefix) {
        if (content.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (content[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static String decodeUtf8WithWindows1252Fallback(byte[] content) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer input = ByteBuffer.wrap(content);
        CharBuffer output = CharBuffer.allocate(content.length + 16);
        while (true) {
            CoderResult result = decoder.decode(input, output, true);
            if (result.isUnderflow()) {
                decoder.flush(output);
                break;
            }
            if (result.isOverflow()) {
                output = grow(output);
                continue;
            }
            byte[] invalidBytes = new byte[result.length()];
            input.get(invalidBytes);
            String fallback = new String(invalidBytes, WINDOWS_1252);
            if (output.remaining() < fallback.length()) {
                output = grow(output);
            }
            output.put(fallback);
        }
        output.flip();
        return output.toString();
    }

    private static CharBuffer grow(CharBuffer buffer) {
        CharBuffer bigger = CharBuffer.allocate(buffer.capacity() * 2 + 16);
        buffer.flip();
        bigger.put(buffer);
        return bigger;
    }
}
