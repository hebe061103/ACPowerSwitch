package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.MainActivity.debugList;
import static com.zt.acpowerswitch.MainActivity.goAnim;
import static com.zt.acpowerswitch.MainActivity.send_command_to_server;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class Engineering extends AppCompatActivity {
    public ArrayAdapter<String> adapter;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.engineering_activity);
        Button bt_restart = findViewById(R.id.button_restart);
        bt_restart.setOnClickListener(view -> {
            goAnim(Engineering.this, 50);
            new AlertDialog.Builder(Engineering.this)
                    .setTitle("提 示")
                    .setMessage("确定重启远程设备吗?")
                    .setPositiveButton("取消", null)
                    .setNegativeButton("确定", (dialogInterface, i) -> {
                        goAnim(Engineering.this, 50);
                        if (send_command_to_server("restart")) {
                            new AlertDialog.Builder(this)
                                .setTitle("提示")
                                .setMessage("设备正在重启......")
                                .setNegativeButton("完成", (dialogInterface1, i1) -> goAnim(Engineering.this, 50)).show();
                        }else{
                            new AlertDialog.Builder(this)
                                .setTitle("提示")
                                .setMessage("设备正忙,请稍后再试!")
                                .setNegativeButton("完成", (dialogInterface1, i1) -> goAnim(Engineering.this, 50)).show();
                        }
                    }).show();

        });
        ListView debug_lv = findViewById(R.id.debug_log_device);
        if (debugList.isEmpty()) {
            debugList.add("暂无异常!");
        }
        adapter = new ArrayAdapter<>(Engineering.this, R.layout.list_item, debugList);
        debug_lv.setAdapter(adapter);
    }
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // 当前面板是null时表示没有面板正在显示
        // super.onBackPressed();
        if (!debugList.isEmpty() && !debugList.get(0).equals("暂无异常!")) {
            new AlertDialog.Builder(Engineering.this)
                    .setTitle("提 示")
                    .setMessage("是否要清除所有日志?")
                    .setPositiveButton("取消", (dialogInterface, i) -> {
                        goAnim(Engineering.this, 50);
                        finish();
                    })
                    .setNegativeButton("确定", (dialogInterface, i) -> {
                        goAnim(Engineering.this, 50);
                        if (send_command_to_server("del_debug_log")) {
                            new AlertDialog.Builder(Engineering.this)
                                    .setTitle("提示")
                                    .setMessage("清除完成")
                                    .setNegativeButton("完成", (dialogInterface1, i1) -> {
                                        goAnim(Engineering.this, 50);
                                        debugList.clear();
                                        finish();
                                    }).show();
                        } else {
                            new AlertDialog.Builder(Engineering.this)
                                    .setTitle("提示")
                                    .setMessage("设备正忙,请稍后再试!")
                                    .setNegativeButton("完成", (dialogInterface1, i1) -> goAnim(Engineering.this, 50)).show();
                        }
                    }).show();
        }else {
            debugList.clear();
            finish();
        }
    }
}
