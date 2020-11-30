package com.example.heartwave;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.androidplot.util.Redrawer;
import com.androidplot.xy.AdvancedLineAndPointRenderer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import static android.content.Context.MODE_PRIVATE;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class EcgFragment extends Fragment{
    XYPlot plot;
    Redrawer redrawer;
    private MyBroadcastReceiver receiver;
    double value = 0.0;
    String time;
    private static final String FILE_NAME = "example.txt";
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ecg, container, false);
        initialize(view);
        return view;
    }

    private void initialize(View view) {
        plot = view.findViewById(R.id.plot);

        ECGModel ecgSeries = new ECGModel(1000, 100);

        // add a new series' to the XYPlot:
        MyFadeFormatter formatter = new MyFadeFormatter(1000);
        formatter.setLegendIconEnabled(false);
        plot.addSeries(ecgSeries, formatter);
        plot.setRangeBoundaries(-1, 5, BoundaryMode.FIXED);
        plot.setDomainBoundaries(0, 1000, BoundaryMode.FIXED);
        // reduce the number of range labels
        plot.setLinesPerRangeLabel(3);
        // start generating ecg data in the background:
        ecgSeries.start(new WeakReference<>(plot.getRenderer(AdvancedLineAndPointRenderer.class)));
        // set a redraw rate of 30hz and start immediately:
        redrawer = new Redrawer(plot, 10, true);

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy  hh:mm");
        Calendar calendar = Calendar.getInstance();
        int t = calendar.get(Calendar.AM_PM);
        if(t == Calendar.AM) {
            time = dateFormat.format(new Date()) + "AM\n";
        }
        else {
            time = dateFormat.format(new Date()) + "PM\n";
        }
        FileOutputStream fos = null;
        try {
            fos = getActivity().openFileOutput(FILE_NAME, Context.MODE_APPEND);
            fos.write(time.getBytes());
            String e = null;
            if((ecgSeries.blipInterval>60)&(ecgSeries.blipInterval<100))
                e = Integer.toString(ecgSeries.blipInterval) + " (Normal)\n";
            else
                e = Integer.toString(ecgSeries.blipInterval) + " (Irregular Activity)\n";
            fos.write(e.getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public static class MyFadeFormatter extends AdvancedLineAndPointRenderer.Formatter {

        private int trailSize;

        MyFadeFormatter(int trailSize) {
            this.trailSize = trailSize;
        }

        @Override
        public Paint getLinePaint(int thisIndex, int latestIndex, int seriesSize) {
            // offset from the latest index:
            int offset;
            if(thisIndex > latestIndex) {
                offset = latestIndex + (seriesSize - thisIndex);
            } else {
                offset =  latestIndex - thisIndex;
            }

            float scale = 255f / trailSize;
            int alpha = (int) (255 - (offset * scale));
            getLinePaint().setAlpha(Math.max(alpha, 0));
            return getLinePaint();
        }
    }

    public class ECGModel implements XYSeries {

        private final Number[] data;
        private final long delayMs;
        private final int blipInterval;
        private final Thread thread;
        private boolean keepRunning;
        private int latestIndex;

        private WeakReference<AdvancedLineAndPointRenderer> rendererRef;

        /**
         *
         * @param size Sample size contained within this model
         * @param updateFreqHz Frequency at which new samples are added to the model
         */
        ECGModel(int size, int updateFreqHz) {
            data = new Number[size];
            Arrays.fill(data, 0);

            // translate hz into delay (ms):
            delayMs = 1000 / updateFreqHz;

            blipInterval = size / 7;

            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (keepRunning) {
                            if (latestIndex >= data.length) {
                                latestIndex = 0;
                            }

                            // generate some random data:
                            if (latestIndex % blipInterval == 0) {
                                // insert a "blip" to simulate a heartbeat:
                                data[latestIndex] = value;
                            } else {
                                // insert a random sample:
                                data[latestIndex] = value;
                            }

                            if(latestIndex < data.length - 1) {
                                // null out the point immediately following i, to disable
                                // connecting i and i+1 with a line:
                                data[latestIndex +1] = null;
                            }

                            if(rendererRef.get() != null) {
                                rendererRef.get().setLatestIndex(latestIndex);
                                Thread.sleep(delayMs);
                            } else {
                                keepRunning = false;
                            }
                            latestIndex++;
                        }
                    } catch (InterruptedException e) {
                        keepRunning = false;
                    }
                }
            });
        }

        void start(final WeakReference<AdvancedLineAndPointRenderer> rendererRef) {
            this.rendererRef = rendererRef;
            keepRunning = true;
            thread.start();
        }

        @Override
        public int size() {
            return data.length;
        }

        @Override
        public Number getX(int index) {
            return index;
        }

        @Override
        public Number getY(int index) {
            return data[index];
        }

        @Override
        public String getTitle() {
            return "Signal";
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getAction()) {
                case BleService.ACTION_SCAN_DEVICE:
                    break;
                case BleService.ACTION_SEND_DATA:
                    Bundle extras = intent.getExtras();
                    String state = extras.getString(BleService.EXTRA_DEVICE_BLE);
                    value = Double.parseDouble(state);
                    break;
                default:
            }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_SEND_DATA);
        receiver = new EcgFragment.MyBroadcastReceiver();
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
