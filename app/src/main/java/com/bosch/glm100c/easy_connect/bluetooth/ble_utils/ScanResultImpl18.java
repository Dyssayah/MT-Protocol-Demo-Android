package com.bosch.glm100c.easy_connect.bluetooth.ble_utils;

import android.bluetooth.BluetoothDevice;

import com.bosch.glm100c.easy_connect.bluetooth.IScanResult;

/**
 * Implementation of IScanResult valid for Android JellyBean
 *
 * Created by tos2si1 on 27.06.2017.
 */

public class ScanResultImpl18 implements IScanResult {

    private BluetoothDevice mDevice;
    private byte[] mRawScanRecord;
    private int mRssi;

    ScanResultImpl18(BluetoothDevice device, byte[] rawScanRecord, int rssi) {
        this.mDevice = device;
        this.mRawScanRecord = rawScanRecord;
        this.mRssi = rssi;
    }

    @Override
    public BluetoothDevice getDevice() {
        return mDevice;
    }

    @Override
    public byte[] getRawScanRecord() {
        return mRawScanRecord;
    }

    @Override
    public int getRssi() {
        return mRssi;
    }
}
