package controller;

import javafx.fxml.FXML;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import model.Card;

import java.time.LocalDate;

public class AddCardDialogController {
    @FXML private DialogPane root;
    @FXML private TextField questionField;
    @FXML private TextArea answerArea;

    public Card getResult() {
        return new Card(questionField.getText(), answerArea.getText(), LocalDate.now());
    }
}

