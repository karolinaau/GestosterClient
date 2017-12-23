package pl.edu.uj.gestoster;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by Karolinka on 15.09.2017.
 */
public class NewActivity extends AppCompatActivity implements SensorEventListener, android.view.View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    // konfiguracja hosta docelowego
    private String host_ip;
    private static final int HOST_PORT = 8189;

    private SensorManager mSensorManager;
    private Sensor linearAcceleration;
    private Sensor rv;
    private TextView TVyaw;
    private TextView TVroll;
    private TextView TVpitch;
    private TextView TVax;
    private TextView TVay;
    private TextView TVaz;
    private Socket socket;
    private PrintWriter pw;
    public int kontrolType = 0;

    // Reset YAW
    Button button;
    Switch mouseChange = null;

    //    float[] inclineGravity = new float[3];
    float[] mmrv = null;
    float[] mmrvLast = null;

    //    float orientation[] = new float[3];
    double pitch;
    double roll;
    double yaw;

    //    float accuracy = 0.80f;
    double lastYaw;
    double lastPitch;
    double lastRoll;
    double firstYaw;
    boolean first = true;

    //	private final int sensorType =  Sensor.TYPE_ROTATION_VECTOR;
    float[] rotMat = new float[9];
    float[] vals = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new);

        TVyaw = (TextView) findViewById(R.id.yaw);
        TVroll = (TextView) findViewById(R.id.roll);
        TVpitch = (TextView) findViewById(R.id.pitch);

        TVax = (TextView) findViewById(R.id.ax);
        TVay = (TextView) findViewById(R.id.ay);
        TVaz = (TextView) findViewById(R.id.az);

        Intent intent = getIntent();
        host_ip = intent.getStringExtra(MainActivity.IP_MESSAGE);

        button = (Button) findViewById(R.id.button);
        mouseChange = (Switch) findViewById(R.id.swich1);
        mouseChange.setOnCheckedChangeListener(this);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        linearAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        rv = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        // rv = mSensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);

        mouseChange.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        if (isChecked) {
            kontrolType = 1;
        } else {
            kontrolType = 0;
        }

    }

    public void initListeners() {
        mSensorManager.registerListener(this, linearAcceleration, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, rv, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        mSensorManager.unregisterListener(this);
        super.onBackPressed();
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.e("d", "startConnectionBefore");


        Thread connectionThread = new Thread(new ClientThread());
        connectionThread.start();

        try {
            connectionThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.e("d", "startConnectionAfter");

        initListeners();

        Log.e("d", "exitOnResume");

    }

    @Override
    protected void onPause() {
        mSensorManager.unregisterListener(this);
        super.onPause();

        try {
            if (socket != null) socket.close();
            if (pw != null) pw.close();
        } catch (IOException e) {
            Log.d("d", "IOException");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        //If type is accelerometer only assign values to global property mGravity
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotMat, event.values);

            SensorManager.getOrientation(rotMat, vals);
            yaw = Math.toDegrees(vals[0]); // w stopniach [-180, +180]
            pitch = Math.toDegrees(vals[1]);
            roll = Math.toDegrees(vals[2]);

            //zmiana zakresu [0, 360]
            if (yaw < 0) {
                yaw = yaw + 360;
            }

            if (first) {
                firstYaw = yaw;
                lastYaw = 0;
                lastPitch = pitch;
                lastRoll = roll;
                first = false;

            } else {
                float a = 0.8f;
                //transformujemy układ, wskazane yaw = 0
                yaw = yaw - firstYaw;
                yaw = lastYaw + yaw * a - lastYaw * a;
                pitch = lastPitch + pitch * a - lastPitch * a;
                roll = lastRoll + roll * a - lastRoll * a;
            }

            TVyaw.setText(String.valueOf(yaw));
            TVpitch.setText(String.valueOf(pitch));
            TVroll.setText(String.valueOf(roll));
        }

        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            mmrv = event.values;
            float a = 0.6f;

            if (mmrvLast != null) {
                mmrv[0] = mmrvLast[0] + event.values[0] * a - mmrvLast[0] * a;
                mmrv[1] = mmrvLast[1] + event.values[1] * a - mmrvLast[1] * a;
                mmrv[2] = mmrvLast[2] + event.values[2] * a - mmrvLast[2] * a;

                TVax.setText(String.valueOf((int) (mmrv[0])));
                TVay.setText(String.valueOf((int) (mmrv[1])));
                TVaz.setText(String.valueOf((int) (mmrv[2])));
            }

            mmrvLast = mmrv;

            pw.println(kontrolType + " , " + mmrv[0] + " ," + mmrv[1] + " ," + mmrv[2] + " ," + yaw + " ," + pitch + " ," + roll);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onClick(View v) {
        if (v == button) {
            first = true;
        }
    }

    private class ClientThread implements Runnable {
        public void run() {

            Log.d("d", "newThreat_socket1");
            try {
                Log.e("d", "insideStartConnectionThread!!!!!!!!!!!!!!!!!!!!!");

                socket = new Socket(host_ip, HOST_PORT);
                pw = new PrintWriter(socket.getOutputStream(), true);

                Log.e("d", "hello from Android");

            } catch (UnknownHostException exception) {
                Log.e("e", "unknowHost");
                System.exit(1);
            } catch (IOException exception) {
                Log.e("e", "IOException");
                System.exit(1);
            } catch (Exception exception) {
                exception.printStackTrace();
                System.exit(0);
            }
        }
    }
}