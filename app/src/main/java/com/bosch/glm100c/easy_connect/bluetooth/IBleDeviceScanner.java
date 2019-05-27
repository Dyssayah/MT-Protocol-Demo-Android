package com.bosch.glm100c.easy_connect.bluetooth;

import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;

import java.util.List;

/**
 * Abstract Bluetooth Low Energy Device Scanner
 *
 * Created by tos2si1 on 26.06.2017.
 */

public interface IBleDeviceScanner {

    /**
     * Set ScanSettings for the scanner
     * After setting new ScanSettings start scan again to apply these
     *
     * @param scanSettings the ScanSettings to be set
     */
    void setScanSettings(ScanSettings scanSettings);

    /**
     * Set ScanFilter(s) for the scanner
     * After setting new ScanFilter(s) start scan again to apply these
     *
     * @param scanFilters the Array with ScanFilter(s) to be set
     */
    void setSystemFilter(List<ScanFilter> scanFilters);

    /**
     * Enable filtering of the ScanResult delivered
     * Set disable, if scanner not used for MT devices
     * If activated and implemented, the scanner will deliver only MT devices identified either
     * by name or ba bare tool number
     *
     * @param enable will enable additional filtering if true
     */
    void enableMtFilter(boolean enable);

    /**
     * start scanner with the latest set configuration
     */
    void startScan();

    /**
     * stop scanner
     */
    void stopScan();

    /**
     * Add a handler for device discovery.
     *
     * This listener will be called whenever a device is discovered
     * Remove the handler using
     * {@link #removeOnDeviceDiscoveredHandler(OnDeviceDiscoveredHandler)}.
     *
     * @param handler The handler to attach
     * @see #removeOnDeviceDiscoveredHandler(OnDeviceDiscoveredHandler)
     */
    void addOnDeviceDiscoveredHandler(OnDeviceDiscoveredHandler handler);

    /**
     * Remove a handler for device discovery.
     *
     * This handler will receive no further notifications
     * Add a handler using
     * {@link #addOnDeviceDiscoveredHandler(OnDeviceDiscoveredHandler)}.
     *
     * @param handler The handler to remove
     * @see #addOnDeviceDiscoveredHandler(OnDeviceDiscoveredHandler)
     */
    void removeOnDeviceDiscoveredHandler(OnDeviceDiscoveredHandler handler);

    interface OnDeviceDiscoveredHandler {

        /**
         * Called when a device is found and reached through all filters
         *
         * @param result the scan result for the discovered device
         */
        void onDeviceDiscovered(IScanResult result);
    }
}
