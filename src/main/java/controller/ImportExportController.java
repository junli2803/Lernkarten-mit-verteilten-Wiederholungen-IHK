package controller;

import dao.CardDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.DirectoryChooser;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import util.CsvPretty;
import util.DB;

import javax.sql.DataSource;
import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** CSV import/export (3 tables -> 3 CSV files) */
public class ImportExportController {
    @FXML private Label status;
    @FXML private TextArea logArea;

    private final CardDAO cardDAO = new CardDAO();

    // ---- table & file names (keep them consistent) ----
    private static final String T_CARD = "card";
    private static final String T_PLAN = "review_plan";
    private static final String T_STAT = "review_statistic"; // <-- singular

    private static final String F_CARDS = "cards.csv";
    private static final String F_PLAN  = "review_plan.csv";
    private static final String F_STAT  = "review_statistic.csv"; // <-- singular

    @FXML
    public void initialize() {
        if (status != null) status.setText("");
    }

    @FXML
    private void onExport() {
        try {
            // 1) Let user pick a base file name
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Export: choose a base file name (a folder with 3 CSVs will be created)");
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV", "*.csv"));
            String ts = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                    .format(java.time.LocalDateTime.now());
            fc.setInitialFileName("srs_export_" + ts + ".csv");

            File picked = fc.showSaveDialog(status.getScene().getWindow());
            if (picked == null) return;

            // 2) Create a subfolder to hold 3 CSVs
            String baseName = stripExt(picked.getName());
            File outDir = new File(picked.getParentFile(), baseName);
            if (!outDir.exists() && !outDir.mkdir()) {
                throw new java.io.IOException("Cannot create export folder: " + outDir.getAbsolutePath());
            }

            File fCards = new File(outDir, F_CARDS);
            File fPlan  = new File(outDir, F_PLAN);
            File fStats = new File(outDir, F_STAT);

            // 3) Export cards
            List<model.Card> cards = cardDAO.findAll();
            try (CSVPrinter p = CsvPretty.openWriter(fCards, "id","question","answer","created_at")) {
                for (model.Card k : cards) {
                    p.printRecord(
                            k.getId(),
                            CsvPretty.sanitizeText(k.getQuestion()),
                            CsvPretty.sanitizeText(k.getAnswer()),
                            fmtAnyDate(k.getCreatedAt())
                    );
                }
            }

            // 4) Export plan & statistic
            DataSource ds = DB.getDataSource();
            try (Connection c = ds.getConnection()) {
                // review_plan
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery(
                             "SELECT id, card_id, planned_on, reviewed_on, rating, interval_days, repeats, ease_factor " +
                                     "FROM " + T_PLAN + " ORDER BY id");
                     CSVPrinter p = CsvPretty.openWriter(fPlan,
                             "id","card_id","planned_on","reviewed_on","rating","interval_days","repeats","ease_factor")) {
                    while (rs.next()) {
                        Integer rating = intObj(rs, "rating");
                        p.printRecord(
                                rs.getInt("id"),
                                rs.getInt("card_id"),
                                CsvPretty.fmtTs(ts(rs, "planned_on")),
                                CsvPretty.fmtTs(ts(rs, "reviewed_on")),
                                rating == null ? "" : rating,
                                rs.getInt("interval_days"),
                                rs.getInt("repeats"),
                                String.format(java.util.Locale.ROOT, "%.2f", rs.getDouble("ease_factor"))
                        );
                    }
                }
                // review_statistic (singular)
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery(
                             "SELECT id, card_id, reviewed_at, duration_ms, correct, rating, notes " +
                                     "FROM " + T_STAT + " ORDER BY id");
                     CSVPrinter p = CsvPretty.openWriter(fStats,
                             "id","card_id","reviewed_at","duration_ms","correct","rating","notes")) {
                    while (rs.next()) {
                        p.printRecord(
                                rs.getInt("id"),
                                rs.getInt("card_id"),
                                CsvPretty.fmtTs(ts(rs, "reviewed_at")),
                                rs.getInt("duration_ms"),
                                rs.getBoolean("correct"),
                                rs.getInt("rating"),
                                CsvPretty.sanitizeText(rs.getString("notes"))
                        );
                    }
                }
            }

            status.setText("Export succeeded (created folder '" + outDir.getName() + "' with 3 CSV files)");
            log("Export folder: " + outDir.getAbsolutePath());
        } catch (Exception ex) {
            status.setText("Export failed");
            showErr("Export failed", ex);
        }
    }

    private static String stripExt(String name) {
        int i = name.lastIndexOf('.');
        return (i > 0) ? name.substring(0, i) : name;
    }

    @FXML
    private void onImport() {
        log(">>> onImport()");
        try {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Choose a folder that contains 3 CSV files");
            File dir = dc.showDialog(status.getScene().getWindow());
            if (dir == null) return;

            File fCards = new File(dir, F_CARDS);
            File fPlan  = new File(dir, F_PLAN);
            File fStats = new File(dir, F_STAT);
            if (!fCards.exists() || !fPlan.exists() || !fStats.exists()) {
                throw new IllegalArgumentException("Folder must contain: " + F_CARDS + ", " + F_PLAN + ", " + F_STAT);
            }

            boolean ok = new Alert(Alert.AlertType.CONFIRMATION,
                    "Import will ERASE all current data and restore from CSV. Continue?")
                    .showAndWait()
                    .orElse(javafx.scene.control.ButtonType.CANCEL)
                    == javafx.scene.control.ButtonType.OK;
            if (!ok) return;

            DataSource ds = DB.getDataSource();
            try (Connection c = ds.getConnection()) {
                c.setAutoCommit(false);
                try (Statement st = c.createStatement()) {
                    st.execute("SET FOREIGN_KEY_CHECKS=0");
                    st.execute("TRUNCATE TABLE " + T_STAT);
                    st.execute("TRUNCATE TABLE " + T_PLAN);
                    st.execute("TRUNCATE TABLE " + T_CARD);
                    st.execute("SET FOREIGN_KEY_CHECKS=1");
                }

                // cards
                try (CSVParser parser = CsvPretty.openReader(fCards);
                     PreparedStatement ps = c.prepareStatement(
                             "INSERT INTO " + T_CARD + " (id, question, answer, created_at) VALUES (?,?,?,?)")) {
                    for (CSVRecord r : parser) {
                        ps.setInt(1, Integer.parseInt(r.get("id")));
                        ps.setString(2, emptyToNull(unsanitize(r.get("question"))));
                        ps.setString(3, emptyToNull(unsanitize(r.get("answer"))));
                        setTsOrNull(ps, 4, r.get("created_at"));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                // review_plan
                try (CSVParser parser = CsvPretty.openReader(fPlan);
                     PreparedStatement ps = c.prepareStatement(
                             "INSERT INTO " + T_PLAN + " (id, card_id, planned_on, reviewed_on, rating, interval_days, repeats, ease_factor) " +
                                     "VALUES (?,?,?,?,?,?,?,?)")) {
                    for (CSVRecord r : parser) {
                        ps.setInt(1, Integer.parseInt(r.get("id")));
                        ps.setInt(2, Integer.parseInt(r.get("card_id")));
                        setTsOrNull(ps, 3, r.get("planned_on"));
                        setTsOrNull(ps, 4, r.get("reviewed_on"));

                        String rating = r.get("rating");
                        if (rating == null || rating.isBlank()) ps.setNull(5, Types.INTEGER);
                        else ps.setInt(5, Integer.parseInt(rating.trim()));

                        ps.setInt(6, Integer.parseInt(zeroIfEmpty(r.get("interval_days"))));
                        ps.setInt(7, Integer.parseInt(zeroIfEmpty(r.get("repeats"))));
                        String ef = r.get("ease_factor");
                        ps.setDouble(8, (ef == null || ef.isBlank()) ? 0.0 : Double.parseDouble(ef.trim()));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                // review_statistic (singular)
                try (CSVParser parser = CsvPretty.openReader(fStats);
                     PreparedStatement ps = c.prepareStatement(
                             "INSERT INTO " + T_STAT + " (id, card_id, reviewed_at, duration_ms, correct, rating, notes) " +
                                     "VALUES (?,?,?,?,?,?,?)")) {
                    for (CSVRecord r : parser) {
                        ps.setInt(1, Integer.parseInt(r.get("id")));
                        ps.setInt(2, Integer.parseInt(r.get("card_id")));
                        setTsOrNow(ps, 3, r.get("reviewed_at"));
                        ps.setInt(4, Integer.parseInt(zeroIfEmpty(r.get("duration_ms"))));
                        ps.setBoolean(5, toBool(r.get("correct")));
                        ps.setInt(6, Integer.parseInt(zeroIfEmpty(r.get("rating"))));
                        ps.setString(7, emptyToNull(unsanitize(r.get("notes"))));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                c.commit();
            }

            status.setText("Import succeeded");
            log("Import folder: " + dir.getAbsolutePath());
        } catch (Exception ex) {
            status.setText("Import failed");
            showErr("Import failed", ex);
        }
    }

    // ===== helpers =====
    private void log(String msg) { if (logArea != null) logArea.appendText(msg + "\n"); }

    private void showErr(String title, Exception ex) {
        Alert a = new Alert(Alert.AlertType.ERROR, ex.getMessage());
        a.setHeaderText(title);
        a.showAndWait();
        log("ERROR: " + ex);
    }

    private static String unsanitize(String s) { return s == null ? null : s.replace("\\n", "\n"); }
    private static String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s; }
    private static String zeroIfEmpty(String s) { return (s == null || s.isBlank()) ? "0" : s.trim(); }
    private static boolean toBool(String s) { Boolean v = CsvPretty.toBool(s); return v != null && v; }

    private static LocalDateTime ts(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime();
    }
    private static Integer intObj(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    /** format any date-like object to "yyyy-MM-dd HH:mm:ss" */
    private static String fmtAnyDate(Object any) {
        if (any == null) return "";
        if (any instanceof LocalDateTime) return CsvPretty.fmtTs((LocalDateTime) any);
        if (any instanceof LocalDate)     return CsvPretty.fmtTs((LocalDate) any);
        if (any instanceof Timestamp)     return CsvPretty.fmtTs(((Timestamp) any).toLocalDateTime());
        if (any instanceof java.sql.Date) return CsvPretty.fmtTs(((java.sql.Date) any).toLocalDate().atStartOfDay());
        return any.toString();
    }

    /** v empty -> NULL; supports date/datetime */
    private static void setTsOrNull(PreparedStatement ps, int idx, String v) throws SQLException {
        LocalDateTime ldt = CsvPretty.parseLdt(v);
        if (ldt == null) ps.setNull(idx, Types.TIMESTAMP);
        else ps.setTimestamp(idx, Timestamp.valueOf(ldt));
    }

    /** v empty -> NOW; supports date/datetime */
    private static void setTsOrNow(PreparedStatement ps, int idx, String v) throws SQLException {
        LocalDateTime ldt = CsvPretty.parseLdt(v);
        if (ldt == null) ldt = LocalDateTime.now();
        ps.setTimestamp(idx, Timestamp.valueOf(ldt));
    }
}
