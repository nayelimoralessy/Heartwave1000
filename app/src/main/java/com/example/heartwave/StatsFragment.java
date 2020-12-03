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

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

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
    ArrayList a;
    ArrayAdapter aad;
    ListView records;
    private StatsFragment.MessageSender mMessageSenderCallback;
    Context con;
    private static final String FILE_NAME = "example.txt";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void showToast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
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
        if(event.receiver.equals(MessageEvent.File.FRAGMENT_STATS)) {
            switch(event.action) {
                case SCAN:
                    // Do nothing
                    break;
                case SAMPLE_RATE:
                    // Add try-catch block
                    double sampleRate = Double.parseDouble(event.data);
                    break;
                default:
                    // Unhandled action
            }
        }
    }
}
//broadcaster stuff needed here too?no!
