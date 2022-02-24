package com.apples.myapp.blt;

import java.util.ArrayList;
import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.apples.myapp.R;

public class DCListViewAdapter extends BaseAdapter {
    private ArrayList<ListItemEntity> list;
    private LayoutInflater mInflater;

    public DCListViewAdapter(Context context, ArrayList<ListItemEntity> l) {
    	list = l;
		mInflater = LayoutInflater.from(context);
    }

    public int getCount() {
        return list.size();
    }

    public Object getItem(int position) {
        return list.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public int getItemViewType(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
	public View getView(int position, View convertView, ViewGroup parent) {
    	ViewHolder viewHolder = null;
    	ListItemEntity  item=list.get(position);
        if(convertView == null){
        	convertView = mInflater.inflate(R.layout.list_item, null);
        	viewHolder=new ViewHolder((TextView) convertView.findViewById(R.id.chat_msg));
        	convertView.setTag(viewHolder);
        }else{
        	viewHolder = (ViewHolder)convertView.getTag();
        }       
        if(item.isDCPairMe){
        	viewHolder.msg.setBackgroundResource(R.drawable.msgbox_rec);
        }else{
        	viewHolder.msg.setBackgroundResource(R.drawable.msgbox_send);
        }
        viewHolder.msg.setText(item.message);    
        return convertView;
    }

    class ViewHolder {
          protected TextView msg;
  
          public ViewHolder(TextView msg){
              this.msg = msg;
          }
    }
}
