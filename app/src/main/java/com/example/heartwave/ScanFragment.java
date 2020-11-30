package com.example.heartwave;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ScanFragment extends Fragment implements View.OnClickListener {
    private MessageSender mMessageSenderCallback;
    private MyBroadcastReceiver receiver;
    ListView listView;
    ArrayList<String> arrayList;
    ArrayAdapter<String> arrayAdapter;
    ArrayList<String> arrayDevices;
    BluetoothDevice device;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan, container,false);
        initialize(view);
        return view;
    }

    private void initialize(View view) {
        Button scanButton = view.findViewById(R.id.button_scan);
        Button connectButton = view.findViewById(R.id.button_connect);
        Button disconnectButton = view.findViewById(R.id.button_disconnect);
        listView = view.findViewById(R.id.list_view);
//        arrayList = new ArrayList<String>();
        arrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1);
        listView.setAdapter(arrayAdapter);
        arrayDevices = new ArrayList<>();
        scanButton.setOnClickListener(this);
        connectButton.setOnClickListener(this);
        disconnectButton.setOnClickListener(this);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String nameAddress = arrayAdapter.getItem(position);
                String lines[] = nameAddress.split("\\r?\\n");
                device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(lines[1]);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.button_scan:
                mMessageSenderCallback.sendMessage(R.id.button_scan, "");
                break;
            case R.id.button_connect:
                mMessageSenderCallback.sendMessage(R.id.button_connect, device.getAddress());
                break;
            case R.id.button_disconnect:
                mMessageSenderCallback.sendMessage(R.id.button_disconnect, "");
                break;
            default:
        }
    }

    interface MessageSender {
        void sendMessage(int id, String address);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            mMessageSenderCallback = (MessageSender) context;
        } catch (ClassCastException e) {
            Log.d("Error", "exception thrown");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMessageSenderCallback = null;
        requireActivity().unregisterReceiver(receiver);
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getAction()) {
                case BleService.ACTION_SCAN_DEVICE:
                    Bundle extras = intent.getExtras();
                    String state = extras.getString(BleService.EXTRA_DEVICE_BLE);
//                    arrayList.add(state);
//                    arrayAdapter.notifyDataSetChanged();
                    String lines[] = state.split("\\r?\\n");
                    if(!arrayDevices.contains(lines[1])) {
                        arrayAdapter.add(state);
                        arrayDevices.add(lines[1]);
                    }
                    break;
                case BleService.ACTION_SEND_DATA:
                    break;
                default:
            }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_SCAN_DEVICE);
        receiver = new MyBroadcastReceiver();
        requireActivity().registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe
    public void onMessageEvent(MessageEvent event) {
        Log.d("Tag: ", event.message);
    }
}
