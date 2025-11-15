package com.example.myapplication;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity
{

    private EditText hostEdit, userEdit, passEdit;
    private CheckBox rememberCheck;
    private Button connectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        hostEdit = findViewById(R.id.hostEdit);
        userEdit = findViewById(R.id.userEdit);
        passEdit = findViewById(R.id.passEdit);
        rememberCheck = findViewById(R.id.rememberCheck);
        connectButton = findViewById(R.id.connectButton);

        // Load saved login, if any
        loadSavedLogin();

        connectButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                String host = hostEdit.getText().toString().trim();
                String user = userEdit.getText().toString().trim();
                String pass = passEdit.getText().toString().trim();

                if (host.isEmpty() || user.isEmpty() || pass.isEmpty())
                {
                    Toast.makeText(LoginActivity.this, "All fields required", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (rememberCheck.isChecked())
                {
                    saveLogin(host, user, pass);
                } else
                {
                    clearSavedLogin();
                }

                // Go to main screen
                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                i.putExtra("host", host);
                i.putExtra("user", user);
                i.putExtra("pass", pass);
                startActivity(i);
            }
        });
    }

    private void saveLogin(String host, String user, String pass)
    {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("host", host);
        editor.putString("user", user);
        editor.putString("pass", pass);
        editor.putBoolean("remember", true);
        editor.apply();
    }

    private void loadSavedLogin()
    {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);

        boolean remember = prefs.getBoolean("remember", false);

        if (remember) {
            hostEdit.setText(prefs.getString("host", ""));
            userEdit.setText(prefs.getString("user", ""));
            passEdit.setText(prefs.getString("pass", ""));
            rememberCheck.setChecked(true);
        }
    }

    private void clearSavedLogin()
    {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }
}
