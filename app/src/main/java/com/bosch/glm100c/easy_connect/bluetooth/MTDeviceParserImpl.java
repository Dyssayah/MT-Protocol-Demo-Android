package com.bosch.glm100c.easy_connect.bluetooth;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

/**
 * Implementation of the interface, that will parse MTDevice information from the applied scan record
 *
 * Created by tos2si1 on 07.04.2017.
 */

class MTDeviceParserImpl implements MTDeviceParser {

    private static final String TAG = "com.bosch.boschlevellingremoteapp.bluetooth.impl.MTDeviceParserImpl";

    private String devAdvName;
    private String devDisplayName;
    private UUID serviceUUID;
    private String macAddress;
    private boolean isConnectable;
    private boolean isBackupSupplyLow;
    private boolean isToolLocked;
    private boolean isELOWakeUpAllowed;
    private String bareToolNr;
    private String serialNr;
    private int mainSupplyCharge;

    private byte[] record;

    MTDeviceParserImpl(String devName, byte[] scanRecord) {
        this.devAdvName = devName;
        this.record = scanRecord;
        parseDevice();
    }

    @Override
    public String getAdvertisingName() {
        return devAdvName;
    }

    @Override
    public String getDisplayName() {
        return devDisplayName;
    }

    @Override
    public UUID getServiceUUID() {
        return serviceUUID;
    }

    @Override
    public String getMacAddress() {
        return macAddress;
    }

    @Override
    public boolean isConnectable() {
        return isConnectable;
    }

    @Override
    public boolean isBackupSupplyLow() {
        return isBackupSupplyLow;
    }

    @Override
    public boolean isToolLocked() {
        return isToolLocked;
    }

    @Override
    public boolean isELOWakeUpAllowed() {
        return isELOWakeUpAllowed;
    }

    @Override
    public String getBareToolNr() {
        return bareToolNr;
    }

    @Override
    public String getSerialNr() {
        return serialNr;
    }

    @Override
    public int getMainSupplyChargeState() {
        return mainSupplyCharge;
    }

    private void parseDevice() {
        //Log.w(TAG, "Start parsing; devName = :" + devAdvName + ": record length: " + record.length);
        //if (devAdvName == null || devAdvName.length() < 2) {
        if (record != null) {
            // Miraculix device
            if (record.length >= 30) { // Advertising package -> if shorter, than no PT-MT device
                // Flags
                byte flags = record[11];
                isConnectable = ((flags >> 3) & 1) == 1;
                isELOWakeUpAllowed = ((flags >> 2) & 1) == 1;
                isToolLocked = ((flags >> 1) & 1) == 1;
                isBackupSupplyLow = (flags & 1) == 1;
                // MAC Address
                byte[] mAddress = new byte[6];
                System.arraycopy(record, 5, mAddress, 0, 6);
                macAddress = bytesToHex(reverse(mAddress)).replaceAll("..(?!$)", "$0:");
                // UUID
                byte[] sUUID = new byte[16];
                System.arraycopy(record, 14, sUUID, 0, 16);
                serviceUUID = getUUIDFromByteArray(bytesToHex(reverse(sUUID)));
            }
            if (record.length >= 48 && isConnectable) { // Scan response package
                // Bare Tool number
                byte[] bToolNr = new byte[10];
                System.arraycopy(record, 33, bToolNr, 0, 10);
                //Log.w(TAG, "Raw Bare Tool Nr: " + bytesToHex(bToolNr));
                try {
                    //bareToolNr = new String(bToolNr, "US-ASCII");
                    bareToolNr = new String(reverse(bToolNr), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                // display name
                if (bareToolNr != null && !bareToolNr.isEmpty()) {
                    devDisplayName = displayNameFromBareToolNr(bareToolNr);
                }
                // Serial number
                byte[] sNr = new byte[4];
                System.arraycopy(record, 44, sNr, 0, 4);
                try {
                    serialNr = new String(sNr, "UTF-8");
                    //Log.w(TAG, "Raw Bare Tool Nr: " + bytesToHex(bToolNr) + "; Serial Nr: " + bytesToHex(sNr));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (serialNr != null && !serialNr.isEmpty()) {
                    devDisplayName = devDisplayName + " x" + serialNr;
                }
                //serialNr = "0000";
                // Battery charge
                mainSupplyCharge = record[43];
            }
        } else {
            // PAN1026 device: only display name and connectable status (always true) provided
            devDisplayName = devAdvName;
            isConnectable = true;
        }
    }

    public String toString() {
        return "MT Device: displayName = " + devDisplayName + "; bareToolNr = " + bareToolNr + "; serialNr = " + serialNr + "; connectable = " + isConnectable
                + "; backupSupply = " + isBackupSupplyLow + "; locked = " + isToolLocked + "; macAddress = " + macAddress
                + "; UUID = " + (serviceUUID == null ? "NULL" : serviceUUID.toString()) + "; mainSupplyCharge = " + mainSupplyCharge;
    }

    // ##### Helper & private methods #####

    /**
     * Will return UUID build from untrimmed UUID String
     *
     * @param strUUID String containing untrimmed UUID
     * @return UUID
     */
    private UUID getUUIDFromByteArray(String strUUID) {
        if (strUUID != null && strUUID.length() == 32) {
            String trimmed = strUUID.substring(0, 7) + "-" + strUUID.substring(8, 11) + "-" + strUUID.substring(12, 15) + "-" + strUUID.substring(16, 19) + "-" + strUUID.substring(20, 31);
            return UUID.fromString(trimmed);
        }
        return null;
    }

    /**
     * Will assign product name to a known Bare Tool Nr
     *
     * @param bareTN Bare Tool Nr (as String)
     * @return Product name
     */
    private static String displayNameFromBareToolNr(String bareTN) {
        String displayName = "";
        if (BluetoothUtils.GLM_120_DEV_TYPES.containsKey(bareTN)) {
            displayName = BluetoothUtils.GLM_120_DEV_TYPES.get(bareTN);
        } else if (BluetoothUtils.GLM_50_2_DEV_TYPES.containsKey(bareTN)) {
            displayName = BluetoothUtils.GLM_50_2_DEV_TYPES.get(bareTN);
        } else if (BluetoothUtils.GLM_150_DEV_TYPES.containsKey(bareTN)) {
            displayName = BluetoothUtils.GLM_150_DEV_TYPES.get(bareTN);
        } else if (BluetoothUtils.GLM_100_DEV_TYPES.containsKey(bareTN)) {
            displayName = BluetoothUtils.GLM_100_DEV_TYPES.get(bareTN);
        }
        return displayName;
    }

    /**
     * Transforms byte array to HEX String
     *
     * @param in byte array to transform
     * @return String
     */
    static String bytesToHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for(byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    /**
     * Will reverse array of bytes
     *
     * @param arrayToReverse array to be reversed
     * @return the reverted array
     */
    private static byte[] reverse(byte[] arrayToReverse) {
        for(int i = 0; i < arrayToReverse.length / 2; i++)
        {
            byte temp = arrayToReverse[i];
            arrayToReverse[i] = arrayToReverse[arrayToReverse.length - i - 1];
            arrayToReverse[arrayToReverse.length - i - 1] = temp;
        }
        return arrayToReverse;
    }
}
