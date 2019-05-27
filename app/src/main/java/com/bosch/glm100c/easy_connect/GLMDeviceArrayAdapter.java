package com.bosch.glm100c.easy_connect;

import java.util.List;
import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bosch.glm100c.easy_connect.bluetooth.BLEService;
import com.bosch.glm100c.easy_connect.bluetooth.MTBluetoothDevice;

public class GLMDeviceArrayAdapter extends ArrayAdapter<MTBluetoothDevice> {
	
	private MainActivity main;
	
	/**
	 * @param context current context
	 * @param resource list layout
	 * @param textViewResourceId text field id
	 * @param objects device list
	 */
	public GLMDeviceArrayAdapter(Context context, int resource, int textViewResourceId, List<MTBluetoothDevice> objects) {
		super(context, resource, textViewResourceId, objects);
		this.main = (MainActivity) context;
	}

	/* (non-Javadoc)
	 * @see android.widget.ArrayAdapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
	@SuppressLint("InflateParams")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		MTBluetoothDevice item = getItem(position);

		ViewHolder holder;

		if (convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_device, null);
			holder = new ViewHolder((ImageView) convertView.findViewById(R.id.checkbox_device), (TextView) convertView.findViewById(R.id.device_name));
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		assert item != null;
		holder.deviceName.setText(item.getDisplayName());

		BLEService btService = main.getBluetoothService();
		
		if(btService != null) {
			MTBluetoothDevice currentDevice = btService.getCurrentDevice();

			if(currentDevice != null && currentDevice.getDevice().getAddress().equals(item.getDevice().getAddress())){
				if(btService.isConnected()){
					holder.checkbox.setImageResource(R.drawable.ic_check_active);
				}else{
					holder.checkbox.setImageResource(R.drawable.ic_check_disabled);
				}
				return convertView;
			}
		}

		holder.checkbox.setImageResource(R.drawable.ic_check_normal);
		return convertView;
	}

	private static class ViewHolder {

		public ImageView checkbox;
		public TextView deviceName;

		public ViewHolder(ImageView checkbox, TextView deviceName) {
			super();
			this.checkbox = checkbox;
			this.deviceName = deviceName;
		}
	}
}
