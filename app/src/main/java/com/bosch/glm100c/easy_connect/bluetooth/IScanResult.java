package com.bosch.glm100c.easy_connect.bluetooth;

import android.bluetooth.BluetoothDevice;

/**
 * Abstract presentation of BLE scan result
 *
 * Created by tos2si1 on 26.06.2017.
 */

public interface IScanResult {
    /**
     * Returns the remote bluetooth device identified by the bluetooth device address.
     */
    BluetoothDevice getDevice();

    /**
     * Returns the scan record, which is a combination of advertisement and scan response.
     */
    byte[] getRawScanRecord();

    /**
     * Returns the received signal strength in dBm. The valid range is [-127, 127].
     */
    int getRssi();
}
