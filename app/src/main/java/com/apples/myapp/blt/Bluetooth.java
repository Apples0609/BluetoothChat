package com.apples.myapp.blt;

import android.Manifest;
import android.app.Activity;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import android.widget.TabHost.OnTabChangeListener;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.apples.myapp.R;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class Bluetooth extends TabActivity {
    /** Called when the activity is first created. */
	public enum ServerOrCilent{
		NONE, SERVICE, CLIENT
	};
    private Context mContext;
    public static AnimationTabHost mTabHost;
    public static String BlueToothAddress = "null";
    public static ServerOrCilent serviceOrCilent = ServerOrCilent.NONE;
    public static boolean isOpen = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		askForPermissions(this);
        mContext = this;
    	setContentView(R.layout.main);
        //实例化
    	mTabHost = (AnimationTabHost) getTabHost();
        mTabHost.addTab(mTabHost.newTabSpec("Tab1")
        		.setIndicator("设备列表",getResources().getDrawable(android.R.drawable.ic_menu_add))
        		.setContent(new Intent(mContext, DeviceActivity.class)));
        mTabHost.addTab(mTabHost.newTabSpec("Tab2")
        		.setIndicator("对话列表",getResources().getDrawable(android.R.drawable.ic_menu_add))
        		.setContent(new Intent(mContext, ChatActivity.class)));
        
        mTabHost.setOnTabChangedListener(new OnTabChangeListener(){
        	public void onTabChanged(String tabId) {
        		if(tabId.equals("Tab1")){

        		}
        	}
        });
        mTabHost.setCurrentTab(0); 
    }

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Toast.makeText(mContext, "address:", Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onDestroy() {
		/* unbind from the service */
		super.onDestroy();
	}


	private boolean askForPermissions(Activity activity) {
		List<String> permissionsToAsk = new ArrayList<>();
		int requestResult = 124;
		if (ContextCompat.checkSelfPermission(activity.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) !=
				PackageManager.PERMISSION_GRANTED) {
			permissionsToAsk.add(Manifest.permission.ACCESS_FINE_LOCATION);
		}
		if (ContextCompat.checkSelfPermission(activity.getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) !=
				PackageManager.PERMISSION_GRANTED) {
			permissionsToAsk.add(Manifest.permission.ACCESS_COARSE_LOCATION);
		}
		if (ContextCompat.checkSelfPermission(activity.getApplicationContext(), Manifest.permission.BLUETOOTH_SCAN) !=
				PackageManager.PERMISSION_GRANTED) {
			permissionsToAsk.add(Manifest.permission.BLUETOOTH_SCAN);
		}
		if (ContextCompat.checkSelfPermission(activity.getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) !=
				PackageManager.PERMISSION_GRANTED) {
			permissionsToAsk.add(Manifest.permission.BLUETOOTH_CONNECT);
		}
		if (permissionsToAsk.size() > 0) {
			ActivityCompat.requestPermissions(activity, permissionsToAsk.toArray(new String[0]), requestResult);
		}
		return permissionsToAsk.size() > 0;
	}
}