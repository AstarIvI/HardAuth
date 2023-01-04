package com.astarivi.hardauth.utils;

import com.astarivi.hardauth.HardAuth;

import java.io.*;
import java.util.*;

/**
 * The ConfigFile class allows to read from .properties files, validate them
 * against the defaults, and regenerate them in case the properties names are invalid.
 * The regenerated files are simply unpacked from the .jar assets again.
 */

public class ConfigFile {
    private final Properties properties;

    // Initializes the Hashmap and loads the properties into it.
    public ConfigFile() {
        properties = load();
    }

    // Returns the content of a Properties element. As the properties files is
    // only loaded once and is checked for errors at that time, it shouldn't
    // be possible to consult a non-existing Properties element.
    public String getProperty(String property) {
        return properties.getProperty(property);
    }

    public int getIntProperty(String property, int propertyDefault) {
        final String propertyStringValue = this.getProperty(property);

        int propertyValue = propertyDefault;

        try {
            propertyValue = Integer.parseInt(propertyStringValue);
        } catch(NumberFormatException e) {
            this.setProperty(property, Integer.toString(propertyDefault));
            this.save();
        }

        return propertyValue;
    }

    public boolean getBooleanProperty(String property, boolean propertyDefault) {
        final String propertyStringValue = this.getProperty(property);

        if (!"true".equalsIgnoreCase(propertyStringValue) && !"false".equalsIgnoreCase(propertyStringValue)) {
            this.setProperty(property, Boolean.toString(propertyDefault));
            this.save();
            return propertyDefault;
        }

        return Boolean.parseBoolean(propertyStringValue);
    }

    public void setProperty(String property, String value) {
        properties.setProperty(property, value);
    }

    public void save(){
        final String filename = "HardAuth.properties";
        File propertiesFile = new File("config/HardAuth/" + filename);
        propertiesFile.getParentFile().mkdirs();

        try {
            FileOutputStream fileos = new FileOutputStream(propertiesFile);
            this.properties.store(fileos, filename);
            fileos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Properties load() {
        final String filename = "HardAuth.properties";
        File propertiesFile = new File("config/HardAuth/" + filename);
        Properties locProperties = new Properties();
        Properties defProperties = new Properties();

        try {
            propertiesFile.getParentFile().mkdirs();
            defProperties.load(HardAuth.class.getResourceAsStream("/assets/hardauth/"+filename));

            if (propertiesFile.exists()) {
                FileInputStream fileis = new FileInputStream(propertiesFile);
                locProperties.load(fileis);
                fileis.close();

                if (locProperties.stringPropertyNames().equals(defProperties.stringPropertyNames())) {
                    return locProperties;
                }
            }

            FileOutputStream fileos = new FileOutputStream(propertiesFile);
            defProperties.store(fileos, filename);
            fileos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return defProperties;
    }
}