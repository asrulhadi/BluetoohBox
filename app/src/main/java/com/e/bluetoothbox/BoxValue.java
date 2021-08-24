package com.e.bluetoothbox;

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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.UUID;

public class BoxValue extends AppCompatActivity {

    TextView status, msg_box;
    Button sendG, sendC, sendM;
    EditText gyro, motor;
    TextClock clock, second;

    Date today = new Date();
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

    private void findViewByIdea()
    {
        status = (TextView) findViewById(R.id.status);
        gyro = (EditText) findViewById(R.id.gyro);
        sendG = (Button) findViewById(R.id.sendGyro);
        clock = (TextClock) findViewById(R.id.clock);
        second = (TextClock) findViewById(R.id.second);
        sendC = (Button) findViewById(R.id.sendClock);
        motor = (EditText) findViewById(R.id.stepperMotor);
        sendM = (Button) findViewById(R.id.sendStepperMotor);
        msg_box = (TextView) findViewById(R.id.msg);
    }

    private void implementListeners() {
        sendG.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String string = String.valueOf(gyro.getText());
                String g = "G" + string + "g";
                gyro.setText("");
                sendRecieve.write(g.getBytes());
            }
        });

        sendC.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String string = String.valueOf(clock.getText());
                String string2 = String.valueOf(second.getText());
                String c = "C" + string + string2 + "c";
                //Log.d("message", string);
                sendRecieve.write(c.getBytes());
            }
        });

        sendM.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String string = String.valueOf(motor.getText());
                String s = "S" + string + "s";
                motor.setText("");
                sendRecieve.write(s.getBytes());
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

    private class ClientClass extends Thread{

        private BluetoothDevice device;
        private BluetoothSocket socket;

        public ClientClass(BluetoothDevice device1)
        {
            device = device1;

            try{
                socket = device.createRfcommSocketToServiceRecord(MYUUID);

            }catch (IOException e){
                e.printStackTrace();
            }
        }

        public  void run()
        {
            try{
                socket.connect();
                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);

                sendRecieve = new SendRecieve(socket);
                sendRecieve.start();

            }catch (IOException e){
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }

    public class SendRecieve extends Thread
    {
        private final  BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendRecieve (BluetoothSocket socket)
        {
            bluetoothSocket=socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try{
                tempIn = bluetoothSocket.getInputStream();
                tempOut = bluetoothSocket.getOutputStream();
            }catch(IOException e){
                e.printStackTrace();
            }

            inputStream=tempIn;
            outputStream=tempOut;
        }

        public void run()
        {
            byte[] buffer = new byte[1024];
            int bytes;

            while(true)
            {
                try{
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes)
        {
            try{
                outputStream.write(bytes);
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }
}
