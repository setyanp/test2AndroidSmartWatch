package com.styn.hao.wearabletest2;

import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;


import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;


import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private static final int ID_ESTABLISH_CONNECTION = 0;
    private static final int ID_RECEIVED_TIME_REQUEST = 1;
    private static final int ID_RESPONSE_TIME = 2;
    private static final int ID_HEARTBEAT = 3;
    private static final int ID_TERMINATE = 4;
    private static final int ID_SENSOR_DATA = 5;
//    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
//    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
//    private static final int REQUEST_ENABLE_BT = 3;

    private TextView mTextView;
    private static final String TAG = "BTTransfer";
//    private static final int GOO_API_CLIENT_REQUEST_RESOLVE_ERROR = 1000;
    private GoogleApiClient mGoogleApiClient;
    private boolean mbResolvingGooApiClientError = false;
    private boolean ServerConnect = false;
    private static byte[] payload;
//    private String phoneSensorData;
//    private BluetoothAdapter mBluetoothAdapter = null;
//    private BluetoothSocket btSocket = null;
//    private OutputStream outStream = null;
    protected  SensorManager mSensorManager;
    private long timestamp;
//    private String btAddress;
//    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private Button mBtnComfirm;
    private Button mBtnStop;
    private Spinner mSpnDevice;
    private ArrayAdapter<String> DeviceListAA;

//    private BluetoothDevice device = null;

//    private Boolean btIsConnect;
    private Queue<SensorData> timestampQueue;
    private float[] GyroscopeValues =  {0.0f, 0.0f, 0.0f};
    private float[] AccelerometerValues =  {0.0f, 0.0f, 0.0f};
    private float[] GravityValues =  {0.0f, 0.0f, 0.0f};
    private float[] mR = new float[9];
    private float[] OrientationValues = {0.0f, 0.0f, 0.0f};
    private float[] RotationVectorValues = {0.0f, 0.0f, 0.0f};
    private float[] MagnitoValues = {0.0f, 0.0f, 0.0f};
    private float[] LinearAccelerationValues = {0.0f, 0.0f, 0.0f};
    private static final int SensorDelayedRate = 20;

    private Long receivedTime;
    private Long connectionID;
    private Long laststand;
    private Boolean calibrated;
    public Handler mHandler = new Handler();
    public Handler mHandlerHB = new Handler();
    private Pattern msgPattern;
    private Pattern timestampPattern;
//    private String[] btDeviceNameArray;
//    private String[] btDeviceAddressArray;
//    private String   BTPayload;
//    private int BTPayloadCounter = 0;
//    private int rotaionMatrixLength = 0;


    private Handler mUIHandler = new Handler() { //handler for text in UI
        @Override
        public void handleMessage(Message msg) {

            super.handleMessage(msg);
            String msgString = (String)msg.obj;
            mTextView.setText(msgString);

        }
    };

    private Handler mMessageHandler = new Handler() //handler for message data from/into smartwatch
    {
        @Override
        public void handleMessage(Message msg)
        {

            String sendingMsg = new String();
            switch (msg.what)
            {
                case ID_ESTABLISH_CONNECTION:
                    Log.i(TAG,"Establishing");
                    sendingMsg = "0:" + connectionID;
                    payload = sendingMsg.getBytes();
                    break;
                case ID_TERMINATE:
                    sendingMsg = "4:" + connectionID;
                    payload = sendingMsg.getBytes();
                    break;
                case ID_RESPONSE_TIME:
                    Log.i(TAG,"response time");
                    sendingMsg = "2:" + receivedTime + "," + System.currentTimeMillis();
                    payload = sendingMsg.getBytes();
                    mHandlerHeartBeat.run();
                    break;
                case ID_HEARTBEAT:
                    Log.i(TAG,"HeartBeat");
                    sendingMsg = "3:" + System.currentTimeMillis();
                    payload = sendingMsg.getBytes();
                    break;
            }

            AsyncTaskSendMessage at = new AsyncTaskSendMessage();
            at.execute(sendingMsg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        timestamp = 0;
        setContentView(R.layout.activity_main);
        mGoogleApiClient = new GoogleApiClient.Builder(this) //construct the googleapiclient object
                .addApi(Wearable.API) //specify api requested by phone => smartwatch/wearable
                .addConnectionCallbacks(gooApiClientConnCallback) //call data layer API, register listener for connection events
                .addOnConnectionFailedListener(gooApiClientOnConnFail)  //handling error case
                .build();

        /*
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //get setup for local bluetooth adapter
        if (mBluetoothAdapter == null) { //if bluetooth connection not found
            Toast.makeText(this,
                    "Bluetooth is not available.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Set<BluetoothDevice> pairedSet = mBluetoothAdapter.getBondedDevices(); //return object that paired to local bluetooth
        List<String> btDeviceNameList =new ArrayList<String>();
        List<String> btDeviceAddressList =new ArrayList<String>();
        for(BluetoothDevice pairedDevice : pairedSet)
        {
            //Log.i(TAG,pairedDevice.getName() + " : " + pairedDevice.getAddress());
            btDeviceNameList.add(pairedDevice.getName()); //list of devices name that connected to local bluetooth
            btDeviceAddressList.add(pairedDevice.getAddress()); //list address of devices
        }
        btDeviceNameArray = btDeviceNameList.toArray(new String[btDeviceNameList.size()]); //insert connected bluetooth devices into array
        btDeviceAddressArray = btDeviceAddressList.toArray(new String[btDeviceAddressList.size()]); //insert their address to array
        */
        mTextView = (TextView)findViewById(R.id.textView);
        mBtnComfirm = (Button)findViewById(R.id.btnComfirm);
        mBtnStop = (Button)findViewById(R.id.btnStop);
        mSpnDevice = (Spinner)findViewById(R.id.spnDevice);
        mBtnComfirm.setOnClickListener(btnComfirmOnClick);
        mBtnStop.setOnClickListener(btnStopOnClick);
        //DeviceListAA = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_spinner_item,btDeviceNameArray);
        mSpnDevice.setAdapter(DeviceListAA); //show list connection
        mSpnDevice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) { //select list of the connected devices
                //btAddress = btDeviceAddressArray[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        getSensor(); //call getSensor Method

        //btIsConnect = false; //set default value of bluetooth connect
        timestampQueue = new LinkedList<>();
        msgPattern = Pattern.compile("([0-9]):(.*)"); //create pattern for the message communication, consist of number and dot
        timestampPattern= Pattern.compile("([0-9]*),(.*)");
        laststand = (long)0;
        calibrated = false;
        //BTPayload = "";
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect(); //connect it with wearable devices
        /*if (!mBluetoothAdapter.isEnabled()) { //try to connect to bluetooth
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE); //Show a system activity that allows the user to turn on Bluetooth
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT); //if bluetooth on, will send RESULT_OK, means that bluetooth system can start
            return;
        }*/
        Wearable.MessageApi.addListener(mGoogleApiClient,wearableMsgListener); //listen to wearable devices
        Log.i(TAG, "ON START.");
    }

    @Override
    protected void onPause() {
        //unregister listener for all sensors that started
        mSensorManager.unregisterListener(this);
       connectionTerminate();
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (!mbResolvingGooApiClientError && ServerConnect) {
            Wearable.MessageApi.removeListener(mGoogleApiClient, wearableMsgListener);
            mGoogleApiClient.disconnect();
        }
       /* try {
            if(out!=null)
                out.close();
            if(socket!=null)
                socket.close();
        }catch (Exception e) {
            Log.w(TAG, "Error: " + e.getMessage());
        }*/
        mHandler.removeCallbacks(mHandlerTask);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "ON RESUME.");
        getSensor();
        mHandlerTask.run();
    }

    private GoogleApiClient.ConnectionCallbacks gooApiClientConnCallback = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            Log.w(TAG,"onConnected");
            mbResolvingGooApiClientError = false;

        }
        @Override
        public void onConnectionSuspended(int i) { //if connection smartwatch and phone is fail
            Log.w(TAG, "Google API Client connection failed.");
        }
    };

    private GoogleApiClient.OnConnectionFailedListener gooApiClientOnConnFail = new GoogleApiClient.OnConnectionFailedListener(){
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            if (mbResolvingGooApiClientError) {
                return;
            } else {
                mbResolvingGooApiClientError = false;
                Wearable.MessageApi.removeListener(mGoogleApiClient,wearableMsgListener);
            }
        }
    };


    private MessageApi.MessageListener wearableMsgListener = new MessageApi.MessageListener() { //create listener for wearable device
        @Override
        public void onMessageReceived(MessageEvent messageEvent) {
            receivedTime = System.currentTimeMillis(); //get the time when start received data from listener
            if(ServerConnect) { //check if confirm button is pressed or not ?
                payload = messageEvent.getData(); //received data from listener
                //Log.i(TAG,new String(payload));
                Matcher msgMatcher = msgPattern.matcher(new String(payload)); //matches the patern with payload string
                msgMatcher.matches(); //return true or false (?) for what?
                int msgIDType = Integer.parseInt(msgMatcher.group(1));
                //Log.i(TAG, new String(msgIDType);
                switch (msgIDType) {
                    case ID_RECEIVED_TIME_REQUEST:
                        Log.i(TAG,"Get Time Request");
                        mMessageHandler.sendEmptyMessage(ID_RESPONSE_TIME);
                        calibrated = true;
                        break;
                    case ID_SENSOR_DATA: //smartwatch send "5" in front of their sensor data
                        //pick data in queue and send it to pc via bluetooth
                        if(!calibrated) break;
                        //Log.i(TAG, "Get Sensor Data: " + new String(payload));
                        Matcher timestampMatcher = timestampPattern.matcher(msgMatcher.group(2));//matches the pattern of timestamp with data in group2
                        if(!timestampMatcher.matches()) return; //if false stop
                        //Log.i(TAG,"getdata: " + msgMatcher.group(2));
                        Long timestampMsg = Long.parseLong(timestampMatcher.group(1));
                        //Log.i(TAG,timestampMsg.toString());
                        if(timestampMsg - laststand < 20) break;

                        while (true) {
                            if(timestampQueue.isEmpty())
                            {
                                Log.i(TAG,"queue is empty in " +System.currentTimeMillis());
                                break;
                            }
                            SensorData ti = timestampQueue.poll(); //get data from top queue
                            Long l = Math.abs(timestampMsg - ti.getTimestamp()); //absolute (cannot get negative value)
                            //Log.i(TAG,timestampMsg+" , "+ti.getTimestamp());
                            if (l <= 5) {
                                //send to pc
                                //Log.i(TAG, "Synchronize in Watch: " + timestampMsg + " , Phone : " + ti.getTimestamp() + " ,in " + System.currentTimeMillis());
                                //assign bluetooth message
                                //Log.i(TAG,"ti:" + ti.getData());
                                String payloadS = new String(msgMatcher.group(2) + ti.getData()); //msgMatcher.group(2) = consist of sensor data from smartwatch
                                Log.i(TAG,payloadS);
                                //payloadS += "\r\n";

                                /** Code bellow is to send data in the queue to pc using bluetooth */
                                /*AsyncTaskSendMessageToPC at = new AsyncTaskSendMessageToPC(); //async task to send data from wearable to pc
                                at.execute(payloadS);*/

                                laststand = timestampMsg;
                                break;
                            }
                            else if(ti.getTimestamp() > timestampMsg && l >=10) {
                                Log.i(TAG,"Overpoll:" + ti.getTimestamp() + "," + timestampMsg);
                                break;
                            }
                        }
                        break;
                    case ID_TERMINATE:
                        connectionTerminate();
                        break;
                }
            }
            return;
        }
    };

    private static final String WEARABLE_PATH_MASSAGE = "/message";

    private void connectionTerminate() {
        /*try {
            if(btSocket!=null&&btSocket.isConnected()) {
                btSocket.close();
                btIsConnect = false;
                //Log.e(TAG, "ON OVER: TERMINATE APP.");
            }
        } catch (IOException e) {
            Log.e(TAG, "ON OVER: Exception during terminate.", e);
        }*/
        mHandler.removeCallbacks(mHandlerTask);
        mHandlerHB.removeCallbacks(mHandlerHeartBeat);
        if(ServerConnect) {
            mMessageHandler.sendEmptyMessage(ID_TERMINATE);
            ServerConnect = false;
            timestampQueue.clear();
            //BTPayload = "";
            //BTPayloadCounter = 0;
            Message msg;
            msg = mUIHandler.obtainMessage(1,"Connection Terminated");
            mUIHandler.sendMessage(msg);
        }
    }

    private Runnable mHandlerTask = new Runnable()  {
        @Override
        synchronized public void run() {

                if (ServerConnect) {

                    timestampQueue.offer(new SensorData(System.currentTimeMillis(), getSensorData())); //
                    //Log.i(TAG, "put: " + t.getTimestamp());
                    mHandler.postDelayed(mHandlerTask, 5);
                    /*Log.i(TAG, "put: " + getSensorData());
                    SensorManager.getRotationMatrix(mR, null, AccelerometerValues, MagnitoValues);
                    SensorManager.getOrientation(mR, OrientationValues);
                    String s = "," + GyroscopeValues[0] + "," + GyroscopeValues[1] + "," + GyroscopeValues[2] + ","
                            + AccelerometerValues[0] + "," + AccelerometerValues[1] + "," + AccelerometerValues[2] +","
                            + GravityValues[0] + "," + GravityValues[1] + "," + GravityValues[2] + ","
                            + RotationVectorValues[0] + "," + RotationVectorValues[1] + "," + RotationVectorValues[2] + ","
                            + OrientationValues[0] + "," + OrientationValues[1] + "," + OrientationValues[2] + ","
                            + LinearAccelerationValues[0] + "," + LinearAccelerationValues[1] + "," + LinearAccelerationValues[2] + ","
                            + MagnitoValues[0] + "," + MagnitoValues[1] + "," + MagnitoValues[2];
                    Log.i(TAG, "Raw: " + s);*/
                }
            }

    };

    private Runnable mHandlerHeartBeat = new Runnable()
    {
        @Override
        public void run() {
            mMessageHandler.sendEmptyMessage(ID_HEARTBEAT);
            mHandlerHB.postDelayed(mHandlerHeartBeat, 3000);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private  View.OnClickListener btnStopOnClick = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            if(ServerConnect) connectionTerminate();
            return;
        }
    };
    private View.OnClickListener btnComfirmOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!ServerConnect) {

                /*if (MACValidator(btAddress)) {
                    if (!mbResolvingGooApiClientError) {
                        mTextView.setText("Connecting Bluetooth Server " + btAddress);
                        ServerConnect = true;
                        //mGoogleApiClient.connect();

                        device = mBluetoothAdapter.getRemoteDevice(btAddress);
                    }
                } else {
                    mTextView.setText("IP Format isn't corrected");
                }*/

                try {
                    /*btSocket = device.createRfcommSocketToServiceRecord(MY_UUID); //this to make smartphone bluetooth as client
                    btSocket.connect();
                    outStream = btSocket.getOutputStream(); //to handle out transmission*/
                    //mGoogleApiClient.connect();
                    ServerConnect = true;
                    /** for smartwatch party */
                    connectionID = System.currentTimeMillis();
                    mMessageHandler.sendEmptyMessage(ID_ESTABLISH_CONNECTION);
                    mHandlerTask.run();

                    Log.e(TAG, "ON CLICK: Socket created.");
                    mTextView.setText("Connection Established");
                } catch (Exception e) {
                    Log.e(TAG, "ON CLICK: Socket creation failed.", e);
                    ServerConnect = false;
                    Message msg;
                    msg = mUIHandler.obtainMessage(1,"Bluetooth Connection Cannot Established");
                    mUIHandler.sendMessage(msg);
                }
            }
        }
    };

//    private boolean MACValidator(String IP) {
//        Pattern pat;
//        Matcher mat;
//        String rule = "^([0-9a-fA-F][0-9a-fA-F]:){5}([0-9a-fA-F][0-9a-fA-F])$";
//        pat = Pattern.compile(rule);
//        mat = pat.matcher(IP);
//
//        return mat.matches();
//
//    }

    protected void getSensor(){ //get sensor of the devices (smartphone)
        mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE)); //access this device sensor
        Sensor mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE); //get gyroscope data
        Sensor mAccelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); //accelero
        Sensor mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY); //gravity
        Sensor mRotationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR ); //rotation
        Sensor mMagSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD ); //magnetic
        Sensor mLinearAccelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION ); //linear acceleration
        if (mGyroSensor != null) {
            mSensorManager.registerListener(this, mGyroSensor, SensorDelayedRate); //all registerListener is to send sensor's data to the listener
            Log.w(TAG, "Gyroscope found");
            //mGyroExist.setText("Gyroscope found");
        } else {
            Log.w(TAG, "No Gyroscope found");
            //mGyroExist.setText("No Gyroscope found");
        }
        if (mAccelSensor != null) {
            mSensorManager.registerListener(this, mAccelSensor, SensorManager.SENSOR_DELAY_FASTEST);
            Log.w(TAG, "Accelerometer found");
            //mGyroExist.setText("Accelerometer found");
        } else {
            Log.w(TAG, "No Accelerometer found");
            //mGyroExist.setText("No Accelerometer found");
        }
        if (mGravitySensor != null) {
            mSensorManager.registerListener(this, mGravitySensor, SensorManager.SENSOR_DELAY_FASTEST);
            Log.w(TAG, "Gravity sensor found");
            //mGyroExist.setText("Gravity sensor found");
        } else {
            Log.w(TAG, "No Gravity sensor found");
            //mGyroExist.setText("No Gravity sensor found");
        }if (mRotationVector != null) {
            mSensorManager.registerListener(this, mRotationVector, SensorManager.SENSOR_DELAY_FASTEST);
            Log.w(TAG, "RotationVector sensor found");
            //mGyroExist.setText("Gravity sensor found");
        } else {
            Log.w(TAG, "No RotationVector sensor found");
            //mGyroExist.setText("No Gravity sensor found");
        }if (mMagSensor != null) {
            mSensorManager.registerListener(this, mMagSensor, SensorManager.SENSOR_DELAY_FASTEST);
            Log.w(TAG, "Magnito sensor found");
            //mGyroExist.setText("Gravity sensor found");
        } else {
            Log.w(TAG, "No Magnito sensor found");
            //mGyroExist.setText("No Gravity sensor found");
        }if (mLinearAccelSensor != null) {
            mSensorManager.registerListener(this, mLinearAccelSensor, SensorManager.SENSOR_DELAY_FASTEST);
            Log.w(TAG, "Linear Acceleration sensor found");
            //mGyroExist.setText("Gravity sensor found");
        } else {
            Log.w(TAG, "Linear Acceleration sensor found");
            //mGyroExist.setText("No Gravity sensor found");
        }
    }

//    private class AsyncTaskSendMessageToPC extends AsyncTask<String,Void,Void> {
//        @Override
//        protected Void doInBackground(String... s) {
//            try{
//                //Log.w(TAG, "Waitting to connect......");
//
//
//                //Log.w(TAG,"Connected!!");
//
//                //Log.w(TAG, "send");
//                //Log.i(TAG,"Ready to send:"+s[0]);
//                String msg = s[0] + "\r\n";
//
//                //sending messege for 0.5 second
//
//                BTPayload += msg;
//                BTPayloadCounter++;
//                if(BTPayloadCounter >= 10) {
//                    if(btSocket.isConnected()) {
//                        //btSocket.connect();
//                        //outStream = btSocket.getOutputStream();
//                        mUIHandler.sendEmptyMessage(0);
//                        byte[] npayload = BTPayload.getBytes();
//                        outStream.write(npayload); //write data into socket and send it to PC
//
//                        BTPayload="";
//                        BTPayloadCounter = 0;
//                        //btSocket.close();
//                        Log.e(TAG, "BT Payload sent.");
//                    }
//                }
//
//                /*
//                //sending message immediately
//                BTPayload = msg;
//                if(btSocket.isConnected()) {
//                    //btSocket.connect();
//                    //outStream = btSocket.getOutputStream();
//                    mUIHandler.sendEmptyMessage(0);
//                    byte[] npayload = BTPayload.getBytes();
//                    outStream.write(npayload);
//                }*/
//
//                //outStream.close();
//
//            }catch(Exception e) {
//                Log.w(TAG, "BT Error: " + e.getMessage());
//
//
//            }
//            return null;
//        }
//    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "onAccuracyChanged - accuracy: " + accuracy);
    }
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            GyroscopeValues[0] = event.values[0];
            GyroscopeValues[1] = event.values[1];
            GyroscopeValues[2] = event.values[2];
            //timestamp = System.currentTimeMillis();
            //new AsyncTaskSendMessageToPhone().execute();
            //Log.i(TAG,"Gyroscope onChanged:" + System.currentTimeMillis() + " , " + event.values[0]);
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            AccelerometerValues[0] = event.values[0];
            AccelerometerValues[1] = event.values[1];
            AccelerometerValues[2] = event.values[2];
            //timestamp = System.currentTimeMillis();
            //new AsyncTaskSendMessageToPhone().execute();
            //Log.i(TAG,"ACCELEROMETER onChanged:" + System.currentTimeMillis() + " , " + event.values[0]);
        }
        if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            GravityValues[0] = event.values[0];
            GravityValues[1] = event.values[1];
            GravityValues[2] = event.values[2];
            //timestamp = System.currentTimeMillis();
            //new AsyncTaskSendMessageToPhone().execute();
            //Log.i(TAG,"GravityValues onChanged:" + System.currentTimeMillis() + " , " + event.values[0]);
        }
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            RotationVectorValues[0] = event.values[0];
            RotationVectorValues[1] = event.values[1];
            RotationVectorValues[2] = event.values[2];
            //timestamp = System.currentTimeMillis();
            //new AsyncTaskSendMessageToPhone().execute();
            //Log.i(TAG,"Rotation onChanged:" + System.currentTimeMillis() + " , " + event.values[0]);
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            MagnitoValues[0] = event.values[0];
            MagnitoValues[1] = event.values[1];
            MagnitoValues[2] = event.values[2];
            //timestamp = System.currentTimeMillis();
            //new AsyncTaskSendMessageToPhone().execute();
            //Log.i(TAG,"Magnito onChanged:" + System.currentTimeMillis() + " , " + event.values[0]);
        }
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            LinearAccelerationValues[0] = event.values[0];
            LinearAccelerationValues[1] = event.values[1];
            LinearAccelerationValues[2] = event.values[2];
            //timestamp = System.currentTimeMillis();
            //new AsyncTaskSendMessageToPhone().execute();
            //Log.i(TAG," LinearAcceleration onChanged:" + System.currentTimeMillis() + " , " + event.values[0]);
        }
    }

    private String getSensorData()
    {
        SensorManager.getRotationMatrix(mR, null, AccelerometerValues, MagnitoValues);
        SensorManager.getOrientation(mR, OrientationValues);
        //rotaionMatrixLength = mR.length;
        //Log.w(TAG, "Rotation Matrix Length:"+rotaionMatrixLength);
        String s = "," + GyroscopeValues[0] + "," + GyroscopeValues[1] + "," + GyroscopeValues[2] + ","
                + AccelerometerValues[0] + "," + AccelerometerValues[1] + "," + AccelerometerValues[2] +","
                + GravityValues[0] + "," + GravityValues[1] + "," + GravityValues[2] + ","
                + RotationVectorValues[0] + "," + RotationVectorValues[1] + "," + RotationVectorValues[2] + ","
                + OrientationValues[0] + "," + OrientationValues[1] + "," + OrientationValues[2] + ","
                + LinearAccelerationValues[0] + "," + LinearAccelerationValues[1] + "," + LinearAccelerationValues[2] + ","
                + MagnitoValues[0] + "," + MagnitoValues[1] + "," + MagnitoValues[2] + ","
                + mR[0]  + "," + mR[1]  + "," + mR[2]  + "," + mR[3]  + "," + mR[4]  + ","
                + mR[5]  + "," + mR[6]  + "," + mR[7]  + "," + mR[8];
        return s;
    }
    private class AsyncTaskSendMessage extends AsyncTask<String,Void,Void> {
        @Override
        protected Void doInBackground(String... s) {
            byte[] payload = s[0].getBytes();
            NodeApi.GetConnectedNodesResult connectedPhone = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
            for(Node node : connectedPhone.getNodes()) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                        mGoogleApiClient, node.getId(), WEARABLE_PATH_MASSAGE, payload).await();

                if (result.getStatus().isSuccess())
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.w(TAG, "Send Message successful");
                            //Log.w(TAG, msgS);
                        }
                    });
                else
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.w(TAG, "Send Message failed");
                        }
                    });

            }
            return null;
        }
    }
}

