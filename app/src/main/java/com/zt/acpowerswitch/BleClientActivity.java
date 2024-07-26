package com.zt.acpowerswitch;

import static android.widget.Toast.LENGTH_SHORT;
import static com.zt.acpowerswitch.MainActivity.goAnim;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** @noinspection deprecation*/
public class BleClientActivity extends AppCompatActivity {
    public static String TAG = "BleClientActivity";
    public static String chara;
    public static List<BluetoothDevice> mlist = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private BlueDeviceItemAdapter mRecycler;
    private Button re_scan;
    private ProgressDialog pd;
    public static int item_locale;
    public static boolean connect_ok;
    public static BluetoothGatt bluetoothGatt;
    public BluetoothManager bluetoothManager;
    public BluetoothAdapter bluetoothAdapter;
    public BluetoothDevice bluetoothDeviceName;
    public static BluetoothGattCharacteristic writeCharacteristic;
    public boolean discoveryFinished,BLE_ON;
    private static ProgressDialog pd1;
    public ComponentName topActivity;
    @SuppressLint("MissingPermission")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_scan);
        pd1 = new ProgressDialog(this);
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "本设备没有蓝牙功能", Toast.LENGTH_SHORT).show();
            finish();
        }
        if (!bluetoothAdapter.isEnabled()) {
            // 蓝牙未开启，弹出对话框请求用户开启蓝牙
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 11);
        }else{
            BLE_ON=true;
        }
        init_setting();
    }
    public void init_setting(){
        if (BLE_ON){
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                // 不支持BLE功能
                Toast.makeText(this, "该设备不支持低功耗蓝牙功能!", Toast.LENGTH_SHORT).show();
            }
            re_scan = findViewById(R.id.re_scan);
            re_scan.setOnClickListener(v -> {
                goAnim(this,50);
                connect_ok=false;
                searchBluetooth();
            });
            //设置过滤器，过滤因远程蓝牙设备被找到而发送的广播 BluetoothDevice.ACTION_FOUND
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(BluetoothDevice.ACTION_FOUND);
            iFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            iFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            iFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            registerReceiver(foundReceiver, iFilter);
            //设置广播接收器和安装过滤器
            displayList();//刷新列表
            searchBluetooth();
        }
    }
    /**
     * 当找到一个远程蓝牙设备时执行的广播接收者
     *
     */
    public final BroadcastReceiver foundReceiver = new BroadcastReceiver() {
        @SuppressLint({"MissingPermission", "SetTextI18n", "UnsafeIntentLaunch"})
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);//获取此时找到的远程设备对象
            if (device != null && device.getName() != null) {
                Log.e(TAG, "发现蓝牙设备:" + device.getName() + "\n" + device.getAddress());
                if (!mlist.contains(device)) {
                    mlist.add(device);
                }
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
                Log.e(TAG, "扫描完成");
                discoveryFinished = true;
            }
        }
    };
    private void displayList() {
        mRecyclerView = findViewById(R.id.rv_device_list);//设置固定大小
        mRecyclerView.setHasFixedSize(true);//创建线性布局
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        mRecyclerView.addItemDecoration(new LinearSpacingItemDecoration(8));//添加间距
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL)); //添加分隔线
        mRecyclerView.setLayoutManager(layoutManager);
        mRecycler = new BlueDeviceItemAdapter(mlist, this);
        mRecyclerView.setAdapter(mRecycler);
        mRecycler.setRecyclerItemClickListener(position -> {
            goAnim(this,50);
            item_locale = position;
            BleClientActivity.this.showPopupMenu(mRecyclerView.getChildAt(position));
        });
    }

    @SuppressLint({"ObsoleteSdkInt", "MissingPermission"})
    public void searchBluetooth() {
        if (!connect_ok) {
            if (mlist != null) {
                mlist.clear();
            }
            displayList();//刷新列表
            bluetoothAdapter.startDiscovery();
            Log.e(TAG, "defaultDevice: 开始搜索设备");
            re_scan.setText("正在扫描");
            pd = new ProgressDialog(this);
            pd.setMessage("正在扫描,请稍等......");
            pd.show();
            pd.setCancelable(false);
            new Thread(() -> {
                while (!discoveryFinished) {
                    try {
                        Thread.sleep(500);
                        Message message = new Message();
                        message.what = 1;
                        myHandler.sendMessage(message);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                Message message = new Message();
                message.what = 2;
                myHandler.sendMessage(message);
            }).start();
        }
    }
    @SuppressLint("HandlerLeak")
    Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                mRecycler = new BlueDeviceItemAdapter(mlist, BleClientActivity.this);
                mRecyclerView.setAdapter(mRecycler);
            }
            if (msg.what == 2) {
                stopDiscovery();
                discoveryFinished=false;
                connect_ok=false;
            }
        }
    };
    @SuppressLint("MissingPermission")
    private void stopDiscovery() {
        displayList();
        pd.dismiss();
        re_scan.setTextSize(16);
        re_scan.setText("重新扫描");
    }
    @SuppressLint({"MissingPermission", "ObsoleteSdkInt"})
    private void showPopupMenu(final View view) {
        final PopupMenu popupMenu = new PopupMenu(this, view, Gravity.END);
        //menu 布局
        popupMenu.getMenuInflater().inflate(R.menu.connectmenu, popupMenu.getMenu());
        //点击事件
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.connect_item) {//连接蓝牙
                goAnim(this,50);
                if (connect_ok) {
                    Intent intent = new Intent(BleClientActivity.this, WifiListActivity.class);
                    startActivities(new Intent[]{intent});
                }else {
                    pd1.setMessage("正在连接蓝牙,请稍等");
                    pd1.show();
                    pd1.setCancelable(false);
                    bluetoothDeviceName = mlist.get(item_locale);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                        bluetoothGatt = mlist.get(item_locale).connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
                    } else {
                        bluetoothGatt = mlist.get(item_locale).connectGatt(this, false, gattCallback);
                    }
                }
            }
            if (itemId == R.id.disconnect_item){
                if (bluetoothGatt != null) {
                    connect_ok = false;
                    bluetoothGatt.close();
                    bluetoothGatt.disconnect();
                    bluetoothGatt = null;
                }
            }
            return false;
        });
        popupMenu.show();//显示菜单
    }
    String SERVICE_UUID="6e400001-b5a3-f393-e0a9-e50e24dcca9e";//这个是我要链接的蓝牙设备的ServiceUUID
    String READ_UUID="6e400003-b5a3-f393-e0a9-e50e24dcca9e";//这个是我要链接的蓝牙设备的消息读UUID值
    String WRITE_UUID="6e400002-b5a3-f393-e0a9-e50e24dcca9e";//这个是我要链接的蓝牙设备的消息写UUID值
    BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint({"MissingPermission", "ObsoleteSdkInt"})
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.e(TAG, "连接成功");
                gatt.discoverServices();
                connect_ok = true;
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e(TAG, "连接断开");
                connect_ok = false;
                if (bluetoothDeviceName!=null){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                        bluetoothAdapter.cancelDiscovery();
                        bluetoothGatt = mlist.get(item_locale).connectGatt(BleClientActivity.this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
                    } else {
                        bluetoothAdapter.cancelDiscovery();
                        bluetoothGatt = mlist.get(item_locale).connectGatt(BleClientActivity.this, false, gattCallback);
                    }
                }
            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                //TODO 在实际过程中，该方法并没有调用
                Log.e(TAG, "连接中....");
            }
        }
        //获取GATT服务发现后的回调
        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "GATT_SUCCESS"); //服务发现
                for (BluetoothGattService bluetoothGattService : gatt.getServices()) {
                    Log.e(TAG, "Service_UUID:" + bluetoothGattService.getUuid()); // 我们可以遍历到该蓝牙设备的全部Service对象。然后通过比较Service的UUID，我们可以区分该服务是属于什么业务的
                    if (SERVICE_UUID.equals(bluetoothGattService.getUuid().toString())) {
                        for (BluetoothGattCharacteristic characteristic : bluetoothGattService.getCharacteristics()) {
                            prepareBroadcastDataNotify(gatt, characteristic); //给满足条件的属性配置上消息通知
                            writeCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(WRITE_UUID));
                            if (writeCharacteristic != null && writeCharacteristic.getProperties() == BluetoothGattCharacteristic.PROPERTY_WRITE) {
                                Log.e(TAG, "找到write特征,可以写入");
                                pd1.dismiss();
                                ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                                List<ActivityManager.RunningTaskInfo> runningTasks = activityManager.getRunningTasks(1);

                                if (runningTasks != null && !runningTasks.isEmpty()) {
                                    topActivity = runningTasks.get(0).topActivity;
                                    String packageName = Objects.requireNonNull(topActivity).getPackageName();
                                    String className = topActivity.getClassName();

                                    Log.e(TAG,"Top Activity:"+topActivity+",Package Name:" + packageName + ",Class Name: " + className);
                                }
                                if(!topActivity.toString().equals("ComponentInfo{com.zt.acpowerswitch/com.zt.acpowerswitch.WifiListActivity}")) {
                                    Intent intent = new Intent(BleClientActivity.this, WifiListActivity.class);
                                    startActivities(new Intent[]{intent});
                                }
                            }
                        }
                        return;//结束循环操作
                    }
                }
            }else {
                Log.e(TAG, "onServicesDiscovered received: " + status);
            }
        }
        @SuppressLint("MissingPermission")
        private void prepareBroadcastDataNotify(BluetoothGatt mBluetoothGatt, BluetoothGattCharacteristic characteristic) {
            Log.e(TAG, "Characteristic_UUID:" + characteristic.getUuid().toString());
            int charaProp = characteristic.getProperties();
            //判断属性是否支持消息通知
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString( "00002902-0000-1000-8000-00805f9b34fb"));
                if (descriptor != null) {
                    //注册消息通知
                    mBluetoothGatt.setCharacteristicNotification(characteristic, true);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothGatt.writeDescriptor(descriptor);
                    Log.e(TAG, "注册消息通知完成");
                }
            }
        }
        //蓝牙设备发送消息后的自动监听
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // readUUID 是我要链接的蓝牙设备的消息读UUID值，跟通知的特性的UUID比较。这样可以避免其他消息的污染。
            if (READ_UUID.equals(characteristic.getUuid().toString())) {
                chara = new String(characteristic.getValue(), StandardCharsets.UTF_8);
                Log.e(TAG, "消息内容:"+chara);
            }
        }
    };
    @SuppressLint("MissingPermission")
    public static void write_data_ble(String data){
        // 设置要写入的数据
        writeCharacteristic.setValue(data);
        // 将数据写入设备
        bluetoothGatt.writeCharacteristic(writeCharacteristic);
        Log.e(TAG, "写入:"+data);
    }
    @SuppressLint("MissingPermission")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 11) {
            if (resultCode == RESULT_OK) {
                // 用户开启了蓝牙
                BLE_ON=true;
                init_setting();
            } else if (resultCode == RESULT_CANCELED) {
                // 用户取消了开启蓝牙的请求
                // TODO: 处理用户取消开启蓝牙的情况
                Toast.makeText(this, "请开启蓝牙，否则无法使用.", LENGTH_SHORT).show();
            }
        }
    }

    protected void onDestroy() {
        if (foundReceiver != null) unregisterReceiver(foundReceiver); //停止监听
        bluetoothDeviceName = null;
        super.onDestroy();
    }
    @SuppressLint("MissingPermission")
    protected void onResume() {
        super.onResume();
    }
}
