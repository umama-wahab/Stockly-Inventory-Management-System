package com.stockly.model;

/**
 * The kind of change recorded in the "recent activity" feed.
 *
 * @author Umama Wahab
 */
public enum ActivityType {

    /** A new product was added. */
    ADDED,

    /** An existing product was edited. */
    UPDATED,

    /** A product was removed. */
    DELETED
}
