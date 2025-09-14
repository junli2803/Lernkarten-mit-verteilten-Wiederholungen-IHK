package app;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class MainApp extends Application {
    @FXML
    private void noop(ActionEvent e) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("SRSC FX");
        alert.setContentText("Spaced Repetition System (JavaFX).");
        alert.showAndWait();
    }
@Override
public void start(Stage stage) throws Exception {
    var fxml = Objects.requireNonNull(
            getClass().getResource("/view/main.fxml"),
            "Cannot find /view/main.fxml (please place it in src/main/resources/view)"
    );
    Scene scene = new Scene(new FXMLLoader(fxml).load());
    var iconUrl = getClass().getResource("/logo.png");
    if (iconUrl != null) stage.getIcons().add(new Image(iconUrl.toExternalForm()));

    stage.setTitle("Spaced Repetition Study Cards FX");
    stage.sizeToScene();
    stage.setScene(scene);
    stage.show();
}

    public static void main(String[] args) { launch(args); }
}
