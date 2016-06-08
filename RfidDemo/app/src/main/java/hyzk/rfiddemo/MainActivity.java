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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button btn3=(Button)findViewById(R.id.button3);
        ExtApi.setButtonStateChangeListener(btn3);
        btn3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                RfidReader.getInstance().SendCmd(0x03);
                bread=true;
            }
        });
        editText=(EditText)findViewById(R.id.editText1);

        MtGpio.getInstance().RFPowerSwitch(true);
        RfidReader.getInstance().openSerialPort();
        RfidReader.getInstance().SetMessageHandler(rfidHandler);
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
