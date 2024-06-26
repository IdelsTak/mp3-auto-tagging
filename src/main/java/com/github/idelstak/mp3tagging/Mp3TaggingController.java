package com.github.idelstak.mp3tagging;

import javafx.beans.binding.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.concurrent.*;
import javafx.event.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import org.slf4j.*;

import java.io.*;

import static com.github.idelstak.mp3tagging.Fxml.*;
import static javafx.application.Platform.*;
import static javafx.beans.binding.Bindings.*;
import static javafx.collections.FXCollections.*;
import static org.slf4j.LoggerFactory.*;

public class Mp3TaggingController extends FxmlController {

    private static final Logger LOG = getLogger(Mp3TaggingController.class);
    private final ObservableList<File> files;
    private final ObjectProperty<File> selectedFile;
    private final Mp3DetailsViewController detailsController;
    @FXML
    private BorderPane viewPane;
    @FXML
    private Hyperlink viewPlaceholder;
    @FXML
    private Button downloadTagsButton;
    @FXML
    private SplitPane splitPane;

    public Mp3TaggingController() throws IOException {
        files = observableArrayList();
        selectedFile = new SimpleObjectProperty<>();
        detailsController = (Mp3DetailsViewController) MP3S_DETAILS_VIEW.controller();
    }

    @Override
    protected void initialize() {
        downloadTagsButton.disableProperty().bind(selectedFile.isNull());
        viewPane.visibleProperty().bind(viewPlaceholder.visibleProperty().not());
        BooleanBinding mp3sUnavailable = createBooleanBinding(files::isEmpty, files);
        viewPlaceholder.visibleProperty().bind(mp3sUnavailable);

        splitPane.skinProperty().addListener(_ -> runLater(() -> {
            try {
                Mp3sViewController controller = (Mp3sViewController) MP3S_VIEW.controller();
                controller.setFiles(files);
                controller.bindToViewSelection(selectedFile);

                detailsController.setFile(selectedFile);

                splitPane.getItems().addAll(MP3S_VIEW.root(), MP3S_DETAILS_VIEW.root());
                splitPane.setDividerPositions(0.2f);
            } catch (IOException e) {
                Window owner = splitPane.getScene().getWindow();
                new ErrorAlert(owner, e, "Couldn't load %s".formatted(MP3S_VIEW.path()));
                LOG.error("", e);
            }
        }));
    }

    @FXML
    private void selectDirectory(ActionEvent actionEvent) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Directory");
        String home = System.getProperty("user.home");
        chooser.setInitialDirectory(new File(home));
        Window owner = ((Node) actionEvent.getSource()).getScene().getWindow();
        File directory = chooser.showDialog(owner);

        LOG.info("Add mp3s from directory: {}", directory);

        if (directory != null) {
            files.clear();

            Service<Void> loadService = new Service<>() {
                @Override
                protected Task<Void> createTask() {
                    return new Task<>() {
                        @Override
                        protected Void call() {
                            loadMp3Files(directory);
                            return null;
                        }

                        private void loadMp3Files(File file) {
                            if (file.exists() && Mp3.isMp3(file)) {
                                runLater(() -> files.add(file));
                            } else {
                                File[] listed = file.listFiles();
                                if (listed != null) {
                                    for (File f : listed) {
                                        loadMp3Files(f);
                                    }
                                }
                            }
                        }
                    };
                }
            };

            loadService.start();

        }
    }

    @FXML
    private void downloadTagInfo(ActionEvent event) throws IOException {
        detailsController.startSearch(event);
    }

}