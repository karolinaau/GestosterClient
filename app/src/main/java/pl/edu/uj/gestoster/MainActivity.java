package pl.edu.uj.gestoster;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity implements android.view.View.OnClickListener {
    public static final String IP_MESSAGE = "pl.edu.uj.gestoster.IP";

    private EditText ip;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = (Button) findViewById(R.id.button);
        ip = (EditText) findViewById(R.id.ip);
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    public void onClick(View v) {
        if (v == button) {
            Intent intent = new Intent(MainActivity.this, NewActivity.class);

            String message = ip.getText().toString();
            intent.putExtra(IP_MESSAGE, message);

            // Launch the Activity using the intent
            startActivity(intent);
        }
    }
}
