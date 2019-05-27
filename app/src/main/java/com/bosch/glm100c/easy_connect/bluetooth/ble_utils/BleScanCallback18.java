package com.bosch.glm100c.easy_connect.bluetooth.ble_utils;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Build;

import com.bosch.glm100c.easy_connect.bluetooth.IBleScanCallback;
import com.bosch.glm100c.easy_connect.bluetooth.IScanResult;

/**
 * Callback interface used to deliver LE scan results under Jelly Bean.
 *
 * @see BluetoothAdapter#startLeScan(BluetoothAdapter.LeScanCallback)
 * @see BluetoothAdapter#startLeScan(java.util.UUID[], BluetoothAdapter.LeScanCallback)
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleScanCallback18 implements BluetoothAdapter.LeScanCallback {

    private final IBleScanCallback mScanCallback;

    /**
     * Create a new {@link BleScanCallback18} with the passed {@link IBleScanCallback}.
     *
     * @param scanCallback the {@link IBleScanCallback}
     */
    BleScanCallback18(final IBleScanCallback scanCallback) {
        this.mScanCallback = scanCallback;
    }

    @Override
    public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        onScanResult(new ScanResultImpl18(device, scanRecord, rssi));
    }

    private void onScanResult(final IScanResult result) {
        mScanCallback.onScanResult(result);
    }
}
