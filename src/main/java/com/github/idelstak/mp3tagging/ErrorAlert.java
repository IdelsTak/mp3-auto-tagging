package com.github.idelstak.mp3tagging;

import javafx.css.*;
import javafx.scene.control.*;
import javafx.stage.*;

import static com.github.idelstak.mp3tagging.Css.*;

class ErrorAlert extends Alert {

    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");

    ErrorAlert(Window owner, Throwable throwable, String headerText) {
        this(owner, AlertType.ERROR, throwable, headerText);
    }

    private ErrorAlert(Window owner, AlertType alertType, Throwable throwable, String headerText) {
        super(alertType);
        initOwner(owner);
        setHeaderText(headerText);
        setContentText("Reason:");

        DialogPane pane = getDialogPane();
        pane.getStylesheets().add(STYLES.toExternalForm());
        Label content = new Label();
        content.pseudoClassStateChanged(ERROR, true);
        content.setText(throwable.getMessage());
        pane.setExpandableContent(content);
        pane.setExpanded(true);
    }

}