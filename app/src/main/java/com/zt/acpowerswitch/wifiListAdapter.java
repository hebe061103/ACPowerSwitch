package com.zt.acpowerswitch;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class wifiListAdapter extends RecyclerView.Adapter<wifiListAdapter.MyViewHolder> {

    private final List<String> mListData;
    private final Context context;

    public wifiListAdapter(List<String> mListData, Context context) {
        this.mListData = mListData;
        this.context = context;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //拿到布局
        View view = View.inflate(context, R.layout.listinfo, null);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.tv.setText(mListData.get(position));
    }

    @Override
    public int getItemCount() {
        return mListData.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private final TextView tv;
        public MyViewHolder(View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.list_text);
            itemView.setOnClickListener(view -> {
                if (null != MonItemClickListener) {
                    MonItemClickListener.onRecyclerItemClickListener(getAdapterPosition());
                }
            });
        }
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
