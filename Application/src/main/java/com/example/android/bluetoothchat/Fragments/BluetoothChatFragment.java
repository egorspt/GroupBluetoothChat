package com.example.android.bluetoothchat.Fragments;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.graphics.Canvas;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.android.bluetoothchat.Adapters.ParticipantsAdapter;
import com.example.android.bluetoothchat.BluetoothChatService;
import com.example.android.bluetoothchat.ChatKit.DialogAvatar;
import com.example.android.bluetoothchat.Constants;
import com.example.android.bluetoothchat.Database.AppDatabase;
import com.example.android.bluetoothchat.Database.MessageDao;
import com.example.android.bluetoothchat.Database.PersonDialogDao;
import com.example.android.bluetoothchat.Activities.DeviceListActivity;
import com.example.android.bluetoothchat.Activities.MainActivity;
import com.example.android.bluetoothchat.ChatKit.MessageChat;
import com.example.android.bluetoothchat.ChatKit.PersonDialog;
import com.example.android.bluetoothchat.R;
import com.example.android.bluetoothchat.ChatKit.User;
import com.example.android.bluetoothchat.ChatKit.UserAvatar;
import com.stfalcon.chatkit.commons.ImageLoader;
import com.stfalcon.chatkit.messages.MessageInput;
import com.stfalcon.chatkit.messages.MessagesList;
import com.stfalcon.chatkit.messages.MessagesListAdapter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import static android.app.Activity.RESULT_OK;

public class BluetoothChatFragment extends Fragment {

    private static final String TAG = "BluetoothChatFragment";

    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    private MessagesList messagesList;
    private MessageInput messageInput;
    private ImageLoader imageLoader;
    private MessagesListAdapter<MessageChat> adapter;
    private MessageChat lastMessage = null;
    private OnFragmentListener fragmentListener;

    private AppDatabase db;
    private AppDatabase db1;
    private MessageDao messageDao;
    PersonDialogDao personDialogDao;

    private PersonDialog personDialog;
    private String mDialogName = null;
    private String mDialogId = null;

    private BluetoothAdapter mBluetoothAdapter = null;

    private BluetoothChatService mChatService = null;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    public BluetoothChatFragment() {

    }

    public static BluetoothChatFragment newInstance(BluetoothChatService mChatServce, String address) {
        BluetoothChatFragment fragment = new BluetoothChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, address);
        args.putSerializable(ARG_PARAM2, mChatServce);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            mDialogId = getArguments().getString(ARG_PARAM1);
            initDatabase();
            personDialog = personDialogDao.getById(mDialogId);
            personDialog.setUnreadCount(0);
            mChatService = (BluetoothChatService) getArguments().getSerializable(ARG_PARAM2);
            mChatService.setHandler(mHandler);
            mChatService.setmHandler(mmHandler);
            mDialogName = personDialog.getDialogName();

            if (personDialog.getAdmin() != null) {
                if (personDialog.getAdmin().equals("0"))
                    setStatus((personDialog.getUsers().size() + 1) + " members");
                else
                    setStatus(personDialog.getUsers().size() + " members");
            } else if (mChatService.listOfConnection.containsKey(mDialogId)) {
                setStatus("online");
                personDialog.getUsers().get(0).setOnline(true);
            } else {
                setStatus("offline");
                if (personDialog.getAdmin() != null)
                    setStatus("group chat");
            }
        }

        setNameActionBar(mDialogName);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            fragmentListener = (OnFragmentListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " должен реализовывать интерфейс OnFragmentInteractionListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setNameActionBar("BlueChat");
        personDialog.setLastMessage(lastMessage);
        personDialogDao.update(personDialog);
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.clear();
        adapter.addToEnd(messageDao.getA(), true);
        lastMessage = messageDao.getLastMessage();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        imageLoader = new ImageLoader() {
            @Override
            public void loadImage(ImageView imageView, String url) {
                if (url.charAt(1) > 47 && url.charAt(1) < 58 && url.length() > 10)
                    if (MainActivity.byteArrayImage.get(url) != null)
                        Glide.with(getActivity())
                                .load(convertCompressedByteArrayToBitmap(MainActivity.byteArrayImage.get(url)))
                                .placeholder(R.drawable.ic_photo_holder)
                                .override(900, 900)
                                .into(imageView);
                    else
                        Glide.with(getActivity())
                                .load(R.drawable.ic_photo_holder)
                                .override(100, 100)
                                .into(imageView);
                else if(url != null)
                    Glide.with(getActivity())
                            .load(createUserAvatar(url.charAt(0)))
                            .into(imageView);
            }
        };
        messagesList = (MessagesList) view.findViewById(R.id.messagesList);
        adapter = new MessagesListAdapter<>("0", imageLoader);
        messagesList.setAdapter(adapter);

        messageInput = (MessageInput) view.findViewById(R.id.input);
        messageInput.setInputListener(new MessageInput.InputListener() {
            @Override
            public boolean onSubmit(CharSequence input) {
                sendMessage(input.toString());
                return true;
            }
        });
        messageInput.setAttachmentsListener(new MessageInput.AttachmentsListener() {
            @Override
            public void onAddAttachments() {
                if (personDialog.getAdmin() == null && !mChatService.listOfConnectionKey().contains(personDialog.getId())) {
                    Snackbar.make(messagesList, personDialog.getDialogName() + " is offline", Snackbar.LENGTH_LONG).show();
                    return;
                } else if (personDialog.getAdmin() != null) {
                    int online_users = 0;
                    for (User user : personDialog.getUsers())
                        if (user.isOnline())
                            online_users++;
                    if (online_users == 0) {
                        mHandler.obtainMessage(Constants.MESSAGE_TOAST_LOST_GROUP).sendToTarget();
                        return;
                    }
                }
                Intent i = new Intent(Intent.ACTION_PICK);
                i.setType("image/*");
                i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(i, 11);
            }
        });
    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    private void sendMessage(String message) {
        if (personDialog.getAdmin() == null && !mChatService.listOfConnectionKey().contains(personDialog.getId())) {
            Snackbar.make(messagesList, personDialog.getDialogName() + " is offline", Snackbar.LENGTH_LONG).show();
            return;
        } else {
            int online_users = 0;
            for (User user : personDialog.getUsers())
                if (BluetoothChatService.listOfConnection.containsKey(user.getId()))
                    online_users++;
            if (online_users == 0) {
                mHandler.obtainMessage(Constants.MESSAGE_TOAST_LOST_GROUP).sendToTarget();
                return;
            }
        }

        if (message.length() > 0) {
            String admin = personDialogDao.getById(mDialogId).getAdmin();
            if (admin == null) {
                byte[] send = (message + "," + mDialogId + "," + "null" + "\r" + "\r").getBytes();
                mChatService.write(send, Constants.TEXT, personDialogDao.getById(mDialogId).getId(), null);
            } else if (admin.equals("0")) {
                byte[] send = (message + "," + mDialogId + "," + "0" + "\r" + "\r").getBytes();
                mChatService.write(send, -1, mDialogName, personDialogDao.getById(mDialogId).getUsers(), null, null);
            } else {
                byte[] send = (message + "," + mDialogId + "," + "1" + "\r" + "\r").getBytes();
                mChatService.write(send, Constants.TEXT, personDialogDao.getById(mDialogId).getAdmin(), null);
            }
        }
    }

    private void sendMessage(byte[] message, String id_message) {
        int online_users = 0;
        for (User user : personDialog.getUsers())
            if (BluetoothChatService.listOfConnection.containsKey(user.getId()))
                online_users++;
        if (online_users == 0)
            mHandler.obtainMessage(Constants.MESSAGE_TOAST_LOST_GROUP).sendToTarget();

        if (message != null) {
            String admin = personDialogDao.getById(mDialogId).getAdmin();
            if (admin == null)
                mChatService.write(message, Constants.IMAGE, personDialogDao.getById(mDialogId).getId(), id_message);
            else if (admin.equals("0"))
                mChatService.write(message, -2, mDialogName, personDialogDao.getById(mDialogId).getUsers(), null, id_message);
            else
                mChatService.write(message, Constants.IMAGE, personDialogDao.getById(mDialogId).getAdmin(), id_message);


        }
    }

    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);

    }

    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }


    private void setNameActionBar(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setTitle(subTitle);
    }

    private final Handler mmHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.GROUP_ONE_CONNECT:
                    int i = personDialogDao.getById(msg.obj.toString()).getUsers().size();
                    String[] arr = new String[i];
                    for (int j = 0; j < i; j++)
                        arr[j] = personDialogDao.getById(msg.obj.toString()).getUsers().get(j).getId();
                    if (personDialogDao.getById(msg.obj.toString()).getUsers().size() > msg.arg1)
                        mChatService.connectGroup(mBluetoothAdapter.getRemoteDevice(personDialogDao.getById(msg.obj.toString()).getUsers().get(msg.arg1).getId()), true,
                                msg.obj.toString(), personDialogDao.getById(msg.obj.toString()).getDialogName(), arr, msg.arg1);
                    if (msg.getData() != null)
                        updateDialog(msg.getData().getString(Constants.DEVICE_ADDRESS), msg.obj.toString());
                    break;
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            String device_address, device_name;
            if (activity != null)
                switch (msg.what) {
                    case Constants.DIALOG_CREATE_OTHER:
                        String address = msg.getData().getString(Constants.DEVICE_ADDRESS);
                        String name = msg.getData().getString(Constants.DEVICE_NAME);
                        personDialogDao.insert(new PersonDialog(name, address,
                                new ArrayList<User>() {{
                                    add(new User(address, name, null, true));
                                }}, null));
                        personDialogDao.getById(address).getUsers().get(0).setOnline(true);
                        Snackbar.make(messagesList, "Connected to " + name, Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        if (mDialogId.equals(address))
                            setStatus("online");
                        break;
                    case Constants.GROUP_INFO:
                        String group_message = msg.obj.toString();
                        String group_id = group_message.split(",")[0];
                        String group_name = group_message.split(",")[1];
                        int num_user = group_message.split(",").length - 3;
                        String admin = group_message.split(",")[group_message.split(",").length - 1];
                        if (personDialogDao.getById(group_id) == null) {
                            personDialog = new PersonDialog(group_name, group_id,
                                    new ArrayList<User>() {{
                                        add(new User(admin, admin, null, true));
                                        for (int i = 2; i < num_user + 2; i++)
                                            add(new User(group_message.split(",")[i], group_message.split(",")[i], group_message.split(",")[i], true));

                                    }}, admin);
                            personDialogDao.insert(personDialog);
                        }
                        Snackbar.make(messagesList, "Welcome to " + group_name, Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        break;
                    case Constants.MESSAGE_WRITE:
                        byte[] writeBuf = (byte[]) msg.obj;
                        String writeMessage = new String(writeBuf);
                        lastMessage = new MessageChat(Long.toString(UUID.randomUUID().getLeastSignificantBits()), new User("0", "0", null, true), writeMessage.split(",")[0]);
                        messageDao.insert(lastMessage);
                        adapter.addToStart(lastMessage, true);
                        break;
                    case Constants.MESSAGE_WRITE_IMAGE:
                        lastMessage = new MessageChat(msg.getData().getString(Constants.MESSAGE_ID), new User("0", "0", null, true), 0);
                        messageDao.insert(lastMessage);
                        adapter.addToStart(lastMessage, true);
                        break;
                    case Constants.MESSAGE_READ:
                        String readBuf = msg.obj.toString();
                        String readMessage = readBuf.split(",")[0];
                        String dialog_id = readBuf.split(",")[1];
                        device_address = readBuf.split(",")[2];
                        device_name = mBluetoothAdapter.getRemoteDevice(readBuf.split(",")[3]).getName();

                        if (device_address.equals("0"))
                            if (dialog_id.equals(mDialogId)) {
                                lastMessage = new MessageChat(Long.toString(UUID.randomUUID().getLeastSignificantBits()), new User(readBuf.split(",")[3],
                                        device_name, device_name, true), readMessage);
                                messageDao.insert(lastMessage);
                                adapter.addToStart(lastMessage, true);
                            } else {
                                lastMessage = new MessageChat(Long.toString(UUID.randomUUID().getLeastSignificantBits()), new User(readBuf.split(",")[3],
                                        device_name, device_name, true), readMessage);
                                updateMessageList(dialog_id, lastMessage);
                            }

                        if (device_address.equals("null"))
                            if (readBuf.split(",")[3].equals(mDialogId)) {
                                lastMessage = new MessageChat(Long.toString(UUID.randomUUID().getLeastSignificantBits()), new User(readBuf.split(",")[3], personDialogDao.getById(readBuf.split(",")[3]).getDialogName(),
                                        null, true), readMessage);
                                messageDao.insert(lastMessage);
                                adapter.addToStart(lastMessage, true);
                            } else {
                                lastMessage = new MessageChat(Long.toString(UUID.randomUUID().getLeastSignificantBits()), new User(readBuf.split(",")[3], personDialogDao.getById(readBuf.split(",")[3]).getDialogName(),
                                        null, true), readMessage);
                                updateMessageList(readBuf.split(",")[3], lastMessage);
                            }

                        if (device_address.equals("1")) {
                            if (dialog_id.equals(mDialogId)) {
                                lastMessage = new MessageChat(Long.toString(UUID.randomUUID().getLeastSignificantBits()), new User(readBuf.split(",")[3],
                                        device_name, device_name, true), readMessage);
                                messageDao.insert(lastMessage);
                                adapter.addToStart(lastMessage, true);
                            } else {
                                lastMessage = new MessageChat(Long.toString(UUID.randomUUID().getLeastSignificantBits()), new User(readBuf.split(",")[3],
                                        device_name, device_name, true), readMessage);
                                updateMessageList(dialog_id, lastMessage);
                            }
                            byte[] send = (readMessage + "," + mDialogId + "," + "01" + "," + readBuf.split(",")[3] + "," +
                                    device_name + "\r" + "\r").getBytes();
                            mChatService.write(send, 0, mDialogName, personDialogDao.getById(mDialogId).getUsers(), readBuf.split(",")[3], null);
                        }

                        if (device_address.equals("01")) {
                            if (dialog_id.equals(mDialogId)) {
                                lastMessage = new MessageChat(Long.toString(UUID.randomUUID().getLeastSignificantBits()),
                                        new User(readBuf.split(",")[3], readBuf.split(",")[4], readBuf.split(",")[4], true), readMessage);
                                messageDao.insert(lastMessage);
                                adapter.addToStart(lastMessage, true);
                            } else {
                                lastMessage = new MessageChat(Long.toString(UUID.randomUUID().getLeastSignificantBits()),
                                        new User(readBuf.split(",")[3], readBuf.split(",")[4], readBuf.split(",")[4], true), readMessage);
                                updateMessageList(dialog_id, lastMessage);
                            }
                        }
                        break;
                    case Constants.MESSAGE_READ_IMAGE:
                        try {
                            byte[] readImageBuf = (byte[]) msg.obj;
                            dialog_id = null;
                            device_address = null;
                            device_name = null;
                            String info_dialog = null;
                            String message_id = Long.toString(UUID.randomUUID().getLeastSignificantBits());
                            byte[] imageByte = new byte[1024*1024];
                            int imageInt = 0;
                            for (int i = 0; i < readImageBuf.length; i++) {
                                if (readImageBuf[i] == ',' && readImageBuf[i + 1] == ',' && readImageBuf.length - i < 1000) {
                                    imageInt = i;
                                    byte[] info = new byte[readImageBuf.length - i];
                                    System.arraycopy(readImageBuf, 0, imageByte, 0, i);
                                    MainActivity.byteArrayImage.put(message_id, readImageBuf);
                                    System.arraycopy(readImageBuf, i + 1, info, 0, readImageBuf.length - i - 1);

                                    info_dialog = new String(info, 1, info.length - 2);
                                    dialog_id = info_dialog.split(",")[0];
                                    device_address = info_dialog.split(",")[1];
                                    device_name = mBluetoothAdapter.getRemoteDevice(info_dialog.split(",")[2]).getName();
                                }

                            }

                            if (device_address.equals("0"))
                                if (dialog_id.equals(mDialogId)) {
                                    lastMessage = new MessageChat(message_id, new User(info_dialog.split(",")[2],
                                            device_name, device_name, true), 0);
                                    messageDao.insert(lastMessage);
                                    adapter.addToStart(lastMessage, true);
                                } else {
                                    lastMessage = new MessageChat(message_id, new User(info_dialog.split(",")[2],
                                            device_name, device_name, true), 0);
                                    updateMessageList(dialog_id, lastMessage);
                                }

                            if (device_address.equals("null"))
                                if (info_dialog.split(",")[2].equals(mDialogId)) {
                                    lastMessage = new MessageChat(message_id, new User(info_dialog.split(",")[2], personDialogDao.getById(info_dialog.split(",")[2]).getDialogName(), null, true), 0);
                                    messageDao.insert(lastMessage);
                                    adapter.addToStart(lastMessage, true);
                                } else {
                                    lastMessage = new MessageChat(message_id, new User(info_dialog.split(",")[2], personDialogDao.getById(info_dialog.split(",")[2]).getDialogName(),
                                            null, true), 0);
                                    updateMessageList(info_dialog.split(",")[2], lastMessage);
                                }

                            if (device_address.equals("1")) {
                                if (dialog_id.equals(mDialogId)) {
                                    lastMessage = new MessageChat(message_id, new User(info_dialog.split(",")[2],
                                            device_name, device_name, true), 0);
                                    messageDao.insert(lastMessage);
                                    adapter.addToStart(lastMessage, true);
                                } else {
                                    lastMessage = new MessageChat(message_id, new User(info_dialog.split(",")[2],
                                            device_name, device_name, true), 0);
                                    updateMessageList(dialog_id, lastMessage);
                                }
                                byte[] s = ("," + "," + mDialogId + "," + "01" + "," + info_dialog.split(",")[2] + "," +
                                        device_name + "\r" + "\r").getBytes();
                                byte[] send = new byte[imageInt + s.length];
                                System.arraycopy(imageByte, 0, send, 0, imageInt);
                                System.arraycopy(s, 0, send, imageInt, s.length);
                                mChatService.write(send, 0, mDialogName, personDialogDao.getById(mDialogId).getUsers(), info_dialog.split(",")[2], null);
                            }

                            if (device_address.equals("01")) {
                                if (dialog_id.equals(mDialogId)) {
                                    lastMessage = new MessageChat(message_id,
                                            new User(info_dialog.split(",")[2], info_dialog.split(",")[3], info_dialog.split(",")[3], true), 0);
                                    messageDao.insert(lastMessage);
                                    adapter.addToStart(lastMessage, true);
                                } else {
                                    lastMessage = new MessageChat(message_id,
                                            new User(info_dialog.split(",")[2], info_dialog.split(",")[3], info_dialog.split(",")[3], true), 0);
                                    updateMessageList(dialog_id, lastMessage);
                                }
                            }
                            break;
                        } catch (Exception e) {
                            Snackbar.make(messagesList, "failed to get image", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                        }
                    case Constants.MESSAGE_TOAST:
                        if (null != activity) {
                            Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                    Toast.LENGTH_SHORT).show();
                            personDialog.getUsers().get(0).setOnline(false);
                        }
                        break;
                    case Constants.MESSAGE_TOAST_LOST:
                        if (null != getActivity()) {
                            device_address = msg.getData().getString(Constants.TOAST);
                            Toast.makeText(getActivity(), mBluetoothAdapter.getRemoteDevice(device_address).getName() + " connection was lost",
                                    Toast.LENGTH_SHORT).show();
                            connectionLost(device_address);
                        }
                        break;
                    case Constants.MESSAGE_TOAST_LOST_GROUP:
                        if (null != getActivity())
                            Snackbar.make(messagesList, "All users are offline", Snackbar.LENGTH_LONG).show();

                        break;
                }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 11:
                if (resultCode == RESULT_OK && data.getClipData() != null) {
                    if (data.getClipData().getItemCount() > 1) {
                        Snackbar.make(messagesList, "max selection - 1 image", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        break;
                    }
                    ClipData mClipData = data.getClipData();
                    List mArrayUri = new ArrayList<Uri>();
                    new Thread(() -> {
                        try {
                            Bitmap q, selectedImage;
                            int i = 0;
                            for (int j = 0; j < mClipData.getItemCount(); j++) {
                                mArrayUri.add(mClipData.getItemAt(j).getUri());
                                InputStream imageStream = null;
                                try {
                                    imageStream = getActivity().getContentResolver().openInputStream(mClipData.getItemAt(j).getUri());
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                }
                                selectedImage = BitmapFactory.decodeStream(imageStream);
                                if (selectedImage.getWidth() + selectedImage.getHeight() > 7000)
                                    i = (selectedImage.getHeight() + selectedImage.getWidth() - 7000) / 200;
                                q = Bitmap.createScaledBitmap(selectedImage, selectedImage.getWidth() - i * 100, selectedImage.getHeight() - i * 100, false);

                                String id_message = Long.toString(UUID.randomUUID().getLeastSignificantBits());
                                byte[] image_storage = convertBitmapToByteArray(q);
                                MainActivity.byteArrayImage.put(id_message, image_storage);
                                sendMessage(convertBitmapToByteArrayForMessage(q), id_message);
                                q.recycle();
                                selectedImage = null;
                                image_storage = null;
                            }
                        } catch (Exception e) {
                        }

                    }
                    ).start();

                }
                break;
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    //setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        String dialog_id = Long.toString(UUID.randomUUID().getLeastSignificantBits()).substring(0, 17);
        mChatService.connect(device, secure);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (personDialog.getAdmin() != null)
            inflater.inflate(R.menu.participants_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.participants:
                showConnectedDeviceDialog();
        }
        return false;
    }

    private void showConnectedDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View view = getLayoutInflater().inflate(R.layout.connected_device_list, null);
        TextView textView = (TextView) view.findViewById(R.id.id_text);
        textView.setText("Members of the group");

        ArrayList<ParticipantsAdapter.Participant> participants = new ArrayList<>();

        participants.add(new ParticipantsAdapter.Participant("You", Constants.ONLINE));
        if (personDialog.getAdmin().equals("0")) {
            for (User user : personDialog.getUsers())
                if (mBluetoothAdapter.getRemoteDevice(user.getId()).getName() != null) {
                    int online;
                    if (BluetoothChatService.listOfConnection.containsKey(user.getId())) {
                        online = Constants.ONLINE;
                        personDialog.getUsers().get(personDialog.getUsers().indexOf(user)).setOnline(true);
                        personDialogDao.update(personDialog);
                    } else {
                        online = Constants.OFFLINE;
                        personDialog.getUsers().get(personDialog.getUsers().indexOf(user)).setOnline(false);
                        personDialogDao.update(personDialog);
                    }
                    participants.add(new ParticipantsAdapter.Participant(mBluetoothAdapter.getRemoteDevice(user.getId()).getName() + "\n" + user.getId(),
                            online));
                }
        } else {
            if (BluetoothChatService.listOfConnection.containsKey(mBluetoothAdapter.getRemoteDevice(personDialog.getAdmin()).getAddress())) {
                participants.add(new ParticipantsAdapter.Participant(mBluetoothAdapter.getRemoteDevice(personDialog.getAdmin()).getName()
                        + " (admin)" + "\n" + personDialog.getAdmin(), Constants.ONLINE));
            }
            else
                participants.add(new ParticipantsAdapter.Participant(mBluetoothAdapter.getRemoteDevice(personDialog.getAdmin()).getName()
                        + " (admin)" + "\n" + personDialog.getAdmin(), Constants.OFFLINE));
            for (int i = 1; i < personDialog.getUsers().size(); i++)
                if (mBluetoothAdapter.getRemoteDevice(personDialog.getUsers().get(i).getId()).getName() != null)
                    participants.add(new ParticipantsAdapter.Participant(mBluetoothAdapter.getRemoteDevice(personDialog.getUsers().get(i).getId()).getName()
                            + "\n" + personDialog.getUsers().get(i).getId(), Constants.NULL));
        }
        ParticipantsAdapter participantsAdapter = new ParticipantsAdapter(getActivity(), participants);

        ListView connectedListView = (ListView) view.findViewById(R.id.connected_devices);
        connectedListView.setAdapter(participantsAdapter);
        builder.setView(view);
        builder.create().show();
    }

    public byte[] convertBitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] q;
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 10, baos);
            q = baos.toByteArray();
            return q;
        } catch (Exception e) {
            return null;
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                    q = null;
                } catch (IOException e) {
                    Log.e(BluetoothChatFragment.class.getSimpleName(), "ByteArrayOutputStream was not closed");
                }
            }
        }
    }

    public byte[] convertBitmapToByteArrayForMessage(Bitmap bitmap) {
        String admin = personDialogDao.getById(mDialogId).getAdmin();
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 10, baos);
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            b = baos;

            if (admin == null) {
                b.write(',');
                b.write(',');
                b.write(mDialogId.getBytes());
                b.write(',');
                b.write("null".getBytes());
            } else if (admin.equals("0")) {
                b.write(',');
                b.write(',');
                b.write(mDialogId.getBytes());
                b.write(',');
                b.write("0".getBytes());
            } else {
                b.write(',');
                b.write(',');
                b.write(mDialogId.getBytes());
                b.write(',');
                b.write("1".getBytes());
            }

            b.write('\r');
            b.write('\r');
            byte[] q = baos.toByteArray();
            return q;
        } catch (Exception e) {
            return null;
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    Log.e(BluetoothChatFragment.class.getSimpleName(), "ByteArrayOutputStream was not closed");
                }
            }
        }
    }


    public static synchronized Bitmap convertCompressedByteArrayToBitmap(byte[] src) {
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeByteArray(src, 0, src.length - 2);
        } catch (Exception e){
            bitmap = null;
        }
        return bitmap;
    }

    public interface OnFragmentListener {
        void updateDialog(PersonDialog personDialog);
    }


    public void initDatabase() {
        db = Room.databaseBuilder(getContext(),
                AppDatabase.class, mDialogId)
                .allowMainThreadQueries()
                .build();
        db1 = Room.databaseBuilder(getContext(),
                AppDatabase.class, "dialogs")
                .allowMainThreadQueries()
                .build();
        messageDao = db.getMessageDao();
        personDialogDao = db1.getPersonDao();
    }

    public MessageDao initDatabase(String address) {
        return Room.databaseBuilder(getContext(),
                AppDatabase.class, address)
                .allowMainThreadQueries()
                .build().getMessageDao();
    }

    public Bitmap createUserAvatar(char symbol) {
        View v = new UserAvatar(getActivity(), symbol);
        Bitmap bitmap = Bitmap.createBitmap(500/*width*/, 500/*height*/, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        v.draw(canvas);
        return bitmap;
    }

    private void connectionLost(String address) {
        if (personDialog.getAdmin() == null && personDialogDao.getById(address) == null) {
            PersonDialog pd = personDialogDao.getById(address);
            pd.getUsers().get(0).setOnline(false);
            personDialogDao.update(pd);
            setStatus("offline");
        } else {
            for (int i = 0; i < personDialog.getUsers().size(); i++) {
                if (personDialog.getUsers().get(i).getId().equals(address))
                    personDialog.getUsers().get(i).setOnline(false);
                personDialogDao.update(personDialog);

            }
        }
    }

    private void updateDialog(String address, String group_id) {
        if (mChatService.listOfConnection.get(address) != null && personDialogDao.getById(address) != null) {
            PersonDialog p = personDialogDao.getById(address);
            p.getUsers().get(0).setOnline(true);
            personDialogDao.update(p);
        } else {
            for (int i = 0; i < personDialog.getUsers().size(); i++) {
                if (personDialog.getUsers().get(i).getId().equals(address))
                    personDialog.getUsers().get(i).setOnline(false);
                personDialogDao.update(personDialog);
            }
        }
    }

    public void updateMessageList(String dialog_id, MessageChat lastMessage) {
        if (dialog_id == null)
            return;
        MessageDao md = initDatabase(dialog_id);
        md.insert(lastMessage);
        PersonDialog pd = personDialogDao.getById(dialog_id);
        pd.setLastMessage(lastMessage);
        pd.addUnreadCount();
        personDialogDao.update(pd);
    }
}
