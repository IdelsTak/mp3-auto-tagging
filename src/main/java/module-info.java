module com.github.idelstak.mp3tagging {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.boxicons;
    requires org.slf4j;
    requires jaudiotagger;
    requires com.dlsc.gemsfx;
    requires java.desktop;
    requires org.json;

    opens com.github.idelstak.mp3tagging;
    exports com.github.idelstak.mp3tagging;
}