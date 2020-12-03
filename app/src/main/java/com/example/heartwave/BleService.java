package com.example.heartwave;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.nio.ByteBuffer;
import java.util.UUID;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class BleService extends Service {
    private final IBinder binder = new LocalBinder();
    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    BluetoothLeScanner bluetoothLeScanner;
    BluetoothGatt bluetoothGatt;

    byte lowByte, highByte;
    int levelADC;
    double voltageADC;

    long counter = 0;
    Long startTimestamp;
    Long currentTimestamp;
    Long difference;

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        bleInit();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    private void bleInit() {
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        if(bluetoothAdapter != null) {
            bluetoothAdapter.enable();
        }
    }

    public BleService() {
    }

    public class LocalBinder extends Binder {
        BleService getService() {
            return BleService.this;
        }
    }

    public void sendMessage(int id, String address){
        switch(id) {
            case R.id.button_scan:
                scanBleDevices();
                break;
            case R.id.button_connect:
                connectToGatt(address);
                break;
            case R.id.button_disconnect:
                Log.d("tag", String.valueOf(id));
                break;
            default:
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void scanBleDevices() {
        Handler handler = new Handler();
        final long SCAN_PERIOD = 15000;
        bluetoothLeScanner.stopScan(leScanCallback);
        bluetoothLeScanner.stopScan(leScanCallback);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothLeScanner.stopScan(leScanCallback);
            }
        }, SCAN_PERIOD);

        bluetoothLeScanner.startScan(leScanCallback);
    }

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            String msg = result.getDevice().getName() + "\n" + result.getDevice().getAddress() + "\n"
                    + result.getRssi() + " dBm";
            EventBus.getDefault().post(new MessageEvent(msg, MessageEvent.File.SERVICE,
                    MessageEvent.File.FRAGMENT_SCAN, MessageEvent.Action.SCAN));
        }
    };

    private void connectToGatt(String address) {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        bluetoothLeScanner.stopScan(leScanCallback);
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if(newState == BluetoothProfile.STATE_CONNECTED) {
                if(status == BluetoothGatt.GATT_SUCCESS) {
                        gatt.discoverServices();
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            UUID SERVICE_UUID = UUID.randomUUID();
            UUID CHARACTERISTIC_UUID = UUID.randomUUID();
            UUID DESCRIPTOR_UUID = UUID.randomUUID();

            if(bluetoothGatt.getDevice().getAddress().equals("04:91:62:9F:94:83")) {    // RN4870
                SERVICE_UUID = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455");
                CHARACTERISTIC_UUID = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");
                DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");
            }
            else if(bluetoothGatt.getDevice().getAddress().equals("A4:DA:32:52:20:6E")) { // HM-19
                SERVICE_UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB");
                CHARACTERISTIC_UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB");
                DESCRIPTOR_UUID = UUID.fromString("00002901-0000-1000-8000-00805F9B34FB");
            }

            if(status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
                gatt.setCharacteristicNotification(characteristic, true);
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(DESCRIPTOR_UUID);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            byte[] buffer = characteristic.getValue();

            String msg = new String(buffer);
            msg = msg.replaceAll("\\r\\n", "");
            if(isNumeric(msg)) {
                EventBus.getDefault().post(new MessageEvent(msg, MessageEvent.File.SERVICE,
                        MessageEvent.File.FRAGMENT_ECG, MessageEvent.Action.ADC));
            }

            if(counter == 0) {
                startTimestamp = System.currentTimeMillis() / 1000;
            }
            else {
                currentTimestamp = System.currentTimeMillis() / 1000;
                difference = currentTimestamp - startTimestamp;
            }

            counter++;
            if(counter % 1000 == 0) {
                double rate = ((double) counter) / ((double) difference);
                String dataRate = String.valueOf(rate);
                EventBus.getDefault().post(new MessageEvent(dataRate, MessageEvent.File.SERVICE,
                        MessageEvent.File.FRAGMENT_STATS, MessageEvent.Action.SAMPLE_RATE));
            }
        }
    };

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }

    @Subscribe
    public void onMessageEvent(MessageEvent event) {
        Log.d("Tag: ", event.data);
    }
}
