package com.bosch.glm100c.easy_connect.bluetooth;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;

/**
 * Wrapper for Android BluetoothDevice which contains also display name
 * for the case, that the name of the BluetoothDevice is null
 *
 * Created by tos2si1 on 11.04.2017.
 */
public class MTBluetoothDevice implements Serializable{

    private BluetoothDevice device;
    private String displayName;

    public MTBluetoothDevice(BluetoothDevice btDevice, String displayName) {
        this.device = btDevice;
        this.displayName = displayName;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public boolean equals(Object other) {
        // If the object is compared with itself -> true
        if (other == this) {
            return true;
        }

        // If the object is no instance of MTBluetoothDevice -> false
        if (!(other instanceof MTBluetoothDevice)) {
            return false;
        }

        MTBluetoothDevice otherDev = (MTBluetoothDevice) other;

        // Compare
        return otherDev.getDevice().getAddress().equals(this.getDevice().getAddress());
    }
}
