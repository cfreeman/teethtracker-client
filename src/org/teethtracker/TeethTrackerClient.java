/*
 * Copyright (c) Clinton Freeman & Luke Atherton 2011
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.teethtracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.IBluetooth;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.widget.TextView;

public class TeethTrackerClient extends Activity {	

	// Is our scanning process running or not?
	private boolean running = true;

	private BluetoothSocket btSocket;

	private TextView tv;

	//private IBluetooth ib;

	private Boolean detected = false;
	private Exchanger<Boolean> exchanger = new Exchanger<Boolean>();

	final static String NODE_NAME = "zoneD";

	final static int SCAN_TIMEOUT = 10000;

	/**
	 * The various possible device states, either a departure or an arrival.
	 */
	private enum DeviceStateChange {
		DEPARTURE("departure"),
		ARRIVAL("arrival");

		private String type;

		private DeviceStateChange(String changeType) {
			type = changeType;
		}

		public String getType() {
			return type;
		}
	}

	/**
	 * @return The list of bluetooth devices that we are looking at following in this particular node.
	 */
	private List<String> getDeviceList() {
		List<String> results = new ArrayList<String>();
		updateUI("***getDeviceList\n");

		try {
			URL centralTracker = new URL("http://teethtracker.heroku.com/devices.json");

			BufferedReader in = new BufferedReader(new InputStreamReader(centralTracker.openStream()));
			String JSONBlob = "";
			String inputLine;

			// Read the JSON from the server.
			while ((inputLine = in.readLine()) != null) {
				JSONBlob = JSONBlob + inputLine;
			}
			in.close();

			// Parse the JSON from the server - pulling out the device ID's.
			JSONArray list = (JSONArray) new JSONTokener(JSONBlob).nextValue();
			for (int i = 0; i < list.length(); i++) {
				JSONObject device = list.getJSONObject(i).getJSONObject("device");
				String bluetoothID = device.getString("bluetooth_id").toUpperCase();
				results.add(bluetoothID);
			}

		} catch (MalformedURLException e) {			
			updateUI("Unable to create URL: " + e.toString() + "\n");
		} catch (IOException e) {
			updateUI("Unable to open connection: " + e.toString() + "\n");
		} catch (JSONException e) {
			updateUI("Unable to parse JSON: " + e.toString() + "\n");
		}
	
		return results;
	}
	
	/**
	 * Helper method to update the user interface (in the correct thread).
	 *
	 * @param content The content you want to display in the user interface.
	 */
	private void updateUI(final String content) {
		runOnUiThread(new Runnable() {
			public void run() {
				tv.append(content);
			}
		});
	}
	
	/**
	 * Method that logs a tracking message at the central server.
	 *
	 * @param id The Bluetooth MAC address of the device we are updating.
	 * @param stateChange The new state of the supplied device id.
	 */
	private void trackDevice(final String id, final DeviceStateChange stateChange) {
		updateUI("***trackDevice: " + id + " - " + stateChange.getType() + "\n");

		try {
		    URL centralTracker = new URL("http://teethtracker.heroku.com/device_movements/new?type="
		    						     + stateChange.getType() + "&node=" + NODE_NAME + "&bluetooth_id=" + id);
		    URLConnection trackerConnection = centralTracker.openConnection();
		    trackerConnection.getContentLength();

		} catch (MalformedURLException e) {
			updateUI("Malformed URL Exception: " + e.toString() + "\n");
		} catch (IOException e) {
			updateUI("IOException: " + e.toString() + "\n");
		}
	}	
	
	/**
	 * Determines if the supplied bluetooth device is present in the cell.
	 *
	 * @param id The ID of the bluetooth device that you want to look for.
	 *
	 * @return True if the device is present in this node, false otherwise.
	 */
	private boolean isDeviceHere(String id) {
		updateUI("***isDeviceHere: " + id + "\n");
		String bluetoothID = id.substring(0, 2) + ":" +
							 id.substring(2, 4) + ":" +
							 id.substring(4, 6) + ":" +
							 id.substring(6, 8) + ":" +
							 id.substring(8, 10) + ":" +
							 id.substring(10, 12);
		
		BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
      	ba.cancelDiscovery();

      	// Enable Bluetooth if it is switched off.
      	if (!ba.isEnabled()) {
      		ba.enable();
      	}

      	IBluetooth ib = getIBluetooth();
		detected = false;

    	try {
    		ib.createBond(bluetoothID);
    		exchanger.exchange(detected, SCAN_TIMEOUT, TimeUnit.MILLISECONDS);
		} catch (RemoteException e) {
			updateUI("Problem creating bond: " + e.toString() + "\n");
		} catch (InterruptedException e) {
			updateUI("Interrupted wait: " + e.toString() + "\n");
		} catch (TimeoutException e) {
			// Do nothing... We timeout when we can't find the device.
		}

		return detected;
	}

	/**
	 * Intent receiver for listening to for bluetooth connections.
	 */
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        try {
				detected = true;
				exchanger.exchange(detected);
			} catch (InterruptedException e) {
				updateUI("interrupted exchange");
			}
	    }
	};

	private final BroadcastReceiver mReceiver2 = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			try {
				if (device.getBondState() == BluetoothDevice.BOND_BONDING) {
					updateUI("Cancelling device Bond: " + device.getAddress() + "\n");
					IBluetooth ib = getIBluetooth();
					if (ib != null) {
						ib.cancelPairingUserInput(device.getAddress());
						ib.cancelBondProcess(device.getAddress());
					}
				}
			} catch (RemoteException e) {
				updateUI("Unable to cancel bond: " + e.getMessage() + "\n");
			}
		}
	};
	
    /**
     * Called when activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tv = new TextView(this);

        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
        registerReceiver(mReceiver2, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        tv.setText("Start\n");
        setContentView(tv);              
        
        new Thread(new Runnable() {
            public void run() {            	
            	Looper.prepare();
                // Prevent the phone from going to sleep.
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            	PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "TeethTracker");
                wl.acquire();

                HashMap<String, Boolean> lastLocatedDevices = new HashMap<String, Boolean>();
                // TODO: Allow people to enter a NODE name.

                while (running) {
                	HashMap<String, Boolean> locatedDevices = new HashMap<String, Boolean>();
                	List<String> allDevices = getDeviceList();
        	    	for (String bluetoothID : allDevices) {
        	    		if (isDeviceHere(bluetoothID)) {
        	    			// Only send a message to the tracking server if the device wasn't present last pass through.
        	    			if (lastLocatedDevices.get(bluetoothID) == null) {
        	    				trackDevice(bluetoothID, DeviceStateChange.ARRIVAL);

        	    			// Remove device from last located list - everything left in this list at the end of the scan have left the cell.
        	    			} else {
        	    				lastLocatedDevices.remove(bluetoothID);
        	    			}

        	    			locatedDevices.put(bluetoothID, true);
        	    		}
        	    	}

        	    	for (String bluetoothID : lastLocatedDevices.keySet()) {
        	    		trackDevice(bluetoothID, DeviceStateChange.DEPARTURE);
        	    	}

        	    	// Update the last located devices.
        	    	lastLocatedDevices.clear();
        	    	lastLocatedDevices = locatedDevices;

        	    	// Might need to shift this up into the inner device loop....
        	    	try {
        	    		// Pause so that we have the opportunity to cancel the bonding request.
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        	    }

            	updateUI("done\n");
            	wl.release();
            }            
        }).start();
    }
    
    private IBluetooth getIBluetooth() {
    	IBluetooth ibt = null;

    	try {
    	    Class c2 = Class.forName("android.os.ServiceManager");

    	    Method m2 = c2.getDeclaredMethod("getService",String.class);
    	    IBinder b = (IBinder) m2.invoke(null, "bluetooth");

    	    Class c3 = Class.forName("android.bluetooth.IBluetooth");

    	    Class[] s2 = c3.getDeclaredClasses();

    	    Class c = s2[0];
    	    Method m = c.getDeclaredMethod("asInterface",IBinder.class);
    	    m.setAccessible(true);
    	    ibt = (IBluetooth) m.invoke(null, b);

    	} catch (Exception e) {
    		updateUI("IBluetooth problem: " + e.getMessage() + "\n");
    	}

    	return ibt;
    }

    @Override
    public void onResume() {
    	super.onResume();
    }

    @Override
    public void onStop() {
    	running = false;	
    	super.onStop();
    }

    @Override
    public void onDestroy() {
    	running = false;
    	super.onDestroy();
    }
}