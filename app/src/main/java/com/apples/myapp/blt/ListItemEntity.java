package com.apples.myapp.blt;

/**
 *
 * @author kaifang
 * @date 2015-7-10 下午3:45:22
 */
public class ListItemEntity {
	/** 展示的信息内容 */
	String message;
	/** 蓝牙设备页标识是否已配对<br>聊天页标识是否我的发的消息 */
	boolean isDCPairMe;

	public ListItemEntity(String msg, boolean siri) {
		message = msg;
		isDCPairMe = siri;
	}
}
