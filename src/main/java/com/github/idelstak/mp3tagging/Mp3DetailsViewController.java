package com.github.idelstak.mp3tagging;

import com.dlsc.gemsfx.*;
import javafx.beans.property.*;
import javafx.concurrent.*;
import javafx.event.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import org.jaudiotagger.audio.*;
import org.slf4j.*;

import java.io.*;
import java.net.*;

import static javafx.application.Platform.*;

public class Mp3DetailsViewController extends FxmlController {

    private static final Logger LOG = LoggerFactory.getLogger(Mp3DetailsViewController.class);
    private final ObjectProperty<File> file;
    private final ObjectProperty<Mp3> mp3;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private GridPane detailsPane;
    @FXML
    private TextField albumField;
    @FXML
    private Label fileNameLabel;
    @FXML
    private TextField yearField;
    @FXML
    private TextField trackField;
    @FXML
    private Label headerValuesLabel;
    @FXML
    private AvatarView coverArtView;
    @FXML
    private TextField titleField;
    @FXML
    private TextField artistField;
    @FXML
    private TextField genreField;

    public Mp3DetailsViewController() {
        file = new SimpleObjectProperty<>();
        mp3 = new SimpleObjectProperty<>();
    }

    @Override
    protected void initialize() {
        detailsPane.visibleProperty().bind(progressIndicator.visibleProperty().not());

        file.addListener((_, _, file) -> {
            if (file == null) {
                return;
            }

            Service<AudioFile> service = new Service<>() {
                @Override
                protected Task<AudioFile> createTask() {
                    return new Task<>() {
                        @Override
                        protected AudioFile call() throws Exception {
                            updateProgress(-1, -1);
                            return AudioFileIO.read(file);
                        }
                    };
                }
            };

            progressIndicator.progressProperty().bind(service.progressProperty());
            progressIndicator.visibleProperty().bind(service.runningProperty());

            service.setOnFailed(event -> {
                Throwable e = event.getSource().getException();
                Window owner = fileNameLabel.getScene().getWindow();
                new ErrorAlert(owner, e, "Couldn't read MP3 audioFile").show();
                LOG.error(null, e);
            });

            service.setOnSucceeded(event -> {
                AudioFile audioFile = (AudioFile) event.getSource().getValue();
                runLater(() -> this.mp3.set(new Mp3(audioFile)));
            });

            service.start();
        });

        mp3.addListener((_, _, mp3) -> {
            if (mp3 == null) {
                return;
            }

            runLater(() -> updateFields(mp3));
        });
    }

    private void updateFields(Mp3 mp3) {
        fileNameLabel.setText(mp3.file());
        headerValuesLabel.setText(mp3.header());
        titleField.setText(mp3.title());
        artistField.setText(mp3.artist());
        albumField.setText(mp3.album());
        yearField.setText(mp3.year());
        trackField.setText(mp3.track());
        genreField.setText(mp3.genre());
        coverArtView.setImage(mp3.coverArt());
    }

    void setFile(ObjectProperty<File> file) {
        this.file.bind(file);
    }

    void startSearch(ActionEvent event) throws IOException {
        String trackTitle = titleField.getText();
        String artist = artistField.getText();
        Window owner = ((Node) event.getSource()).getScene().getWindow();

        new TagSearchResultsDialog(owner, trackTitle, artist).showAndWait();
    }

}