package com.example.heartwave;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class StatsFragment extends Fragment {
    private MyBroadcastReceiver receiver;
    private TextView textView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);
        textView = view.findViewById(R.id.text);
        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        requireActivity().unregisterReceiver(receiver);
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getAction()) {
                case BleService.ACTION_SCAN_DEVICE:
                    break;
                case BleService.ACTION_SEND_DATA:
                    break;
                case BleService.ACTION_SAMPLE_RATE:
                    Bundle extras = intent.getExtras();
                    String state = extras.getString(BleService.EXTRA_DEVICE_BLE);
                    textView.setText(state);
                    break;
                default:
            }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_SAMPLE_RATE);
        receiver = new MyBroadcastReceiver();
        requireActivity().registerReceiver(receiver, intentFilter);
    }
}
