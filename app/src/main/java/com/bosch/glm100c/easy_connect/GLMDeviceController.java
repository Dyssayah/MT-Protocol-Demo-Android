package com.bosch.glm100c.easy_connect;

import com.bosch.glm100c.easy_connect.bluetooth.BLEConnection;
import com.bosch.glm100c.easy_connect.bluetooth.BluetoothUtils;
import com.bosch.glm100c.easy_connect.bluetooth.MTBluetoothDevice;
import com.bosch.mtprotocol.MtConnection;
import com.bosch.mtprotocol.MtMessage;
import com.bosch.mtprotocol.MtProtocol;
import com.bosch.mtprotocol.MtProtocol.MTProtocolEvent;
import com.bosch.mtprotocol.MtProtocol.MTProtocolEventObserver;
import com.bosch.mtprotocol.glm100C.MtProtocolBLEImpl;
import com.bosch.mtprotocol.glm100C.MtProtocolImpl;
import com.bosch.mtprotocol.glm100C.event.MtProtocolFatalErrorEvent;
import com.bosch.mtprotocol.glm100C.event.MtProtocolReceiveMessageEvent;
import com.bosch.mtprotocol.glm100C.event.MtProtocolRequestTimeoutEvent;
import com.bosch.mtprotocol.glm100C.message.edc.EDCInputMessage;
import com.bosch.mtprotocol.glm100C.message.edc.EDCOutputMessage;
import com.bosch.mtprotocol.glm100C.message.laser.LaserOffMessage;
import com.bosch.mtprotocol.glm100C.message.laser.LaserOnMessage;
import com.bosch.mtprotocol.glm100C.message.sync.SyncInputMessage;
import com.bosch.mtprotocol.glm100C.message.sync.SyncOutputMessage;
import com.bosch.mtprotocol.thermo.message.edct.EDCTInputMessage;
import com.bosch.mtprotocol.thermo.message.edct.EDCTOutputMessage;

import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class GLMDeviceController implements MTProtocolEventObserver {
	private static final String TAG = "GLMDeviceController";

	private static final String ACTION_ERROR = "ERROR";

	static final String ACTION_SYNC_CONTAINER_RECEIVED = "SYNC_CONTAINER_RECEIVED";

	static final String ACTION_THERMAL_CONTAINER_RECEIVED = "THERMAL_CONTAINER_RECEIVED";

	static final String EXTRA_MEASUREMENT = "MEASUREMENT";

	private Context context;

	private MtProtocol protocol;

	private MTBluetoothDevice bluetoothDevice;

	private boolean initSyncRequest;

	private boolean ready;

	public GLMDeviceController(Context context) {
		super();
		this.context = context;
	}

	/**
	 * Test utility:
	 * Use this method to turn the laser of connected GLM device on
	 */
	public void turnLaserOn(){
		if(isReady()){
			ready = false;
			protocol.sendMessage(new LaserOnMessage());
		}
	}

	/**
	 * Test utility:
	 * Use this method to turn the laser of connected GLM device off
	 */
	public void turnLaserOff(){
		if(isReady()){
			ready = false;
			protocol.sendMessage(new LaserOffMessage());
		}
	}

	/**
	 * Starts sync mode between app and GLM device
	 * When sync mode is started the GLM device will send every event to the app
	 */
	private void turnAutoSyncOn() {
		if (this.isReady()) {
			this.ready = false;
			if(bluetoothDevice!=null) {
				if (BluetoothUtils.validateGLM100Name(bluetoothDevice)) {
					// GLM 100 device
					final SyncOutputMessage requestDoSync = new SyncOutputMessage();
					requestDoSync.setSyncControl(SyncOutputMessage.MODE_AUTOSYNC_CONTROL_ON);
					this.protocol.sendMessage(requestDoSync);
					Log.d(TAG, "Sync started GLM 100...");
				} else if (BluetoothUtils.validateEDCDevice(bluetoothDevice)) {
					// Exchange Data Container (EDC) based device
					final EDCOutputMessage requestEDCSync = new EDCOutputMessage();
					requestEDCSync.setSyncControl(EDCOutputMessage.MODE_AUTOSYNC_CONTROL_ON);
					requestEDCSync.setDevMode(EDCOutputMessage.READ_ONLY_MODE);
					this.protocol.sendMessage(requestEDCSync);
					Log.d(TAG, "Sync started EDC device...");
				} else if (BluetoothUtils.validateGISName(bluetoothDevice)) {
					// GIS device
					final EDCTOutputMessage requestEDCTSync = new EDCTOutputMessage();
					requestEDCTSync.setSyncControl(EDCTOutputMessage.MODE_AUTOSYNC_CONTROL_ON);
					requestEDCTSync.setRemoteMode(EDCTOutputMessage.REMOTE_SET_PING_CYCLE);
					requestEDCTSync.setRemoteCtrlData(20);
					this.protocol.sendMessage(requestEDCTSync);
					Log.d(TAG, "Sync started GIS device...");
				}
			}
		}
	}

	@Override
	public void onEvent(MTProtocolEvent event) {

		ready = true;

		if(event instanceof MtProtocolFatalErrorEvent){

			// fatal error
			Log.d(TAG, "Received MtProtocolFatalErrorEvent");
			protocol.reset();
			context.sendBroadcast(new Intent(ACTION_ERROR));

		} else if(event instanceof MtProtocolReceiveMessageEvent) {

			// received MT message -> act considering message type
			MtMessage message = ((MtProtocolReceiveMessageEvent) event).getMessage();
			if(message instanceof SyncInputMessage) { // Sync Message Type used by GLM 100 C
				SyncInputMessage syncMessage = (SyncInputMessage) message;

				if(initSyncRequest) { // Ignore first response
					initSyncRequest = false;
					return;
				}
				Log.d(TAG, "SyncInputMessageReceived: " + syncMessage.toString());
				if(syncMessage.getMode() == SyncInputMessage.MEAS_MODE_SINGLE && syncMessage.getLaserOn() == 0) {
					// Handle only distance measurements
					broadcastMeasurement(ACTION_SYNC_CONTAINER_RECEIVED, syncMessage.getResult());
				}
			} else if(message instanceof EDCInputMessage) { // Exchange Data Container (EDC) Message Type used by all other connected GLM devices

				if(initSyncRequest) { // Ignore first response
					initSyncRequest = false;
					return;
				}
				Log.d(TAG, "Received EDC: " + message.toString());
				EDCInputMessage edcMessage = (EDCInputMessage) message;
				Log.d(TAG, "EDCInputMessageReceived: " + edcMessage.toString());
				if(edcMessage.getDevMode() == EDCInputMessage.MODE_SINGLE_DISTANCE || edcMessage.getDevMode() == EDCInputMessage.MODE_CONTINUOUS_DISTANCE) {
					// Handle only distance measurements
					broadcastMeasurement(ACTION_SYNC_CONTAINER_RECEIVED, edcMessage.getResult());
				}
			} else if (message instanceof EDCTInputMessage) { // Exchange Data Container for Thermal device (EDCT) message type used by GIS 1000 C

				if(initSyncRequest) { // Ignore first response
					initSyncRequest = false;
					return;
				}
				Log.d(TAG, "EDCT message received from " + bluetoothDevice.getDisplayName());
				handleEDCTMessage((EDCTInputMessage) message);
			} else {

				Log.d(TAG, "Received other message");
			}
		} else if(event instanceof MtProtocolRequestTimeoutEvent){

			// protocol timeout
			Log.d(TAG, "Received MtProtocolRequestTimeoutEvent");
			context.sendBroadcast(new Intent(ACTION_ERROR));
		} else {
			Log.e(TAG, "Received unknown event");
		}
		initSyncRequest = false;
	}

	private void broadcastMeasurement(String action, float value)
	{
		Intent i = new Intent(action);
		i.putExtra(EXTRA_MEASUREMENT, value);
		context.sendBroadcast(i);
	}

	/**
	 * Delivers information if MT Protocol is ready for communication
	 * @return TRUE, if protocol is ready and FALSE otherwise
	 */
	private boolean isReady(){
		return protocol != null && ready;
	}

	/**
	 * Initializes the device controller. Must be called once before using the controller
	 * @param connection current connection
	 * @param btDevice connected device
	 */
	void init(MtConnection connection, MTBluetoothDevice btDevice){

		destroy();

		this.bluetoothDevice = btDevice;

		if (connection instanceof BLEConnection) {
			// MirX based device
			protocol = new MtProtocolBLEImpl();
		} else {
			// PAN 1026 based device
			protocol = new MtProtocolImpl();
		}
		protocol.addObserver(this);
		protocol.setTimeout(5000);
		protocol.initialize(connection);

		ready = true;

		initSyncRequest = true;
		turnAutoSyncOn();
	}

	/**
	 * Destroys the device controller. Always call after BluetoothConnection is lost
	 */
	void destroy(){

		if(protocol != null){
			protocol.removeObserver(this);
			protocol.destroy();
			protocol = null;
		}
	}

	/**
	 * Return the current Bluetooth device
	 * @return connected Bluetooth device if any; null otherwise
	 */
	public MTBluetoothDevice getBTDevice() {
		return bluetoothDevice;
	}

	/**
	 * GIS 1000 C sends one measurement in 4 EDCT messages which come consecutively, when measurement triggered on the device.
	 * Refer to MT_connectivity_protocol_Thermo_command_set document for contained data
	 * @param edctMessage received exchange data container thermal message
	 */
	private void handleEDCTMessage(final EDCTInputMessage edctMessage) {
		final int packNum = edctMessage.getPacketNum();
		Log.d(TAG, "EDCT Message Packet Number: " + packNum);
		if (packNum == EDCTInputMessage.PACKET_NUM_1) {
			// handle packet 1 only for this example
			broadcastMeasurement(ACTION_THERMAL_CONTAINER_RECEIVED, edctMessage.getResult());
		}
	}
}
