package com.github.idelstak.mp3tagging;

import javafx.fxml.*;
import javafx.scene.*;

import java.io.*;
import java.net.*;

public enum Fxml {
    MP3S_VIEW("/com/github/idelstak/mp3tagging/mp3s-view.fxml"),
    MP3S_DETAILS_VIEW("/com/github/idelstak/mp3tagging/mp3-details-view.fxml"),
    TAG_SEARCH_DIALOG("/com/github/idelstak/mp3tagging/tag-search.fxml");

    private final String path;
    private Object controller;
    private Node root;

    Fxml(String path) {this.path = path;}

    Node root() throws IOException {
        if (root == null) {
            load();
        }
        return root;
    }

    private void load() throws IOException {
        URL resource = getClass().getResource(path);
        FXMLLoader loader = new FXMLLoader(resource);
        root = loader.load();
        controller = loader.getController();
    }

    String path() {
        return path;
    }

    Object controller() throws IOException {
        if (controller == null) {
            load();
        }
        return controller;
    }
}