package com.github.idelstak.mp3tagging;

import javafx.application.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.stage.*;

import java.io.*;

public class Mp3TaggingApplication extends Application {

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Mp3TaggingApplication.class.getResource("mp3-tagging-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("MP3 Tags");
        stage.setScene(scene);
        stage.show();
    }
}