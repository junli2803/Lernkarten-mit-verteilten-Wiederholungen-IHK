package controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javax.sql.DataSource;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import util.DB;

public class MainController {

    // injected by main.fxml
    @FXML private TabPane tabPane;
    @FXML private Tab reviewTab;
    // matches <fx:include fx:id="reviewView"> -> must be reviewViewController
    @FXML private ReviewController reviewViewController;

    @FXML
    public void initialize() {
        // when switching to Review tab, ask first
        if (tabPane != null && reviewTab != null) {
            tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab == reviewTab) {
                    if (reviewViewController != null && !reviewViewController.isSessionRunning()) {
                        boolean ok = reviewViewController.confirmStartTimer();
                        if (ok) {
                            reviewViewController.startSession();
                        } else {
                            // switch back to previous tab (use runLater to avoid recursive trigger)
                            if (oldTab != null) {
                                Platform.runLater(() -> tabPane.getSelectionModel().select(oldTab));
                            }
                        }
                    }
                }
            });
        }
    }

    @FXML
    private void showAbout() {
        String javaVer = System.getProperty("java.version");
        String fxVer = System.getProperty("javafx.runtime.version");
        if (fxVer == null) fxVer = System.getProperty("javafx.version");

        StringBuilder sb = new StringBuilder();
        sb.append("SRS FX (SM-2)\n")
                .append("Java: ").append(javaVer).append('\n')
                .append("JavaFX: ").append(fxVer == null ? "unknown" : fxVer).append('\n');

        try {
            DataSource ds = DB.getDataSource();
            // English table/column names
            int cards = count(ds, "SELECT COUNT(*) FROM card");
            int due = count(ds, "SELECT COUNT(*) FROM review_plan WHERE planned_on <= CURDATE()");
            sb.append("\nDB: OK").append('\n')
                    .append("Cards: ").append(cards).append('\n')
                    .append("Due (<= today): ").append(due).append('\n');
            infoDialog("About", sb.toString(), null);
        } catch (Exception ex) {
            sb.append("\nDB: FAILED").append('\n')
                    .append("Reason: ").append(ex.getMessage()).append('\n');
            infoDialog("About (with DB error)", sb.toString(), ex);
        }
    }

    @FXML
    private void exitApp() { Platform.exit(); }

    // ===== helpers =====
    private static int count(DataSource ds, String sql) throws Exception {
        try (Connection c = ds.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static void infoDialog(String title, String content, Throwable t) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);

        if (t == null) {
            alert.setContentText(content);
            alert.showAndWait();
            return;
        }

        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        TextArea area = new TextArea(content + "\n\nStacktrace:\n" + sw);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(18);
        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }
}
