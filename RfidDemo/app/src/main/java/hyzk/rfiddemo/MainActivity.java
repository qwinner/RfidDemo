package hyzk.rfiddemo;

import android.content.DialogInterface;
import android.content.Intent;
import android.fpi.MtGpio;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.Timer;
import java.util.TimerTask;

import android_serialport_api.RfidReader;

public class MainActivity extends AppCompatActivity {
    private boolean bread=true;
    private EditText editText;
    private Timer startTimer=null;
    private TimerTask startTask;
    Handler startHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button btn3=(Button)findViewById(R.id.button3);

        btn3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                RfidReader.getInstance().SendCmd(0x02);
                TimerStart();
                bread=true;
            }
        });

        final Button btn4=(Button)findViewById(R.id.button4);

        btn4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                RfidReader.getInstance().SendCmd(0x03);
                TimerStart();
                bread=true;
            }
        });
        editText=(EditText)findViewById(R.id.editText1);

        MtGpio.getInstance().RFPowerSwitch(true);
        RfidReader.getInstance().openSerialPort();
        RfidReader.getInstance().SetMessageHandler(rfidHandler);
    }

    public void TimerStart() {
        if(startTimer!=null)
            return;

        startTimer = new Timer();
        startHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                if(editText.getText().length()>5){
                    TimerStop();
                }else{
                    RfidReader.getInstance().SendCmd(0x02);
                }
                super.handleMessage(msg);
            }
        };
        startTask = new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = 1;
                startHandler.sendMessage(message);
            }
        };
        startTimer.schedule(startTask, 1000, 1000);
    }

    public void TimerStop() {
        if (startTimer!=null) {
            startTimer.cancel();
            startTimer = null;
            startTask.cancel();
            startTask=null;
        }
    }

    private final Handler rfidHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case RfidReader.STATE_ADDHIGH:
                {
                    if(bread){
                        byte[] sn=(byte[])msg.obj;
                        editText.setText(new String(sn));
                        bread=false;
                    }
                }
                break;
                case RfidReader.STATE_ADDITEM:
                {
                    if(bread){
                        int sv=msg.arg2;
                        byte[] sn=(byte[])msg.obj;
                        editText.setText(new String(sn));
                    }
                }
                break;
            }
        }
    };



}
