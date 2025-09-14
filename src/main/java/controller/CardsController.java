package controller;

import dao.CardDAO;
import dao.ReviewPlanDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import model.Card;

import java.io.IOException;
import java.time.LocalDate;

public class CardsController {
    private final CardDAO cardDAO = new CardDAO();
    private final ReviewPlanDAO reviewPlanDAO = new ReviewPlanDAO();

    @FXML private TableView<Card> table;
    @FXML private TableColumn<Card, Integer> colId;
    @FXML private TableColumn<Card, String> colQuestion;
    @FXML private TableColumn<Card, String> colAnswer;
    @FXML private TableColumn<Card, LocalDate> colCreatedAt;
    @FXML private TableColumn<Card, Void> colActions;
    @FXML private Button refreshButton;
    @FXML private Button addButton;

    private final ObservableList<Card> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // setCellValueFactory defines how to read each field for every row.
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colQuestion.setCellValueFactory(new PropertyValueFactory<>("question"));
        colAnswer.setCellValueFactory(new PropertyValueFactory<>("answer"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Delete");
            {
                btn.setOnAction(e -> {
                    // Access the current row's item via the table reference on the cell.
                    Card c = getTableView().getItems().get(getIndex());
                    if (c != null) {
                        boolean ok = new Alert(Alert.AlertType.CONFIRMATION,
                                "Delete card #" + c.getId() + "?", ButtonType.OK, ButtonType.CANCEL)
                                .showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
                        if (ok) {
                            // FK cascade will remove related plans/stats as well.
                            if (cardDAO.deleteById(c.getId())) load();
                        }
                    }
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });

        table.setItems(data);
        refreshButton.setOnAction(e -> load());
        addButton.setOnAction(e -> openAddDialog());
        load();
    }

    private void load() {
        data.setAll(cardDAO.findAll());
    }

    private void openAddDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add_card_dialog.fxml"));
            DialogPane pane = loader.load();
            AddCardDialogController ctrl = loader.getController();

            Dialog<ButtonType> dlg = new Dialog<>();
            dlg.setTitle("New Card");
            dlg.setDialogPane(pane);
            dlg.initModality(Modality.APPLICATION_MODAL);

            dlg.showAndWait().ifPresent(bt -> {
                if (bt.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                    Card c = ctrl.getResult();
                    int id = cardDAO.insertCard(c);
                    if (id > 0) {
                        reviewPlanDAO.generateFirstPlan(id);
                        load();
                    }
                }
            });
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
