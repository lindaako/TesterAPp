package com.example.testerapp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.StrictMode;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
{

    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothDevice bluetoothDevice ;
    OutputStream mmOutStream = null;
    private static BluetoothSocket mmSocket;
    String bluetooth_message = "Hello world";

    boolean deviceFound;
    boolean alreadyConnected;

    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
    public DeviceListAdapter mDeviceListAdapter;
    ListView lvNewDevices;




    private void checkBTPermissions()
    {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0)
            {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }
        else
            {
            Log.d("the app", "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
            }
    }


    private void makeDiscoverable()
    {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
        Log.i("Log", "Discoverable ");
    }

    private BroadcastReceiver myReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Message msg = Message.obtain();
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action))
            {
                //Found, add to a device list
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                Log.d("Broadcast Received", "onReceive: " + device.getName() + ": " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                lvNewDevices.setAdapter(mDeviceListAdapter);

                if(device.getName() != null && device.getName().contains("raspberrypi"))
                {
                    Log.d("BluetoothDevice ", "The bluetooth device seen is " + device.getName());
                    bluetoothDevice = device;

                    try {
                        createBond(bluetoothDevice);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

            }

        }
    };

    public void Discover_Devices()
    {
        Log.d("The app", "btnDiscover: Looking for unpaired devices.");

        if(bluetoothAdapter .isDiscovering()){
            bluetoothAdapter .cancelDiscovery();
            Log.d("The app", "btnDiscover: Canceling discovery.");

            //check BT permissions in manifest
            checkBTPermissions();

            bluetoothAdapter .startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(myReceiver, discoverDevicesIntent);
        }
        if(!bluetoothAdapter.isDiscovering()){

            //check BT permissions in manifest
            checkBTPermissions();

            bluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(myReceiver, discoverDevicesIntent);

        }
    }


    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d("mBroadcastReceiver4", "BroadcastReceiver: BOND_BONDED.");
                }
                //case2: creating a bone
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d("mBroadcastReceiver4", "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d("mBroadcastReceiver4", "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };


    public boolean createBond(BluetoothDevice btDevice)
            throws Exception
    {
        Class class1 = Class.forName("android.bluetooth.BluetoothDevice");
        Method createBondMethod = class1.getMethod("createBond");
        Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
        System.out.println("Pairing status = "+ returnValue);
        return returnValue.booleanValue();
    }

    private void findDevice()
    {

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("raspberrypi")) {
                    bluetoothDevice = device;
                    deviceFound = true;
                    System.out.println("raspberry pi found status = "+ deviceFound);
                    Toast.makeText(MainActivity.this,"raspberry pi found status = "+ deviceFound,Toast.LENGTH_LONG).show();
                    break;
                }
            }
        }
    }



    public void BTConnect()
    {

        final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        BluetoothSocket socket = null;
        //String RPi_MAC = "b8:27:eb:66:01:cf";
        int no_of_times_data_sent = 0;

        // If there are paired devices
        if (pairedDevices.size() > 0) {

            // Loop through paired devices
            for (BluetoothDevice device2 : pairedDevices)
            {
                //Toast.makeText(MainActivity.this,"BTConnect found paired devices list!",Toast.LENGTH_LONG).show();
                if ((device2.getName().equals("raspberrypi")) && no_of_times_data_sent<1)
                {

                    Toast.makeText(MainActivity.this,"BTConnect found it!",Toast.LENGTH_LONG).show();

                    try
                    {
                        // Get a BluetoothSocket to connect with the given BluetoothDevice.
                        // MY_UUID is the app's UUID string, also used in the server code.
                        if(mmSocket == null || mmSocket.getRemoteDevice() == null ||
                                !mmSocket.getRemoteDevice().getAddress().equals(device2.getAddress())) {
                            mmSocket = device2.createRfcommSocketToServiceRecord(MY_UUID);
                            alreadyConnected = false;
                            Toast.makeText(MainActivity.this," Already Connect = false!",Toast.LENGTH_LONG).show();

                        }
                        else
                            {
                                alreadyConnected = true;
                            Toast.makeText(MainActivity.this,"Already Connect = true!",Toast.LENGTH_LONG).show();
                            }

                    }
                    catch (IOException e)
                    {
                        Log.e("BTConnect()", "Socket's create() method failed", e);
                    }


                    if(!alreadyConnected)
                    {
                        // Cancel discovery because it otherwise slows down the connection.
                        bluetoothAdapter.cancelDiscovery();

                        try
                        {
                            // Connect to the remote device through the socket. This call blocks
                            // until it succeeds or throws an exception.
                            mmSocket.connect();
                            if (mmSocket.isConnected())
                            {
                                Toast.makeText(MainActivity.this, "Connection successful!", Toast.LENGTH_LONG).show();

                                mmOutStream = mmSocket.getOutputStream();
                                mmOutStream.write(bluetooth_message.getBytes());
                            }
                        }
                        catch (IOException connectException)
                        {
                            // Unable to connect; close the socket and return.
                            try
                            {
                                mmSocket.close();
                                Toast.makeText(MainActivity.this,"Connect failed! socket closed",Toast.LENGTH_LONG).show();
                            } catch (IOException closeException)
                            {
                                Log.e("BTConnect()", "Could not close the client socket", closeException);
                                Toast.makeText(MainActivity.this,"Could not close the client socket",Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                    no_of_times_data_sent++;
                }
            }
        }
    }

    @Override
    protected void onDestroy()
    {
        Log.d("hey", "onDestroy: called.");
        super.onDestroy();
        unregisterReceiver(myReceiver);
        unregisterReceiver(mBroadcastReceiver4);

    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
        mBTDevices = new ArrayList<>();

        IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(myReceiver, discoverDevicesIntent);
        makeDiscoverable();

        //Broadcasts when bond state changes (ie:pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);

        Discover_Devices(); //create bond in myReciever class
        findDevice();
        BTConnect();


    }







}




