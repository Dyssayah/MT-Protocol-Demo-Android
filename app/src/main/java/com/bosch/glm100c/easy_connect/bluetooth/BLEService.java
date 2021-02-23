package com.bosch.glm100c.easy_connect.bluetooth;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.bosch.glm100c.easy_connect.bluetooth.ble_utils.BleDeviceScanner18;
import com.bosch.glm100c.easy_connect.bluetooth.ble_utils.BleDeviceScanner21;
import com.bosch.glm100c.easy_connect.exc.BluetoothNotSupportedException;
import com.bosch.mtprotocol.MtConnection;
import com.bosch.mtprotocol.glm100C.connection.MtAsyncConnection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.bosch.mtprotocol.glm100C.connection.MtAsyncConnection.STATE_CONNECTING;

public class BLEService extends Service implements MtAsyncConnection.MTAsyncConnectionObserver, IBleDeviceScanner.OnDeviceDiscoveredHandler {

    public static final String ACTION_DEVICE_LIST_UPDATED = "DEVICE_LIST_UPDATED";
    public static final String ACTION_CONNECTION_STATUS_UPDATE = "CONNECTION_STATUS_UPDATE";

    public static final String EXTRA_DEVICE = "DEVICE";
    public static final String EXTRA_CONNECTION_STATUS = "CONNECTION_STATUS";

    public static final int REQUEST_ENABLE_BT = 3;

    private static final String TAG = "BLEService";

    private static final long PERIOD_TO_SCAN_IN_MS = 5000;

    private final IBinder mBinder = new BLELocalBinder();

    private Handler mHandler;
    private BluetoothAdapter btAdapter;
    private ScanSettings scanSettingsForDiscovery;
    private List<ScanFilter> scanFiltersForDiscovery;
    private IBleDeviceScanner mBleDeviceScanner;

    private MtAsyncConnection connection;
    private MTBluetoothDevice currentDevice;

    private boolean isScanning = false;

    private Set<MTBluetoothDevice> visibleDevices = new HashSet<>();

    /**
     * Start Bluetooth scan
     */
    public void startDiscovery() throws BluetoothNotSupportedException {
        if (btAdapter == null) { // initialize, if necessary
            btAdapter = getBluetoothAdapter();
        }

        if (mBleDeviceScanner == null) { // initialize, if necessary
            initializeDeviceScanner();
        }

        if (isScanning) {
            Log.w(TAG, " ## DISCOVERY ONGOING: NO NEED TO START ## ");
            return;
        }

        Log.w(TAG, "START BLE SCANNER...");

        mBleDeviceScanner.setScanSettings(scanSettingsForDiscovery);
        mBleDeviceScanner.setSystemFilter(scanFiltersForDiscovery);

        scanLeDevice(true);
    }

    /**
     * Stop Bluetooth scan
     */
    public void cancelDiscovery() {
        try {
            if (this.getBluetoothAdapter().getState() != BluetoothAdapter.STATE_ON) {
                return;
            }
        } catch (BluetoothNotSupportedException be) {
            be.printStackTrace();
            Log.e(TAG, "Bluetooth not Supported!");
        }

        Log.w(TAG, " ## CANCEL DISCOVERY ## ");
        scanLeDevice(false);
    }

    /**
     * Open connection to MTBluetoothDevice mtDevice
     * BluetoothConnection (classic connection) is opened if the connected mtDevice supports dual mode
     * BLEConnection (BLE connection) is opened, if the connected mtDevice supports only BLE
     * @return true if successful, false otherwise
     * @throws BluetoothNotSupportedException if Bluetooth not supported
     */
    public boolean connect(final MTBluetoothDevice mtDevice) throws BluetoothNotSupportedException {
        if (connection != null && connection.isOpen()) {
            disconnect();
        }

        if (connection != null && connection.getState() == STATE_CONNECTING) {
            Log.w(TAG, "Trying connection: Ignore");
            return false;
        }

        cancelDiscovery();
        this.currentDevice = mtDevice;

        if (BluetoothUtils.validateGLM120Name(mtDevice) || BluetoothUtils.validateGLM50_2Name(mtDevice)) { // GLM 120 C family device or GLM 50-2 family device - BLE only
            Log.d(TAG, "Trying BLE connection to " + currentDevice.getDisplayName());
            connection = new BLEConnection(currentDevice, this);
            connection.addObserver(this);
            connection.openConnection();
        } else { // GLM 50 C, GLM 100 C or PLR device
            Log.d(TAG, "Trying Classic connection to " + currentDevice.getDisplayName());
            connection = new BluetoothConnection(mtDevice.getDevice());
            connection.addObserver(this);
            connection.openConnection();
        }

        return true;
    }

    /**
     * Close an existing connection
     */
    public void disconnect() {
        if (connection != null) {
            connection.closeConnection();
            synchronized (this) {
                if(connection!=null)
                    connection.removeObserver(this);
            }
            if (connection instanceof BluetoothConnection) {
                connection = null; // set to null only, if not BLEConnetcion
            }
            currentDevice = null;
        }
    }

    /**
     * Get currently discovered Bluetooth devices
     * @return set of visible Bluetooth devices
     */
    public Set<MTBluetoothDevice> getVisibleDevices() {
        return visibleDevices;
    }

    /* (non-Javadoc)
     * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mHandler = new Handler();
        try {
            startDiscovery();
        } catch (BluetoothNotSupportedException e) {
            Log.e(TAG, "Bluetooth not supported");
            e.printStackTrace();
        }

        return START_STICKY;
    }

    /* (non-Javadoc)
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Callback that handles connection state changes
     * @param connection connection, which state changed
     */
    @Override
    public void onConnectionStateChanged(MtAsyncConnection connection) {
        Intent intent = new Intent(ACTION_CONNECTION_STATUS_UPDATE);
        intent.putExtra(EXTRA_CONNECTION_STATUS, connection.getState());
        intent.putExtra(EXTRA_DEVICE, currentDevice.getDisplayName());
        sendBroadcast(intent);


        if(connection.getState() == MtAsyncConnection.STATE_NONE){
            if(this.connection != null){
                this.connection.removeObserver(this);
                this.connection.closeConnection();
            }
        }
        if (connection.getState() == MtAsyncConnection.STATE_TIMEOUT) { // retry on timeout (error 133)
            Log.w(TAG, "Timeout -> retry");
            if (currentDevice != null) {
                try {
                    connect(currentDevice);
                } catch (BluetoothNotSupportedException be) {
                    Log.e(TAG, "Bluetooth not supported");
                    be.printStackTrace();
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see com.bosch.glm100c.easy_connect.bluetooth.IBleDeviceScanner#onDeviceDiscovered
     */
    @Override
    public void onDeviceDiscovered(IScanResult result) {
        checkDiscoveredDevice(result.getDevice(), result.getRawScanRecord());
    }

    /**
     * Will check if service is connected
     * @return true if service connected, false otherwise
     */
    public boolean isConnected(){
        return connection != null && connection.getState() == MtAsyncConnection.STATE_CONNECTED;
    }

    /**
     * Checks for every discovered device if it is a valid Bosch GLM device and (if true) adds it to the discovered devices list
     * @param device discovered Bluetooth device
     * @param scanRecord full scan record of the discovered device
     */
    private void checkDiscoveredDevice(BluetoothDevice device, byte[] scanRecord) {
        if (getConnectionState() == STATE_CONNECTING) {
            return;
        }
        Log.i(TAG, "BLE Device found: " + (device.getName() == null ? "NULL" : device.getName())
                + "; address = " + (device.getAddress() == null ? "NULL" : device.getAddress())
                + "; scan record = " + (scanRecord == null ? "NULL" : MTDeviceParserImpl.bytesToHex(scanRecord)));

        MTBluetoothDevice foundDevice = null;

        // Process to identify, if MirX based device is visible
        MTDeviceParser parsedDev = new MTDeviceParserImpl(device.getName(), scanRecord);
        //Log.i(TAG, parsedDev.toString());
        if (parsedDev.isConnectable() && parsedDev.getDisplayName() != null && BluetoothUtils.validateDeviceName(parsedDev.getDisplayName())) {
            foundDevice = new MTBluetoothDevice(device, parsedDev.getDisplayName());
            Log.i(TAG, "BLE Device found: " + (foundDevice.getDisplayName() + " x" + parsedDev.getSerialNr()));
        }

        // Process to identify, if PAN 1026 based device is visible
        if (device.getName() != null && BluetoothUtils.validateDeviceName(device.getName())) {
            foundDevice = new MTBluetoothDevice(device, device.getName());
            Log.i(TAG, "BLE Device found: " + (foundDevice.getDisplayName()));
        }

        if (foundDevice != null && (BluetoothUtils.validateGLM50Name(foundDevice) || BluetoothUtils.validateGLM100Name(foundDevice)
                || BluetoothUtils.validatePLRName(foundDevice) || BluetoothUtils.validateGISName(foundDevice)
                || BluetoothUtils.validateGLM120Name(foundDevice) || BluetoothUtils.validateGLM50_2Name(foundDevice))) {
            boolean isNew = true;
            for (MTBluetoothDevice dev : visibleDevices) {
                if (dev.getDevice().getAddress().equals(foundDevice.getDevice().getAddress())) {
                    isNew = false;
                }
            }
            if (isNew) {
                visibleDevices.add(foundDevice);
                Intent i = new Intent(ACTION_DEVICE_LIST_UPDATED);
                sendBroadcast(i);
            }
        }
    }

    /**
     * Helper method to initialize the proper BLE scanner depending on Android phone version, as scanner
     * interface changed with API Level 21 and BLE is supported from API Level 18 on.
     */
    private void initializeDeviceScanner() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Create scanner
            mBleDeviceScanner = new BleDeviceScanner21(btAdapter);
            // Create settings and filters (Lollipop or higher) for discovery
            scanSettingsForDiscovery = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
            scanFiltersForDiscovery = new ArrayList<>();
            // Leave filter list empty, as also devices that don't advertise their service UUID must be discovered (e.g. Bosch GLM 50 C)
        } else {
            // Create scanner
            mBleDeviceScanner = new BleDeviceScanner18(btAdapter);
        }
        // Bind callback handler
        mBleDeviceScanner.addOnDeviceDiscoveredHandler(this);
        // Enable MT filter
        mBleDeviceScanner.enableMtFilter(false);
    }

    /**
     * Operating function to start or stop BLE device scan.
     * Triggering scan will start scanning for a period of {@value #PERIOD_TO_SCAN_IN_MS} and quit scan after this period.
     * Stopping scan will quit scan immediately
     * @param enable will trigger BLE scanner if true and stop the scanner otherwise
     */
    public void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.w(TAG, "STOPPING BLE SCAN");
                    scanLeDevice(false);
                }
            }, PERIOD_TO_SCAN_IN_MS);
            Log.w(TAG, "BLE SCAN STARTED");
            visibleDevices.clear();
            mBleDeviceScanner.startScan();
            isScanning = true;
        } else {
            Log.w(TAG, "BLE SCAN STOPPED");
            mBleDeviceScanner.stopScan();
            isScanning = false;

            // update device lists
            Intent i = new Intent(ACTION_DEVICE_LIST_UPDATED);
            sendBroadcast(i);
        }
    }

    /**
     * @return Currently connected Bluetooth device
     */
    public MTBluetoothDevice getCurrentDevice() {
        return currentDevice;
    }

    /**
     * Will return the current Connection
     * @return the connection
     */
    public MtConnection getConnection() {
        return connection;
    }

    /**
     * Will return current connection state
     * @return connection state as integer from MTAsyncConnection state list
     */
    public int getConnectionState() {
        if (!isBluetoothEnabled()) {
            return BluetoothAdapter.STATE_OFF;
        } else {
            if (this.connection != null) {
                return this.connection.getState();
            } else {
                return BluetoothAdapter.STATE_ON;
            }
        }
    }

    /**
     * Checks if Bluetooth is enabled (switched on) on the Android device
     * @return true if adapter is enabled, false otherwise
     */
    public boolean isBluetoothEnabled() {
        try {
            return this.getBluetoothAdapter().isEnabled();
        } catch (final BluetoothNotSupportedException e) {
            Log.e(TAG, "BluetoothNotSupportedException ", e);
        }
        return false;
    }

    /**
     * Gets the system Bluetooth Adapter. Throws exception if adapter not available
     * @return bluetoothAdapter
     * @throws BluetoothNotSupportedException if Bluetooth not supported
     */
    private BluetoothAdapter getBluetoothAdapter() throws BluetoothNotSupportedException {
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            throw new BluetoothNotSupportedException();
        }
        return bluetoothAdapter;
    }

    /**
     * Binder necessary to start and bind BLEService
     */
    public class BLELocalBinder extends Binder {
        public BLEService getService() {
            return BLEService.this;
        }
    }

    /**
     * Triggers Bluetooth enable on the Android device
     * @param activity current activity
     * @return false, if Bluetooth not supported or adapter not active; true otherwise
     */
    public boolean enableBluetooth(Activity activity) {
        try {
            if (btAdapter == null) {
                btAdapter = this.getBluetoothAdapter();
            }
        } catch (BluetoothNotSupportedException be) {
            Log.e(TAG, "BluetoothNotSupportedException ", be);
            return false;
        }
        if (!btAdapter.isEnabled()) {
            final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return false;
        }
        return true;
    }
}
