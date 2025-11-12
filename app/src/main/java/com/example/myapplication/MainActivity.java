package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.jcraft.jsch.Session;
import android.widget.Button;
import android.widget.EditText;
import com.jcraft.jsch.*;
import android.widget.ScrollView;
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
