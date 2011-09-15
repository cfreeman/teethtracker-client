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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import android.os.RemoteException;
import android.widget.TextView;

public class TeethTrackerClient extends Activity {	
		
	// Is our scanning process running or not?
	private boolean running = true;
	
	private BluetoothSocket btSocket;
	
	private TextView tv;
	
	private IBluetooth ib;
	
	/**
	 * @return The list of bluetooth devices that we are looking at following in this particular node.
	 */
	private List<String> getTrackingList() {
		List<String> results = new ArrayList<String>();
		tv.append("***getTrackingList\n");

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
				tv.append("ID= " + bluetoothID + "\n");
			}

		} catch (MalformedURLException e) {			
			tv.append("Unable to create URL: " + e.toString() + "\n");
		} catch (IOException e) {
			tv.append("Unable to open connection: " + e.toString() + "\n");
		} catch (JSONException e) {
			tv.append("Unable to parse JSON: " + e.toString() + "\n");
		}
	
		return results;
	}
	
	/**
	 * Determines if the supplied bluetooth device is present in the cell.
	 *
	 * @param id The ID of the bluetooth device that you want to look for.
	 *
	 * @return True if the device is present in this node, false otherwise.
	 */
	private boolean isDeviceHere(String id) {
		tv.append("***isDeviceHere\n");
		String bluetoothID = id.substring(0, 2) + ":" +
							 id.substring(2, 4) + ":" +
							 id.substring(4, 6) + ":" +
							 id.substring(6, 8) + ":" +
							 id.substring(8, 10) + ":" +
							 id.substring(10, 12);
		
		tv.append("Looking for: " + bluetoothID + "\n");
		
		BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
      	ba.cancelDiscovery();
      	
      	tv.append("enabling bluetooth\n");
      	
      	// Enable Bluetooth if it is switched off.
      	if (!ba.isEnabled()) {
      		Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      		startActivityForResult(enableIntent, 3);
      	}
		
		ib = getIBluetooth();
		
    	try {
			ib.createBond(bluetoothID);
		} catch (RemoteException e) {
			tv.append("Problem creating bond: " + e.toString() + "\n");
		}
		
		/*try {    	
	      	//connection is sometimes still open from last run
	      	if (btSocket != null) {
	              try {btSocket.close();} catch (Exception e) {}
	              btSocket = null;
	      	}
	      	
	      	BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
	      	ba.cancelDiscovery();
	      	
	      	tv.append("enabling bluetooth\n");
	      	
	      	// Enable Bluetooth if it is switched off.
	      	if (!ba.isEnabled()) {
	      		Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	      		startActivityForResult(enableIntent, 3);
	      	}
	
	        tv.append("attempting to connect\n");
	        ba.cancelDiscovery();
	        BluetoothDevice d = ba.getRemoteDevice(bluetoothID);
	      	
	      	//Method m = d.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] {int.class});
	      	Method m = d.getClass().getMethod("createInsecureRfcommSocket", new Class[] {int.class});
	      	btSocket = (BluetoothSocket) m.invoke(d, UUID.randomUUID());
	      	btSocket.connect();
	      	tv.append("connected\n");
	      	btSocket.close();	
	        tv.append("finished\n");
	
	    	/*BluetoothSocket ss;
	    	
	    	Class BluetoothSocketDefinition;
	        Class[] intArgsClass = new Class[] { int.class, int.class, boolean.class, boolean.class, BluetoothDevice.class, int.class, ParcelUuid.class };
	        Object[] intArgs = new Object[] { new Integer(1), new Integer(-1), new Boolean(false), new Boolean(false), d, new Integer(1), null };
	        Constructor intArgsConstructor;
	   	
	        BluetoothSocketDefinition = Class.forName("android.bluetooth.BluetoothSocket");
	        intArgsConstructor = BluetoothSocketDefinition.getConstructor(intArgsClass);
	        
	        ss = (BluetoothSocket) intArgsConstructor.newInstance(intArgs);
	        ss.connect();    

	    } catch (IllegalArgumentException e) {
	    	tv.append("illegal MAC address: " + e.getMessage() + "\n");
	    } catch (IOException e) {
	    	tv.append("unable to connect: " + e.getMessage() + "\n");
	    } catch (NoSuchMethodException e) {
	    	tv.append("unable to find method: " + e.getMessage() + "\n");
	    } catch (Exception e) {
	    	tv.append("unable to call rf comm socket: " + e.getMessage() + "\n");
	    }*/

		return false;
	}
	
	private final BroadcastReceiver receiver = new BroadcastReceiver(){
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        tv.append("ACTION: " + action + "\n");
	        
	        if(BluetoothDevice.ACTION_FOUND.equals(action)) {
	        	tv.append("DEVICE FOUND!\n");
	            //short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
	            //Toast.makeText(getApplicationContext(),"  RSSI: " + rssi + "dBm", Toast.LENGTH_SHORT).show();
	        }
	    }
	};
	
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	        tv.append(device.getAddress() + "\n");
	        
	        try {
				ib.cancelPairingUserInput(device.getAddress());
				ib.cancelBondProcess(device.getAddress());
			} catch (RemoteException e) {
				tv.append("Unable to cancel bond: " + e.getMessage() + "\n");
			}
	        
	        tv.append("ACTION: " + intent.getAction() + "\n");
	    }
	};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tv = new TextView(this);
        
        /*registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED));
        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));*/

        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
        
        String ACTION_PAIRING_REQUEST = "android.bluetooth.device.action.PAIRING_REQUEST";
        
        registerReceiver(mReceiver, new IntentFilter(ACTION_PAIRING_REQUEST));
        
        tv.setText("Start\n");
        setContentView(tv);
        
        new Thread(new Runnable() {
            public void run() {
            	Looper.prepare();
                isDeviceHere("5855CAC2EE6B");
                //isDeviceHere("0007AB853D8C");
                //isDeviceHere("D49A201D13D0");
                
        /*
                HashMap<String, Boolean> lastLocatedDevices = new HashMap<String, Boolean>();
                
                // TODO: Allow people to enter a NODE name.
                // TODO: Need to be able to update the interface - while we loop and scan.
                // TODO: Need to create the URL's that we are going to poll / message.
                // TODO: Need to perform the below in a loop with pauses in between.

                // While our application is running {
        	    
                	HashMap<String, Boolean> locatedDevices = new HashMap<String, Boolean>();
                	List<String> allDevices = getTrackingList();
        	    	for (String bluetoothID : allDevices) {
        	    		if (isDeviceHere(bluetoothID)) {
        	    			// Only send a message to the tracking server if the device is present.
        	    			if (!lastLocatedDevices.get(bluetoothID)) {
        	    				// MAKE ALL TO:
        	    				// http://teethtracker.heroku.com/device_movements/new?type=arrival&node=NODENAME&bluetoodh_id=bluetoothID	    				
        	    			
        	    			// Remove device from last located list - everything left in this list at the end of the scan have left the cell.
        	    			} else {
        	    				lastLocatedDevices.remove(bluetoothID);
        	    			}

        	    			locatedDevices.put(bluetoothID, true);
        	    		}
        	    	}

        	    	for (String bluetoothID : lastLocatedDevices.keySet()) {
        	    		// http://teethtracker.heroku.com/device_movements/new?type=departure&node=NODENAME&bluetoodh_id=bluetoothID
        	    	}

        	    	// Update the last located devices.
        	    	lastLocatedDevices.clear();
        	    	lastLocatedDevices = locatedDevices;

        	    //   thread.sleep(5000); // wait 5 seconds before polling again.
        	    // }  
        	     */                   
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
    	    tv.append("IBluetooth problem: " + e.getMessage() + "\n");
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