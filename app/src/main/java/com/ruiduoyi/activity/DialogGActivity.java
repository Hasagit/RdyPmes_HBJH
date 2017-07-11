package com.ruiduoyi.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ruiduoyi.R;
import com.ruiduoyi.model.NetHelper;
import com.ruiduoyi.utils.AppUtils;

import java.math.BigInteger;
import java.util.List;

public class DialogGActivity extends BaseDialogActivity implements View.OnClickListener{
    private Intent intent_from;
    private Button ok_btn,cancle_btn,yylb_spinner,yymc_spinner;
    private TextView title_text,tip_text;
    private EditText num_edit;
    private String num;
    private String title,type,zldm,jtbh;
    private FrameLayout yy_bg;
    //接收刷卡串口广播
    private BroadcastReceiver receiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            num=intent.getStringExtra("num");
            num_edit.setText(num);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    List<List<String>>list=NetHelper.getQuerysqlResult("PAD_Read_CardID '"
                            +type+"','"+zldm+"','"+num+"'");
                    if (list!=null){
                        if (list.size()>0){
                            if (list.get(0).size()>0){
                                Message msg=handler.obtainMessage();
                                msg.what=0x100;
                                msg.obj=list.get(0).get(0);
                                handler.sendMessage(msg);
                            }
                        }
                    }else {
                        AppUtils.uploadNetworkError("PAD_Read_CardID",jtbh,sharedPreferences.getString("mac",""));
                    }
                }
            }).start();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_g);
        initView();
        initDate();
    }

    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 0x100:
                    String readCardResult= (String) msg.obj;
                    final String two=readCardResult.substring(0,2);
                    if (readCardResult.substring(0,2).equals("OK")){
                        if (type.equals("OPR")){//执行指令操作
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    List<List<String>>list=NetHelper.getQuerysqlResult("Exec PAD_SrvCon '"+jtbh+"','"+zldm+"','"+num+"',''");
                                    if (list!=null){
                                        if (list.size()>0){
                                            if (list.get(0).size()>0){
                                                if (list.get(0).get(0).trim().equals("OK")){
                                                    Intent intent=new Intent();
                                                    intent.setAction("UpdateInfoFragment");
                                                    sendBroadcast(intent);
                                                    if (!(title.equals("结束")|title.equals("人员上岗")|title.equals("品管巡机"))){
                                                        Intent intent2=new Intent();
                                                        intent2.setAction("com.Ruiduoyi.returnToInfoReceiver");
                                                        sendBroadcast(intent2);
                                                    }
                                                    finish();
                                                }else {
                                                    Message msg=handler.obtainMessage();
                                                    msg.what=0x102;
                                                    msg.obj=list.get(0).get(0);
                                                    handler.sendMessage(msg);
                                                }
                                            }
                                        }
                                    }
                                }
                            }).start();
                        }else {//执行文档操作
                            String wkno=readCardResult.substring(2,readCardResult.length());
                            Intent intent;
                            if (title.equals("【工单管理】")){
                                intent=new Intent(DialogGActivity.this,GdglActivity.class);
                                intent.putExtra("wkno",wkno);
                            }else if(title.equals("【异常分析】")){
                                intent=new Intent(DialogGActivity.this,YcfxActivity.class);
                                intent.putExtra("wkno",wkno);
                            }else{
                                intent=new Intent(DialogGActivity.this,BlfxActivity.class);
                                intent.putExtra("wkno",wkno);
                            }
                            startActivity(intent);
                            finish();

                        }
                    }else {
                        tip_text.setText(readCardResult);
                        tip_text.setTextColor(Color.RED);
                    }
                    break;
                case 0x102://操作失败
                    String tip_str=(String)msg.obj;
                    tip_text.setText(tip_str);
                    tip_text.setTextColor(Color.RED);
                    break;
                default:
                    break;
            }
        }
    };

    private void initView(){
        ok_btn=(Button)findViewById(R.id.ok_btn);
        cancle_btn=(Button)findViewById(R.id.cancle_btn);
        num_edit=(EditText) findViewById(R.id.num_text);
        title_text=(TextView)findViewById(R.id.title_text);
        tip_text=(TextView)findViewById(R.id.tip);
        intent_from=getIntent();
        yy_bg=(FrameLayout)findViewById(R.id.yy_bg);
        yylb_spinner=(Button)findViewById(R.id.yylb);
        yymc_spinner=(Button)findViewById(R.id.yymc);
        title=intent_from.getStringExtra("title");
        zldm=intent_from.getStringExtra("zldm");
        type=intent_from.getStringExtra("type");
        title_text.setText(title);
        ok_btn.setOnClickListener(this);
        cancle_btn.setOnClickListener(this);
    }

    private void initDate(){
        sharedPreferences=getSharedPreferences("info",MODE_PRIVATE);
        jtbh=sharedPreferences.getString("jtbh","");
        IntentFilter receiverfilter=new IntentFilter();
        receiverfilter.addAction("SerialPortNum");
        registerReceiver(receiver,receiverfilter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.ok_btn:
                AppUtils.sendCountdownReceiver(DialogGActivity.this);
                finish();
                break;
            case R.id.cancle_btn:
                AppUtils.sendCountdownReceiver(DialogGActivity.this);
                finish();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

}
