package com.bosch.glm100c.easy_connect.bluetooth;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.bosch.mtprotocol.glm100C.connection.MtAsyncBLEConnection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BLEConnection implements MtAsyncBLEConnection {
	private static final String TAG = "BLEConnection";

	private static final UUID DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

	// Service and characteristic UUIDs
	private static final UUID MIRX_SERVICE_UUID_GENERIC = UUID.fromString("02A6C0D0-0451-4000-B000-FB3210111989");
	private static final UUID MIRX_CHAR_UUID_LEGACY = UUID.fromString("02A6C0D1-0451-4000-B000-FB3210111989");

	private static final int CONNECTING_TIMEOUT_PERIOD = 7000;
	
	private final static String ACTION_DATA_NOTIFY = "com.bosch.glm100c.easy_connect.ACTION_DATA_NOTIFY";
	private final static String ACTION_DATA_WRITE = "com.bosch.glm100c.easy_connect.ACTION_DATA_WRITE";
	private final static String EXTRA_DATA = "com.bosch.glm100c.easy_connect.EXTRA_DATA";
	private final static String EXTRA_UUID = "com.bosch.glm100c.easy_connect.EXTRA_UUID";
	private final static String EXTRA_STATUS = "com.bosch.glm100c.easy_connect.EXTRA_STATUS";

	private int connectionState = STATE_NONE;
	private MTBluetoothDevice bluetoothDevice;
	private BluetoothGatt bluetoothGatt;

	private BluetoothGattService genericService = null;
	
	private BluetoothGattCharacteristic genericChar = null;

	private Context context;

	private List<MTAsyncConnectionObserver> observers = new ArrayList<>();
	private List<MTAsyncBLEConnectionCallbackRecipient> callbackRecipients = new ArrayList<>();

	/**
	 * @param bluetoothDevice The device to connect to
	 * @param context The current context
	 */
	BLEConnection(MTBluetoothDevice bluetoothDevice, Context context) {
		super();
		this.bluetoothDevice = bluetoothDevice;
		this.context = context;
	}

	/* (non-Javadoc)
	 * @see de.bosch.mtprotocol.glm100c.impl.MTAsyncConnection#getState()
	 */
	@Override
	public int getState() {
		return connectionState;
	}

	private synchronized void setState(int connectionState) {
		if(this.connectionState != connectionState){
			Log.d(TAG, "setState() " + this.connectionState + " -> " + connectionState);
			this.connectionState = connectionState;
			notifyObservers();
		}
	}

	private void notifyObservers() {
		for (MTAsyncConnectionObserver o : observers) {
			o.onConnectionStateChanged(this);
		}
	}
	
	private void forwardBLECallback(byte[] data) {
		for (MTAsyncBLEConnectionCallbackRecipient r : callbackRecipients) {
			r.onBLECharacteristicChanged(data);
		}
	}

	private void forwardWriteFinished(int status) {
		for (MTAsyncBLEConnectionCallbackRecipient r : callbackRecipients) {
			r.onBLEWriteFinished(status);
		}
	}

	/* (non-Javadoc)
	 * @see de.bosch.mtprotocol.glm100c.impl.MTConnection#openConnection()
	 */
	@Override
	public void openConnection() {
		Log.d(TAG, "Open ble connection to: " + bluetoothDevice.getDisplayName());

		setState(STATE_CONNECTING);

		Handler connectingTimeout = new Handler(Looper.getMainLooper());
		connectingTimeout.postDelayed (new Runnable() {
		    @Override
		    public void run() {
				Log.w(TAG, "STATE_CONNECTING timeout expired: check connection state");
				if (getState() == STATE_CONNECTING) {
					Log.w(TAG, "Too long in STATE_CONNECTING state -> disconnect");
					retryConnection();
				}
		    }
		}, CONNECTING_TIMEOUT_PERIOD);

		// Try to connect
		if (bluetoothGatt == null) {
			Handler connectionHandler = new Handler(Looper.getMainLooper());
			connectionHandler.post(new Runnable() {
				@Override
				public void run() {
					Log.i(TAG, "GATT == NULL");
					boolean autoConnect = true;
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
						autoConnect = false;
					}
					//bluetoothGatt = bluetoothDevice.getDevice().connectGatt(context, true, bleGattCallback);
					bluetoothGatt = bluetoothDevice.getDevice().connectGatt(context, autoConnect, bleGattCallback);
					Log.i(TAG, "GATT is " + (bluetoothGatt == null ? "NULL" : "existing") + "; autoConnect = " + (autoConnect ? "TRUE" : "FALSE"));
				}
			});
		}
	}

	/* (non-Javadoc)
	 * @see de.bosch.mtprotocol.MtConnection#isOpen()
	 */
	@Override
	public boolean isOpen() {
		return getState() == STATE_CONNECTED;
	}

	/* (non-Javadoc)
	 * @see de.bosch.mtprotocol.glm100c.impl.MTConnection#closeConnection()
	 */
	@Override
	public void closeConnection() {

		setState(STATE_NONE);
		if (bluetoothGatt != null) {
			Log.d(TAG, "Closing Bluetooth Gatt...");
			bluetoothGatt.disconnect();
			bluetoothGatt.close();
			//bluetoothGatt = null;
		}
	}

	/* (non-Javadoc)
	 * @see de.bosch.mtprotocol.glm100c.impl.MTConnection#read(byte[])
	 */
	@Override
	public int read(byte[] buffer) {
		// Unnecessary for BLE connection - read will be done with callback. Legacy code
		return 0;
	}

	/* (non-Javadoc)
	 * @see de.bosch.mtprotocol.glm100c.impl.MTConnection#write(byte[])
	 */
	@Override
	public void write(final byte[] data) throws IOException { // Legacy version
		Log.i(TAG, "Write operation started for data with length " + data.length + " Bytes and content: " + MTDeviceParserImpl.bytesToHex(data));
		boolean success = this.writeCharacteristic(genericChar, data);
		if (!success) {
			throw new IOException();
		}
	}

	/**
	 * @param observer Observer, that will get callbacks from the BLEConnection
	 */
	@Override
	public void addObserver(MTAsyncConnectionObserver observer) {
		observers.add(observer);
	}

	/**
	 * @param observer Observer, that will get callbacks from the BLEConnection
	 */
	@Override
	public void removeObserver(MTAsyncConnectionObserver observer) {
		observers.remove(observer);
	}
	
	@Override
	public void addCallbackRecipient(MTAsyncBLEConnectionCallbackRecipient recipient) {
		callbackRecipients.add(recipient);
	}
	
	@Override
	public void removeCallbackRecipient(MTAsyncBLEConnectionCallbackRecipient recipient) {
		callbackRecipients.remove(recipient);
	}

	private final BluetoothGattCallback bleGattCallback = new BluetoothGattCallback() {

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
			// this will get called anytime you perform a read or write characteristic operation
			byte[] data = characteristic.getValue();
			Log.i(TAG, "On characteristic changed for " + characteristic.getUuid().toString() + "; value: " + MTDeviceParserImpl.bytesToHex(data));
			
			if (characteristic.getUuid().equals(MIRX_CHAR_UUID_LEGACY)) {
				Log.i(TAG, "Start callback to MTProtocol");
				forwardBLECallback(data);
			}
			
			broadcastUpdate(ACTION_DATA_NOTIFY, characteristic, BluetoothGatt.GATT_SUCCESS);
		}

		@Override
		public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) { 
			// Handle connect and disconnect
			if (status == BluetoothGatt.GATT_SUCCESS) {
				switch (newState) {
				case BluetoothProfile.STATE_CONNECTED:
					if (connectionState == STATE_CONNECTED) {
						Log.w(TAG, "Already connected - no need to discover services.");
						return;
					}
					Log.i(TAG, "CONNECTING; DISCOVER SERVICES");
					boolean success = gatt.discoverServices();
					if (!success) {
						retryConnection();
					}
					break;
				case BluetoothProfile.STATE_DISCONNECTED:
					Log.e(TAG, "STATE_DISCONNECTED");
					setState(STATE_NONE);
					break;
				default:
					Log.e(TAG, "STATE_UNKNOWN");
					setState(STATE_NONE);
				}
			} else {
				Log.e(TAG, "onConnectionStateChange error with status: " + status);
				if (status == 133) {
					retryConnection();
				} else {
					closeConnection();
				}
				//setState(STATE_NONE);
			}
		}

		@Override
		public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
			if (connectionState == STATE_CONNECTED) {
				Log.w(TAG, "All services discovered and connection running - no need to discover additional services");
				return;
			}
			// Find necessary services
			List<BluetoothGattService> services = gatt.getServices();
			Log.i("onServicesDiscovered", services.toString());
			//Log.i(TAG, "Miraculix UUID: " + BLEService.MIRACULIX_SERVICE_UUID.toString());
			String uuids = "Detected UUIDs:\n";
			for (int i = 0; i < services.size(); i++) {
				Log.i(TAG, "Service discovered: " + services.get(i).getUuid().toString());
				String uuidName = services.get(i).getUuid().toString();
				uuids += uuidName + ";\n";
				if (uuidName.equals(MIRX_SERVICE_UUID_GENERIC.toString())) {
					Log.i(TAG, "Found generic service with UUID " + services.get(i).getUuid().toString());
					genericService = services.get(i);
				}
			}
			Log.d(TAG, uuids);
			
			if (genericService != null) { // Generic service found
				Log.i(TAG, "Resolving generic characteristics...");
				for (int i = 0; i < genericService.getCharacteristics().size(); i++) {
					Log.i(TAG, "Char discovered: " + genericService.getCharacteristics().get(i).getUuid().toString());
					// Get notifications for generic characteristic
					if (genericService.getCharacteristics().get(i).getUuid().equals(MIRX_CHAR_UUID_LEGACY)) {
						genericChar = genericService.getCharacteristics().get(i);
						boolean success = setCharacteristicIndication(genericChar, true);
						if (success) {
							Log.i(TAG, "STATE_CONNECTED");
							setState(STATE_CONNECTED);
						} else {
							retryConnection();
						}
						//gatt.readCharacteristic(genericChar);
					}	
				}
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			Log.i(TAG, "On characteristic read for " + characteristic.getUuid().toString());
		}
		
		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			forwardWriteFinished(status);
			Log.i(TAG, "On characteristic write for " + characteristic.getUuid().toString());
			broadcastUpdate(ACTION_DATA_WRITE, characteristic, status);
		}
	};
	
	private boolean setCharacteristicIndication(BluetoothGattCharacteristic characteristic, boolean enable) {
		if (bluetoothGatt != null) {
			boolean success;
			success = bluetoothGatt.setCharacteristicNotification(characteristic, enable);
			Log.i(TAG, "Setting indication " + characteristic.getUuid().toString() + (success ? " success!" : " NO"));
			success = false;
			BluetoothGattDescriptor descriptor = characteristic.getDescriptor(DESCRIPTOR_UUID);
			if (descriptor != null) {
				descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
				success = bluetoothGatt.writeDescriptor(descriptor);
			}
			Log.i(TAG, "Setting indication descriptor " + characteristic.getUuid().toString() + (success ? " success!" : " NO"));
			return success;
		}
		return false;
	}
	
	private boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data) {
		characteristic.setValue(data);
		return bluetoothGatt.writeCharacteristic(characteristic);
	}
	
	private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic, final int status) {
		final Intent intent = new Intent(action);
		intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());
		intent.putExtra(EXTRA_DATA, characteristic.getValue());
		intent.putExtra(EXTRA_STATUS, status);
		context.sendBroadcast(intent);
	}

	private void retryConnection() {
		closeConnection();
		setState(STATE_TIMEOUT);
	}
}
