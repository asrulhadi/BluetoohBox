package com.teknomatrik.bluetoothbox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.UUID;
import java.util.Calendar;

public class BoxValue extends AppCompatActivity {

    TextView status, msg_box;
    Button sendD, sendG, sendC, sendM, sendR, sendA;
    EditText gyro, motor, raw;
    TextClock clock, date;
    CheckBox clrfD, clrfC, clrfG, clrfM, clrfR, clrfA;

    Date today = new Date();
    Calendar cal = Calendar.getInstance();
    String time = today.getHours() + ":" + today.getMinutes() + ":" + today.getSeconds();


    private BluetoothAdapter mBlueAdapter;
    SendRecieve sendRecieve;


    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECEIVED = 5;

    private static final String APP_NAME = "Teknomatrik";
    private static final UUID MYUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothDevice bDevice;
    String bName;
    Boolean connect = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_box_value);
        getSupportActionBar().setTitle("GSC");

        bDevice = getIntent().getExtras().getParcelable("device");
        bName = getIntent().getExtras().getString("deviceName");

        findViewByIdea();

        ClientClass clientClass = new ClientClass(bDevice);
        clientClass.start();

        implementListeners();
    }

    private void findViewByIdea() {
        status = (TextView) findViewById(R.id.status);
        msg_box = (TextView) findViewById(R.id.msg);
        date = (TextClock) findViewById(R.id.date);
        clrfD = (CheckBox) findViewById(R.id.clrfDate);
        sendD = (Button) findViewById(R.id.sendDate);
        clock = (TextClock) findViewById(R.id.clock);
        clrfC = (CheckBox) findViewById(R.id.clrfClock);
        sendC = (Button) findViewById(R.id.sendClock);
        gyro = (EditText) findViewById(R.id.gyro);
        clrfG = (CheckBox) findViewById(R.id.clrfGyro);
        sendG = (Button) findViewById(R.id.sendGyro);
        motor = (EditText) findViewById(R.id.stepperMotor);
        clrfM = (CheckBox) findViewById(R.id.clrfMotor);
        sendM = (Button) findViewById(R.id.sendStepperMotor);
        raw = (EditText) findViewById(R.id.rawMsg);
        sendR = (Button) findViewById(R.id.sendRawMsg);
        clrfR = (CheckBox) findViewById(R.id.clrfRaw);
        sendA = (Button) findViewById(R.id.sendAll);
        clrfA = (CheckBox) findViewById(R.id.clrfAll);
    }

    private void SendMessage(TextView textView, CheckBox withCLRF, String pre, String post) {
        SendMessage(textView.getText().toString(), withCLRF, pre, post);
    }

    private void SendMessage(TextView textView, CheckBox withCLRF) {
        SendMessage(textView.getText().toString(), withCLRF, "", "");
    }

    private void SendMessage(String s, CheckBox withCLRF, String pre, String post) {
        String c = pre + s + post;
        if (withCLRF.isChecked()) c += "\r\n";
        sendRecieve.write(c.getBytes());
        Toast.makeText(getApplicationContext(),"Sending: " + c,Toast.LENGTH_SHORT).show();
    }

    private void implementListeners() {
        sendD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                SendMessage(date, clrfD, "D", "d");
            }
        });

        sendC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                SendMessage(clock, clrfC, "C", "c");
            }
        });

        sendG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                SendMessage(gyro, clrfG);
            }
        });

        sendM.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                SendMessage(motor, clrfM);
            }
        });

        sendR.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                SendMessage(raw, clrfR);
            }
        });

        sendA.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String g = String.valueOf(gyro.getText());
                String m = String.valueOf(motor.getText());
                String s = String.valueOf(raw.getText());
                String all = g + m + s;
                SendMessage(all, clrfA, "", "");
            }
        });
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what)
            {
                case STATE_LISTENING:
                    status.setText("Listening");
                    break;
                case STATE_CONNECTING:
                    status.setText("Connecting");
                    break;
                case STATE_CONNECTED:
                    connect = true;
                    status.setText("Connected " + bName);
                    break;
                case STATE_CONNECTION_FAILED:
                    status.setText("Connection Failed");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuff = (byte[]) msg.obj;
                    String tempMsg = new String(readBuff,0,msg.arg1);
                    msg_box.setText(tempMsg);
                    break;
            }
            return  true;
        }
    });

    private class ClientClass extends Thread {

        private BluetoothDevice device;
        private BluetoothSocket socket;

        public ClientClass(BluetoothDevice device1) {
            device = device1;

            try {
                socket = device.createRfcommSocketToServiceRecord(MYUUID);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public  void run() {
            try{
                socket.connect();
                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);

                sendRecieve = new SendRecieve(socket);
                sendRecieve.start();

            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }

    public class SendRecieve extends Thread {
        private final  BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendRecieve (BluetoothSocket socket) {
            bluetoothSocket=socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn = bluetoothSocket.getInputStream();
                tempOut = bluetoothSocket.getOutputStream();
            } catch(IOException e) {
                e.printStackTrace();
            }

            inputStream=tempIn;
            outputStream=tempOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while(true) {
                try {
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
}
