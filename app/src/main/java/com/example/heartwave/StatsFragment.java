package com.example.heartwave;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class StatsFragment extends Fragment{
    private MyBroadcastReceiver receiver;
    ArrayList a;
    ArrayAdapter aad;
    ListView records;
    private StatsFragment.MessageSender mMessageSenderCallback;
    Context con;
    private static final String FILE_NAME = "example.txt";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);
//        dbh = new DatabaseHelper(getActivity());

        init(view);
        return view;
    }
    public void init(View view){
        records = view.findViewById(R.id.records);
        FileInputStream fis = null;
        try {
            fis = getActivity().openFileInput(FILE_NAME);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            a = new ArrayList();
            String data, date = null;
            for (int i=1;(data= br.readLine()) != null;i++) {
                if((i%2) == 0)
                    a.add(date + "\nHeart rate: " + data);
                else
                    date = data;
            }
            aad = new ArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, a);
            records.setAdapter(aad);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    @Override
    public void onDetach() {
        super.onDetach();
        mMessageSenderCallback = null;
        requireActivity().unregisterReceiver(receiver);
    }
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        con = context;
        try {
            //mMessageSenderCallback = (MessageSender) context;
            con = context;
        }
        catch (ClassCastException e) {
            Log.d("Error", "exception thrown");
        }
    }
    interface MessageSender {
        void sendMessage(int id, String address);
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
//                    textView.setText(state);
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
    private void showToast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
    }
}
//broadcaster stuff needed here too?
