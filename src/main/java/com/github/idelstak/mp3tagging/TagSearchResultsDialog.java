package com.github.idelstak.mp3tagging;

import javafx.scene.control.*;
import javafx.stage.*;

import java.io.*;

import static com.github.idelstak.mp3tagging.Fxml.*;
import static javafx.application.Platform.*;

public class TagSearchResultsDialog extends Dialog<Mp3> {

    public TagSearchResultsDialog(Window owner, String trackTitle, String artist) throws IOException {
        setTitle("Search for \"%s\"".formatted(trackTitle));
        initOwner(owner);
        setDialogPane((DialogPane) TAG_SEARCH_DIALOG.root());
        TagSearchController controller = (TagSearchController) TAG_SEARCH_DIALOG.controller();

        setOnShown(_ -> runLater(() -> controller.setSearchQuery(new SearchQuery(trackTitle, artist))));
    }
}