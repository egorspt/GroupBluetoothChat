package com.example.android.bluetoothchat.Fragments;

import android.support.v7.app.ActionBar;
import android.arch.persistence.room.Room;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.android.bluetoothchat.BluetoothChatService;
import com.example.android.bluetoothchat.Constants;
import com.example.android.bluetoothchat.Database.AppDatabase;
import com.example.android.bluetoothchat.Database.DialogViewHolder;
import com.example.android.bluetoothchat.Database.MessageDao;
import com.example.android.bluetoothchat.Database.PersonDialogDao;
import com.example.android.bluetoothchat.Activities.DeviceListActivity;
import com.example.android.bluetoothchat.ChatKit.DialogAvatar;
import com.example.android.bluetoothchat.Activities.MainActivity;
import com.example.android.bluetoothchat.ChatKit.MessageChat;
import com.example.android.bluetoothchat.ChatKit.PersonDialog;
import com.example.android.bluetoothchat.R;
import com.example.android.bluetoothchat.ChatKit.User;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.stfalcon.chatkit.dialogs.DialogsList;
import com.stfalcon.chatkit.dialogs.DialogsListAdapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import static android.app.Activity.RESULT_OK;

public class DialogsFragment extends Fragment implements Serializable {
    DialogsList dialogsListView;
    OnFragmentListener fragmentListener;

    AppDatabase db;
    PersonDialogDao personDialogDao;
    private PersonDialog personDialog;
    private MessageChat lastMessage = null;

    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final int REQUEST_CONNECT_GROUP = 4;
    private static final String ARG_PARAM1 = "param1";

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothChatService mChatService = null;
    DialogsListAdapter dialogsListAdapter;

    FloatingActionButton fab_dailog, fab_group, fab_anonymous;
    FloatingActionMenu fab_menu;

    public DialogsFragment() {

    }

    public static DialogsFragment newInstance(BluetoothChatService mService) {
        DialogsFragment fragment = new DialogsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM1, mService);

        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dialogs, container, false);

        dialogsListView = (DialogsList) view.findViewById(R.id.dialogsList);
        initAdapter();

        fab_dailog = (FloatingActionButton) view.findViewById(R.id.fab_dialog);
        fab_group = (FloatingActionButton) view.findViewById(R.id.fab_group);
        fab_anonymous = (FloatingActionButton) view.findViewById(R.id.fab_anonymous);
        fab_menu = (FloatingActionMenu) view.findViewById(R.id.fab_menu);

        fab_dailog.setOnClickListener(v -> {
            if (!mBluetoothAdapter.isEnabled()) {
                Snackbar.make(dialogsListView, "Turn on bluetooth", Snackbar.LENGTH_LONG).show();
                fab_menu.close(true);
                return;
            }
            Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
            serverIntent.putExtra("choice", "single");
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
        });

        fab_group.setOnClickListener(v -> {
            if (!mBluetoothAdapter.isEnabled()) {
                Snackbar.make(dialogsListView, "Turn on bluetooth", Snackbar.LENGTH_LONG).show();
                fab_menu.close(true);
                return;
            }
            Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
            serverIntent.putExtra("choice", "multi");
            startActivityForResult(serverIntent, REQUEST_CONNECT_GROUP);

        });

        fab_anonymous.setOnClickListener(v -> {
        });
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            mChatService = (BluetoothChatService) getArguments().getSerializable(ARG_PARAM1);
            mChatService.setHandler(mHandler);
            mChatService.setmHandler(mmHandler);
            initDatabase();

        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        for (PersonDialog pd : personDialogDao.getA()) {
            for (User user : pd.getUsers())
                user.setOnline(false);
            personDialogDao.update(pd);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        if (mChatService != null) {
            mChatService.setHandler(mHandler);
        }
        ArrayList<PersonDialog> personDialogs = new ArrayList<>(personDialogDao.getA());
        for (PersonDialog personDialog : personDialogs) {
            boolean have = false;
            for(String key : mChatService.listOfConnectionKey()) {
                if (personDialog.getId().equals(key)) {
                    have = true;
                    personDialog.getUsers().get(0).setOnline(true);
                    personDialogDao.update(personDialog);
                }
            }
            if (!have && personDialog.getAdmin() == null)
                connectionLost(personDialog.getId());
        }
        ArrayList<PersonDialog> pDialogs = new ArrayList<>(personDialogDao.getA());
        Collections.reverse(pDialogs);
        dialogsListAdapter.setItems(pDialogs);
        setStatus("");
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            fragmentListener = (OnFragmentListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement an interface OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void initAdapter() {
        dialogsListAdapter = new DialogsListAdapter<>(R.layout.dialog_view_holder,
                DialogViewHolder.class,
                (imageView, url) -> {
                    if(url != null)
                    Glide.with(getActivity())
                            .load(createDialogAvatar(url.charAt(0)))
                            .into(imageView);
                });

        dialogsListAdapter.setOnDialogClickListener(dialog ->
                fragmentListener.onFragmentInteraction(mChatService, dialog.getId()));
        dialogsListAdapter.setOnDialogLongClickListener(dialog -> new AlertDialog.Builder(getActivity()).setMessage("Delete dialogue with " + dialog.getDialogName()).setNegativeButton("No", (dialog1, which) -> {
                })
                        .setPositiveButton("Yes", (dialog1, which) -> {
                            personDialogDao.delete(personDialogDao.getById(dialog.getId()));
                            removeMessagesDialog(dialog.getId());
                            ArrayList<PersonDialog> personDialogs = new ArrayList<>(personDialogDao.getA());
                            Collections.reverse(personDialogs);
                            dialogsListAdapter.setItems(personDialogs);
                        }).show()
        );

        dialogsListView.setAdapter(dialogsListAdapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.connected_device: {
                showConnectedDeviceDialog();
                return true;
            }
            case R.id.secure_connect_scan: {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }

    private void connectDevice(Intent data, boolean secure) {
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mChatService.connect(device, secure);
    }

    private void createDialog(String device_name, String device_address) {
        personDialogDao.insert(new PersonDialog(device_name, device_address,
                new ArrayList<User>() {{
                    add(new User(device_address, device_name, null, true));
                }}, null));
        fragmentListener.onFragmentInteraction(mChatService, device_address);
    }

    private void connectGroup(Intent data) {
        String[] devices = data.getStringArrayExtra(DeviceListActivity.EXTRA_DEVICES_ADDRESS);
        String group_name = data.getStringExtra(DeviceListActivity.EXTRA_GROUP_NAME);
        String group_id = Long.toString(UUID.randomUUID().getLeastSignificantBits()).substring(0, 17);

        personDialog = new PersonDialog(group_name, group_id,
                new ArrayList<User>() {{
                    for (String device : devices) {
                        BluetoothDevice dev = mBluetoothAdapter.getRemoteDevice(device);
                        //mChatService.connectGroup(dev, true, group_id, group_name, devices);
                        add(new User(device, dev.getName(), dev.getName(), true));
                    }
                }}, "0");
        personDialogDao.insert(personDialog);
        dialogsListAdapter.addItem(0, personDialog);

        BluetoothDevice dev = mBluetoothAdapter.getRemoteDevice(devices[0]);
        mChatService.connectGroup(dev, true, group_id, group_name, devices, 0);

        fragmentListener.onFragmentInteraction(mChatService, group_id);
    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
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
                    if(msg.getData() != null)
                        updateDialodList(msg.getData().getString(Constants.DEVICE_ADDRESS), null);
                    break;
            }
        }
    };

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.DIALOG_CREATE_I:
                    String device_address = msg.getData().getString(Constants.DEVICE_ADDRESS);
                    String device_name = msg.getData().getString(Constants.DEVICE_NAME);
                    createDialog(device_name, device_address);
                    break;
                case Constants.DIALOG_CREATE_OTHER:
                    String dev_address = msg.getData().getString(Constants.DEVICE_ADDRESS);
                    String dev_name = msg.getData().getString(Constants.DEVICE_NAME);
                    createDialog(dev_name, dev_address);
                    break;
                case Constants.GROUP_ADMIN:
                    byte[] buffer = (byte[]) msg.obj;
                    String group = new String(buffer);
                    final String group_info = group.substring(0, group.length() - 1);
                    String groupId = group_info.split(",")[0];
                    String groupName = group_info.split(",")[1];
                    int users = group_info.split(",").length - 2;
                    personDialog = new PersonDialog(groupName, groupId,
                            new ArrayList<User>() {{
                                for (int i = 2; i < users + 2; i++)
                                    add(new User(group_info.split(",")[i], group_info.split(",")[i], "", true));

                            }}, "0");
                    personDialogDao.insert(personDialog);
                    fragmentListener.onFragmentInteraction(mChatService, groupId);
                    break;
                case Constants.GROUP_INFO:
                    String group_message = msg.obj.toString();
                    String group_id = group_message.split(",")[0];
                    String group_name = group_message.split(",")[1];
                    int num_user = group_message.split(",").length - 3;
                    String admin = group_message.split(",")[group_message.split(",").length - 1];
                    personDialog = new PersonDialog(group_name, group_id,
                            new ArrayList<User>() {{
                                add(new User(admin, admin, "123", true));
                                for (int i = 2; i < num_user + 2; i++)
                                    add(new User(group_message.split(",")[i], group_message.split(",")[i], group_message.split(",")[i], true));

                            }}, admin);
                    personDialogDao.insert(personDialog);
                    if (personDialogDao.getById(group_id) != null)
                        Toast.makeText(getActivity(), "Welcome to " + group_name, Toast.LENGTH_LONG);

                    fragmentListener.onFragmentInteraction(mChatService, group_id);
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != getActivity()) {
                        Toast.makeText(getActivity(), msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST_LOST:
                    if (null != getActivity()) {
                        String address = msg.getData().getString(Constants.TOAST);
                        Toast.makeText(getActivity(), mBluetoothAdapter.getRemoteDevice(address).getName() + " connection was lost",
                                Toast.LENGTH_SHORT).show();
                        connectionLost(address);
                    }
                    break;
                case Constants.MESSAGE_READ:
                    String readBuf = msg.obj.toString();
                    String readMessage = readBuf.split(",")[0];
                    String dialog_id = readBuf.split(",")[1];
                    String address = readBuf.split(",")[2];

                    if (address.equals("0")) {
                        lastMessage = new MessageChat(Long.toString(UUID.randomUUID().getLeastSignificantBits()), new User(readBuf.split(",")[3],
                                mBluetoothAdapter.getRemoteDevice(readBuf.split(",")[3]).getName(),
                                mBluetoothAdapter.getRemoteDevice(readBuf.split(",")[3]).getName(), true), readMessage);
                        updateDialodList(dialog_id, lastMessage);
                    }

                    if (address.equals("null")) {
                        lastMessage = new MessageChat(Long.toString(UUID.randomUUID().getLeastSignificantBits()), new User(readBuf.split(",")[3], personDialogDao.getById(readBuf.split(",")[3]).getDialogName(),
                                null, true), readMessage);
                        updateDialodList(readBuf.split(",")[3], lastMessage);
                    }

                    if (address.equals("1")) {
                        lastMessage = new MessageChat(Long.toString(UUID.randomUUID().getLeastSignificantBits()), new User(readBuf.split(",")[3],
                                mBluetoothAdapter.getRemoteDevice(readBuf.split(",")[3]).getName(), mBluetoothAdapter.getRemoteDevice(readBuf.split(",")[3]).getName(), true), readMessage);
                        updateDialodList(dialog_id, lastMessage);

                        byte[] send = (readBuf + "," + dialog_id + "," + "01" + readBuf.split(",")[3] +
                                mBluetoothAdapter.getRemoteDevice(readBuf.split(",")[3]).getName() + "\r").getBytes();
                        mChatService.write(send, -1, personDialogDao.getById(dialog_id).getDialogName(), personDialogDao.getById(dialog_id).getUsers(), readBuf.split(",")[3], null);
                    }

                    if (address.equals("01")) {
                        lastMessage = new MessageChat(Long.toString(UUID.randomUUID().getLeastSignificantBits()),
                                new User(readBuf.split(",")[3], readBuf.split(",")[4], readBuf.split(",")[4], true), readMessage);
                        updateDialodList(dialog_id, lastMessage);
                    }
                    break;
                case Constants.MESSAGE_READ_IMAGE:
                    byte[] readImageBuf = (byte[]) msg.obj;
                    dialog_id = null;
                    address = null;
                    String name_device = null;
                    String info_dialog = null;
                    String message_id = Long.toString(UUID.randomUUID().getLeastSignificantBits());
                    PersonDialog pd;
                    try {
                        for (int i = 0; i < readImageBuf.length; i++) {
                            if (readImageBuf[i] == ',' && readImageBuf[i + 1] == ',' && readImageBuf.length - i < 1000) {
                                byte[] imageByte = new byte[i];
                                byte[] info = new byte[readImageBuf.length - i];
                                System.arraycopy(readImageBuf, 0, imageByte, 0, i);
                                MainActivity.byteArrayImage.put(message_id, readImageBuf);
                                System.arraycopy(readImageBuf, i + 1, info, 0, readImageBuf.length - i - 1);

                                info_dialog = new String(info, 1, info.length - 2);
                                dialog_id = info_dialog.split(",")[0];
                                address = info_dialog.split(",")[1];
                                name_device = mBluetoothAdapter.getRemoteDevice(info_dialog.split(",")[2]).getName();
                            }

                        }

                        if (address.equals("0")) {
                            lastMessage = new MessageChat(message_id, new User(info_dialog.split(",")[2],
                                    name_device, name_device, true), 0);
                            updateDialodList(dialog_id, lastMessage);
                        }

                        if (address.equals("null")) {
                            lastMessage = new MessageChat(message_id, new User(info_dialog.split(",")[2], personDialogDao.getById(info_dialog.split(",")[2]).getDialogName(),
                                    null, true), 0);
                            updateDialodList(info_dialog.split(",")[2], lastMessage);
                        }

                        if (address.equals("1")) {
                            lastMessage = new MessageChat(message_id, new User(info_dialog.split(",")[2],
                                    name_device, name_device, true), 0);
                            updateDialodList(dialog_id, lastMessage);

                            byte[] send = (readImageBuf + "," + dialog_id + "," + "01" + "," + info_dialog.split(",")[2] + "," +
                                    name_device + "\r").getBytes();
                            mChatService.write(send, 0, personDialogDao.getById(dialog_id).getDialogName(), personDialogDao.getById(dialog_id).getUsers(), info_dialog.split(",")[2], null);
                        }

                        if (address.equals("01")) {
                            lastMessage = new MessageChat(message_id,
                                    new User(info_dialog.split(",")[2], info_dialog.split(",")[3], info_dialog.split(",")[3], true), 0);
                            updateDialodList(dialog_id, lastMessage);

                        }
                        break;
                    } catch (Exception e) {
                        Snackbar.make(dialogsListView, "failed to get image from " + name_device, Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }


            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Bitmap q, selectedImage;
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                if (resultCode == RESULT_OK) {
                    connectDevice(data, true);
                }
                if (resultCode == Constants.RESULT_REPLACE) {
                    boolean h = false;
                    String device_address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    String device_name = mBluetoothAdapter.getRemoteDevice(device_address).getName();
                    for (PersonDialog pd : personDialogDao.getA())
                        if (pd.getId().equals(device_address)) {
                            h = true;
                            Snackbar.make(dialogsListView, "Уже подключено", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                        }
                    if (h == false)
                        createDialog(device_address, device_name);
                }
                break;
            case REQUEST_CONNECT_GROUP:
                if (resultCode == RESULT_OK) {
                    connectGroup(data);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                if (resultCode == RESULT_OK) {
                    connectDevice(data, false);
                }
                if (resultCode == Constants.RESULT_REPLACE) {
                    boolean have = false;
                    for (PersonDialog pd : personDialogDao.getA())
                        if (pd.getId().equals(data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS))) {
                            have = true;
                            Snackbar.make(dialogsListView, "Уже подключено", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                        }
                    if (have == false)
                        connectDevice(data, true);
                }
                break;
        }
    }

    public interface OnFragmentListener {
        void onFragmentInteraction(BluetoothChatService mChatService, String address);
    }

    public void initDatabase() {
        db = Room.databaseBuilder(getContext(),
                AppDatabase.class, "dialogs")
                .allowMainThreadQueries()
                .build();
        personDialogDao = db.getPersonDao();
    }

    public MessageDao initDatabase(String address) {
        return Room.databaseBuilder(getContext(),
                AppDatabase.class, address)
                .allowMainThreadQueries()
                .build().getMessageDao();
    }

    private void removeMessagesDialog(String id) {
        AppDatabase db = Room.databaseBuilder(getContext(),
                AppDatabase.class, id)
                .allowMainThreadQueries()
                .build();
        MessageDao md = db.getMessageDao();
        md.deleteAll();
    }

    public void updateDialog(PersonDialog personDialog) {
        personDialogDao.update(personDialog);
    }

    private void showConnectedDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View view = getLayoutInflater().inflate(R.layout.connected_device_list, null);

        ArrayAdapter<String> connectedDeviceAdapter = new ArrayAdapter<String>(getActivity(), R.layout.device_name);
        for (String device : BluetoothChatService.listOfConnection.keySet())
            connectedDeviceAdapter.add(mBluetoothAdapter.getRemoteDevice(device).getName()
                    + "\n" + device);
        ListView connectedListView = (ListView) view.findViewById(R.id.connected_devices);
        connectedListView.setAdapter(connectedDeviceAdapter);
        builder.setView(view);
        builder.create().show();
        connectedListView.setOnItemClickListener((AdapterView<?> parent, View view1, int position, long id) -> {
            String info = ((TextView) view1).getText().toString();
            String name = info.substring(0, info.length() - 18);
            String address = info.substring(info.length() - 17);
            new android.app.AlertDialog.Builder(getActivity()).setMessage("Break the connection with " + name).setNegativeButton("No", (dialog1, which) -> {
            })
                    .setPositiveButton("Yes", (dialog1, which) -> {
                        mChatService.closeSocket(address);
                        connectedDeviceAdapter.remove(info);
                        connectionLost(address);
                    }).create().show();
        });
    }

    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        ;
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    public Bitmap createDialogAvatar(char symbol) {
        View v = new DialogAvatar(getActivity(), symbol);
        Bitmap bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        v.draw(canvas);
        return bitmap;
    }

    private void connectionLost(String address) {
        if (personDialogDao.getById(address) == null)
            return;
        PersonDialog pd = personDialogDao.getById(address);
        pd.getUsers().get(0).setOnline(false);
        personDialogDao.update(pd);
        dialogsListAdapter.updateItemById(pd);
    }

    public void updateDialodList(String dialog_id, MessageChat lastMessage) {
        if (dialog_id == null || personDialogDao.getById(dialog_id) == null)
            return;
        PersonDialog pd = personDialogDao.getById(dialog_id);
        if(lastMessage != null) {
            MessageDao md = initDatabase(dialog_id);
            md.insert(lastMessage);
            pd.setLastMessage(lastMessage);
            pd.addUnreadCount();
        }
        pd.getUsers().get(0).setOnline(true);
        personDialogDao.update(pd);
        dialogsListAdapter.updateItemById(pd);
    }
}
