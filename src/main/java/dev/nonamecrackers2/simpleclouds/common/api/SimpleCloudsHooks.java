package dev.nonamecrackers2.simpleclouds.common.api;

public class SimpleCloudsHooks {
    private static boolean externalWeatherControl = false;

    public static void setExternalWeatherControl(boolean control) {
        externalWeatherControl = control;
    }

    public static boolean isExternalWeatherControlEnabled() {
        return externalWeatherControl;
    }
}
