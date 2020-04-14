package com.example.mockgps;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.service.MockGpsService;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TaskActivity extends AppCompatActivity {

    public List<FLocation> FLocationPoints = new ArrayList<>();

    public static final int MSG_END_TASK = 1; // 任务结束扫尾工作
    public static final int MSG_UPDATE_LOC = 2; // 更新UI数据
    public static final int MSG_NETWORK_FAIL = -1; // 网络出错

    public boolean isStartTask = false;


    //log debug
    private static Logger log = Logger.getLogger(TaskActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.task_activity);

        final TextView taskId = findViewById(R.id.task_id);
        final TextView account = findViewById(R.id.task_account);
        final TextView passwd = findViewById(R.id.task_passwd);
        Switch startTask = findViewById(R.id.task_switch);

        isStartTask = startTask.isChecked();

        Button amap = findViewById(R.id.task_amap);
        Button baidumap = findViewById(R.id.task_baidumap);
        Button tencent = findViewById(R.id.task_tencent);
        Button google = findViewById(R.id.task_google);

        startTask.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    // 开启
                    DisplayToast("开始任务");
                    startMyTask(taskId.getText().toString().trim(), account.getText().toString().trim(), passwd.getText().toString().trim());
                }else {
                    // 关闭
                    DisplayToast("结束任务");
                    stopMyTask();
                }
            }
        });

        setUpMapClickListener(amap, "com.autonavi.minimap");
        setUpMapClickListener(baidumap, "com.baidu.BaiduMap");
        setUpMapClickListener(tencent, "com.tecent.map");
        setUpMapClickListener(google, "com.google.android.apps.maps");


    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            Switch startTask = findViewById(R.id.task_switch);
            switch (msg.what){
                case MSG_END_TASK:
                    // 用于任务完成后的收尾工作，更新UI
                    startTask.setChecked(false);
                    break;
                case MSG_UPDATE_LOC:
                    updateLocationView(msg.arg1);
                    break;
                case MSG_NETWORK_FAIL:
                    DisplayToast("无法连接服务器");
                    FLocationPoints.clear();
                    startTask.setChecked(false);
                    break;
            }
        }
    };

    public void updateLocationView(int index){
        TextView lat = findViewById(R.id.task_info_lat_con);
        TextView lng = findViewById(R.id.task_info_lng_con);
        TextView cid = findViewById(R.id.task_info_cid_con);
        TextView lac = findViewById(R.id.task_info_lac_con);
        TextView mcc = findViewById(R.id.task_info_mcc_con);
        TextView mnc = findViewById(R.id.task_info_mnc_con);
        TextView bsss = findViewById(R.id.task_info_bsss_con);
        FLocation fl = FLocationPoints.get(index);
        lat.setText(fl.getLatitude());
        lng.setText(fl.getLongitude());
        cid.setText(fl.getCID());
        lac.setText(fl.getLAC());
        mcc.setText(fl.getMCC());
        mnc.setText(fl.getMNC());
        bsss.setText(fl.getBSSS());
    }

    public void setUpMapClickListener(Button btn, final String packageName){
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Switch startTask = findViewById(R.id.task_switch);
                if(!startTask.isChecked()){
                    DisplayToast("请先开始众包任务");
                    return;
                }
                if(FLocationPoints.size() == 0){
                    DisplayToast("任务数据还未返回");
                    return;
                }
                FLocation firstPoint = FLocationPoints.get(0);
                FLocation lastPoint = FLocationPoints.get(FLocationPoints.size()-1);
                MapUtils.startGuide(TaskActivity.this, packageName,
                        firstPoint.getLatitude(), firstPoint.getLongitude(), lastPoint.getLatitude(), lastPoint.getLongitude());
            }
        });
    }

    public void startMyTask(final String task_id, final String account, final String passwd){
        isStartTask = true;
        FLocationPoints.clear();
        new Thread(){
            @Override
            public void run(){
                // 请求数据
                if(!requestData(task_id, account, passwd)){
                    Message message = Message.obtain();
                    message.what = MSG_NETWORK_FAIL;
                    handler.sendMessage(message);
                    return;
                }

                Log.d("DEBUG","requestData returned");

                for (int i=0;i<FLocationPoints.size();i++){
                    if(!isStartTask) break;
                    try {
                        //start mock location service
                        Intent mockLocServiceIntent = new Intent(TaskActivity.this, MockGpsService.class);
                        FLocation point = FLocationPoints.get(i);
                        mockLocServiceIntent.putExtra("key", point.getLongitude()+"&"+point.getLatitude());
                        if (Build.VERSION.SDK_INT >= 26) {
                            startForegroundService(mockLocServiceIntent);
                            Log.d("DEBUG", "startForegroundService: MOCK_GPS");
                            log.debug("startForegroundService: MOCK_GPS");
                        } else {
                            startService(mockLocServiceIntent);
                            Log.d("DEBUG", "startService: MOCK_GPS");
                            log.debug("startService: MOCK_GPS");
                        }
                        // 更新UI
                        Message message = Message.obtain();
                        message.what = MSG_UPDATE_LOC;
                        message.arg1 = i;
                        handler.sendMessage(message);

                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // 任务完成
                Message message = Message.obtain();
                message.what = MSG_END_TASK;
                handler.sendMessage(message);
            }
        }.start();
    }

    public void stopMyTask(){
        isStartTask = false;
        //end mock location
        Intent mockLocServiceIntent = new Intent(TaskActivity.this, MockGpsService.class);
        stopService(mockLocServiceIntent);
    }


    // 请求众包任务
    public boolean requestData(final String task_id, final String account, final String passwd) {
        boolean ret = true;
        //网络操作不能在主线程中进行
        try {
            String url = "http://hhmoumoumouhh.51vip.biz/web/LoginServlet";
            URL obj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
            // method POST
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            DataOutputStream wr=new DataOutputStream(conn.getOutputStream());
            //要提交的参数
            String content = "TaskId="+task_id+"&AccountNumber="+account+"&Password="+passwd; // 请求数据待定
            //将要上传的内容写入流中
            wr.writeBytes(content);
            //刷新、关闭
            wr.flush();
            wr.close();

            conn.setReadTimeout(6000);

            //获取响应码的同时会连接网络
            if (conn.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                String output;
                StringBuffer response = new StringBuffer();

                while ((output = in.readLine()) != null) {
                    response.append(output);
                }
                in.close();
                // 处理response数据
                ret = onResponse(response.toString());

                conn.disconnect();
            }else {
                ret = false;
            }
        } catch (MalformedURLException e){
            e.printStackTrace();
            ret = false;
        } catch (IOException e) {
            e.printStackTrace();
            ret = false;
        }

        return ret;
    }

    // 处理网络请求返回数据，返回格式待定，暂定为List<JSONObject>
    public boolean onResponse(String response) {
        boolean ret = true;
        try {
            JSONObject jsonObject = new JSONObject(response);
            String result = ((JSONObject)jsonObject.get("params")).getString("Result");
            if (result.equals("success")) {
                //做登录成功的操作
                //解析数据
                JSONArray points = jsonObject.getJSONArray("points");

                FLocationPoints.clear();

                for (int i=0;i<points.length();i++){
                    JSONObject point = jsonObject.getJSONObject(""+i);
                    String latitude= point.getString("latitude");
                    String longitude= point.getString("longitude");

                    // MCC，Mobile Country Code，移动国家代码（中国的为460）；
                    // MNC，Mobile Network Code，移动网络号码（中国移动为0，中国联通为1，中国电信为2）； 
                    // LAC，Location Area Code，位置区域码；
                    // CID，Cell Identity，基站编号；
                    // BSSS，Base station signal strength，基站信号强度。
                    String MCC = point.getString("MCC");
                    String LAC = point.getString("LAC");
                    String MNC = point.getString("MNC");
                    String CID = point.getString("CID");
                    String BSSS = point.getString("BSSS");

                    // 这里使用全局变量FLocationPoints保存数据
                    FLocationPoints.add(new FLocation(latitude, longitude, MCC, MNC, LAC, BSSS, CID));
                }

            } else {
                //做登录失败的操作
                Log.d("DEBUG",result);
                ret = false;
            }
        } catch (JSONException e) {
            //做http请求异常的操作
            e.printStackTrace();
            ret = false;
        }
        return ret;
    }

    public void DisplayToast(String str) {
        Toast toast = Toast.makeText(TaskActivity.this, str, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP, 0, 220);
        toast.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
