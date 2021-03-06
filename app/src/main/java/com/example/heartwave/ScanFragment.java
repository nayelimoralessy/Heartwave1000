package com.example.heartwave;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.os.Bundle;
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
    ListView listView;
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
        listView = view.findViewById(R.id.list_view);
        arrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1);
        listView.setAdapter(arrayAdapter);
        arrayDevices = new ArrayList<>();
        scanButton.setOnClickListener(this);
        connectButton.setOnClickListener(this);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String nameAddress = arrayAdapter.getItem(position);
                String[] lines= nameAddress.split("\\r?\\n");
                device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(lines[1]);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.button_scan:
                EventBus.getDefault().post(new MessageEvent("SCAN",
                        MessageEvent.File.FRAGMENT_SCAN, MessageEvent.File.SERVICE,
                        MessageEvent.Action.BUTTON_SCAN));
                break;
            case R.id.button_connect:
                String address = (device != null) ? device.getAddress() : "";
                EventBus.getDefault().post(new MessageEvent(address,
                        MessageEvent.File.FRAGMENT_SCAN, MessageEvent.File.SERVICE,
                        MessageEvent.Action.BUTTON_CONNECT));
                break;
            default:
                // Do nothing
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        if(event.receiver.equals(MessageEvent.File.FRAGMENT_SCAN)) {
            switch(event.action) {
                case SCAN:
                    String data = event.data;
                    String[] lines = data.split("\\r?\\n");
                    if(!arrayDevices.contains(lines[1])) {
                        arrayAdapter.add(data);
                        arrayDevices.add(lines[1]);
                    }
                    break;
                case SAMPLE_RATE:
                    // Do nothing
                    break;
                default:
                    // Unhandled action
            }
        }
    }
}
