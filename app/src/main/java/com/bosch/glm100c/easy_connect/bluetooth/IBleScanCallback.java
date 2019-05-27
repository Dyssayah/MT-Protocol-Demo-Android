package com.bosch.glm100c.easy_connect.bluetooth;

/**
 * Abstract BLE Scan Callback used to deliver Scan Results
 *
 * Created by tos2si1 on 26.06.2017.
 */

public interface IBleScanCallback {

    /**
     * Callback when a BLE advertisement has been found.
     *
     * @param result scan result
     */
    void onScanResult(final IScanResult result);
}
