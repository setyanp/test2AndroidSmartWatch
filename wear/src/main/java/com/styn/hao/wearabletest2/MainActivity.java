package com.styn.hao.wearabletest2;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements SensorEventListener {

    private static final String TAG = "MainActivity";
    private static final int GOO_API_CLIENT_REQUEST_RESOLVE_ERROR = 1000;
    private GoogleApiClient mGoogleApiClient;
    private boolean mbResolvingGooApiClientError = false;
    private boolean GETNODE = false;
    private int aliveCounter = 0;
    private Long timestamp;
    private TextView mTextms;
    public static String msgS;
    protected  SensorManager mSensorManager;

    /** variable for handler message */
    private static final int ID_ESTABLISH_CONNECTION = 0;
    private static final int ID_RECEIVED_TIME_REQUEST = 1;
    private static final int ID_RESPONSE_TIME = 2;
    private static final int ID_HEARTBEAT = 3;
    private static final int ID_TERMINATE = 4;
    private static final int ID_SENSOR_DATA = 5;

    /** variable for sensors data */
    private float[] GyroscopeValues =  {0.0f, 0.0f, 0.0f};
    private float[] AccelerometerValues =  {0.0f, 0.0f, 0.0f};
    private float[] GravityValues =  {0.0f, 0.0f, 0.0f};
    private float[] mR = new float[9];
    private float[] OrientationValues = {0.0f, 0.0f, 0.0f};
    private float[] RotationVectorValues = {0.0f, 0.0f, 0.0f};
    private float[] MagnitoValues = {0.0f, 0.0f, 0.0f};
    private float[] LinearAccelerationValues = {0.0f, 0.0f, 0.0f};

    private Long offset = (long)0;
    public Handler mHandler = new Handler();
    private NodeApi.GetConnectedNodesResult connectedPhone;
    private Node phoneNode;
    private Boolean connection;
    private Long connectionID;
    private Long caliburationStart;
    private Pattern msgPattern;
    private Pattern respPattern;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub); //create stub for UI application
        //msgS = new String("");

        mGoogleApiClient = new GoogleApiClient.Builder(this) //build api to get sensor data and communication
                .addApi(Wearable.API)
                .addConnectionCallbacks(gooApiClientConnCallback)
                .addOnConnectionFailedListener(gooApiClientOnConnFail)
                .build();

        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                /*mGyroExist = (TextView) stub.findViewById(R.id.gyroExist);
                mGyroX = (TextView) stub.findViewById(R.id.gyroX);
                mGyroY = (TextView) stub.findViewById(R.id.gyroY);
                mGyroZ = (TextView) stub.findViewById(R.id.gyroZ);*/
                mTextms = (TextView) stub.findViewById(R.id.TextMs);
                //getSensor();
                //mHandlerTask.run();
            }
        });
        msgPattern = Pattern.compile("([0-9]):(.*)"); //create pattern for sensor data message, to get number followed by colon
        respPattern = Pattern.compile("([0-9]*),([0-9]*)"); //create pattern for response message
        connection = false;
    }

    @Override
    protected void onPause() {
        //mMessageHandler.sendEmptyMessage(ID_TERMINATE);
        if (!mbResolvingGooApiClientError) {
            mGoogleApiClient.disconnect();
        }
        //unregister
        mSensorManager.unregisterListener(this);
        Log.i(TAG,"REMOVE LISTENER");
        Wearable.MessageApi.removeListener(mGoogleApiClient, wearableMsgListener);
        mHandler.removeCallbacks(mHandlerTask);
        connection = false;

        super.onPause();

    }

    @Override
    protected void onStop() {
        if (!mbResolvingGooApiClientError) {
            mGoogleApiClient.disconnect();
        }
        mSensorManager.unregisterListener(this);
        mHandler.removeCallbacks(mHandlerTask);
        Wearable.MessageApi.removeListener(mGoogleApiClient, wearableMsgListener);
        Log.i(TAG,"ONSTOP");
        super.onStop();
    }

    @Override
    protected void onResume() {

        getSensor(); //call method getSensor() to get sensor data
        if(!mbResolvingGooApiClientError) {
            mGoogleApiClient.connect(); //connect to google services in the background
            Wearable.MessageApi.addListener(mGoogleApiClient, wearableMsgListener);
            Log.i(TAG,"ONRESUME");
        }
        //mHandlerTask.run();
        super.onResume();
    }

    /**  called when the client is connected or disconnected from the service */
    private GoogleApiClient.ConnectionCallbacks gooApiClientConnCallback = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            mbResolvingGooApiClientError = false;
        }
        @Override
        public void onConnectionSuspended(int i) {
            Log.w(TAG, "Google API Client connection failed.");
        }
    };

    /** Provides callbacks for scenarios that result in a failed attempt to connect the client to the service */
    private GoogleApiClient.OnConnectionFailedListener gooApiClientOnConnFail = new GoogleApiClient.OnConnectionFailedListener(){
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            if (mbResolvingGooApiClientError) {
                return;
            } else {
                mbResolvingGooApiClientError = false;
            }
        }
    };

    protected void getSensor(){ //method to get sensor data from smartwatches
        mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));
        Sensor mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor mAccelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        Sensor mRotationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR );
        Sensor mMagSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD );
        Sensor mLinearAccelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION );
        if (mGyroSensor != null) {
            /** "this" referred to onSensorChange and onAccuracyChange */
            mSensorManager.registerListener(this, mGyroSensor,20000); //get sensor data at 20Hz = 20000 (?)
            Log.w(TAG, "Gyroscope found");
            //mGyroExist.setText("Gyroscope found");
        } else {
            Log.w(TAG, "No Gyroscope found");
            //mGyroExist.setText("No Gyroscope found");
        }
        if (mAccelSensor != null) {
            mSensorManager.registerListener(this, mAccelSensor, 20000);
            Log.w(TAG, "Accelerometer found");
            //mGyroExist.setText("Accelerometer found");
        } else {
            Log.w(TAG, "No Accelerometer found");
            //mGyroExist.setText("No Accelerometer found");
        }
        if (mGravitySensor != null) {
            mSensorManager.registerListener(this, mGravitySensor, 20000);
            Log.w(TAG, "Gravity sensor found");
            //mGyroExist.setText("Gravity sensor found");
        } else {
            Log.w(TAG, "No Gravity sensor found");
            //mGyroExist.setText("No Gravity sensor found");
        }if (mRotationVector != null) {
            mSensorManager.registerListener(this, mRotationVector, 20000);
            Log.w(TAG, "RotationVector sensor found");
            //mGyroExist.setText("Gravity sensor found");
        } else {
            Log.w(TAG, "No RotationVector sensor found");
            //mGyroExist.setText("No Gravity sensor found");
        }if (mMagSensor != null) {
            mSensorManager.registerListener(this, mMagSensor, 20000);
            Log.w(TAG, "Magnito sensor found");
            //mGyroExist.setText("Gravity sensor found");
        } else {
            Log.w(TAG, "No Magnito sensor found");
            //mGyroExist.setText("No Gravity sensor found");
        }if (mLinearAccelSensor != null) {
            mSensorManager.registerListener(this, mLinearAccelSensor, 20000);
            Log.w(TAG, "Linear Acceleration sensor found");
            //mGyroExist.setText("Gravity sensor found");
        } else {
            Log.w(TAG, "Linear Acceleration sensor found");
            //mGyroExist.setText("No Gravity sensor found");
        }
    }

    private static final String WEARABLE_PATH_MASSAGE = "/message";

    private Runnable mHandlerTask = new Runnable()
    {
        @Override
        public void run() {
            if(true) {
                //Log.i(TAG,"Ready To Send Data");
                Long nowtime = System.currentTimeMillis() + offset;
                String sendingMsg = "5:" + nowtime + getSensorData();

                AsyncTaskSendMessage at = new AsyncTaskSendMessage();
                at.execute(sendingMsg);
                //mTextms.setText("Millisecond: "+millisec);
                //Log.i(TAG,"NOWTIME: "+nowtime);
                aliveCounter--;
                //Log.i(TAG,"alive counter : " + aliveCounter);
                mHandler.postDelayed(mHandlerTask, 20);

            }
            else{
               // connection = false;
                //mHandler.removeCallbacks(mHandlerTask);
                Log.i(TAG, "TIMEOUT:AliVECOUNTER = " + aliveCounter);
            }
        }
    };


    private class AsyncTaskSendMessage extends AsyncTask<String,Void,Void> {
        @Override
        protected Void doInBackground(String... s) {
            byte[] payload = s[0].getBytes();
            connectedPhone = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await(); //get list of connected devices with smartwatch
            for(Node node : connectedPhone.getNodes()) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                        mGoogleApiClient, node.getId(), WEARABLE_PATH_MASSAGE, payload).await();

                if (result.getStatus().isSuccess())
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Log.w(TAG, "Send Message successful");
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


    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "onAccuracyChanged - accuracy: " + accuracy);
    }
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            GyroscopeValues[0] = event.values[0];
            GyroscopeValues[1] = event.values[1];
            GyroscopeValues[2] = event.values[2];
            //new AsyncTaskSendMessageToPhone().execute();
            //Log.i(TAG,"GYROSCOPE:"+System.nanoTime()+","+  GyroscopeValues[0]+","+ GyroscopeValues[1]+","+ GyroscopeValues[2]);
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            AccelerometerValues[0] = event.values[0];
            AccelerometerValues[1] = event.values[1];
            AccelerometerValues[2] = event.values[2];
            //new AsyncTaskSendMessageToPhone().execute();
            //Log.i(TAG,"ACCELEROMETER:"+System.nanoTime()+","+ AccelerometerValues[0]+","+ AccelerometerValues[1]+","+ AccelerometerValues[2]);
        }
        if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            GravityValues[0] = event.values[0];
            GravityValues[1] = event.values[1];
            GravityValues[2] = event.values[2];
            //new AsyncTaskSendMessageToPhone().execute();
            //Log.i(TAG,"GRAVITY:"+System.nanoTime());
        }
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            RotationVectorValues[0] = event.values[0];
            RotationVectorValues[1] = event.values[1];
            RotationVectorValues[2] = event.values[2];
            //new AsyncTaskSendMessageToPhone().execute();
            //Log.i(TAG,"ROTATION_VECTOR:"+System.nanoTime());
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            MagnitoValues[0] = event.values[0];
            MagnitoValues[1] = event.values[1];
            MagnitoValues[2] = event.values[2];
            //new AsyncTaskSendMessageToPhone().execute();
            //Log.i(TAG,"MAGNETIC_FIELD:"+System.nanoTime());
        }
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            LinearAccelerationValues[0] = event.values[0];
            LinearAccelerationValues[1] = event.values[1];
            LinearAccelerationValues[2] = event.values[2];
            //new AsyncTaskSendMessageToPhone().execute();
            //Log.i(TAG,"LINEAR_ACCELERATION:"+System.nanoTime());
        }


    }

    private MessageApi.MessageListener wearableMsgListener = new MessageApi.MessageListener() {
        @Override
        public void onMessageReceived(MessageEvent messageEvent) {
            Long receivedTime = System.currentTimeMillis();

            byte [] payload = messageEvent.getData();
            Log.i(TAG,"Recieved: " + new String(payload));
            Matcher msgMatcher = msgPattern.matcher(new String(payload));
            msgMatcher.matches();
            int msgIDType = Integer.parseInt(msgMatcher.group(1));
            switch (msgIDType) {
                case ID_ESTABLISH_CONNECTION:
                    if(!connection){
                        connectionID = Long.parseLong(msgMatcher.group(2));
                        connection = true;
                        caliburationStart = receivedTime;
                        Log.i(TAG,"CONNECTION ESTABLISHED");
                        mMessageHandler.sendEmptyMessage(ID_RECEIVED_TIME_REQUEST);
                    }
                    break;
                case ID_HEARTBEAT:
                    if(connection) aliveCounter += 150;
                    Log.i(TAG,"RECIEVED HEARTBEAT");
                    break;
                case ID_RESPONSE_TIME:
                    if(connection){
                        //START Sending message
                        Matcher caliMatcher = respPattern.matcher(msgMatcher.group(2));
                        caliMatcher.matches();
                        Long serverResponseTime = Long.parseLong(caliMatcher.group(2)) - Long.parseLong(caliMatcher.group(1));
                        Long timelag = ((receivedTime-caliburationStart) - serverResponseTime)/2;
                        offset = (Long.parseLong(caliMatcher.group(2)) + timelag)-System.currentTimeMillis();
                        Log.i(TAG,"TIME: " + offset);
                        aliveCounter = 150;

                        mHandlerTask.run(); //task to send message to smartphone
                    }
                    break;
                case ID_TERMINATE:
                    if(connection){
                        connection = false;
                        mHandler.removeCallbacks(mHandlerTask);
                        Log.i(TAG, "TERMINATE: " + connectionID);
                    }
                    break;
            }

            return;
        }
    };

    private String getSensorData()
    {
        SensorManager.getRotationMatrix(mR, null, AccelerometerValues, MagnitoValues);
        SensorManager.getOrientation(mR, OrientationValues);
        String s = "," + GyroscopeValues[0] + "," + GyroscopeValues[1] + "," + GyroscopeValues[2] + ","
                + AccelerometerValues[0] + "," + AccelerometerValues[1] + "," + AccelerometerValues[2] +","
                + GravityValues[0] + "," + GravityValues[1] + "," + GravityValues[2] + ","
                + RotationVectorValues[0] + "," + RotationVectorValues[1] + "," + RotationVectorValues[2] + ","
                + OrientationValues[0] + "," + OrientationValues[1] + "," + OrientationValues[2] + ","
                + LinearAccelerationValues[0] + "," + LinearAccelerationValues[1] + "," + LinearAccelerationValues[2] + ","
                + MagnitoValues[0] + "," + MagnitoValues[1] + "," + MagnitoValues[2] + ","
                + mR[0]  + "," + mR[1]  + "," + mR[2]  + "," + mR[3]  + "," + mR[4]  + ","
                + mR[5]  + "," + mR[6]  + "," + mR[7]  + "," + mR[8];
        Log.i("sensor", s);
        return s;
    }

    private Handler mMessageHandler = new Handler() //Wearable Side
    {
        @Override
        public void handleMessage(Message msg)
        {
            byte[] payload = new byte[0];
            String sendingMsg = new String();
            switch (msg.what)
            {

                case ID_TERMINATE:
                    Log.i(TAG,"Sending Terminate Message");
                    sendingMsg = "4:" + connectionID;
                    payload = sendingMsg.getBytes();
                    break;
                case ID_RECEIVED_TIME_REQUEST:
                    Log.i(TAG,"send time request");
                    sendingMsg = "1:" + System.currentTimeMillis();
                    payload = sendingMsg.getBytes();
                    break;
                case ID_SENSOR_DATA:
                    sendingMsg = "5:" + (System.currentTimeMillis()+offset);
                    payload = sendingMsg.getBytes();
                    break;
            }
            AsyncTaskSendMessage at = new AsyncTaskSendMessage();
            at.execute(sendingMsg);


        }
    };

}
