package com.github.idelstak.mp3tagging;

import static java.util.Objects.*;

public enum Css {
    STYLES("/com/github/idelstak/mp3tagging/styles.css");

    private final String path;

    Css(String path) {this.path = path;}

    public String toExternalForm() {
        return requireNonNull(getClass().getResource(path)).toExternalForm();
    }
}