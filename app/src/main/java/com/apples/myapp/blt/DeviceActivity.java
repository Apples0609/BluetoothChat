package com.apples.myapp.blt;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.apples.myapp.R;

public class DeviceActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    private ListView mListView;
    private ArrayList<ListItemEntity> list;
    private Button seachButton, serviceButton;
    private DCListViewAdapter mAdapter;
    private Context mContext;

    /* 取得默认的蓝牙适配器 */
    private BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        if (!mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 3);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.devices);
        mContext = this;
        init();
    }

    private void init() {
        list = new ArrayList<ListItemEntity>();
        mAdapter = new DCListViewAdapter(this, list);
        mListView = (ListView) findViewById(R.id.list);
        mListView.setAdapter(mAdapter);
        mListView.setFastScrollEnabled(true);
        mListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts when a device is discovered
        IntentFilter discoveryFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, discoveryFilter);

        // Register for broadcasts when discovery has finished
        IntentFilter foundFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, foundFilter);

        obtainAllPairBluetooth();

        seachButton = (Button) findViewById(R.id.start_seach);
        seachButton.setOnClickListener(seachButtonClickListener);

        serviceButton = (Button) findViewById(R.id.start_service);
        serviceButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Bluetooth.serviceOrCilent = Bluetooth.ServerOrCilent.SERVICE;
                Bluetooth.mTabHost.setCurrentTab(1);
            }
        });
    }

    /**
     * 获取所有已经配对的蓝牙设备
     */
    private void obtainAllPairBluetooth() {
        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                list.add(new ListItemEntity("名称：" + device.getName() + "\n地址：" + device.getAddress(), true));
                mAdapter.notifyDataSetChanged();
            }
        } else {
            list.add(new ListItemEntity("没有设备已经配对", true));
            mAdapter.notifyDataSetChanged();
        }
    }

    private OnClickListener seachButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View arg0) {
            if (mBtAdapter.isDiscovering()) {
                mBtAdapter.cancelDiscovery();
                seachButton.setText("重新搜索");
            } else {
                list.clear();
                mAdapter.notifyDataSetChanged();
                obtainAllPairBluetooth();
                /* 开始搜索 */
                mBtAdapter.startDiscovery();
                seachButton.setText("停止搜索");
            }
        }
    };

    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // Cancel discovery because it's costly and we're about to connect
            ListItemEntity item = list.get(position);
            String info = item.message;
            String address = info.substring(info.length() - 17);
            Bluetooth.BlueToothAddress = address;
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);// 定义一个弹出框对象
            dialogBuilder.setTitle("连接");// 标题
            dialogBuilder.setMessage(item.message);
            dialogBuilder.setPositiveButton("连接", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mBtAdapter.cancelDiscovery();
                    seachButton.setText("重新搜索");
                    Bluetooth.serviceOrCilent = Bluetooth.ServerOrCilent.CLIENT;
                    Bluetooth.mTabHost.setCurrentTab(1);
                }
            });
            dialogBuilder.setNegativeButton("取消",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Bluetooth.BlueToothAddress = null;
                        }
                    });
            dialogBuilder.show();
        }
    };

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed
                // already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    String address = device.getAddress();
                    String name = device.getName();
                    if (!TextUtils.isEmpty(name)) {
                        boolean ic = false;
                        for (ListItemEntity listItemEntity : list) {
                            if (ic = listItemEntity.message.contains(name) && listItemEntity.message.contains(address)) {
                                break;
                            }
                        }
                        if (!ic) {
                            list.add(new ListItemEntity("名称：" + name + "\n地址：" + address, false));
                            mAdapter.notifyDataSetChanged();

                        }
                    }
                    System.out.println("======" + name + "，" + address);
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (mListView.getCount() == 0) {
                    list.add(new ListItemEntity("没有发现蓝牙设备", false));
                    mAdapter.notifyDataSetChanged();
                }
                seachButton.setText("重新搜索");
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

}