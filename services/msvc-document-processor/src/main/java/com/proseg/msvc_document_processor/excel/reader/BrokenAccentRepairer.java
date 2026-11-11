package com.proseg.msvc_document_processor.excel.reader;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class BrokenAccentRepairer {

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
    private static final Map<Character, Integer> WINDOWS_1252_EXTRA_BYTES = windows1252ExtraBytes();
    private static final Map<Character, Character> LOWERCASE_MARKERS = Map.of(
            '!', 'á', ')', 'é', '-', 'í', '3', 'ó', ':', 'ú', '1', 'ñ', '<', 'ü', '\'', 'ç'
    );
    private static final String DOUBLE_ENCODED_PREFIX = "b\u0008\u001A";
    private static final Map<Character, Character> DOUBLE_ENCODED_UPPERCASE_MARKERS = Map.of(
            ',', 'Ó', '+', 'Ñ', '$', 'É', '\'', 'Í', '6', 'Ú', '\u0005', 'Á'
    );
    private static final Map<Character, Character> UPPERCASE_MARKERS = Map.of(
            '\u0001', 'Á', '\u0011', 'Ñ', '\u0013', 'Ó', '\u001A', 'Ú', '\u001C', 'Ü'
    );
    private static final Map<Character, Character> PUNCTUATION_MARKERS = Map.of(
            '\u0013', '–', '\u0014', '—', '\u0018', '‘', '\u0019', '’', '\u001C', '“', '\u001D', '”'
    );
    private static final String WORD_INITIAL_MARKERS = "!3:)";
    private static final String CAPITAL_VOWELS = "ÉÍ";
    private static final int MAX_MOJIBAKE_PASSES = 3;
    private static final int MIN_EVIDENCE_CELLS = 3;
    private static final int MIN_EVIDENCE_CELLS_TO_GUESS = 10;
    private static final int MIN_RIGHT_FRAGMENT_TO_GUESS = 2;
    private static final Set<String> KNOWN_WORDS = Set.copyOf(List.of(
            "léase", "vehículo", "vehículos", "teléfono", "teléfonos", "préstamo", "préstamos", "día", "días",
            "académico", "académica", "académicos", "académicas", "jiménez", "pérez", "martínez", "rodríguez",
            "gutiérrez", "ramírez", "díaz", "también", "técnico", "técnica", "técnicos", "técnicas", "cédula",
            "después", "éste", "ésta", "éstos", "éxito", "mínimo", "mínima", "mínimos", "crédito", "vía", "vías",
            "energía", "categoría", "categorías", "compañía", "compañías", "policía", "ingeniería", "geografía",
            "biología", "ecología", "tecnología", "economía", "secretaría", "miércoles", "inglés", "francés",
            "país", "países", "comité", "café", "josé", "andrés", "víctor", "eléctrico", "eléctrica", "séptimo",
            "séptima", "décimo", "décima", "término", "términos", "además", "así", "aquí", "allí", "qué", "quién",
            "él", "sí", "mí", "recibí", "sería", "estaría", "habría", "podría", "debería", "tendría", "había",
            "tenía", "podía", "salía", "hacía", "asistía", "físico", "física", "químico", "química", "médico",
            "médica", "clínico", "clínica", "hídrico", "híbrido", "pérdida", "aéreo", "aérea", "histórico",
            "histórica", "artístico", "artística", "estadística", "estadístico", "logística", "logístico",
            "geográfico", "geográfica", "gráfico", "gráfica"
    ));

    private final boolean strippedUtf8Detected;
    private final boolean guessCapitalVowels;
    private final Set<String> vocabulary;

    private BrokenAccentRepairer(boolean strippedUtf8Detected, boolean guessCapitalVowels, Set<String> vocabulary) {
        this.strippedUtf8Detected = strippedUtf8Detected;
        this.guessCapitalVowels = guessCapitalVowels;
        this.vocabulary = vocabulary;
    }

    static BrokenAccentRepairer analyze(List<String> texts) {
        int evidenceCells = 0;
        Set<String> vocabulary = new HashSet<>();
        for (String text : texts) {
            String decoded = decodeUtf8ReadAsWindows1252(text);
            String withoutLostBits = decodeStrippedUtf8(decoded, null, false);
            if (!withoutLostBits.equals(decoded)) {
                evidenceCells++;
            }
            collectAccentedWords(withoutLostBits, vocabulary);
        }
        boolean detected = evidenceCells >= MIN_EVIDENCE_CELLS;
        return new BrokenAccentRepairer(detected, evidenceCells >= MIN_EVIDENCE_CELLS_TO_GUESS, vocabulary);
    }

    String repair(String raw) {
        if (raw == null) {
            return null;
        }
        String decoded = decodeUtf8ReadAsWindows1252(raw);
        if (!strippedUtf8Detected) {
            return decoded;
        }
        return decodeStrippedUtf8(decoded, this, guessCapitalVowels);
    }

    private static String decodeUtf8ReadAsWindows1252(String text) {
        String current = text;
        for (int pass = 0; pass < MAX_MOJIBAKE_PASSES; pass++) {
            String decoded = decodeMojibakeOnce(current);
            if (decoded.equals(current)) {
                break;
            }
            current = decoded;
        }
        return current;
    }

    private static String decodeMojibakeOnce(String text) {
        if (!containsMojibakeLead(text)) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        int index = 0;
        while (index < text.length()) {
            int consumed = isMojibakeLead(text.charAt(index)) ? appendMojibakeSequence(text, index, out) : 0;
            if (consumed == 0) {
                out.append(text.charAt(index));
                index++;
            } else {
                index += consumed;
            }
        }
        return out.toString();
    }

    private static boolean containsMojibakeLead(String text) {
        for (int index = 0; index < text.length(); index++) {
            if (isMojibakeLead(text.charAt(index))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMojibakeLead(char character) {
        return character == 'Â' || character == 'Ã' || character == 'â';
    }

    private static int appendMojibakeSequence(String text, int index, StringBuilder out) {
        char lead = text.charAt(index);
        int continuationCount = lead == 'â' ? 2 : 1;
        if (index + continuationCount >= text.length()) {
            return 0;
        }
        byte[] bytes = new byte[continuationCount + 1];
        bytes[0] = (byte) (lead == 'Â' ? 0xC2 : lead == 'Ã' ? 0xC3 : 0xE2);
        for (int offset = 1; offset <= continuationCount; offset++) {
            int value = continuationByte(text.charAt(index + offset));
            if (value < 0) {
                return 0;
            }
            bytes[offset] = (byte) value;
        }
        try {
            out.append(StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)));
            return continuationCount + 1;
        } catch (CharacterCodingException e) {
            return 0;
        }
    }

    private static int continuationByte(char character) {
        if (character >= 0x80 && character <= 0xBF) {
            return character;
        }
        Integer extra = WINDOWS_1252_EXTRA_BYTES.get(character);
        return extra == null ? -1 : extra;
    }

    private static Map<Character, Integer> windows1252ExtraBytes() {
        Map<Character, Integer> bytesByCharacter = new HashMap<>();
        for (int value = 0x80; value <= 0x9F; value++) {
            char decoded = new String(new byte[]{(byte) value}, WINDOWS_1252).charAt(0);
            if (decoded != '�' && decoded != value) {
                bytesByCharacter.put(decoded, value);
            }
        }
        return bytesByCharacter;
    }

    private static String decodeStrippedUtf8(String text, BrokenAccentRepairer resolver, boolean guessCapitalVowels) {
        if (!containsStrippedLead(text)) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        int index = 0;
        while (index < text.length()) {
            char current = text.charAt(index);
            int consumed = 0;
            if (current == 'C') {
                consumed = appendLatinAccent(text, index, out, resolver, guessCapitalVowels);
            } else if (current == 'B') {
                consumed = appendInvertedPunctuation(text, index, out);
            } else if (current == 'b') {
                consumed = appendDoubleEncodedUppercase(text, index, out);
                if (consumed == 0) {
                    consumed = appendTypographicPunctuation(text, index, out);
                }
            }
            if (consumed == 0) {
                out.append(current);
                index++;
            } else {
                index += consumed;
            }
        }
        return out.toString();
    }

    private static boolean containsStrippedLead(String text) {
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == 'C' || character == 'B' || character == 'b') {
                return true;
            }
        }
        return false;
    }

    private static int appendLatinAccent(String text, int index, StringBuilder out, BrokenAccentRepairer resolver,
                                         boolean guessCapitalVowels) {
        if (index + 1 >= text.length()) {
            return 0;
        }
        char marker = text.charAt(index + 1);
        Character uppercase = UPPERCASE_MARKERS.get(marker);
        if (uppercase != null) {
            out.append(uppercase);
            return 2;
        }
        Character lowercase = LOWERCASE_MARKERS.get(marker);
        if (lowercase != null && acceptsLowercaseMarker(text, index, out)) {
            out.append(lowercase);
            return 2;
        }
        if (marker == ' ' && resolver != null) {
            return appendCapitalVowel(text, index, out, resolver, guessCapitalVowels);
        }
        return 0;
    }

    private static boolean acceptsLowercaseMarker(String text, int index, StringBuilder out) {
        char previous = out.length() == 0 ? ' ' : out.charAt(out.length() - 1);
        char marker = text.charAt(index + 1);
        char next = charAt(text, index + 2);
        char afterNext = charAt(text, index + 3);
        boolean numericLookalike = (Character.isDigit(marker) || marker == '-') && Character.isDigit(next);
        if (numericLookalike) {
            return false;
        }
        if (Character.isLetter(previous)) {
            if (Character.isLowerCase(previous) || Character.isLowerCase(next)) {
                return true;
            }
            return next == 'C' && LOWERCASE_MARKERS.containsKey(afterNext);
        }
        return WORD_INITIAL_MARKERS.indexOf(marker) >= 0
                && !Character.isDigit(previous)
                && Character.isLowerCase(next)
                && Character.isLowerCase(afterNext);
    }

    private static int appendCapitalVowel(String text, int index, StringBuilder out, BrokenAccentRepairer resolver,
                                          boolean guessCapitalVowels) {
        if (!Character.isUpperCase(charAt(text, index + 2)) || out.length() == 0
                || !Character.isUpperCase(out.charAt(out.length() - 1))) {
            return 0;
        }
        String left = trailingUppercaseRun(out);
        String right = leadingUppercaseRun(text, index + 2);
        for (int position = 0; position < CAPITAL_VOWELS.length(); position++) {
            char vowel = CAPITAL_VOWELS.charAt(position);
            if (resolver.isKnownWord((left + vowel + right).toLowerCase(Locale.ROOT))) {
                out.append(vowel);
                return 2;
            }
        }
        if (guessCapitalVowels && right.length() >= MIN_RIGHT_FRAGMENT_TO_GUESS) {
            out.append(CAPITAL_VOWELS.charAt(0));
            return 2;
        }
        return 0;
    }

    private static String trailingUppercaseRun(StringBuilder out) {
        int start = out.length();
        while (start > 0 && Character.isUpperCase(out.charAt(start - 1))) {
            start--;
        }
        return out.substring(start);
    }

    private static String leadingUppercaseRun(String text, int start) {
        int end = start;
        while (end < text.length() && Character.isUpperCase(text.charAt(end))) {
            end++;
        }
        return text.substring(start, end);
    }

    private static int appendInvertedPunctuation(String text, int index, StringBuilder out) {
        if (index + 2 >= text.length() || !Character.isLetter(text.charAt(index + 2))) {
            return 0;
        }
        char previous = out.length() == 0 ? ' ' : out.charAt(out.length() - 1);
        if (Character.isLetterOrDigit(previous)) {
            return 0;
        }
        char marker = text.charAt(index + 1);
        if (marker == '?') {
            out.append('¿');
            return 2;
        }
        if (marker == '!') {
            out.append('¡');
            return 2;
        }
        return 0;
    }

    private static int appendDoubleEncodedUppercase(String text, int index, StringBuilder out) {
        if (!text.startsWith(DOUBLE_ENCODED_PREFIX, index)) {
            return 0;
        }
        int markerIndex = index + DOUBLE_ENCODED_PREFIX.length();
        if (charAt(text, markerIndex) != 'C') {
            return 0;
        }
        Character uppercase = DOUBLE_ENCODED_UPPERCASE_MARKERS.get(charAt(text, markerIndex + 1));
        if (uppercase == null) {
            return 0;
        }
        out.append(uppercase);
        return DOUBLE_ENCODED_PREFIX.length() + 2;
    }

    private static int appendTypographicPunctuation(String text, int index, StringBuilder out) {
        int markerIndex = index + 1;
        if (charAt(text, markerIndex) == '\u0000') {
            markerIndex++;
        }
        Character replacement = PUNCTUATION_MARKERS.get(charAt(text, markerIndex));
        if (replacement == null) {
            return 0;
        }
        out.append(replacement);
        return markerIndex - index + 1;
    }

    private static char charAt(String text, int index) {
        return index < text.length() ? text.charAt(index) : '\u0000';
    }

    private static void collectAccentedWords(String text, Set<String> vocabulary) {
        int start = -1;
        boolean accented = false;
        for (int index = 0; index <= text.length(); index++) {
            boolean letter = index < text.length() && Character.isLetter(text.charAt(index));
            if (letter) {
                if (start < 0) {
                    start = index;
                    accented = false;
                }
                accented = accented || isSpanishAccent(text.charAt(index));
            } else if (start >= 0) {
                if (accented) {
                    vocabulary.add(text.substring(start, index).toLowerCase(Locale.ROOT));
                }
                start = -1;
            }
        }
    }

    private static boolean isSpanishAccent(char character) {
        return "áéíóúñüÁÉÍÓÚÑÜ".indexOf(character) >= 0;
    }

    private boolean isKnownWord(String word) {
        return vocabulary.contains(word) || KNOWN_WORDS.contains(word);
    }
}
