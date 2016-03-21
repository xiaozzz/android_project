package com.example.demo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.Process;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.util.SparseArray;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@TargetApi(21)
public class MyService extends Service {
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 2000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    private HandlerThread thread;
    private boolean running;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.
            mHandler = new Handler();
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
//                    Toast.makeText(this, "BLE Not Supported", Toast.LENGTH_SHORT).show();
                showNotification("BLE Not Supported");
                stopSelf();
            }
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();

            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                        .build();
                filters = new ArrayList<ScanFilter>();
            }
            scanLeDevice(true);

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
//            stopSelf(msg.arg1);
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());

            if (result.getDevice().getName() == null || !result.getDevice().getName().equals("CLARITY"))
                return;

//            tv.setText(appendText(tv.getText().toString(), "callbackType", String.valueOf(callbackType)));
////            tv.setText(appendText(tv.getText().toString(), "result", result.toString()));
//            tv.setText(appendText(tv.getText().toString(), "Device", result.getDevice().toString()));

            SparseArray<byte[]> tmp = result.getScanRecord().getManufacturerSpecificData();
//            tv.setText(appendText(tv.getText().toString(), "ManufacturerSpecificData", dataToString(tmp)));

            Log.i("Device", result.getDevice().toString());
            Log.i("MSD", dataToString(tmp));
            String str = "";
            Map<ParcelUuid, byte[]>  map = result.getScanRecord().getServiceData();
            for (Map.Entry<ParcelUuid, byte[]> entry : map.entrySet()) {
                str += entry.getKey();
                str += "=";
                str += bytesToHex(entry.getValue());
            }

//            showNotification(dataToString(tmp), str);
            String[] strarr = parseData(dataToString(tmp));
            if (strarr == null) {
                strarr = parseData(str);
            }
            if (strarr == null)
                showNotification(result.getDevice().toString(), "No data");
            else{
                showNotification(result.getDevice().toString(), strarr[0] + "-" + strarr[1] + "/" + strarr[2]);
            }

            MainActivity.appendTV(result.toString());

            BluetoothDevice btDevice = result.getDevice();
            connectToDevice(btDevice);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
//                showNotification("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
//            showNotification("Scan Failed", "Error Code: " + errorCode);
        }
    };

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            scanLeDevice(false);// will stop after first device detection
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
//            tv.setText(appendText(tv.getText().toString(), "onConnectionStateChange", "Status: " + status));
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
//                    Log.i("gattCallback", "STATE_CONNECTED");
//                    tv.setText(appendText(tv.getText().toString(), "gattCallback", "STATE_CONNECTED"));
//                    showNotification("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
//                    Log.e("gattCallback", "STATE_DISCONNECTED");
//                    tv.setText(appendText(tv.getText().toString(), "gattCallback", "STATE_DISCONNECTED"));
//                    showNotification("gattCallback", "STATE_DISCONNECTED");

                    break;
                default:
//                    Log.e("gattCallback", "STATE_OTHER");
//                    tv.setText(appendText(tv.getText().toString(), "gattCallback", "STATE_OTHER"));
//                    showNotification("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
//            Log.i("onServicesDiscovered", services.toString());
//            tv.setText(appendText(tv.getText().toString(), "onServicesDiscovered", services.toString()));
//            showNotification("onServicesDiscovered", services.toString());
            gatt.readCharacteristic(services.get(1).getCharacteristics().get
                    (0));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
//            Log.i("onCharacteristicRead", characteristic.toString());
//            tv.setText(appendText(tv.getText().toString(), "onCharacteristicRead", characteristic.toString()));
//            showNotification("onCharacteristicRead", characteristic.toString());
            gatt.disconnect();
        }
    };

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 21) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    } else {
                        mLEScanner.stopScan(mScanCallback);
                    }
                    if (running)
                        scanLeDevice(true);
                }
            }, SCAN_PERIOD);
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                mLEScanner.startScan(filters, settings, mScanCallback);
            }
        } else {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("onLeScan", device.toString());
                            connectToDevice(device);
                        }
                    });
                }
            };

    public MyService() {
    }

    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        running = true;
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification noti = new Notification.Builder(this)
                .setContentTitle("Demo service")
                .setContentText("Running...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(0xff, noti);
        return Service.START_STICKY;

//        // If we get killed, after returning from here, restart
//        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        running = false;

        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
        if (mGatt == null) {
            return;
        }
        mGatt.close();
        mGatt = null;
        super.onDestroy();
    }

    private PendingIntent getDefaultIntent(int flags){
        PendingIntent pendingIntent= PendingIntent.getActivity(this, 1, new Intent(), flags);
        return pendingIntent;
    }

    private void showNotification(String str) {
        showNotification("Demo", str);
    }

    private void showNotification(String title, String text){
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle(title)//设置通知栏标题
                .setContentText(text) //设置通知栏显示内容
        .setContentIntent(getDefaultIntent(Notification.FLAG_AUTO_CANCEL)) //设置通知栏点击意图
//  .setNumber(number) //设置通知集合的数量
                .setTicker("收到蓝牙广播") //通知首次出现在通知栏，带上升动画效果的
                .setWhen(System.currentTimeMillis())//通知产生的时间，会在通知信息里显示，一般是系统获取到的时间
                .setPriority(Notification.PRIORITY_DEFAULT) //设置该通知优先级
//  .setAutoCancel(true)//设置这个标志当用户单击面板就可以让通知将自动取消
                .setOngoing(false)//ture，设置他为一个正在进行的通知。他们通常是用来表示一个后台任务,用户积极参与(如播放音乐)或以某种方式正在等待,因此占用设备(如一个文件下载,同步操作,主动网络连接)
//                .setDefaults(Notification.DEFAULT_VIBRATE)//向通知添加声音、闪灯和振动效果的最简单、最一致的方式是使用当前的用户默认设置，使用defaults属性，可以组合
                        //Notification.DEFAULT_ALL  Notification.DEFAULT_SOUND 添加声音 // requires VIBRATE permission
                .setSmallIcon(R.mipmap.ic_launcher);//设置通知小ICON
        notificationManager.notify(1, mBuilder.build());
    }


    private static String bytesToHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for (final byte b: in)
            builder.append(String.format("%02x", b));
        return builder.toString();
    }

    private String dataToString(SparseArray<byte[]> tmp) {
        if (tmp.size() <= 0) {
            return "{}";
        }

        StringBuilder buffer = new StringBuilder(tmp.size() * 28);
        buffer.append('{');
        for (int i=0; i<tmp.size(); i++) {
            if (i > 0) {
                buffer.append(", ");
            }
            int key = tmp.keyAt(i);
            buffer.append(key);
            buffer.append('=');
            Object value = tmp.valueAt(i);
            if (value != this) {
                buffer.append(bytesToHex((byte[])value));
            } else {
                buffer.append("(this Map)");
            }
            if (i > 0) {
                buffer.append(", ");
            }
        }
        buffer.append('}');
        return buffer.toString();
    }

    private String[] parseData(String str){
        String a[] = str.split("=");
        if (a[1] == null)
            return null;
        str = a[1];
        str = str.substring(0, 12);
        str = str.toUpperCase();

        String str1 = str.substring(0, 4);
        String str2 = str.substring(4, 8);
        String str3 = str.substring(8, 12);

        str1 = str1.substring(2, 4) + str1.substring(0, 2);
        str2 = str2.substring(2, 4) + str2.substring(0, 2);
        str3 = str3.substring(2, 4) + str3.substring(0, 2);

        String[] ret = new String[3];
        ret[0] = str1;
        ret[1] = Integer.valueOf(str2,16).toString();
        ret[2] = Integer.valueOf(str3,16).toString();
        return ret;
    }
}
