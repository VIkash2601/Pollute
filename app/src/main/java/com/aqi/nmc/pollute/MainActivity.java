package com.aqi.nmc.pollute;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    //GUI Components
    private TextView mBluetoothStatus;
    private TextView mReadBuffer;

    private Button mShowDataBtn;
    private Button mScanBtn;
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
    }
}
