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
import android.widget.Button;
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
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import static android.content.Context.MODE_PRIVATE;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class EcgFragment extends Fragment{
    XYPlot plot;
    Redrawer redrawer;
    double value = 0.0;
    String time,e;
    Button start, save;
    String sec1, min1, sec2, min2;
    int flag = 0, heartbeat = 0;
    double heartrate = 0;
    SimpleDateFormat second1, minute1, second2, minute2;
    ECGModel ecgSeries;
    MyFadeFormatter formatter;
    int m1, m2, s1, s2;
    private static final String FILE_NAME = "example.txt";

    Long startTimestamp;
    Long currentTimestamp;
    Long difference;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ecg, container, false);
        initialize(view);
        return view;
    }

    private void initialize(View view) {
        plot = view.findViewById(R.id.plot);
        start = view.findViewById(R.id.start);
        save = view.findViewById(R.id.save);
        plot.setRangeBoundaries(-1, 5, BoundaryMode.FIXED);
        plot.setDomainBoundaries(0, 1000, BoundaryMode.FIXED);
        // reduce the number of range labels
        plot.setLinesPerRangeLabel(3);
        ecgSeries = new ECGModel(1000, 100);
        formatter = new MyFadeFormatter(1000);
        start.setVisibility(VISIBLE);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTimestamp = System.currentTimeMillis() / 1000;
                start.setVisibility(INVISIBLE);
                formatter.setLegendIconEnabled(false);
                // add a new series' to the XYPlot:
                plot.addSeries(ecgSeries, formatter);
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

//                minute1 = new SimpleDateFormat("mm");
//                second1 = new SimpleDateFormat("ss");
//                min1 = minute1.format(new Date());
//                sec1 = second1.format(new Date());
//                m1 = Integer.parseInt(min1);
//                s1 = Integer.parseInt(sec1);
//                if ((value > 2.5)&&(flag == 0)){
//                    heartbeat++;
//                    flag = 1;
//                } //change 2 to 1.5
//                else if((value < 2.5)&&(flag == 1))
//                    flag = 0;
            }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentTimestamp = System.currentTimeMillis() / 1000;
                difference = currentTimestamp - startTimestamp;
                FileOutputStream fos = null;
                heartrate = ((double) heartbeat * 60.0) / ((double) difference);
//                int dif;
//
//                minute2 = new SimpleDateFormat("mm");
//                second2 = new SimpleDateFormat("ss");
//                min2 = minute2.format(new Date());
//                sec2 = second2.format(new Date());
//                m2 = Integer.parseInt(min2);
//                s2 = Integer.parseInt(sec2);
//                if((m2 > m1)&&(s2>=s1)) dif = (60*(m2-m1)) + (s2-s1);
//                else if((m2 > m1)&&(s2<s1)) dif = ((60*(m2-m1))- s1)+ s2;
//                else dif = s2-s1;
//                if(dif<1) dif = 1;
//                heartrate = (heartbeat*60)/dif;
                if ((heartrate >= 60) && (heartrate <= 100))
                    e = ((int) (heartrate)) + " (Normal)\n";
                else
                    e = ((int) (heartrate)) + " (Irregular Activity)\n";
                if (e != null) {
                    try {
                        fos = getActivity().openFileOutput(FILE_NAME, Context.MODE_APPEND);
                        fos.write(time.getBytes());
                        fos.write(e.getBytes());
                        showToast("Saved to records.");
//                        showToast("Time used" + difference + "s" + heartbeat);
                        fos.close();
                        plot.removeSeries(ecgSeries);
                        heartbeat = 0;
                        flag = 0;
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                else showToast("There is nothing to save.");
            }

        });
    }
    private void showToast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
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
                            data[latestIndex] = value;
                            if((value > 3) && (flag == 0)) {
                                heartbeat++;
                                flag = 1;
                            }
                            else if((value < 3) && (flag == 1)){
                                flag = 0;
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
        if(event.receiver.equals(MessageEvent.File.FRAGMENT_ECG)) {
            switch(event.action) {
                case SCAN:
                    // Do nothing
                    break;
                case SAMPLE_RATE:
                    // Do nothing
                    break;
                case ADC:
                    String data = event.data;
                    value = Double.parseDouble(data);
                    break;
                default:
                    // Unhandled action
            }
        }
    }
}
