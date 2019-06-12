package com.bosch.glm100c.easy_connect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bosch.glm100c.easy_connect.bluetooth.BLEService;
import com.bosch.glm100c.easy_connect.bluetooth.MTBluetoothDevice;
import com.bosch.glm100c.easy_connect.exc.BluetoothNotSupportedException;
import com.bosch.mtprotocol.glm100C.connection.MtAsyncConnection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class MainActivity extends Activity implements OnItemClickListener{

	private static final String TAG = "MainActivity";

	public static final int REQUEST_CODE_ASK_PERMISSIONS_LOCATION = 41;

	private GLMDeviceArrayAdapter deviceArrayAdapter;
	private List<MTBluetoothDevice> devices = new ArrayList<>();
	private BLEService btService;
	private GLMDeviceController deviceController;
	private TextView measTextView;
	private TextView devTextView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Start and bind BLE service here
		Intent serviceIntent = new Intent(this, BLEService.class);
		startService(serviceIntent);
		Intent mIntent = new Intent(this, BLEService.class);
		bindService(mIntent, mConnection, BIND_AUTO_CREATE);
		
		setContentView(R.layout.activity_main);

		deviceArrayAdapter = new GLMDeviceArrayAdapter(this, R.layout.item_device, 0, devices);
		ListView deviceListView = findViewById(R.id.device_list_view);
		deviceListView.setAdapter(deviceArrayAdapter);
		deviceListView.setOnItemClickListener(this);
		measTextView = findViewById(R.id.measurement_text_view);
		devTextView = findViewById(R.id.device_text_view);
	}
	
	@Override
	public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {

		// If app is connecting - do nothing
		if(btService.getConnectionState() == MtAsyncConnection.STATE_CONNECTING) {
			Log.w(TAG, "App is connecting, no connection started");
			return;
		}

		try {
			MTBluetoothDevice device = deviceArrayAdapter.getItem(position);
			assert device != null;
			Log.d(TAG, "Selected device " + device.getDisplayName() + "; MAC = " + device.getDevice().getAddress());

			// If already connected to selected device -> disconnect
			if(btService.getConnectionState() == MtAsyncConnection.STATE_CONNECTED && btService.getCurrentDevice().getDevice().getAddress() != null
					&& device.getDevice().getAddress().equals(btService.getCurrentDevice().getDevice().getAddress())) {
				btService.disconnect();
				refreshDeviceList();
				Log.d(TAG, "App already connected to " + device.getDisplayName() + " so only disconnect");
				return;
			}

			// If not connected or selected device is not the connected device -> start connection
			btService.connect(device);
			
		} catch (BluetoothNotSupportedException e) {
			Log.e(TAG,"BluetoothNotSupportedException",e);
		}

	}

	/**
	 * Binds the Bluetooth service to the activity
	 */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			btService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			BLEService.BLELocalBinder mLocalBinder = (BLEService.BLELocalBinder) service;
			btService = mLocalBinder.getService();
		}
	};
	
	/**
	 * Initializes the GLMDeviceController class, that will handle messages from and to GLM device
	 */
	private void setupDeviceController(){
		if (btService.isConnected()){
			if (deviceController == null){
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
	private void destroyDeviceController(){
		if(deviceController != null){
			deviceController.destroy();
			deviceController = null;
		}
	}

	@Override
	protected void onPause() {

		super.onPause();

		unregisterReceiver(mReceiver);
		
		// stop Bluetooth scan
		Log.w(TAG, "Device activity on pause: cancel discovery");
		if(btService != null) {
			btService.cancelDiscovery();
		}
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
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
		if (Build.VERSION.SDK_INT >= 23) {
			int hasLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
			if (hasLocationPermission != PackageManager.PERMISSION_GRANTED) {
				requestLocationPermission();
				return;
			}
		}

		// check if Bluetooth on and start it, if necessary
		if (btService != null && btService.enableBluetooth(this)) {
			// start Bluetooth scan
			Log.w(TAG, "Device activity on resume: start discovery");
			startDiscovery();
		}
	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == BLEService.REQUEST_ENABLE_BT) {
			if (resultCode == Activity.RESULT_OK) {
				startDiscovery();
			} else {
				Toast.makeText(this,getString(R.string.bluetooth_on_denied), Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		switch (requestCode) {
			case REQUEST_CODE_ASK_PERMISSIONS_LOCATION:
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					// Permission Granted
					if (btService != null && btService.enableBluetooth(this)) {
						// start Bluetooth scan
						Log.w(TAG, "Device activity on permission result: start discovery");
						startDiscovery();
					}
				} else {
					// Permission Denied
					Toast.makeText(this, getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show();
				}
				break;
			default:
				super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}

	@TargetApi(23)
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
		requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_ASK_PERMISSIONS_LOCATION);
	}

	private void showPermissionMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
		new AlertDialog.Builder(this)
				.setMessage(message)
				.setPositiveButton(getString(R.string.ok), okListener)
				.setNegativeButton(getString(R.string.cancel), null)
				.create()
				.show();
	}

	/**
	 *  The BroadcastReceiver that handles notifications for discovered devices and new measurements
	 */
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@SuppressLint("SetTextI18n")
		@Override
		public void onReceive(Context context, Intent intent) {

			if(intent != null && BLEService.ACTION_CONNECTION_STATUS_UPDATE.equals(intent.getAction())) {
				
				// Device was connected or disconnected - handle device list accordingly
				refreshDeviceList();
				if(deviceArrayAdapter != null){
					deviceArrayAdapter.notifyDataSetChanged();
				}
				// If device was connected -> start GLMDeviceController to handle communication
				int connectionStatus = intent.getIntExtra(BLEService.EXTRA_CONNECTION_STATUS, MtAsyncConnection.STATE_NONE);
				if(connectionStatus == MtAsyncConnection.STATE_CONNECTED ){
					setupDeviceController();
					String deviceName = intent.getStringExtra(BLEService.EXTRA_DEVICE);
					devTextView.setText(getResources().getString(R.string.connected_to) + deviceName);
				} else {
					destroyDeviceController();
					devTextView.setText(getResources().getString(R.string.no_device_connected));
				}
				
			} else if(intent != null && BLEService.ACTION_DEVICE_LIST_UPDATED.equals(intent.getAction())) {
				
				// Device list updated
				refreshDeviceList();
				
			} else if(intent != null && GLMDeviceController.ACTION_SYNC_CONTAINER_RECEIVED.equals(intent.getAction())) {
				
				// Measurement received
				if(!Objects.requireNonNull(intent.getExtras()).isEmpty()) {
					float measurement = intent.getFloatExtra(GLMDeviceController.EXTRA_MEASUREMENT, 0);
					measTextView.setText(Float.toString(measurement) + getResources().getString(R.string.meter));
				}
				
			} else if(intent != null && GLMDeviceController.ACTION_THERMAL_CONTAINER_RECEIVED.equals(intent.getAction())) {

                // Measurement received
                if(!Objects.requireNonNull(intent.getExtras()).isEmpty()) {
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
	private synchronized void refreshDeviceList(){

		devices.clear();

		if(btService != null) {
			devices.addAll(btService.getVisibleDevices());
		}

		Collections.sort(devices, new Comparator<MTBluetoothDevice>() {

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
	 * @return binded BLEService
	 */
	public BLEService getBluetoothService() {
		return btService;
	}
}
