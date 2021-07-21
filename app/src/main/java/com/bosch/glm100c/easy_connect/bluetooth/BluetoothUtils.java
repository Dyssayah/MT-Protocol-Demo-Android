package com.bosch.glm100c.easy_connect.bluetooth;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Collection of helper methods and constants for Bluetooth and BLE handling
 *
 * Created by tos2si1 on 06.02.2018.
 */

public class BluetoothUtils {

    /**
     * Static map for GLM 120 family devices. Bare tool number is key. Device name is value.
     */
    public static final Map<String, String> GLM_120_DEV_TYPES = new HashMap<>();
    static {
        GLM_120_DEV_TYPES.put("3601K72F00", "Bosch GLM 120 C");
        GLM_120_DEV_TYPES.put("00F27K1063", "Bosch GLM 120 C");
        GLM_120_DEV_TYPES.put("3601K72F51", "Bosch GLM 120 C JP");
        GLM_120_DEV_TYPES.put("15F27K1063", "Bosch GLM 120 C JP");
        GLM_120_DEV_TYPES.put("3601K72F10", "Bosch GLM400C");
        GLM_120_DEV_TYPES.put("01F27K1063", "Bosch GLM400C");
        GLM_120_DEV_TYPES.put("3601K72F13", "Bosch GLM400CL");
        GLM_120_DEV_TYPES.put("13F27K1063", "Bosch GLM400CL");
        GLM_120_DEV_TYPES.put("3601K72FK0", "Bosch GLM 150 C");
        GLM_120_DEV_TYPES.put("0KF27K1063", "Bosch GLM 150 C");
        GLM_120_DEV_TYPES.put("3601K72F50", "Bosch GLM 150 C");
        GLM_120_DEV_TYPES.put("05F27K1063", "Bosch GLM 150 C");
        GLM_120_DEV_TYPES.put("3601K72FC0", "Bosch GLM 150 C");
        GLM_120_DEV_TYPES.put("0CF27K1063", "Bosch GLM 150 C");
    }

    /**
     * Static map for GLM 150-27 devices, treated like GLM 120.
     * Bare tool number is key. Device name is value.
     */
    static final Map<String, String> GLM_150_DEV_TYPES = new HashMap<>();

    static {
        GLM_150_DEV_TYPES.put("3601K72Z00", "Bosch GLM 150-27 C");
        GLM_150_DEV_TYPES.put("00Z27K1063", "Bosch GLM 150-27 C");
        GLM_150_DEV_TYPES.put("3601K72Z50", "Bosch GLM 150-27 C");
        GLM_150_DEV_TYPES.put("05Z27K1063", "Bosch GLM 150-27 C");
    }

    /**
     * Static map for GLM 100-25 devices, treated like GLM 120.
     * Bare tool number is key. Device name is value.
     */
    static final Map<String, String> GLM_100_DEV_TYPES = new HashMap<>();

    static {
        GLM_100_DEV_TYPES.put("3601K72Y00", "Bosch GLM 100-25 C");
        GLM_100_DEV_TYPES.put("00Y27K1063", "Bosch GLM 100-25 C");
        GLM_100_DEV_TYPES.put("3601K72Y50", "Bosch GLM 100-25 C");
        GLM_100_DEV_TYPES.put("05Y27K1063", "Bosch GLM 100-25 C");
        GLM_100_DEV_TYPES.put("3601K72YK0", "Bosch GLM 100-25 C");
        GLM_100_DEV_TYPES.put("0KY27K1063", "Bosch GLM 100-25 C");
    }

    /**
     * Static map for GLM 50-2 family devices. Bare tool number is key. Device name is value.
     */
    public static final Map<String, String> GLM_50_2_DEV_TYPES = new HashMap<>();
    static {
        GLM_50_2_DEV_TYPES.put("3601K72T00", "Bosch GLM50-27 C");
        GLM_50_2_DEV_TYPES.put("00T27K1063", "Bosch GLM50-27 C");
        GLM_50_2_DEV_TYPES.put("3601K72U00", "Bosch GLM50-27 CG");
        GLM_50_2_DEV_TYPES.put("00U27K1063", "Bosch GLM50-27 CG");
        GLM_50_2_DEV_TYPES.put("3601K72T10", "Bosch Blaze165-27C");
        GLM_50_2_DEV_TYPES.put("01T27K1063", "Bosch Blaze165-27C");
        GLM_50_2_DEV_TYPES.put("3601K72U10", "Bosch Blaze165-27CG");
        GLM_50_2_DEV_TYPES.put("01U27K1063", "Bosch Blaze165-27CG");
        GLM_50_2_DEV_TYPES.put("3601K72UK0", "Bosch GLM50-27 CG AP");
        GLM_50_2_DEV_TYPES.put("0KU27K1063", "Bosch GLM50-27 CG AP");
        GLM_50_2_DEV_TYPES.put("3601K72U50", "Bosch GLM50-27 CG JP");
        GLM_50_2_DEV_TYPES.put("05U27K1063", "Bosch GLM50-27 CG JP");
    }

    /**
     * Validates if the paired device is a GLM 100 device
     *
     * @param device device to be validated
     * @return true if validation successful
     */
    public static boolean validateGLM100Name(MTBluetoothDevice device) {
        return device!=null && device.getDisplayName() != null
                && device.getDisplayName().toLowerCase(Locale.getDefault()).contains("bosch")
                && device.getDisplayName().toLowerCase(Locale.getDefault()).contains("glm1") //updated the name with postfix 1 to validate only for GLM100 devices. T
                && device.getDisplayName().toLowerCase(Locale.getDefault()).contains("100");
    }

    /**
     * Validates if the paired device is a GLM 50 device
     *
     * @param device device to be validated
     * @return true if validation successful
     */
    public static boolean validateGLM50Name(MTBluetoothDevice device) {
        return device!=null && device.getDisplayName() != null
                && device.getDisplayName().toLowerCase(Locale.getDefault()).contains("bosch")
                && device.getDisplayName().toLowerCase(Locale.getDefault()).contains("glm5") //updated the name with postfix 5 to validate only for GLM50 devices.
                && device.getDisplayName().toLowerCase(Locale.getDefault()).contains("50")
                && !device.getDisplayName().toLowerCase(Locale.getDefault()).contains("-2");
    }

    /**
     * Validates if the paired device is a GIS 1000 device
     *
     * @param device device to be validated
     * @return true if validation successful
     */
    public static boolean validateGISName(MTBluetoothDevice device) {
        return device.getDisplayName() != null
                && device.getDisplayName().toLowerCase(Locale.getDefault()).contains("bosch")
                && device.getDisplayName().toLowerCase(Locale.getDefault()).contains("gis")
                && device.getDisplayName().toLowerCase(Locale.getDefault()).contains("1000");

    }

    /**
     * Validates if the paired device is a PLR device
     *
     * @param device device to be validated
     * @return true if validation successful
     */
    public static boolean validatePLRName(MTBluetoothDevice device) {
        return device!=null && device.getDisplayName() != null
                && device.getDisplayName().toLowerCase(Locale.getDefault()).contains("bosch")
                && device.getDisplayName().toLowerCase(Locale.getDefault()).contains("plr")
                && (device.getDisplayName().toLowerCase(Locale.getDefault()).contains("30")
                || device.getDisplayName().toLowerCase(Locale.getDefault()).contains("40")
                || device.getDisplayName().toLowerCase(Locale.getDefault()).contains("50"));
    }

    /**
     * Validates if the paired device is a GLM 120 device
     *
     * @param device device to be validated
     * @return true if validation successful
     */
    public static boolean validateGLM120Name(MTBluetoothDevice device) {
        String deviceName = device != null ? device.getDisplayName() : null;
        if (deviceName != null && deviceName.contains("x")) {
            String toolName = deviceName.substring(0, deviceName.indexOf("x") -1);
            return GLM_120_DEV_TYPES.containsValue(toolName)
                    || GLM_100_DEV_TYPES.containsValue(toolName)
                    || GLM_150_DEV_TYPES.containsValue(toolName);
        }
        return false;
    }

    /**
     * Validates if the paired device is a GLM 120 device
     *
     * @param device device to be validated
     * @return true if validation successful
     */
    public static boolean validateGLM50_2Name(MTBluetoothDevice device) {
        return device!=null && device.getDisplayName() != null && GLM_50_2_DEV_TYPES.containsValue(device.getDisplayName().substring(0, device.getDisplayName().indexOf("x") - 1));
    }

    /**
     * Validates if the paired device is a EDC based device
     *
     * @param device device to be validated
     * @return true if validation successful
     */
    public static boolean validateEDCDevice(MTBluetoothDevice device) {
        return validateGLM50Name(device) || validateGLM120Name(device) || validateGLM50_2Name(device) || validatePLRName(device);
    }

    /**
     * Validate Bosch device.
     *
     * @param deviceName the device
     * @return true, if successful
     */
    public static boolean validateDeviceName(String deviceName) {
        return deviceName != null && deviceName.toLowerCase(Locale.getDefault()).contains("bosch");
    }
}
