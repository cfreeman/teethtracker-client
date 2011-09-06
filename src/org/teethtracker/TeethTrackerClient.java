package org.teethtracker;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
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
        try {
        	BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
        	
        	Log.v("bluetracker", "enabling bluetooth");
        	//tv.setText("enabling bluetooth");
        	output = output + "- enabling bluetooth";
        	Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 3);
            // or ba.enable();

            Log.v("bluetracker", "attempting to connect");
            output = output + "- attempting to connect";
        	//BluetoothDevice d = ba.getRemoteDevice("58:55:CA:C2:EE:6B");
        	//ba.cancelDiscovery();
        	//BluetoothSocket s = d.createRfcommSocketToServiceRecord(UUID.randomUUID());
        	//BluetoothSocket s = new BluetoothSocket(1, -1, false, false, d, UUID.randomUUID(), null);        			

        	
        	//        	BluetoothSocket.TYPE_RFCOMM, -1, false, false, this, port,
//        	755                null
        	
//        	Method m = d.getClass().getMethod("createInsecureRfcommSocket", new Class[] {int.class});
        	//Method m = d.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
            //BluetoothSocket s = (BluetoothSocket) m.invoke(d, 1);
        	
        	// NOT in my API: d.createInsecureRfcommSocketToServiceRecord(UUID.randomUUID());
        	//s.connect();
        	
        	output = output + "- connected";

        } catch (IllegalArgumentException e) {
        	Log.v("bluetracker", "illegal MAC addrss");
        	//tv.setText("illegal MAC address");
        	output = output + " - illegal MAC address";
       /* } catch (IOException e) {
        	Log.v("bluetracker", "unable to connect");
        	//tv.setText("unable to connect: " + e.getMessage());
        	output = output + " - unable to connect: " + e.getMessage();
        } catch (NoSuchMethodException e) {
        	Log.v("bluetracker", "unable to find method");
        	output = output + " - unable to find rf comm socket";
        */} catch (Exception e) {
			Log.v("bluetracker", "unable to call method");
        	output = output + " - unable to call rf comm socket";
        }
        
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