package com.example.android.bluetoothchat.Activities;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.widget.FrameLayout;

import com.example.android.bluetoothchat.BluetoothChatService;
import com.example.android.bluetoothchat.ChatKit.PersonDialog;
import com.example.android.bluetoothchat.Fragments.BluetoothChatFragment;
import com.example.android.bluetoothchat.Fragments.DialogsFragment;
import com.example.android.bluetoothchat.InstrumentedHashMap;
import com.example.android.bluetoothchat.R;

public class MainActivity extends AppCompatActivity implements DialogsFragment.OnFragmentListener, BluetoothChatFragment.OnFragmentListener {

    private BluetoothChatFragment bluetoothChatFragment;
    DialogsFragment frag;
    BluetoothChatService mService;
    BluetoothAdapter mBluetoothAdapter;
    FrameLayout content_fragment;
    public static InstrumentedHashMap byteArrayImage = new InstrumentedHashMap(10);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(broadcastStateBluetooth, filter);

        content_fragment = (FrameLayout) findViewById(R.id.content_fragment);

        mService = new BluetoothChatService(this, null);
        enableBluetooth();

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            frag = DialogsFragment.newInstance(mService);
            transaction.replace(R.id.content_fragment, frag);
            transaction.commit();
        }

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                1);
    }

    @Override
    public void onFragmentInteraction(BluetoothChatService mChatService, String address) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        bluetoothChatFragment = BluetoothChatFragment.newInstance(mChatService, address);
        transaction.replace(R.id.content_fragment, bluetoothChatFragment)
                .addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void updateDialog(PersonDialog personDialog) {
        frag.updateDialog(personDialog);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            mService.closeAllSockets();
            mService.stop();
        }
        unregisterReceiver(broadcastStateBluetooth);
        byteArrayImage.clear();
    }

    public void enableBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
        else
            mService.start();
    }

    private final BroadcastReceiver broadcastStateBluetooth = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Snackbar.make(content_fragment, "Bluetooth turned off", Snackbar.LENGTH_LONG).show();
                        mService.closeAllSockets();
                        mService.stop();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Snackbar.make(content_fragment, "Bluetooth turned on", Snackbar.LENGTH_LONG).show();
                        mService.start();
                        break;
                }
            }

        }
    };
}
