package com.example.myapplication;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class MainActivity extends AppCompatActivity {

    private Spinner mainSpinner;
    private Button disconnectBtn;
    private Button sendBtn;
    private Button spaceBtn;
    private Button upBtn;
    private EditText cmdInput;
    private TextView terminalOutput;
    private ScrollView terminalScroll;

    // SSH
    private Session session = null;
    private ChannelShell channel = null;
    private OutputStream commandOut = null;
    private InputStream commandIn = null;
    private volatile boolean reading = false;

    // Saved popup inputs
    private String savedPackageName = "";
    private String savedSecondaryOption = "";
    private String savedField1 = "";
    private String savedField2 = "";
    private String savedField3 = "";

    private String cliArgsRadio = "CRA";
    private String cmdToSend = null;
    private boolean isManualInput = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainSpinner = (Spinner) findViewById(R.id.mainSpinner);
        disconnectBtn = (Button) findViewById(R.id.disconnectBtn);
        sendBtn = (Button) findViewById(R.id.sendBtn);
        spaceBtn = (Button) findViewById(R.id.spaceBtn);
        cmdInput = (EditText) findViewById(R.id.cmdInput);
        terminalOutput = (TextView) findViewById(R.id.terminalOutput);
        terminalScroll = (ScrollView) findViewById(R.id.terminalScroll);
        spaceBtn.setBackgroundColor(Color.parseColor("#00FF00"));

        setupSpinner();
        setupButtons();
    }

    private void setupSpinner() {
        String[] options = new String[] { "Select a Script", "Package A", "Package B", "Package C", "Manual Input"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mainSpinner.setAdapter(adapter);

        mainSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = (String) parent.getItemAtPosition(position);

                if ("Select a Script".equals(selected))
                {
                    // go back to home directory
                    sendCommand("cd ~");
                    appendToTerminal("Please select a script ⬆\n");
                    cmdToSend = null;
                    isManualInput = false;
                }
                else if ("Manual Input".equals(selected))
                {
                    cmdInput.setVisibility(VISIBLE);
                    sendBtn.setText("SEND");
                    isManualInput = true;
                }
                else
                {
                    cmdInput.setVisibility(GONE);
                    isManualInput = false;
                    sendBtn.setText("LAUNCH");
                    // For Package A/B/C: show popup and on OK cd into matching folder
                    showPackagePopup(selected);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void setupButtons() {
        disconnectBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                disconnectSSH();
                goToLogin();
            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (cmdToSend != null)
                {
                    String cmd = cmdToSend;
                    if (cmd == null) cmd = "";
                    sendCommand(cmd);
                    cmdInput.setText("");
                }
                else
                {
                    if (!isManualInput)
                    {
                        appendToTerminal("No Script Selected\n");
                    }
                }
            }
        });

        spaceBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                sendRaw(" "); // a space character (no newline)
            }
        });
    }

    private void disconnectSSH()
    {
        try
        {
            reading = false;
            if (channel != null && channel.isConnected())
            {
                channel.disconnect();
            }
            if (session != null && session.isConnected())
            {
                session.disconnect();
            }
        } catch (Exception ignored) {}
        session = null;
        channel = null;
        commandOut = null;
        commandIn = null;
    }

    private void sendCommand(final String cmd)
    {
        if (commandOut == null || session == null || !session.isConnected()) {
            return;
        }
        final String toSend = cmd + "\n";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    commandOut.write(toSend.getBytes());
                    commandOut.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            appendToTerminal("\nSend failed\n");
                        }
                    });
                }
            }
        }).start();
    }

    private void sendRaw(final String raw) {
        if (commandOut == null || session == null || !session.isConnected()) {
            appendToTerminal("\nNot connected\n");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    commandOut.write(raw.getBytes());
                    commandOut.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            appendToTerminal("\nSend failed\n");
                        }
                    });
                }
            }
        }).start();
    }

    private void startReading()
    {
        if (reading || commandIn == null) return;
        reading = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final byte[] buffer = new byte[2048];
                    int read;
                    while (reading && (read = commandIn.read(buffer)) != -1) {
                        final String out = new String(buffer, 0, read);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                appendToTerminal(out);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            appendToTerminal("Read loop ended\n");
                        }
                    });
                } finally {
                    reading = false;
                }
            }
        }).start();
    }

    private void appendToTerminal(String text) {
        terminalOutput.append("[STARGATE]" + text);
        terminalScroll.post(new Runnable() {
            @Override
            public void run() {
                terminalScroll.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    // Show popup when Package A/B/C selected; save inputs on OK and cd into matching folder.
    private void showPackagePopup(final String packageName) {
        final Activity act = this;
        LayoutInflater inflater = LayoutInflater.from(act);
        final View view = inflater.inflate(R.layout.popup_package, null);

        final Spinner secondarySpinner = (Spinner) view.findViewById(R.id.secondarySpinner);
        final EditText fld1 = (EditText) view.findViewById(R.id.editField1);
        final EditText fld2 = (EditText) view.findViewById(R.id.editField2);
        final EditText fld3 = (EditText) view.findViewById(R.id.editField3);

        // Fill secondary spinner with example entries
        String[] secOptions = new String[] { "Opt 1", "Opt 2", "Opt 3" };
        ArrayAdapter<String> secAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, secOptions);
        secAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        secondarySpinner.setAdapter(secAdapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(packageName + " Settings");
        builder.setView(view);
        builder.setPositiveButton("OK", null); // override later
        builder.setNegativeButton("Cancel", null);
        final AlertDialog dialog = builder.create();
        dialog.show();

        // Override OK to prevent auto-dismiss until we save
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                savedPackageName = packageName;
                savedSecondaryOption = secondarySpinner.getSelectedItem().toString();
                savedField1 = fld1.getText().toString().trim();
                savedField2 = fld2.getText().toString().trim();
                savedField3 = fld3.getText().toString().trim();

                // Locate selected script folder
                sendCommand(getLocateScriptFolderCmd(savedPackageName));

                appendToTerminal(" Applied " + packageName + " Arguments: " +
                        "agrs1=" + savedSecondaryOption +
                        " agrs2=" + savedField1 + " agrs3=" + savedField2 + " agrs4=" + savedField3 + "\n");

                dialog.dismiss();
            }
        });
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        disconnectSSH();
    }

    private void goToLogin()
    {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

//    private String buildScriptCmd()
//    {
//
//    }

    private String getLocateScriptFolderCmd(String script)
    {
        String pattern = "*" + script + "*";
        return "cd \"$(find ~ -maxdepth 1 -type d -name '" + pattern + "' | head -n 1)\" || cd ~";
    }
}
