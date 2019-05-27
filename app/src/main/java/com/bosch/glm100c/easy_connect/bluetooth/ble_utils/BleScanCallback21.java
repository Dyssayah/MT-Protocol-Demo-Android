package com.bosch.glm100c.easy_connect.bluetooth.ble_utils;

import android.annotation.TargetApi;
import android.bluetooth.le.ScanCallback;
import android.os.Build;
import android.util.Log;

import com.bosch.glm100c.easy_connect.bluetooth.IBleScanCallback;
import com.bosch.glm100c.easy_connect.bluetooth.IScanResult;

import java.util.List;

/**
 * Bluetooth LE scan callback under Lollipop.
 *
 * @see android.bluetooth.le.BluetoothLeScanner#startScan
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BleScanCallback21 extends ScanCallback {

    private static final String TAG = "BleScanCallback21";

    private final IBleScanCallback mBleScanCallback;

    /**
     * Create a new {@link BleScanCallback21} with the passed {@link IBleScanCallback}.
     *
     * @param scanCallback the {@link IBleScanCallback}
     */
    BleScanCallback21(final IBleScanCallback scanCallback) {
        this.mBleScanCallback = scanCallback;
    }

    @Override
    public void onScanResult(final int callbackType, final android.bluetooth.le.ScanResult result) {
        onScanResult(new ScanResultImpl21(result));
    }

    @Override
    public void onBatchScanResults(final List<android.bluetooth.le.ScanResult> results) {
        for (final android.bluetooth.le.ScanResult result : results) {
            onScanResult(new ScanResultImpl21(result));
        }
    }

    @Override
    public void onScanFailed(final int errorCode) {
        Log.e(TAG, "Scan failed with error Code: " + errorCode);
    }

    private void onScanResult(final IScanResult result) {
        mBleScanCallback.onScanResult(result);
    }
}
