package com.shopwallet.ituchallenger;

/**
 * Represents an authentication history entry with details such as title, date, and status.
 */
public class AuthHistory {
    private final String title;
    private final String dateTime;
    private final String status;

    /**
     * Constructs an AuthHistory instance with the specified title, date and time, and status.
     *
     * @param title     The title of the authentication event.
     * @param dateTime  The date and time when the authentication event occurred.
     * @param status    The status of the authentication event (e.g., success, failure).
     */
    public AuthHistory(String title, String dateTime, String status) {
        this.title = title;
        this.dateTime = dateTime;
        this.status = status;
    }

    /**
     * Gets the title of the authentication event.
     *
     * @return The title of the authentication event.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the date and time when the authentication event occurred.
     *
     * @return The date and time of the authentication event.
     */
    public String getDateTime() {
        return dateTime;
    }

    /**
     * Gets the status of the authentication event.
     *
     * @return The status of the authentication event.
     */
    public String getStatus() {
        return status;
    }
}