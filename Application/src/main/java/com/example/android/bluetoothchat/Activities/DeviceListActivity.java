package com.example.android.bluetoothchat.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.example.android.bluetoothchat.BluetoothChatService;
import com.example.android.bluetoothchat.Constants;
import com.example.android.bluetoothchat.R;

import java.util.ArrayList;
import java.util.Set;

public class DeviceListActivity extends Activity {
    private static final String TAG = "DeviceListActivity";

    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    public static String EXTRA_DEVICES_ADDRESS = "devices_address";
    public static String EXTRA_GROUP_NAME = "group_name";

    private BluetoothAdapter mBtAdapter;

    private boolean choice;

    private ListView pairedListView;
    private ListView newDevicesListView;

    EditText name_group;

    private ArrayAdapter<String> pairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().getStringExtra("choice").equals("single"))
            choice = true;
        else
            choice = false;

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device_list);

        setResult(Activity.RESULT_CANCELED);

        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });

        Button okButton = (Button) findViewById(R.id.button_ok);
        okButton.setOnClickListener(mDeviceMultiClickListener);

        name_group = (EditText) findViewById(R.id.name_group);

        if (choice) {
            okButton.setVisibility(View.GONE);
            name_group.setVisibility(View.GONE);
            pairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
            mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
            name_group.setHint("Enter a conversation name");
        } else {
            okButton.setVisibility(View.VISIBLE);
            name_group.setVisibility(View.VISIBLE);
            pairedDevicesArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_checked);
            mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice);
            name_group.setHint("Enter a group name");
        }

        pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(pairedDevicesArrayAdapter);

        if (choice)
            pairedListView.setOnItemClickListener(mDeviceClickListener);
        else {
            pairedListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        }

        newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);

        if (choice)
            newDevicesListView.setOnItemClickListener(mDeviceClickListener);
        else {
            newDevicesListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        }

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            pairedDevicesArrayAdapter.add(noDevices);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        this.unregisterReceiver(mReceiver);
    }

    private void doDiscovery() {
        Log.d(TAG, "doDiscovery()");

        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        mBtAdapter.startDiscovery();
    }

    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            mBtAdapter.cancelDiscovery();

            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            if (BluetoothChatService.listOfConnection.get(address) != null) {
                Intent intent = new Intent();
                intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
                setResult(Constants.RESULT_REPLACE, intent);
                finish();
            }

            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    private View.OnClickListener mDeviceMultiClickListener
            = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mBtAdapter.cancelDiscovery();

            if(name_group.getText().toString().length() == 0) {
                name_group.setError("Group name");
                return;
            }

            String group_name = name_group.getText().toString();

            ArrayList<String> devices = new ArrayList<>();
            SparseBooleanArray sbArray = pairedListView.getCheckedItemPositions();
            SparseBooleanArray sArray = newDevicesListView.getCheckedItemPositions();
            for(int i = 0; i < pairedDevicesArrayAdapter.getCount(); i++){
                if(sbArray.get(i)) {
                    String address = pairedDevicesArrayAdapter.getItem(i)
                            .substring(pairedDevicesArrayAdapter.getItem(i).length() - 17);
                    devices.add(address);
                }
            }
            for(int i = 0; i < mNewDevicesArrayAdapter.getCount(); i++){
                if(sArray.get(i)) {
                    String address = mNewDevicesArrayAdapter.getItem(i)
                            .substring(mNewDevicesArrayAdapter.getItem(i).length() - 17);
                    devices.add(address);
                }
            }

            if(devices.size() == 0){
                TextView textView = new TextView(DeviceListActivity.this);
                textView.setText("Select devices");
                textView.setGravity(Gravity.CENTER_HORIZONTAL);
                textView.setPadding(0,5,0,0);
                textView.setTextSize(25);
                new AlertDialog.Builder(DeviceListActivity.this).setView(textView).show();
                return;
            }

            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICES_ADDRESS, devices.toArray(new String[0]));
            intent.putExtra(EXTRA_GROUP_NAME, group_name);

            setResult(Activity.RESULT_OK, intent);
            finish();

        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };

}
