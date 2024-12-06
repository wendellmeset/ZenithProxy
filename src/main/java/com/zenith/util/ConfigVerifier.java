package com.zenith.util;

import java.lang.reflect.Field;

import static com.zenith.Shared.*;

// Verifies that all fields in the loaded configs are not null
// gson will deserialize invalid json values to null
// but this will cause issues as we expect all fields to be non-null
public final class ConfigVerifier {
    private ConfigVerifier() {}

    public static void verifyConfigs() {
        if (!verifyConfig()) {
            failVerify("config");
        }
        if (!verifyLaunchConfig()) {
            failVerify("launch_config");
        }
    }

    private static void failVerify(String configName) {
        DEFAULT_LOG.error("{} verification failed", configName);
        DEFAULT_LOG.error("{}.json must be manually fixed or deleted", configName);
        DEFAULT_LOG.error("Shutting down in 10s");
        Wait.wait(10);
        System.exit(1);
    }

    private static boolean verifyConfig() {
        // recursively check all fields for null
        return verifyNoNullFields(CONFIG, Config.class);
    }

    private static boolean verifyLaunchConfig() {
        // recursively check all fields for null
        return verifyNoNullFields(LAUNCH_CONFIG, LaunchConfig.class);
    }

    private static boolean verifyNoNullFields(Object obj, Class<?> clazz) {
        // recursively check all fields for null
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            try {
                if (field.getType().isPrimitive()) {
                    continue;
                }
                Object value = field.get(obj);
                if (value == null) {
                    DEFAULT_LOG.error("Field: '{}' in '{}' is null", field.getName(), clazz.getName());
                    return false;
                }
                if (field.getType().getName().startsWith("com.zenith")
                    && !field.getType().isEnum()) {
                    if (!verifyNoNullFields(value, field.getType())) {
                        return false;
                    }
                }
            } catch (Throwable e) {
                DEFAULT_LOG.error("Error verifying config fields", e);
            }
        }
        return true;
    }
}
