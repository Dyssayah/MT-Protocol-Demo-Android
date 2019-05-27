package com.bosch.glm100c.easy_connect.bluetooth.ble_utils;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.os.Build;

import com.bosch.glm100c.easy_connect.bluetooth.IBleDeviceScanner;
import com.bosch.glm100c.easy_connect.bluetooth.IBleScanCallback;
import com.bosch.glm100c.easy_connect.bluetooth.IScanResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of BLE device scanner for Android Lollipop and higher
 *
 * Created by tos2si1 on 27.06.2017.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BleDeviceScanner21 implements IBleDeviceScanner, IBleScanCallback {

    private static final String TAG = "bluetooth.impl.BleDeviceScanner21";

    private boolean mMtFilterEnabled;
    private ScanSettings mScanSettings;
    private List<ScanFilter> mScanFilters;
    private BleScanCallback21 mScanCallback;
    private BluetoothLeScanner mBtScanner;

    private List<IBleDeviceScanner.OnDeviceDiscoveredHandler> mHandlers = new ArrayList<>();

    public BleDeviceScanner21(BluetoothAdapter btAdapter) {
        // initialize scanner
        mBtScanner = btAdapter.getBluetoothLeScanner();

        // set initial settings, as these cannot be null
        mScanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

        // create callback
        mScanCallback = new BleScanCallback21(this);
    }

    @Override
    public void setScanSettings(ScanSettings scanSettings) {
        mScanSettings = scanSettings;
    }

    @Override
    public void setSystemFilter(List<ScanFilter> scanFilters) {
        mScanFilters = scanFilters;
    }

    @Override
    public void enableMtFilter(boolean enable) {
        mMtFilterEnabled = enable;
    }

    @Override
    public void startScan() {
        if (mBtScanner != null) {
            mBtScanner.startScan(mScanFilters, mScanSettings, mScanCallback);
        }
    }

    @Override
    public void stopScan() {
        if(mBtScanner!=null) {
            mBtScanner.stopScan(mScanCallback);
        }
    }

    @Override
    public void onScanResult(IScanResult result) {
        if (!mMtFilterEnabled) {
            for (OnDeviceDiscoveredHandler handler : mHandlers) {
                handler.onDeviceDiscovered(result);
            }
        }
    }

    @Override
    public void addOnDeviceDiscoveredHandler(OnDeviceDiscoveredHandler handler) {
        if (!mHandlers.contains(handler)) {
            mHandlers.add(handler);
        }
    }

    @Override
    public void removeOnDeviceDiscoveredHandler(OnDeviceDiscoveredHandler handler) {
        mHandlers.remove(handler);
    }
}
