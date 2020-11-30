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
    final static String ACTION_SCAN_DEVICE = "com.example.heartwave.SCAN_DEVICE";
    final static String ACTION_SEND_DATA = "com.example.heartwave.SEND_DATA";
    final static String ACTION_SAMPLE_RATE = "com.example.heartwave.SAMPLE_RATE";
    final static String EXTRA_DEVICE_BLE = "com.example.heartwave.KEY";
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

    /*  Rename - current function name conflicts with another */
    private void sendBroadcast(String msg, String type) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DEVICE_BLE, msg);

        switch(type) {
            case ACTION_SCAN_DEVICE:
                intent.setAction(ACTION_SCAN_DEVICE);
                break;
            case ACTION_SEND_DATA:
                intent.setAction(ACTION_SEND_DATA);
                break;
            case ACTION_SAMPLE_RATE:
                intent.setAction(ACTION_SAMPLE_RATE);
            default:
                break;
        }

        sendBroadcast(intent);
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

            sendBroadcast(result.getDevice().getName() + "\n" +
                    result.getDevice().getAddress() + "\n" + result.getRssi() + " dBm", ACTION_SCAN_DEVICE);
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
            UUID SERVICE_UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB");
            UUID CHARACTERISTIC_UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB");
            UUID DESCRIPTOR_UUID = UUID.fromString("00002901-0000-1000-8000-00805F9B34FB");

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
                sendBroadcast(msg, ACTION_SEND_DATA);
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
                sendBroadcast(String.valueOf(rate), ACTION_SAMPLE_RATE);
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
        Log.d("Tag: ", event.message);
    }
}
