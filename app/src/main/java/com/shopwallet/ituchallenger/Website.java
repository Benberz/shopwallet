package com.shopwallet.ituchallenger;

/**
 * Represents a website with a name.
 * This class encapsulates the name of the website and provides a method to access it.
 */
public class Website {
    private final String name;

    /**
     * Constructs a Website instance with the specified name.
     *
     * @param name The name of the website.
     */
    public Website(String name) {
        this.name = name;
    }

    /**
     * Gets the name of the website.
     *
     * @return The name of the website.
     */
    public String getName() {
        return name;
    }
}