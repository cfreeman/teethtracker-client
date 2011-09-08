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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class TeethTrackerClient extends Activity {	
	
	private String output;
	
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        //BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	        //tv.setText("ACTION: " + action);
	        output = output + "- ACTION: " + action;
	    }
	};
	
	/**
	 * @return The list of bluetooth devices that we are looking at following in this particular node.
	 */
	private List<String> getTrackingList() {
		List<String> results = new ArrayList<String>();

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
				String id = device.getString("bluetooth_id").toUpperCase();
				String bluetooth_id = id.substring(0, 2) + ":" +
								      id.substring(2, 4) + ":" +
								      id.substring(4, 6) + ":" +
								      id.substring(6, 8) + ":" +
								      id.substring(8, 10) + ":" +
								      id.substring(10, 12);

				results.add(bluetooth_id);
			}

		} catch (MalformedURLException e) {
			Log.v("teethtracker", "Unable to create URL for centralTracker");
			output = output + " " + e.toString();
		} catch (IOException e) {
			Log.v("teethtracker", "Unable to open connection to centralTracker");
			output = output + " " + e.toString();
		} catch (JSONException e) {
			Log.v("teethtracker", "Unable to parse JSON");
			output = output + " " + e.toString();
		}
	
		return results;
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        
        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiver, filter1);
        this.registerReceiver(mReceiver, filter2);
        this.registerReceiver(mReceiver, filter3);
        
        
        
        Log.v("bluetracker", "****** starting ******");
        output = "Start";
        List<String> devices = getTrackingList();
        
//        try {
//        	BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
//        	
//        	Log.v("bluetracker", "enabling bluetooth");
//        	output = output + "- enabling bluetooth";
//        	Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableIntent, 3);
//            // or ba.enable();
//
//            Log.v("bluetracker", "attempting to connect");
//            output = output + "- attempting to connect";
//        	BluetoothDevice d = ba.getRemoteDevice("58:55:CA:C2:EE:6B");
//        	ba.cancelDiscovery();
//        	//BluetoothSocket s = d.createRfcommSocketToServiceRecord(UUID.randomUUID());
//        	
//        	Method m = d.getClass().getMethod("createInsecureRfcommSocket", new Class[] {int.class});
//        	BluetoothSocket tmp = (BluetoothSocket) m.invoke(d, 1);
//        	
//        	tmp.connect();
//        	//output = output + tmp.getRemoteDevice().
//        	
//        	
//        	/*BluetoothSocket ss;
//        	
//        	Class BluetoothSocketDefinition;
//            Class[] intArgsClass = new Class[] { int.class, int.class, boolean.class, boolean.class, BluetoothDevice.class, int.class, ParcelUuid.class };
//            Object[] intArgs = new Object[] { new Integer(1), new Integer(-1), new Boolean(false), new Boolean(false), d, new Integer(1), null };
//            Constructor intArgsConstructor;
//       	
//            BluetoothSocketDefinition = Class.forName("android.bluetooth.BluetoothSocket");
//            intArgsConstructor = BluetoothSocketDefinition.getConstructor(intArgsClass);
//            
//            ss = (BluetoothSocket) intArgsConstructor.newInstance(intArgs);
//            ss.connect();*/     
//
//        	//Method mm = ss.getClass().getConstructor(new Class[] {int.class});
//           // BluetoothSocket sss = (BluetoothSocket) mm.invoke(1, -1, false, false, d, 1, null);
//        	
//        	//Method m = d.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
//            //BluetoothSocket s = (BluetoothSocket) m.invoke(d, 1);
//        	
//        	// NOT in my API: d.createInsecureRfcommSocketToServiceRecord(UUID.randomUUID());
//        	//s.connect();
//        	
//        	output = output + "- connected";
//        	
//        	tmp.close();
//        	ba.disable();
//        	ba.enable();
//        	
//        	/*
//        	BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
//        	
//        	Log.v("bluetracker", "enabling bluetooth");
//        	//tv.setText("enabling bluetooth");
//        	output = output + "- enabling bluetooth";
//        	Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableIntent, 3);
//            // or ba.enable();
//
//            Log.v("bluetracker", "attempting to connect");
//            output = output + "- attempting to connect";
//        	//BluetoothDevice d = ba.getRemoteDevice("58:55:CA:C2:EE:6B");
//        	//ba.cancelDiscovery();
//        	//BluetoothSocket s = d.createRfcommSocketToServiceRecord(UUID.randomUUID());
//        	//BluetoothSocket s = new BluetoothSocket(1, -1, false, false, d, UUID.randomUUID(), null);        			
//
//        	
//        	//        	BluetoothSocket.TYPE_RFCOMM, -1, false, false, this, port,
////        	755                null
//        	
////        	Method m = d.getClass().getMethod("createInsecureRfcommSocket", new Class[] {int.class});
//        	//Method m = d.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
//            //BluetoothSocket s = (BluetoothSocket) m.invoke(d, 1);
//        	
//        	// NOT in my API: d.createInsecureRfcommSocketToServiceRecord(UUID.randomUUID());
//        	//s.connect();
//        	
//        	output = output + "- connected";
//        	*/
//
//        } catch (IllegalArgumentException e) {
//        	Log.v("bluetracker", "illegal MAC addrss");
//        	output = output + " - illegal MAC address";
//        } catch (IOException e) {
//        	Log.v("bluetracker", "unable to connect");
//        	output = output + " - unable to connect: " + e.getMessage();
//        } catch (NoSuchMethodException e) {
//        	Log.v("bluetracker", "unable to find method");
//        	output = output + " - unable to find rf comm socket";
//        } catch (Exception e) {
//			Log.v("bluetracker", "unable to call method");
//        	output = output + " - unable to call rf comm socket";
//        }
        
        tv.setText(output);
        setContentView(tv);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    }

    @Override
    public void onStop() {
    	super.onStop();
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    }
}