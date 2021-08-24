package com.e.bluetoothbox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class Bluetooth extends AppCompatActivity {

    Button listen, connection, listDevices;
    ListView listView;
    TextView msg_box, status;

    private BluetoothAdapter mBlueAdapter;
    BluetoothDevice[] bluetoothDevices;

    SendRecieve sendRecieve;

    String bName = " ";

    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECEIVED = 5;
    static final int STATE_DISCONNECT = 6;

    int REQUEST_ENABLE_BLUETOOTH = 1;

    private BluetoothServerSocket serverSocket;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;

    private static final String APP_NAME = "Teknomatrik";
    private static final UUID MYUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        getSupportActionBar().setTitle("Bluetooth Connection");

        findViewByIdea( );
        mBlueAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBlueAdapter == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Not compatible")
                    .setMessage("Your phone does not support Bluetooth")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }

        if (!mBlueAdapter.isEnabled()) {
            connection.setText("Turn On");
            connection.setBackgroundColor(Color.rgb(185,214,163));
            status.setText("Bluetooth Off");
        }
        if (mBlueAdapter.isEnabled()) {
            connection.setText("Turn Off");
            connection.setBackgroundColor(Color.rgb(214,115,121));
            status.setText("Bluetooth On");
        }

        implementListeners();
    }

    private void findViewByIdea() {

        listen = (Button) findViewById(R.id.listen);
        listView = (ListView) findViewById(R.id.listview);
        status = (TextView) findViewById(R.id.status);
        listDevices = (Button) findViewById(R.id.listDevices);
        connection = (Button) findViewById(R.id.connection);
    }

    private void implementListeners() {
        listDevices.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (!mBlueAdapter.isEnabled()) {
                    Toast.makeText(getApplicationContext(),"Please turn on Bluetooth.",Toast.LENGTH_SHORT).show();
                }
                else {
                    Set<BluetoothDevice> bt = mBlueAdapter.getBondedDevices();
                    String[] strings = new String[bt.size()];
                    bluetoothDevices = new BluetoothDevice[bt.size()];
                    int index = 0;

                    if (bt.size() > 0) {
                        for (BluetoothDevice device : bt) {
                            bluetoothDevices[index] = device;
                            strings[index] = device.getName();
                            index++;
                        }
                        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, strings);
                        listView.setAdapter(arrayAdapter);
                    }
                }
            }
        });

        listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBlueAdapter.isEnabled()) {
                    Toast.makeText(getApplicationContext(),"Please turn on Bluetooth.",Toast.LENGTH_SHORT).show();
                }
                else {
                    status.setText("Connecting...");
                    ServerClass serverClass = new ServerClass();
                    serverClass.start();
                }
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public  void onItemClick(AdapterView<?> adapterView, View view, int i, long l){
                if (!mBlueAdapter.isEnabled()) {
                    Toast.makeText(getApplicationContext(),"Please turn on Bluetooth.",Toast.LENGTH_SHORT).show();
                }
                else {
                    bName = bluetoothDevices[i].getName();
                    status.setText(" ");
                    Intent compass = new Intent(Bluetooth.this, BoxValue.class);
                    compass.putExtra("device", bluetoothDevices[i]);
                    compass.putExtra("deviceName", bName);
                    startActivity(compass);
                }
            }
        });

        connection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mBlueAdapter.isEnabled()) {
                    mBlueAdapter.disable();
                    connection.setText("Turn On");
                    connection.setBackgroundColor(Color.rgb(185,214,163));
                    status.setText("Bluetooth Off");
                }

                else if (!mBlueAdapter.isEnabled()) {
                    Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBT, REQUEST_ENABLE_BLUETOOTH);
                    connection.setText("Turn Off");
                    connection.setBackgroundColor(Color.rgb(214,115,121));
                    status.setText("Bluetooth On");
                }
            }
        });
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what)
            {
                case STATE_LISTENING:
                    break;
                case STATE_CONNECTING:
                    break;
                case STATE_CONNECTED:
                    break;
                case STATE_CONNECTION_FAILED:
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

    private class ServerClass extends Thread {
        public ServerClass(){
            try {
                serverSocket = mBlueAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,MYUUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public  void run() {
            BluetoothSocket socket = null;

            while(socket==null) {
                try {
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTING;
                    handler.sendMessage(message);

                    socket=serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();

                    Message message = Message.obtain();
                    message.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }

                if (socket!=null) {
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTED;
                    handler.sendMessage(message);

                    sendRecieve=new SendRecieve(socket);
                    sendRecieve.start();
                    break;
                }
            }
        }

    }

    public class SendRecieve extends Thread {
        public SendRecieve (BluetoothSocket socket) {
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
            try{
                outputStream.write(bytes);
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }
}
