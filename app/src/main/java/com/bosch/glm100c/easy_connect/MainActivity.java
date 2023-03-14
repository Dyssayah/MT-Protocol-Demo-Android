package com.bosch.glm100c.easy_connect;

import static com.bosch.glm100c.easy_connect.bluetooth.BLEService.REQUEST_ENABLE_BT;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;

import android.util.Log;

import android.view.Menu;
import android.view.MenuItem;

import android.view.View;

import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bosch.glm100c.easy_connect.bluetooth.BLEService;
import com.bosch.glm100c.easy_connect.bluetooth.MTBluetoothDevice;
import com.bosch.glm100c.easy_connect.exc.BluetoothNotSupportedException;
import com.bosch.mtprotocol.glm100C.connection.MtAsyncConnection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends Activity implements OnItemClickListener {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_CODE_ASK_PERMISSIONS_LOCATION = 41;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 66;
    private static final int PERMISSION_REQUEST_BACKGROUND_LOCATION = 44;

    private static final int PERMISSION_REQUEST_BLUETOOTH_SCAN = 91;

    private GLMDeviceArrayAdapter deviceArrayAdapter;
    private List<MTBluetoothDevice> devices = new ArrayList<>();
    private BLEService btService;
    private GLMDeviceController deviceController;
    private TextView measTextView;
    private TextView devTextView;

    private ProgressBar spinner;
    private String urlcallback;
    private Toast tosta;

    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Request Bluetooth enable
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, do nothing
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_COARSE_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN},
                    PERMISSION_REQUEST_BLUETOOTH_SCAN);
        }

/*// Request background location permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        PERMISSION_REQUEST_BACKGROUND_LOCATION);
            }
        }*/

        Intent serviceIntent = new Intent(this, BLEService.class);
        startService(serviceIntent);
        Intent mIntent = new Intent(this, BLEService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);

        deviceArrayAdapter = new GLMDeviceArrayAdapter(this, R.layout.item_device, 0, devices);
        ListView deviceListView = findViewById(R.id.device_list_view);
        deviceListView.setAdapter(deviceArrayAdapter);
        deviceListView.setOnItemClickListener(this);
        measTextView = findViewById(R.id.measurement_text_view);
        devTextView = findViewById(R.id.device_text_view);

    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
        Uri data = intent.getData();
        if (data != null) {
            urlcallback = data.getQueryParameter("callback");
            Log.d("MainActivity", "URLCALLBACK: " + urlcallback);
        }
    }

    public void sendData(final String urlString, final String data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d("MainActivity", "STRING DO URL: " + urlString);
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setDoOutput(true);

                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"));
                    HashMap<String, String> postDataParams = new HashMap<>();
                    postDataParams.put("valor", data);

                    writer.write(getPostDataString(postDataParams));
                    writer.flush();
                    writer.close();

                    int responseCode = conn.getResponseCode();
                    BufferedReader br = null;
                    if (100 <= responseCode && responseCode <= 399) {
                        br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        Log.d("DataSender", br.toString());
                    } else {
                        br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        Log.d("DataSender", br.toString());
                    }
                } catch (Exception e) {
                    Log.e("DataSender", "Error sending data", e);
                }
            }
        }).start();
    }

    public String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first)
                first = false;
            else
                result.append("&");
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        return result.toString();
    }

    private boolean mBound = false;


    @Override
    public void onBackPressed() {
        moveTaskToBack(false);
    }


    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, BLEService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    public void startForegroundService() {
        if (mBound) {
            Intent intent = new Intent(this, MyForegroundService.class);
            intent.putExtra("bleService", (Parcelable) btService);
            startForegroundService(intent);

        }
    }


    @Override
    public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {
        tosta = Toast.makeText(this, "A connectar ao dispositivo", Toast.LENGTH_SHORT);

        tosta.show();
        // If app is connecting - do nothing
        if (btService.getConnectionState() == MtAsyncConnection.STATE_CONNECTING) {
            Log.w(TAG, "App is connecting, no connection started");
            return;
        }

        MTBluetoothDevice device = deviceArrayAdapter.getItem(position);

        // To save the MTBluetoothDevice
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String deviceAddress = device.getDevice().getAddress(); // get the device's MAC address
        String deviceName = device.getDisplayName(); // get the device's name

        editor.putString("myDeviceAddress", deviceAddress); // save the MAC address as a string
        editor.putString("myDeviceName", deviceName); // save the device name as a string
        editor.apply();

        connectToDevice(device);
    }

    private void connectToDevice(MTBluetoothDevice device) {
        try {
            assert device != null;
            Log.d(TAG, "Selected device " + device.getDisplayName() + "; MAC = " + device.getDevice().getAddress());

            // If already connected to selected device -> disconnect
            if (btService.getConnectionState() == MtAsyncConnection.STATE_CONNECTED && btService.getCurrentDevice().getDevice().getAddress() != null
                    && device.getDevice().getAddress().equals(btService.getCurrentDevice().getDevice().getAddress())) {
                btService.disconnect();
                refreshDeviceList();
                Log.d(TAG, "App already connected to " + device.getDisplayName() + " so only disconnect");
                return;
            }


            // If not connected or selected device is not the connected device -> start connection
            if (btService.connect(device)) {
                startForegroundService();
            }


        } catch (BluetoothNotSupportedException e) {
            Log.e(TAG, "BluetoothNotSupportedException", e);
        }

    }

    /**
     * Binds the Bluetooth service to the activity
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BLEService.BLELocalBinder binder = (BLEService.BLELocalBinder) service;
            btService = binder.getService();
            mBound = true;


            // To read the MTBluetoothDevice
            SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
            String deviceAddress = sharedPreferences.getString("myDeviceAddress", null);
            String deviceName = sharedPreferences.getString("myDeviceName", null);


            if (deviceAddress != null && deviceName != null) {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice bluetoothDevice = adapter.getRemoteDevice(deviceAddress);
                MTBluetoothDevice device = new MTBluetoothDevice(bluetoothDevice, deviceName);

                int bondState = bluetoothDevice.getBondState();

                Log.d("wow", "" + bondState);
                Log.d("wowie", "" + BluetoothDevice.BOND_BONDED);
                if (bondState == BluetoothDevice.BOND_BONDED) {

                    connectToDevice(device);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            btService = null;
            mBound = false;
        }
    };

    /**
     * Initializes the GLMDeviceController class, that will handle messages from and to GLM device
     */
    private void setupDeviceController() {
        if (btService.isConnected()) {
            if (deviceController == null) {
                deviceController = new GLMDeviceController(btService);
                deviceController.init(btService.getConnection(), btService.getCurrentDevice());
            } else {
                destroyDeviceController();
                setupDeviceController();
            }
        } else {
            destroyDeviceController();
        }
    }

    /**
     * Destroys the GLMDeviceController, when it is not needed anymore
     */
    private void destroyDeviceController() {
        if (deviceController != null) {
            deviceController.destroy();
            deviceController = null;
        }
    }


    @Override
    protected void onPause() {

        super.onPause();

//            unregisterReceiver(mReceiver);

        // stop Bluetooth scan
        Log.w(TAG, "Device activity on pause: cancel discovery");
        if (btService != null) {
            btService.cancelDiscovery();
        }
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onResume() {
        super.onResume();

        setTitle(R.string.app_name);

        refreshDeviceList();

        // register receivers
        IntentFilter filter = new IntentFilter(BLEService.ACTION_DEVICE_LIST_UPDATED);
        this.registerReceiver(mReceiver, filter);
        filter = new IntentFilter(BLEService.ACTION_CONNECTION_STATUS_UPDATE);
        this.registerReceiver(mReceiver, filter);
        filter = new IntentFilter(GLMDeviceController.ACTION_SYNC_CONTAINER_RECEIVED);
        this.registerReceiver(mReceiver, filter);
        filter = new IntentFilter(GLMDeviceController.ACTION_THERMAL_CONTAINER_RECEIVED);
        this.registerReceiver(mReceiver, filter);

        // check if location permission available and request, if not
        int hasLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (hasLocationPermission != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            return;
        }

       /* // check if Bluetooth on and start it, if necessary
        int hasScanningPermission = checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN);
        if (hasScanningPermission != PackageManager.PERMISSION_GRANTED) {
            requestScanningPermission();
            return;
        }*/
        if (btService != null && btService.enableBluetooth(this)) {
            // start Bluetooth scan
            Log.w(TAG, "Device activity on resume: start discovery");
            startDiscovery();
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                startDiscovery();
            } else {
                Toast.makeText(this, getString(R.string.bluetooth_on_denied), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION:
            case PERMISSION_REQUEST_BACKGROUND_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (btService != null && btService.enableBluetooth(this)) {
                        // start Bluetooth scan
                        Log.w(TAG, "Device activity on permission result: start discovery");
                        startDiscovery();
                    }
                } else {
                    for (int i = 0; i < permissions.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            String failedPermission = permissions[i];
                            Toast.makeText(this, "Permissão " + failedPermission + " foi negada.", Toast.LENGTH_LONG).show();
                        }
                    }

                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    private void requestLocationPermission() {
        if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            showPermissionMessageOKCancel(getString(R.string.request_location_permission),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_ASK_PERMISSIONS_LOCATION);
                        }
                    });
            return;
        }
        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_ASK_PERMISSIONS_LOCATION);
    }
/*
    @RequiresApi(api = Build.VERSION_CODES.S)
    private void requestScanningPermission() {
        if (!shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_SCAN)) {
            showPermissionMessageOKCancel(getString(R.string.request_scanning_permission),
                    new DialogInterface.OnClickListener() {
                        @RequiresApi(api = Build.VERSION_CODES.S)
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, PERMISSION_REQUEST_BLUETOOTH_SCAN);
                        }
                    });
            return;
        }
        requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, PERMISSION_REQUEST_BLUETOOTH_SCAN);
    }*/

    private void showPermissionMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok), okListener)
                .setNegativeButton(getString(R.string.cancel), null)
                .create()
                .show();
    }

    /**
     * The BroadcastReceiver that handles notifications for discovered devices and new measurements
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent != null && BLEService.ACTION_CONNECTION_STATUS_UPDATE.equals(intent.getAction())) {

                // Device was connected or disconnected - handle device list accordingly
                refreshDeviceList();
                if (deviceArrayAdapter != null) {
                    deviceArrayAdapter.notifyDataSetChanged();
                }
                // If device was connected -> start GLMDeviceController to handle communication
                int connectionStatus = intent.getIntExtra(BLEService.EXTRA_CONNECTION_STATUS, MtAsyncConnection.STATE_NONE);
                if (connectionStatus == MtAsyncConnection.STATE_CONNECTED) {
                    setupDeviceController();
                    String deviceName = intent.getStringExtra(BLEService.EXTRA_DEVICE);
                    devTextView.setText(getResources().getString(R.string.connected_to) + deviceName);
                    onBackPressed();
                } else {
                    destroyDeviceController();
                    devTextView.setText(getResources().getString(R.string.no_device_connected));
                }

            } else if (intent != null && BLEService.ACTION_DEVICE_LIST_UPDATED.equals(intent.getAction())) {

                // Device list updated
                refreshDeviceList();

                spinner = (ProgressBar) findViewById(R.id.progressBarSearch);

                spinner.setVisibility(View.GONE);
            } else if (intent != null && GLMDeviceController.ACTION_SYNC_CONTAINER_RECEIVED.equals(intent.getAction())) {

                // Measurement received
                if (!Objects.requireNonNull(intent.getExtras()).isEmpty()) {
                    float measurement = intent.getFloatExtra(GLMDeviceController.EXTRA_MEASUREMENT, 0);
                    measTextView.setText(Float.toString(measurement) + getResources().getString(R.string.meter));
                    sendData(urlcallback, Float.toString(measurement));
                }

            } else if (intent != null && GLMDeviceController.ACTION_THERMAL_CONTAINER_RECEIVED.equals(intent.getAction())) {

                // Measurement received
                if (!Objects.requireNonNull(intent.getExtras()).isEmpty()) {
                    float measurement = intent.getFloatExtra(GLMDeviceController.EXTRA_MEASUREMENT, 0);
                    measTextView.setText(Float.toString(measurement) + getResources().getString(R.string.degree));
                }

            } else {

                // Received intent is null or not known -> ignore
                Log.w(TAG, "Unknown intent or intent is null: ignore");

            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.refresh:
                // Item will trigger discovery for 5s
                refreshDeviceList();
                startDiscovery();
                return true;
            case R.id.bluetooth_settings:
                // Item will open phone's Bluetooth settings
                Intent settingsIntent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(settingsIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Used to refresh the device list shown. If Bluetooth scanning is not enabled, the list will be empty
     */
    private synchronized void refreshDeviceList() {
        spinner = (ProgressBar) findViewById(R.id.progressBarSearch);


        devices.clear();
        spinner.setVisibility(View.VISIBLE);
        if (btService != null) {
            devices.addAll(btService.getVisibleDevices());
        }

        devices.sort(new Comparator<MTBluetoothDevice>() {

            @Override
            public int compare(MTBluetoothDevice lhs, MTBluetoothDevice rhs) {
                return lhs.getDisplayName().compareToIgnoreCase(rhs.getDisplayName());
            }
        });

        deviceArrayAdapter.notifyDataSetChanged();
    }

    /**
     * Triggers discovery of Bluetooth devices for 5 seconds
     */
    private void startDiscovery() {
        if (btService != null) {
            try {
                btService.startDiscovery();
            } catch (BluetoothNotSupportedException be) {
                Log.e(TAG, "Bluetooth not supported");
                be.printStackTrace();
            }
        }

    }

    /**
     * Get the binded BLEService
     *
     * @return binded BLEService
     */
    public BLEService getBluetoothService() {
        return btService;
    }

}
