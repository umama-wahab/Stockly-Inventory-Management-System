package com.stockly;

import javafx.application.Application;

/**
 * Application entry point.
 * <p>
 * This class deliberately does <em>not</em> extend {@link Application}. When a JavaFX application is
 * packaged as a runnable JAR on the classpath, the JVM launcher refuses to start a main class that extends
 * {@code Application}. A tiny separate launcher class avoids that problem.
 * </p>
 *
 * @author Umama Wahab
 */
public final class Main {

    /** Utility class - not meant to be instantiated. */
    private Main() {
    }

    /**
     * Starts the JavaFX runtime and launches {@link InventoryApplication}.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        Application.launch(InventoryApplication.class, args);
    }
}
