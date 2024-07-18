package com.zt.acpowerswitch;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


public class BlueDeviceItemAdapter extends RecyclerView.Adapter<BlueDeviceItemAdapter.MyViewHolder> {
    private final List<BluetoothDevice> mDeviceList;    //接受数据
    private final Context context;    //接受上下文

    public BlueDeviceItemAdapter(List<BluetoothDevice> mDeviceList, Context context) {
        this.mDeviceList = mDeviceList;
        this.context = context;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //拿到布局
        View view = View.inflate(context, R.layout.listinfo, null);
        //在ViewHolder中进行编码操作
        return new MyViewHolder(view);
    }

    @SuppressLint({"MissingPermission", "SetTextI18n"})
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        if(!isPaired(mDeviceList.get(position))) {
            String no_bond=mDeviceList.get(position).getName() + "\n" + mDeviceList.get(position).getAddress();
            holder.tv.setText(no_bond);
        }else {
            String bonded=mDeviceList.get(position).getName() + "\n" + mDeviceList.get(position).getAddress() + "  (巳配对)";
            holder.tv.setText(bonded);
        }
    }
    @SuppressLint("MissingPermission")
    public boolean isPaired(BluetoothDevice device) {
        return device.getBondState() == BluetoothDevice.BOND_BONDED;
    }
    @Override
    public int getItemCount() {
        return mDeviceList == null ? 0 : mDeviceList.size();
    }

    //创建ViewHolder类继承RecyclerView.ViewHolder
    public static class MyViewHolder extends RecyclerView.ViewHolder {

        private final TextView tv;

        public MyViewHolder(View itemView) {
            super(itemView);
            //找到控件
            tv = itemView.findViewById(R.id.list_text);
            itemView.setOnLongClickListener(view -> {
                if (null != MonItemLongClickListener) {
                    MonItemLongClickListener.onRecyclerItemLongClickListener(getAdapterPosition());
                }
                return true;
            });
            itemView.setOnClickListener(view -> {
                if (null != MonItemClickListener) {
                    MonItemClickListener.onRecyclerItemClickListener(getAdapterPosition());
                }
            });
        }
    }
    private static OnRecyclerItemLongClickListener MonItemLongClickListener=null;
    //设置点击监听事件用于外部引用
    public void setRecyclerItemLongClickListener(OnRecyclerItemLongClickListener listener){
        MonItemLongClickListener=listener;
    }
    //创建点击类接口
    public interface OnRecyclerItemLongClickListener{
        void onRecyclerItemLongClickListener(int postion);
    }
    private static OnRecyclerItemClickListener MonItemClickListener=null;
    //设置点击监听事件用于外部引用
    public void setRecyclerItemClickListener(OnRecyclerItemClickListener listener){
        MonItemClickListener=listener;
    }
    public interface OnRecyclerItemClickListener{
        void onRecyclerItemClickListener(int postion);
    }
}