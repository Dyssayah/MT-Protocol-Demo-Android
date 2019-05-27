package com.bosch.glm100c.easy_connect.bluetooth;

import java.util.UUID;

/**
 * Interface, that will parse MTDevice information from the applied scan record
 *
 * Should be initialized with
 *
 * Created by tos2si1 on 07.04.2017.
 */

public interface MTDeviceParser {

    /**
     * Initializes the parser object with the necessary data
     * After initializing, all data is available via the
     * corresponding methods
     *
     * @param devName Advertising name of the device
     * @param scanRecord Scan record of the device
     */
    //void init(String devName, byte[] scanRecord);

    /**
     * Returns advertising name of the device
     *
     * @return advertising name of the device (could be null)
     */
    String getAdvertisingName();

    /**
     * Returns display name of the device
     *
     * @return display name of the device
     */
    String getDisplayName();

    /**
     * Returns service UUID of the device
     *
     * @return service UUID of the device
     */
    UUID getServiceUUID();

    /**
     * Returns MAC address of the device
     *
     * @return MAC address of the device
     */
    String getMacAddress();

    /**
     * Indicates if device is connectable or in pure TmT mode
     * Try connection only if this value is true
     *
     * @return true if device connectable, false otherwise
     */
    boolean isConnectable();

    /**
     * Indicates if backup battery is low or in normal condition
     *
     * @return true if backup battery is low, false otherwise
     */
    boolean isBackupSupplyLow();

    /**
     * Indicates if the tool is locked or not
     *
     * @return true if tool locked, false otherwise
     */
    boolean isToolLocked();

    /**
     * Indicates if ELO wake up is allowed
     * @return true if wake up allowed, false otherwise
     */
    boolean isELOWakeUpAllowed();

    /**
     * Returns Bare Tool Nr.
     *
     * @return Bare tool number of the device
     */
    String getBareToolNr();

    /**
     * Returns Serial Nr.
     *
     * @return last 4 digits of serial number of the device
     */
    String getSerialNr();

    /**
     * Reruns main battery state of charge in percent (0 - 100%)
     *
     * @return main battery charge in percent
     */
    int getMainSupplyChargeState();
}
