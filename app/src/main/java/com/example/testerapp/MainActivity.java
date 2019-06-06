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
    BluetoothDevice raspberrypi ;
    DataOutputStream os;
    boolean deviceFound;

    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
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
                if(device.getName().equals("raspberrypi"))
                {
                    bluetoothDevice = device;
                    Log.d("BluetoothDevice ", "The bluetooth device seen is " + bluetoothDevice.getName());
                    PairDevice(bluetoothDevice);


                }
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                lvNewDevices.setAdapter(mDeviceListAdapter);
            }

            else
            {
                Log.i("Bluetooth", "NOOOOOOOOOOOTTT FOUNDDDDDDDDDDDD " );
            }
        }
    };

    public void Discover_Devices_and_Pair()
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

/*
    private void pairDevice(BluetoothDevice mydevice)
    {
        try
        {
            Log.d("pairDevice()", "Start Pairing...");
            Method m = mydevice.getClass().getMethod("createBond", (Class[]) null);
            m.invoke(mydevice, (Object[]) null);
            Log.d("pairDevice()", "Pairing finished.");
        } catch (Exception e) {
            Log.e("pairDevice()", e.getMessage());
        }
    }*/

    private void PairDevice(BluetoothDevice mydevice)
    {


        //create the bond.
        //NOTE: Requires API 17+? I think this is JellyBean
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2)
        {
            Log.d("PairDevice", "Trying to pair with " + mydevice);
            mydevice.createBond();
        }
    }

/*
    public boolean createBond(BluetoothDevice btDevice)
            throws Exception
    {
        Class class1 = Class.forName("android.bluetooth.BluetoothDevice");
        Method createBondMethod = class1.getMethod("createBond");
        Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
        System.out.println("Pairing status = "+ returnValue);
        return returnValue.booleanValue();
    }*/

    private void findDevice()
    {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("raspberrypi")) {
                    bluetoothDevice = device;
                    deviceFound = true;
                    System.out.println("raspberry pi found status = "+ deviceFound);
                    break;
                }
            }
        }
    }



    public void BTConnect()
    {

        final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothSocket socket = null;
        String RPi_MAC = "b8:27:eb:66:01:cf";

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {

            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices)
            {
                if (device.getAddress().equals(RPi_MAC)) {
                    try {
                        socket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                    } catch (IOException e0) {
                        Log.d("BT_TEST", "Cannot create socket");
                        e0.printStackTrace();
                    }

                    try {
                        socket.connect();
                    } catch (IOException e1) {
                        try {
                            socket.close();
                            Log.d("BT_TEST", "Cannot connect");
                            e1.printStackTrace();
                        } catch (IOException e2) {
                            Log.d("BT_TEST", "Socket not closed");
                            e2.printStackTrace();
                        }
                    }
                }
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
        mBTDevices = new ArrayList<>();

        //Broadcasts when bond state changes (ie:pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);


        makeDiscoverable();
        Discover_Devices_and_Pair();

        findDevice();
        //BTConnect();


    }







}




