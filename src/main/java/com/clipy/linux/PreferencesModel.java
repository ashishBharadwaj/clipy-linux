package com.clipy.linux;

public class PreferencesModel {
    public int maxHistory;
    public int maxTrayItems;
    public boolean plainTextOnly;

    public PreferencesModel() {
    }

    public PreferencesModel(int maxHistory, int maxTrayItems, boolean plainTextOnly) {
        this.maxHistory = maxHistory;
        this.maxTrayItems = maxTrayItems;
        this.plainTextOnly = plainTextOnly;
    }

    public static PreferencesModel defaultPrefs() {
        return new PreferencesModel(100, 10, true);
    }
}
