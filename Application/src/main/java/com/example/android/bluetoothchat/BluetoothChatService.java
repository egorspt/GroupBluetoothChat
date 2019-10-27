package com.example.android.bluetoothchat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.android.bluetoothchat.ChatKit.User;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class BluetoothChatService implements Serializable {

    transient public static HashMap<String, ConnectedThread> listOfConnection = new HashMap<>();
    private static final String TAG = "BluetoothChatService";

    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    transient public BluetoothAdapter mAdapter;
    transient private Handler mHandler;
    transient private Handler mmHandler;
    transient private AcceptThread mSecureAcceptThread;
    transient private AcceptThread mInsecureAcceptThread;
    transient private ConnectThread mConnectThread;
    transient private ConnectedThread mConnectedThread;

    public BluetoothChatService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void setHandler(Handler handler) {
        this.mHandler = handler;
    }

    public void setmHandler(Handler handler) {
        this.mmHandler = handler;
    }

    public synchronized void start() {
        Log.d(TAG, "start");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread = null;
        }

        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }
    }

    public synchronized void connect(BluetoothDevice device, boolean secure) {
        Log.d(TAG, "connect to: " + device);

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread = null;
        }

        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
    }

    public synchronized void connectGroup(BluetoothDevice device, boolean secure, String id, String group_name, String[] devices, int i) {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null)
            mConnectedThread = null;

        if (listOfConnection.get(device.getAddress()) == null) {
            mConnectThread = new ConnectThread(device, id, group_name, devices, i);
            mConnectThread.start();
        } else {
            String first_message = id + "," + group_name;
            for (String dev : devices)
                first_message = first_message + "," + dev;
            ConnectedThread connectedThread = listOfConnection.get(device.getAddress());
            connectedThread.write((first_message + "\n\n").getBytes(), Constants.GROUP_ADMIN, null);
            mmHandler.obtainMessage(Constants.GROUP_ONE_CONNECT, i + 1, -1, id).sendToTarget();
        }
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType, String from) {
        Log.d(TAG, "connected, Socket Type:" + socketType);

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null)
            mConnectedThread = null;

        mConnectedThread = new ConnectedThread(socket, socketType, device, from);
        mConnectedThread.start();
        listOfConnection.put(device.getAddress(), mConnectedThread);
    }

    public synchronized void connectedGroup(BluetoothSocket socket, BluetoothDevice
            device, String group_id, String group_name, String[] devices, int num_device) {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null)
            mConnectedThread = null;

        mConnectedThread = new ConnectedThread(socket, device.getAddress(), group_id, group_name, devices);
        mConnectedThread.start();
        listOfConnection.put(device.getAddress(), mConnectedThread);

        mmHandler.obtainMessage(Constants.GROUP_ONE_CONNECT, num_device + 1, -1, group_id).sendToTarget();
    }

    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
    }

    public void write(byte[] out, int view, String dialog_name, ArrayList<User> devices, String sender, String id_message) {
        ConnectedThread r = null;
        int activeUsers = 0;
        synchronized (this) {
            for (User user : devices)
                if (listOfConnection.get(user.getId()) != null) {
                    if (!user.getId().equals(sender)) {
                        r = listOfConnection.get(user.getId());
                        r.write(out, view, null);
                        activeUsers++;
                    } else activeUsers++;
                }
        }
        if (activeUsers == 0)
            connectionLostGroup(dialog_name);
        else {
            if (view == -1)
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, out)
                        .sendToTarget();
            if(view == -2) {
                Message m = mHandler.obtainMessage(Constants.MESSAGE_WRITE_IMAGE);
                Bundle b = new Bundle();
                b.putString(Constants.MESSAGE_ID, id_message);
                m.setData(b);
                mHandler.sendMessage(m);
            }
        }
    }

    public void write(byte[] out, int view, String admin, String id_message) {

        ConnectedThread r = null;

        synchronized (this) {
            if (listOfConnection.get(admin) == null) {
                connectionLost(admin);
                return;
            }
            r = listOfConnection.get(admin);
        }
        r.write(out, view, id_message);
    }

    private void connectionFailed() {
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

    }

    private void connectionLost(String address) {
        if (listOfConnection.get(address) != null) {
            listOfConnection.get(address).cancel();
            listOfConnection.remove(address);

            Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST_LOST);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.TOAST, address);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
    }

    private void connectionLostGroup(String group_name) {
            Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST_LOST_GROUP);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.TOAST, group_name);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
    }

    public void closeAllSockets() {
        for (String key : listOfConnection.keySet()) {
            listOfConnection.get(key).cancel();
            listOfConnection.remove(key);
        }
    }

    public void closeSocket(String address) {
        listOfConnection.get(address).cancel();
        listOfConnection.remove(address);
    }

    public ArrayList<String> listOfConnectionKey(){
        ArrayList<String> arrayList = new ArrayList<>();
        for(String key : listOfConnection.keySet())
            arrayList.add(key);
        return arrayList;
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            MY_UUID_SECURE);
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "Socket Type: " + mSocketType +
                    "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                if (socket != null) {
                    synchronized (BluetoothChatService.this) {
                        if (!listOfConnection.containsKey(socket.getRemoteDevice().getAddress()))
                            connected(socket, socket.getRemoteDevice(),
                                    mSocketType, "accept");
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;
        private boolean group;
        String group_id;
        String group_name;
        String[] devices;
        int num_device;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            group = false;
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public ConnectThread(BluetoothDevice device, String group_id, String group_name, String[] devices, int i) {
            group = true;
            this.group_id = group_id;
            this.group_name = group_name;
            this.devices = devices;
            this.num_device = i;
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = "Secure";

            try {
                tmp = device.createRfcommSocketToServiceRecord(
                        MY_UUID_SECURE);
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            mAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                if (group) {
                    Message msg = mmHandler.obtainMessage(Constants.GROUP_ONE_CONNECT, num_device + 1, -1, group_id);
                    Bundle bundle = new Bundle();
                    bundle.putString(Constants.DEVICE_ADDRESS, mmDevice.getAddress());
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);
                }
                return;
            }

            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }

            if (group) {
                connectedGroup(mmSocket, mmDevice, group_id, group_name, devices, num_device);
            } else
                connected(mmSocket, mmDevice, mSocketType, "connect");
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final String mmDeviceAddress;
        private String mmDeviceName;
        private boolean group;
        private String from;
        String group_id, group_name;
        String[] devices;


        public ConnectedThread(BluetoothSocket socket, String socketType, BluetoothDevice device, String from) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            group = false;
            this.from = from;
            mmDeviceAddress = device.getAddress();
            mmDeviceName = device.getName();
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public ConnectedThread(BluetoothSocket socket, String address, String group_id, String group_name, String[] devices) {
            group = true;
            this.group_id = group_id;
            this.group_name = group_name;
            this.devices = devices;
            mmDeviceAddress = address;
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }


        public void run() {

            if (group) {
                group = false;
                String first_message = group_id + "," + group_name;
                for (String device : devices)
                    first_message = first_message + "," + device;
                write((first_message + "\n\n").getBytes(), Constants.GROUP_ADMIN, null);
            } else if (from.equals("connect")) {
                write("one\n\n".getBytes(), Constants.DIALOG_CREATE_I, null);
            }

            byte[] buffer = new byte[1024];
            byte[] imgBuffer = new byte[1024 * 1024];
            List<byte[]> q = new ArrayList<>();
            int bytes = 0;
            int pos = 0;
            byte[] w = null;

            while (true) {
                int qwe = 0;
                try {
                    pos = 0;
                    bytes = 1;
                    while (bytes != 0) {

                        bytes = mmInStream.read(buffer);
                        System.arraycopy(buffer, 0, imgBuffer, pos, bytes);
                        pos += bytes;
                        if (buffer[bytes - 1] == '\r' && buffer[bytes - 2] == '\r') {
                            bytes = 0;
                            pos -= 2;
                            w = new byte[pos];
                            System.arraycopy(imgBuffer, 0, w, 0, pos);
                            break;
                        }
                        if (buffer[bytes - 1] == '\n' && buffer[bytes - 2] == '\n') {
                            bytes = 0;
                            pos -= 2;
                            w = new byte[pos];
                            System.arraycopy(imgBuffer, 0, w, 0, pos);
                            group = true;
                        }
                    }
                    if (group) {
                        if (w.length < 6) {
                            group = false;
                            Message msg = mHandler.obtainMessage(Constants.DIALOG_CREATE_OTHER);
                            Bundle bundle = new Bundle();
                            bundle.putString(Constants.DEVICE_NAME, mmDeviceName);
                            bundle.putString(Constants.DEVICE_ADDRESS, mmDeviceAddress);
                            msg.setData(bundle);
                            mHandler.sendMessage(msg);
                        } else {
                            group = false;
                            String group_message = new String(w, 0, pos);
                            mHandler.obtainMessage(Constants.GROUP_INFO, pos, -1, group_message + "," + mmDeviceAddress)
                                    .sendToTarget();
                        }
                    } else if (pos > 300) {
                        byte[] q1 = new byte[w.length + mmDeviceAddress.getBytes().length + 1];
                        System.arraycopy(w, 0, q1, 0, w.length);
                        System.arraycopy(("," + mmDeviceAddress).getBytes(), 0, q1, w.length, ("," + mmDeviceAddress).getBytes().length);

                        mHandler.obtainMessage(Constants.MESSAGE_READ_IMAGE, pos, -1, q1)
                                .sendToTarget();
                    } else {
                        String readMessage = new String(w, 0, pos);
                        mHandler.obtainMessage(Constants.MESSAGE_READ, pos, -1, readMessage + "," + mmDeviceAddress)
                                .sendToTarget();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost(mmDeviceAddress);
                    break;
                }
            }
        }

        public void write(byte[] buffer, int view, String id_message) {
            try {
                mmOutStream.write(buffer);

                switch (view) {
                    case Constants.TEXT:
                        mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                                .sendToTarget();
                        break;
                    case Constants.GROUP_ADMIN:
                        break;
                    case Constants.IMAGE:
                        Message m = mHandler.obtainMessage(Constants.MESSAGE_WRITE_IMAGE);
                        Bundle b = new Bundle();
                        b.putString(Constants.MESSAGE_ID, id_message);
                        m.setData(b);
                        mHandler.sendMessage(m);
                        break;
                    case Constants.DIALOG_CREATE_I:
                        Message msg = mHandler.obtainMessage(Constants.DIALOG_CREATE_I);
                        Bundle bundle = new Bundle();
                        bundle.putString(Constants.DEVICE_NAME, mmDeviceName);
                        bundle.putString(Constants.DEVICE_ADDRESS, mmDeviceAddress);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                        break;
                }
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
