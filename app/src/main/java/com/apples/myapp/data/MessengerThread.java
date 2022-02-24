package com.apples.myapp.data;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

/**
 * MessengerThread.java
 * @desc
 * @author wankaifang
 * @date 2021/9/4 13:34
 */
public class MessengerThread extends Thread {
    private int iok;

    @SuppressLint("HandlerLeak")
    private Handler inComingHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            if (iok == 6) return;

            Log.d("MessengerThread", msg.obj.toString());

            try {
                Message reMsg = Message.obtain();
                reMsg.replyTo = MessengerThread.this.getMessenger();
                reMsg.obj = "reply:hello ,am " + getName();
                msg.replyTo.send(reMsg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            iok++;
        }
    };
    private Messenger messenger;
    private MessengerThread another;

    public MessengerThread(String name) {
        super(name);
        messenger = new Messenger(inComingHandler);

    }

    public void setAnother(MessengerThread another) {
        this.another = another;
    }

    public Messenger getMessenger() {
        return messenger;
    }

    @Override
    public void run() {
        if (another != null) {
            Message msg = Message.obtain();
            msg.replyTo = another.getMessenger();
            msg.obj = "hello i am " + getName();
            try {
                another.getMessenger().send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        super.run();
    }
}
