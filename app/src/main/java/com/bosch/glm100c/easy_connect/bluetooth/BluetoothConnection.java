package com.bosch.glm100c.easy_connect.bluetooth;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.bosch.mtprotocol.glm100C.connection.MtAsyncConnection;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.util.Log;

public class BluetoothConnection implements MtAsyncConnection {
	private static final String TAG = "BluetoothConnection";
	private static final String APP_UUID = "00001101-0000-1000-8000-00805F9B34FB";

	private int connectionState = STATE_NONE;
	private BluetoothDevice bluetoothDevice;
	private BluetoothSocket bluetoothSocket;
	// connection thread
	private ConnectThread connectThread;
	
	private List<MTAsyncConnectionObserver> observers = new ArrayList<MTAsyncConnectionObserver>();
	
	/**
	 * @param bluetoothDevice
	 */
	public BluetoothConnection(BluetoothDevice bluetoothDevice) {
		super();
		this.bluetoothDevice = bluetoothDevice;
	}

	/* (non-Javadoc)
	 * @see de.bosch.mtprotocol.glm100c.impl.MTConnection#getState()
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

	/* (non-Javadoc)
	 * @see de.bosch.mtprotocol.glm100c.impl.MTConnection#openConnection()
	 */
	@Override
	public void openConnection() {
		
		synchronized (this) {
			Log.d(TAG, "open connectection to: " + bluetoothDevice.getName());

			// Cancel any thread attempting to make a connection
			if (connectThread != null) {
				connectThread.cancel();
				connectThread = null;
			}

			setState(STATE_CONNECTING);

			// Start the thread to connect with the given device
			connectThread = new ConnectThread(bluetoothDevice);
			connectThread.setPriority(Thread.MIN_PRIORITY);
			connectThread.start();
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

		synchronized (this) {
			if (connectThread != null) {
				connectThread.cancel();
				connectThread = null;
			}

			if (bluetoothSocket != null) {
				try {
					bluetoothSocket.getInputStream().close();
					bluetoothSocket.getOutputStream().close();
					bluetoothSocket.close();
					
				} catch (IOException e) {
					Log.e(TAG, "close() of connect socket failed", e);
				}finally{
					bluetoothSocket = null;
				}
			}
			
			setState(STATE_NONE);
		}
		
		
	}


	@Override
	public int read(byte[] buffer) throws IOException {
		if(bluetoothSocket != null){
			return bluetoothSocket.getInputStream().read(buffer);
		}
		return 0;
	}

	/* (non-Javadoc)
	 * @see de.bosch.mtprotocol.glm100c.impl.MTConnection#write(byte[])
	 */
	@Override
	public void write(byte[] data) throws IOException {
		if(bluetoothSocket != null){
			bluetoothSocket.getOutputStream().write(data);
		}
	}

	/*
	 * Indicate that the connection attempt failed
	 */
	private void connectionFailed() {
		setState(STATE_NONE);
	}
	
	/*
	 * @param socket
	 *            The BluetoothSocket on which the connection was made
	 * @param device
	 *            The BluetoothDevice that has been connected
	 */
	private synchronized void connected(BluetoothSocket socket,
			BluetoothDevice device) {
		Log.d(TAG, "connected");
		
		// Cancel the thread that completed the connection
		if (connectThread != null) {
			connectThread.cancel();
			connectThread = null;
		}

		this.bluetoothSocket = socket;
		setState(STATE_CONNECTED);
	}
	
	/**
	 * @param observer
	 */
	@Override
	public void addObserver(MTAsyncConnectionObserver observer) {
		observers.add(observer);
	}

	/**
	 * @param observer
	 */
	@Override
	public void removeObserver(MTAsyncConnectionObserver observer) {
		observers.remove(observer);
	}
	
	
	private class ConnectThread extends Thread {
		private final BluetoothSocket mSocket;
		private final BluetoothDevice mDevice;

		
		@SuppressLint("NewApi")
		ConnectThread(BluetoothDevice device) {
			mDevice = device;
			BluetoothSocket tmp = null;

			// Get a BluetoothSocket for a connection with the
			// given BluetoothDevice
			try {

				if(Build.VERSION.SDK_INT >= 10){

					tmp = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(APP_UUID));
				}else{
					// The createRFcommSocketToServiceRecord method is not working
					// in Android 2.1/2.2.
					// Use a workaround to call a non-public method to create the
					// socket

					Method m = device.getClass().getMethod("createRfcommSocket",
							new Class[] { int.class });
					tmp = (BluetoothSocket) m.invoke(device, Integer.valueOf(1));
				}

			} catch (Exception e) {
				Log.e(TAG, "create() failed", e);
			}
			mSocket = tmp;
		}

		@Override
		public void run() {
			Log.i(TAG, "BEGIN ConnectThread");
			setName("ConnectThread");

			if(mSocket == null){
				connectionFailed();
				return;
			}
			
			// Make a connection to the BluetoothSocket
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				mSocket.connect();
			} catch (IOException e) {
				Log.e(TAG, "Unable to connect", e);
				connectionFailed();
				try {
					// Close the socket
					mSocket.close();
				} catch (IOException e2) {
					e2.printStackTrace();
					Log.e(TAG, "unable to close() socket during connection failure", e2);
				}

				return;
			}

			// Reset the ConnectThread because we're done
			synchronized (BluetoothConnection.this) {
				connectThread = null;
			}

			// Start the connected thread
			connected(mSocket, mDevice);
		}

		protected void cancel() {
			Log.i(TAG, "ConnectThread CANCEL");

			try {
				if(mSocket != null){
					mSocket.getInputStream().close();
					mSocket.getOutputStream().close();
					mSocket.close();
				}
				Log.i(TAG, "ConnectThread CLOSED");
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}


	/**
	 * @return the bluetoothDevice
	 */
	public BluetoothDevice getBluetoothDevice() {
		return bluetoothDevice;
	}
}
