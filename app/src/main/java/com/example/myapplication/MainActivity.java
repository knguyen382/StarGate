package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import com.jcraft.jsch.Session;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import com.jcraft.jsch.*;

import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    EditText hostInput, userInput, passInput, cmdInput;
    Button connectBtn, sendBtn;
    TextView outputView;
    ScrollView scrollView;
    Session session;
    OutputStream out;
    Spinner mainSpinner;
    LinearLayout dynamicContainer;
    boolean connected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hostInput = findViewById(R.id.hostInput);
        userInput = findViewById(R.id.userInput);
        passInput = findViewById(R.id.passInput);
        cmdInput = findViewById(R.id.cmdInput);
        connectBtn = findViewById(R.id.connectBtn);
        sendBtn = findViewById(R.id.sendBtn);
        outputView = findViewById(R.id.outputView);
        scrollView = findViewById(R.id.scrollView);

        mainSpinner = findViewById(R.id.mainSpinner);
        dynamicContainer = findViewById(R.id.dynamicContainer);

        String[] options = {"None", "Package A", "Package B", "Package C"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mainSpinner.setAdapter(adapter);

        mainSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = (String) parent.getItemAtPosition(position);

                // Clear previous dynamic views
                dynamicContainer.removeAllViews();

                if (selected.equals("Package A")) {
                    // Add 3 EditTexts dynamically
                    for (int i = 1; i <= 3; i++) {
                        EditText et = new EditText(MainActivity.this);
                        et.setHint("Input " + i);
                        et.setTextColor(Color.WHITE);
                        et.setBackgroundColor(Color.parseColor("#222222"));
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        params.setMargins(0, 8, 0, 0);
                        et.setLayoutParams(params);
                        dynamicContainer.addView(et);
                    }

                    // Add secondary spinner
                    Spinner secondarySpinner = new Spinner(MainActivity.this);

// Set spinner options
                    String[] secondaryOptions = {"Option 1", "Option 2", "Option 3"};
                    ArrayAdapter<String> secondaryAdapter = new ArrayAdapter<>(MainActivity.this,
                            android.R.layout.simple_spinner_item, secondaryOptions) {
                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            TextView tv = (TextView) super.getView(position, convertView, parent);
                            tv.setTextColor(Color.WHITE); // white text
                            return tv;
                        }

                        @Override
                        public View getDropDownView(int position, View convertView, ViewGroup parent) {
                            TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                            tv.setTextColor(Color.WHITE); // white text in dropdown
                            return tv;
                        }
                    };
                    secondaryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    secondarySpinner.setAdapter(secondaryAdapter);

// Grey background
                    secondarySpinner.setBackgroundColor(Color.parseColor("#555555"));

// Layout params
                    LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    spinnerParams.setMargins(0, 8, 0, 0);
                    secondarySpinner.setLayoutParams(spinnerParams);

// Add to container
                    dynamicContainer.addView(secondarySpinner);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
            });

        connectBtn.setOnClickListener(v -> startSSH());
        sendBtn.setOnClickListener(v -> sendCommand());
    }

    private void startSSH() {
        new Thread(() -> {
            try {
                String host = hostInput.getText().toString().trim();
                String user = userInput.getText().toString().trim();
                String pass = passInput.getText().toString().trim();

                appendOutput("Connecting to " + host + "...\n");

                JSch jsch = new JSch();
                session = jsch.getSession(user, host, 22);
                session.setPassword(pass);
                session.setConfig("StrictHostKeyChecking", "no");
                session.connect(5000);

                ChannelShell channel = (ChannelShell) session.openChannel("shell");
                InputStream in = channel.getInputStream();
                out = channel.getOutputStream();
                channel.connect();

                connected = true;
                appendOutput("Connected!\n");

                byte[] buf = new byte[1024];
                int len;
                while (connected && (len = in.read(buf)) != -1) {
                    appendOutput(new String(buf, 0, len));
                }

            } catch (Exception e) {
                appendOutput("Error: " + e.getMessage() + "\n");
            } finally {
                disconnectSSH();
            }
        }).start();
    }

    private void sendCommand() {
        if (!connected || out == null) {
            appendOutput("Not connected.\n");
            return;
        }
        try {
            String cmd = cmdInput.getText().toString().trim() + "\n";
            out.write(cmd.getBytes());
            out.flush();
            runOnUiThread(() -> cmdInput.setText(""));
        } catch (Exception e) {
            appendOutput("Failed: " + e.getMessage() + "\n");
        }
    }

    private void appendOutput(String text) {
        runOnUiThread(() -> {
            outputView.append(text);
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }

    private void disconnectSSH() {
        try {
            connected = false;
            if (session != null && session.isConnected()) session.disconnect();
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectSSH();
    }
    }
