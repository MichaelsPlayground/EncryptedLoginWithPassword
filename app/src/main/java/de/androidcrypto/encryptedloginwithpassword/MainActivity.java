package de.androidcrypto.encryptedloginwithpassword;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button login, settings;
    Intent loginIntent, settingsIntent;

    boolean isAppPasswordSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        login = findViewById(R.id.btnMainLogin);
        loginIntent = new Intent(MainActivity.this, LoginActivity.class);
        settings = findViewById(R.id.btnMainSettings);
        settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);

        // setup the encryptedSharedPreferencesUtility
        EncryptedSharedPreferencesUtils.setupEncryptedSharedPreferences(getApplicationContext());

        // checking that the appPassword is set
        isAppPasswordSet = EncryptedSharedPreferencesUtils.getAppPasswordStatus();
        if (!isAppPasswordSet) {
            startActivity(settingsIntent);
        } else {
            System.out.println("*** app password is set, proceed to login");
            startActivity(loginIntent);
        }

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(loginIntent);
            }
        });

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(settingsIntent);
            }
        });
    }


}