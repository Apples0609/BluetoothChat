package com.apples.myapp.blt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.apples.myapp.R;

public class ChatActivity extends Activity implements OnItemClickListener, OnItemLongClickListener {
    /**
     * Called when the activity is first created.
     */
    private ListView mListView;
    private ArrayList<ListItemEntity> list;
    private Button sendButton;
    private Button disconnectButton;
    private EditText editMsgView;
    private DCListViewAdapter mAdapter;
    private Context mContext;

    /* 常量代表服务器的名称 */
    private final String PROTOCOL_SCHEME_RFCOMM = "btspp";

    private BluetoothServerSocket mserverSocket = null;
    private ServerThread startServerThread = null;
    private ClientThread clientConnectThread = null;
    private BluetoothSocket socket = null;
    private BluetoothDevice device = null;
    private ReadThread mreadThread = null;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private final String TAG = "======";
    private final String bUUID = "00001101-0000-1000-8000-00805F9B34FB";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);
        mContext = this;
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        init();
    }

    private void init() {
        list = new ArrayList<ListItemEntity>();
        mAdapter = new DCListViewAdapter(this, list);
        mListView = (ListView) findViewById(R.id.list);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);
        mListView.setFastScrollEnabled(true);
        editMsgView = (EditText) findViewById(R.id.MessageText);
        editMsgView.clearFocus();

        sendButton = (Button) findViewById(R.id.btn_msg_send);
        sendButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                String msgText = editMsgView.getText().toString().trim();
                if (!TextUtils.isEmpty(msgText)) {
                    sendMessageHandle(msgText);
                    editMsgView.setText("");
                    editMsgView.clearFocus();
                    //close InputMethodManager
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(editMsgView.getWindowToken(), 0);
                } else {
                    Toast.makeText(mContext, "发送内容不能为空！", Toast.LENGTH_SHORT).show();
//                    Message msg2 = new Message();
//                    msg2.obj = "请稍候，正在连接服务器:" + Bluetooth.BlueToothAddress;
//                    msg2.what = 0;
//                    LinkDetectedHandler.sendMessage(msg2);
                }
            }
        });

        disconnectButton = (Button) findViewById(R.id.btn_disconnect);
        disconnectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (Bluetooth.serviceOrCilent == Bluetooth.ServerOrCilent.CLIENT) {
                    shutdownClient();
                } else if (Bluetooth.serviceOrCilent == Bluetooth.ServerOrCilent.SERVICE) {
                    shutdownServer();
                }
                Bluetooth.isOpen = false;
                Bluetooth.serviceOrCilent = Bluetooth.ServerOrCilent.NONE;
                Toast.makeText(mContext, "已断开连接！", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("HandlerLeak")
    private Handler LinkDetectedHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //Toast.makeText(mContext, (String)msg.obj, Toast.LENGTH_SHORT).show();
            if (msg.what == 1) {
                list.add(new ListItemEntity((String) msg.obj, true));
            } else {
                list.add(new ListItemEntity((String) msg.obj, false));
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(ListView.FOCUS_DOWN);//刷新到底部
            if (":炸弹".equals(list.get(list.size() - 1).message)) {
                int fb = 0;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {// API 26 and above
                    mVibrator.vibrate(VibrationEffect.createWaveform(VIBRATE_PATTERN, fb));
                } else {// Below API 26
                    mVibrator.vibrate(VIBRATE_PATTERN, fb);
                }
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 100, 0);//音量最大
                mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.m2);
                mediaPlayer.start();
                Intent startMain = new Intent(Intent.ACTION_MAIN);//回到桌面
                startMain.addCategory(Intent.CATEGORY_HOME);
                startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startMain);
            }
        }
    };

    private final long[] VIBRATE_PATTERN = {100, 800, 900};
    private Vibrator mVibrator;
    private AudioManager audioManager;
    private MediaPlayer mediaPlayer;


    @Override
    public synchronized void onPause() {
        super.onPause();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (Bluetooth.isOpen) {
            Toast.makeText(mContext, "连接已经打开，可以通信。\n如果要再建立连接，请先断开！", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Bluetooth.serviceOrCilent == Bluetooth.ServerOrCilent.CLIENT) {
            String address = Bluetooth.BlueToothAddress;
            if (!"null".equals(address)) {
                device = mBluetoothAdapter.getRemoteDevice(address);
                clientConnectThread = new ClientThread();
                clientConnectThread.start();
                Bluetooth.isOpen = true;
            } else {
                Toast.makeText(mContext, "蓝牙连接地址是空的", Toast.LENGTH_LONG).show();
            }
        } else if (Bluetooth.serviceOrCilent == Bluetooth.ServerOrCilent.SERVICE) {
            startServerThread = new ServerThread();
            startServerThread.start();
            Bluetooth.isOpen = true;
        }
    }

    /**
     * 开启客户端
     */
    private class ClientThread extends Thread {
        public void run() {
            try {
                // 创建一个Socket连接：只需要服务器在注册时的UUID号
                // socket = device.createRfcommSocketToServiceRecord(BluetoothProtocols.OBEX_OBJECT_PUSH_PROTOCOL_UUID);
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString(bUUID));
                //连接
                Message msg2 = new Message();
                msg2.obj = "请稍候，正在连接服务器:" + Bluetooth.BlueToothAddress;
                msg2.what = 0;
                LinkDetectedHandler.sendMessage(msg2);
                socket.connect();

                Message msg = new Message();
                msg.obj = "已经连接上服务端！可以发送信息。";
                msg.what = 0;
                LinkDetectedHandler.sendMessage(msg);
                //启动接受数据
                mreadThread = new ReadThread();
                mreadThread.start();
            } catch (IOException e) {
                Log.e(TAG, "", e);
                Message msg = new Message();
                msg.obj = "连接服务端异常！断开连接重新试一试。";
                msg.what = 0;
                LinkDetectedHandler.sendMessage(msg);
                Bluetooth.isOpen = false;
            }
        }
    }

    ;

    /**
     * 开启服务器
     */
    private class ServerThread extends Thread {
        public void run() {
            try {
                /* 创建一个蓝牙服务器
                 * 参数分别：服务器名称、UUID	 */
                mserverSocket = mBluetoothAdapter
                        .listenUsingRfcommWithServiceRecord(PROTOCOL_SCHEME_RFCOMM, UUID.fromString(bUUID));

                Log.d(TAG, "wait cilent connect...");
                Message msg = new Message();
                msg.obj = "请稍候，正在等待客户端的连接...";
                msg.what = 0;
                LinkDetectedHandler.sendMessage(msg);

                /* 接受客户端的连接请求 */
                socket = mserverSocket.accept();
                Log.d("server", "accept success !");

                Message msg2 = new Message();
                String info = "客户端已经连接上！可以发送信息。";
                msg2.obj = info;
                msg.what = 0;
                LinkDetectedHandler.sendMessage(msg2);

                //启动接受数据
                mreadThread = new ReadThread();
                mreadThread.start();
            } catch (IOException e) {
                e.printStackTrace();
                Bluetooth.isOpen = false;
            }
        }
    }

    ;

    /**
     * 停止服务器
     */
    private void shutdownServer() {
        new Thread() {
            public void run() {
                if (startServerThread != null) {
                    startServerThread.interrupt();
                    startServerThread = null;
                }
                if (mreadThread != null) {
                    mreadThread.interrupt();
                    mreadThread = null;
                }
                try {
                    if (socket != null) {
                        socket.close();
                        socket = null;
                    }
                    if (mserverSocket != null) {
                        mserverSocket.close();/* 关闭服务器 */
                        mserverSocket = null;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "mserverSocket.close()", e);
                }
            }

            ;
        }.start();
    }

    /**
     * 停止客户端连接
     */
    private void shutdownClient() {
        new Thread() {
            public void run() {
                if (clientConnectThread != null) {
                    clientConnectThread.interrupt();
                    clientConnectThread = null;
                }
                if (mreadThread != null) {
                    mreadThread.interrupt();
                    mreadThread = null;
                }
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    socket = null;
                }
            }

            ;
        }.start();
    }

    /**
     * 发送数据
     *
     * @param msg
     */
    private void sendMessageHandle(String msg) {
        if (socket == null) {
            Toast.makeText(mContext, "没有连接", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            OutputStream os = socket.getOutputStream();
            os.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        list.add(new ListItemEntity(msg, false));
        mAdapter.notifyDataSetChanged();
        mListView.setSelection(ListView.FOCUS_DOWN);//刷新到底部
    }

    /**
     * 读取数据
     */
    private class ReadThread extends Thread {
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            InputStream mmInStream = null;
            try {
                mmInStream = socket.getInputStream();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            while (true) {
                try {
                    // Read from the InputStream
                    if ((bytes = mmInStream.read(buffer)) > 0) {
                        byte[] buf_data = new byte[bytes];
                        for (int i = 0; i < bytes; i++) {
                            buf_data[i] = buffer[i];
                        }
                        String s = new String(buf_data);
                        Message msg = new Message();
                        msg.obj = s;
                        msg.what = 1;
                        LinkDetectedHandler.sendMessage(msg);
                    }
                } catch (IOException e) {
                    try {
                        mmInStream.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Bluetooth.serviceOrCilent == Bluetooth.ServerOrCilent.CLIENT) {
            shutdownClient();
        } else if (Bluetooth.serviceOrCilent == Bluetooth.ServerOrCilent.SERVICE) {
            shutdownServer();
        }
        Bluetooth.isOpen = false;
        Bluetooth.serviceOrCilent = Bluetooth.ServerOrCilent.NONE;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
		/*new AlertDialog.Builder(this)
		.setMessage("删除吗？")
		.setPositiveButton("删除", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				list.remove(position);
				mAdapter.notifyDataSetChanged();
			}
		})
		.setNegativeButton("取消", null)
		.create().show();*/
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
        new AlertDialog.Builder(this)
                .setMessage("删除吗？")
                .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        list.remove(position);
                        mAdapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton("取消", null)
                .create().show();
        return true;
    }
}