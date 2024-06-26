package com.github.idelstak.mp3tagging;

import javafx.beans.property.*;
import javafx.beans.value.*;
import javafx.collections.*;
import javafx.collections.ListChangeListener.*;
import javafx.fxml.*;
import javafx.scene.control.*;

import java.io.*;

import static javafx.application.Platform.*;

public class Mp3sViewController extends FxmlController {

    private final ObservableList<File> files;
    @FXML
    private ListView<File> mp3sListView;

    public Mp3sViewController() {
        files = FXCollections.observableArrayList();
    }

    @Override
    protected void initialize() {
        mp3sListView.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        mp3sListView.setItems(files);
    }

    void setFiles(ObservableList<File> files) {
        files.addListener((Change<? extends File> change) -> {
            if (change.next()) {
                this.files.setAll(change.getList());
                if (!files.isEmpty()) {
                    runLater(mp3sListView.getSelectionModel()::selectFirst);
                }
            }
        });
    }

    void bindToViewSelection(ObjectProperty<File> selectedFile) {
        ObservableValue<File> selectedItem = mp3sListView.getSelectionModel().selectedItemProperty();
        selectedItem.addListener((_, _, file) -> runLater(() -> selectedFile.set(file)));
    }
}