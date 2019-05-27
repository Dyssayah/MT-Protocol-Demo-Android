package com.bosch.glm100c.easy_connect.bluetooth.ble_utils;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.os.Build;

import com.bosch.glm100c.easy_connect.bluetooth.IBleDeviceScanner;
import com.bosch.glm100c.easy_connect.bluetooth.IBleScanCallback;
import com.bosch.glm100c.easy_connect.bluetooth.IScanResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of BLE device scanner for Android JellyBean
 *
 * Created by tos2si1 on 27.06.2017.
 */
@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleDeviceScanner18 implements IBleDeviceScanner, IBleScanCallback {

    private static final String TAG = "bluetooth.impl.BleDeviceScanner18";

    private boolean mMtFilterEnabled;
    private BleScanCallback18 mScanCallback;
    private BluetoothAdapter mBtAdapter;

    private List<IBleDeviceScanner.OnDeviceDiscoveredHandler> mHandlers = new ArrayList<>();

    public BleDeviceScanner18(BluetoothAdapter btAdapter) {
        // initialize scanner
        mBtAdapter = btAdapter;

        // create callback
        mScanCallback = new BleScanCallback18(this);
    }

    @Override
    public void setScanSettings(ScanSettings scanSettings) {
        // do nothing: scan settings are not necessary in JellyBean
    }

    @Override
    public void setSystemFilter(List<ScanFilter> scanFilters) {
        // do nothing: scan filters are not working properly in JellyBean (only short UUID accepted)
    }

    @Override
    public void enableMtFilter(boolean enable) {
        mMtFilterEnabled = enable;
    }

    @Override
    public void startScan() {
        mBtAdapter.startLeScan(mScanCallback);
    }

    @Override
    public void stopScan() {
        mBtAdapter.stopLeScan(mScanCallback);
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
