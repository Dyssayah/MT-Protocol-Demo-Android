package com.bosch.glm100c.easy_connect.bluetooth.ble_utils;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.os.Build;

import com.bosch.glm100c.easy_connect.bluetooth.IScanResult;

/**
 * Implementation of IScanResult valid for Android Lollipop and higher
 *
 * Created by tos2si1 on 27.06.2017.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ScanResultImpl21 implements IScanResult {

    private BluetoothDevice mDevice;
    private byte[] mRawScanRecord;
    private int mRssi;

    ScanResultImpl21(ScanResult result) {
        this.mDevice = result.getDevice();
        byte[] record = new byte[0];
        if (result.getScanRecord() != null) { // necessary, as getScanRecord() may return null
            record = result.getScanRecord().getBytes();
        }
        this.mRawScanRecord = record;
        this.mRssi = result.getRssi();
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
