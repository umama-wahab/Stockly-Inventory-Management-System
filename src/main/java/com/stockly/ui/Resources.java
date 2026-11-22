package com.stockly.ui;

import java.net.URL;
import java.util.Objects;

/**
 * Finds the FXML views and the stylesheet that are packaged inside the application.
 *
 * @author Umama Wahab
 */
public final class Resources {

    private static final String VIEW_FOLDER = "/com/stockly/view/";
    private static final String STYLESHEET = "/com/stockly/css/stockly.css";

    /** Utility class - not meant to be instantiated. */
    private Resources() {
    }

    /**
     * Returns the location of an FXML view.
     *
     * @param fileName the file name inside the view folder, for example {@code "MainView.fxml"}
     * @return the resource URL
     * @throws NullPointerException if the file is missing from the application resources
     */
    public static URL fxml(String fileName) {
        return Objects.requireNonNull(Resources.class.getResource(VIEW_FOLDER + fileName),
                "Missing view resource: " + fileName);
    }

    /**
     * Returns the application stylesheet in the form expected by {@code Scene.getStylesheets()}.
     *
     * @return the external form of the CSS URL
     */
    public static String stylesheet() {
        return Objects.requireNonNull(Resources.class.getResource(STYLESHEET), "Missing stylesheet").toExternalForm();
    }
}
