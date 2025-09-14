package util;

import org.apache.commons.csv.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;

public final class CsvPretty {
    private CsvPretty() {}

    public static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    public static CSVPrinter openWriter(File file, String... headers) throws IOException {
        OutputStream out = new FileOutputStream(file);

        out.write(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF});
        Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8);

        CSVFormat fmt = CSVFormat.DEFAULT.builder()
                .setHeader(headers)
                .setRecordSeparator("\r\n")
                .setTrim(true)
                .setQuoteMode(QuoteMode.MINIMAL)
                .build();
        return new CSVPrinter(w, fmt);
    }

    public static CSVParser openReader(File file) throws IOException {
        Reader r = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
        CSVFormat fmt = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build();
        return new CSVParser(r, fmt);
    }


    public static String sanitizeText(String s) {
        if (s == null) return "";
        return s.replace("\r", "").replace("\n", "\\n").trim();
    }

    public static String fmtTs(LocalDateTime ldt) {
        return ldt == null ? "" : DTF.format(ldt);
    }
    public static String fmtTs(LocalDate ld) {
        return ld == null ? "" : DTF.format(ld.atStartOfDay());
    }

    public static LocalDateTime parseLdt(String v) {
        if (v == null || v.isBlank()) return null;
        String s = v.trim();
        try { return LocalDateTime.parse(s, DTF); } catch (Exception ignore) {}
        try { return LocalDateTime.parse(s); } catch (Exception ignore) {}
        try { return LocalDate.parse(s).atStartOfDay(); } catch (Exception ignore) {}
        return null;
    }

    public static Integer toInt(String s) {
        if (s == null || s.isBlank()) return null;
        return Integer.valueOf(s.trim());
    }
    public static Double toDouble(String s) {
        if (s == null || s.isBlank()) return null;
        return Double.valueOf(s.trim());
    }
    public static Boolean toBool(String s) {
        if (s == null) return null;
        String v = s.trim().toLowerCase();
        if (v.equals("true") || v.equals("1") || v.equals("yes")) return true;
        if (v.equals("false") || v.equals("0") || v.equals("no")) return false;
        return null;
    }
}
