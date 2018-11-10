package com.aqi.nmc.pollute;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    //GUI Components
    private TextView mBluetoothStatus;
    private TextView mReadBuffer;

    //private Button mShowDataBtn;
    private Button mOnBtn;
    private Button mOffBtn;
    private Button mListPairedDeviceBtn;
    private Button mDiscoverBtn;

    private BluetoothAdapter mBTAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private ArrayAdapter<String> mBTArrayAdapter;
    private ListView mDevicesListView;

    String data;

    private Handler mHandler; //Our main handler that will receive callback notifications
    private BluetoothSocket mBTSocket = null; //Bi-directional client-to-client data path
    //private ConnectedThread mConnectedThread;
    //Bluetooth background worker thread to send and receive the data

    private static final UUID BTMODULEUUID = UUID.
            fromString("00001101-0000-10001-8000-00805F9B34FB");
    // "Random" unique identifier

    //Defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1; //Used to identify paired bluetooth names
    private final static int MESSAGE_READ = 2;
    //Used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3;
    //Used in bluetooth handler to identify message status

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothStatus = findViewById(R.id.statusData);
        mReadBuffer = findViewById(R.id.rxData);
        mOnBtn = findViewById(R.id.btnOn);
        mOffBtn = findViewById(R.id.btnOff);
        mDiscoverBtn = findViewById(R.id.discover);
        mListPairedDeviceBtn = findViewById(R.id.showPairedDevices);
        //mShowDataBtn = findViewById(R.id.showData);

        mBTArrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // Get a handle on the bluetooth radio
        mDevicesListView = findViewById(R.id.pairedDeviceListView);

        // Assign model to view
        mDevicesListView.setAdapter(mBTArrayAdapter);
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);

        if (mBTAdapter.isEnabled()) {
            mBluetoothStatus.setText(R.string.enabled);
        }

        mHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == MESSAGE_READ) {
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    if (msg.what == CONNECTING_STATUS) {
                        if (msg.arg1 == 1)
                            mBluetoothStatus.setText(R.string.connectedToDevice +
                                    (String) (msg.obj));
                        else
                            mBluetoothStatus.setText(R.string.connectionFailed);
                    }
                    mReadBuffer.setText(readMessage);
                }
            }
        };

        if (mBTArrayAdapter == null) {
            //Device does not support Bluetooth
            mBluetoothStatus.setText(R.string.bluetoothNotFound);
            Toast.makeText(getApplicationContext(), "Bluetooth device not found!",
                    Toast.LENGTH_LONG).show();
        } else {

            // when bluetooth is turned ON
            mOnBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bluetoothOn(v);
                }
            });

            // when bluetooth is turned OFF
            mOffBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bluetoothOff(v);
                }
            });

            // show paired devices
            mListPairedDeviceBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listPairedDevices(v);
                }
            });

            // discover new bluetooth device
            mDiscoverBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    discover(v);
                }
            });
        }
    }

    private void bluetoothOn(View view) {
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothStatus.setText(R.string.enablingBluetooth);

            //Delay
            try {
                //st time in milli seconds
                Thread.sleep(7000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (mBTAdapter.isEnabled())
                Toast.makeText(getApplicationContext(), "Bluetooth is turned ON",
                        Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Bluetooth is already ON",
                    Toast.LENGTH_LONG).show();
        }
    }

    // Enter here after user selects "YES" or "NO" to enable radio
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact
                // The intent's data Uri identifies which contact was selected
                mBluetoothStatus.setText(R.string.enabled);
            } else
                mBluetoothStatus.setText(R.string.disabled);
            mReadBuffer.setText(R.string.readBuffer);
        }
    }

    private void bluetoothOff(View view) {

        // Turn OFF
        mBTAdapter.disable();

        // Clear the paired device list when adapter is OFF
        mBTArrayAdapter.clear();
        mReadBuffer.setText(R.string.readBuffer);
        mBluetoothStatus.setText(R.string.disabled);
        Toast.makeText(getApplicationContext(), "Bluetooth is turned OFF",
                Toast.LENGTH_LONG).show();
    }

    private void discover(View view) {
        // Check if the device is already discovering
        if (mBTAdapter.isDiscovering()) {
            /*AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.setMessage("Already searching");
            mBTAdapter.cancelDiscovery();
            alertDialog.setMessage("Discovery Stopped");*/
            mBTAdapter.cancelDiscovery();
            mBluetoothStatus.setText(R.string.discoveryStopped);
        } else {
            if (mBTAdapter.isEnabled()) {
                // Clear old list
                mBTArrayAdapter.clear();
                mBluetoothStatus.setText(R.string.discovering);
                mBTAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery Started!",
                        Toast.LENGTH_LONG).show();
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND ));
            } else
                Toast.makeText(getApplicationContext(), "Bluetooth is turned OFF",
                        Toast.LENGTH_LONG).show();
        }
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.
                        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name to the list
                mBTArrayAdapter.add(device.getName() + "\t" + device.getAddress());
                mBTArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    private void listPairedDevices(View view) {
        mPairedDevices = mBTAdapter.getBondedDevices();
        if (mBTAdapter.isEnabled()) {
            if (mDevicesListView != null) {
                /** Overwrite the pairedDevices list
                 each time the Adapter is updated*/
                mBTArrayAdapter.clear();
            }
            // Put it's one to  the adapter
            for (BluetoothDevice device: mPairedDevices)
                mBTArrayAdapter.add(device.getName() + "\t" + device.getAddress());

            Toast.makeText(getApplicationContext(), "Paired Devices",
                    Toast.LENGTH_LONG).show();
        } else
            Toast.makeText(getApplicationContext(), "Bluetooth is turned OFF",
                    Toast.LENGTH_LONG).show();
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new
            AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> av, View view, int arg2, long arg3) {
                    if (!mBTAdapter.isEnabled()) {
                        Toast.makeText(getApplicationContext(), "Bluetooth is turned OFF",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    mBluetoothStatus.setText(R.string.connecting);

                    // Get the device's MAC address, which is the last 17 chars in the View
                    String info = ((TextView) view).getText().toString();
                    final String address = info.substring(info.length() - 17);
                    final String name = info.substring(0, info.length() - 17);
                }
            };

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        // Creates secure outgoing connection with BT device using UUID
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;

            // Get the input streams, using temp objects because member streams are final
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
        }

        public void run() {
            byte[] buffer = new byte[1024]; // Buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.available();
                    if (bytes != 0) {
                        SystemClock.sleep(100); // Pause and wait for rest of data.
                        // Adjust this depending on your sending speed.
                        bytes = mmInStream.available(); // How many bytes are ready to be read?
                        bytes = mmInStream.read(buffer, 0,bytes);
                        // record how many bytes we have actually read
                        mHandler.obtainMessage(MESSAGE_READ, bytes,-1, buffer).sendToTarget();
                        // Send the obtained bytes to the UI activity
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        // Call this from the main activity to shutdown the connection
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }
}
