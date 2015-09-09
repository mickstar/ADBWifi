package com.mickstarify.adbwifi;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;

import static com.mickstarify.adbwifi.R.id.lbl_connect_help;

public class MainActivity extends AppCompatActivity {

    public enum States {
        ACTIVE, INACTIVE, INTRANSITION, FAILED
    }

    private enum ShellOperation {
        CHECK_ACTIVE, CHECK_ACTIVE_QUIET, START, STOP
    }

    public static States state = States.INACTIVE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SU_Executor executor = new SU_Executor();
        executor.execute(ShellOperation.CHECK_ACTIVE);

        Button btn_adb_toggle = (Button) findViewById(R.id.btn_toggleADB);
        btn_adb_toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleState();
            }
        });
    }

    private void checkRootPrivileges() {
        if (!Shell.SU.available()) {
            new AlertDialog.Builder(this)
                    .setTitle("Root Privileges")
                    .setMessage("This application requires root privileges.")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            closeApplication();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    //returns whether or not we are connected to a wifi network.
    private boolean checkWifiState(){
        WifiManager mainWifi = (WifiManager) getSystemService(this.WIFI_SERVICE);
        if (mainWifi.isWifiEnabled()) {
            WifiInfo currentWifi = mainWifi.getConnectionInfo();
            if (currentWifi.getSSID() != null){
                return true;
            }
        }
        return false;
    }

    private void closeApplication() {
        this.finish();
    }

    public void toggleState() {
        if (state == States.INTRANSITION) {
            return;
        }
        SU_Executor executor = new SU_Executor();

        if (state == States.ACTIVE) {
            //we are turning off the adb server
            executor.execute(
                    ShellOperation.STOP
            );

        } else if (state == States.INACTIVE) {
            executor.execute(
                    ShellOperation.START
            );
        }
        else if (state == States.FAILED){
            closeApplication();
        }

        if (!checkWifiState()){
            updateDisplayShell("Error, no wifi connection!\n");
        }
    }

    private void updateUI(States state) {
        final TextView ip_listening = (TextView) findViewById(R.id.lbl_listening_address);
        final TextView adb_connect = (TextView) findViewById(lbl_connect_help);

        Button toggleBtn = (Button) findViewById(R.id.btn_toggleADB);
        RelativeLayout rel_toggleBtn = (RelativeLayout) findViewById(R.id.rel_toggleADB);

        final Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(500);
        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                ip_listening.setVisibility(View.VISIBLE);
                adb_connect.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        final Animation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(500);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                ip_listening.setVisibility(View.INVISIBLE);
                adb_connect.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        if (state == States.ACTIVE){
            getSupportActionBar().setTitle("ADB Wifi (ACTIVE)");
            toggleBtn.setText(R.string.btn_stopServer);
            toggleBtn.setTextColor(Color.parseColor("#2196f3")); //blue
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2196f3")));
            toggleBtn.setText(R.string.btn_stopServer);

            String ip = getIPAddress();

            ip_listening.setText(getString(R.string.listening_on) + ip + ":5555");
            ip_listening.setAnimation(fadeIn);

            adb_connect.setText(getString(R.string.run_command) + " " + ip);
            adb_connect.setAnimation(fadeIn);
        }
        else if (state == States.INACTIVE){
            getSupportActionBar().setTitle("ADB Wifi (INACTIVE)");
            toggleBtn.setText(R.string.btn_startServer);
            toggleBtn.setTextColor(Color.GRAY);
            toggleBtn.setText(R.string.btn_startServer);

            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.GRAY));

            ip_listening.setAnimation(fadeOut);

            adb_connect.setAnimation(fadeOut);
        }
        else if (state == States.FAILED){
            getSupportActionBar().setTitle("ADB Wifi (FAILED)");
            toggleBtn.setTextColor(Color.RED);
            toggleBtn.setText("Exit");
            //getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.RED));
        }
    }


    private String getIPAddress() {
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        return ip;
    }


    //https://stackoverflow.com/questions/2604727/how-can-i-connect-to-android-with-adb-over-tcp
    //we will be using technique described in that post.
    private class SU_Executor extends AsyncTask<ShellOperation, String, States> {
        States prevState;

        @Override
        protected States doInBackground(ShellOperation... shellOps) {
            ShellOperation shellOp = shellOps[0];
            try {
                if (shellOp == ShellOperation.CHECK_ACTIVE)
                {
                    if (Shell.SU.available()) {
                        publishProgress(getString(R.string.checking_status_msg));
                        List<String> ret = Shell.SU.run(
                                "getprop service.adb.tcp.port"
                        );
                        if (ret.get(0).equals("-1")) {
                            publishProgress(getString(R.string.server_inactive));
                            return States.INACTIVE;
                        } else if (ret.get(0).equals("5555")) {
                            publishProgress(getString(R.string.server_active));
                            return States.ACTIVE;
                        } else {
                            Log.e(getPackageName(), "unknown ret from checkOp, " + ret.get(0));
                            return States.FAILED;
                        }
                    }
                    else{
                        publishProgress("Error obtaining Root privileges");
                        return States.FAILED;
                    }
                }
                else if (shellOp == ShellOperation.CHECK_ACTIVE_QUIET){
                    List<String> ret = Shell.SU.run(
                            "getprop service.adb.tcp.port"
                    );
                    if (ret.get(0).equals("-1")) {
                        return States.INACTIVE;
                    } else if (ret.get(0).equals("5555")) {
                        return States.ACTIVE;
                    } else {
                        Log.e(getPackageName(), "unknown ret from checkOp, " + ret.get(0));
                        return States.FAILED;
                    }
                }
                else if (shellOp == ShellOperation.START) {
                    if (Shell.SU.available()) {
                        publishProgress("Executing: setprop service.adb.tcp.port 5555");
                        Shell.SU.run("setprop service.adb.tcp.port 5555");
                        States s = doInBackground(ShellOperation.CHECK_ACTIVE);
                        if (s == States.ACTIVE) {
                            return States.ACTIVE;
                        } else {
                            publishProgress("There was an error enabling ADB Wifi");
                            return s;
                        }
                    } else {
                        publishProgress("Error obtaining Root Privileges!");
                        return States.FAILED;
                    }
                }
                else if (shellOp == ShellOperation.STOP) {
                    if (Shell.SU.available()) {
                        publishProgress("Executing: setprop service.adb.tcp.port -1");
                        Shell.SU.run("setprop service.adb.tcp.port -1");
                        States s = doInBackground(ShellOperation.CHECK_ACTIVE);
                        if (s == States.INACTIVE) {
                            return States.INACTIVE;
                        } else {
                            publishProgress(getString(R.string.error_disabling_adb));
                            return s;
                        }
                    } else {
                        publishProgress("Error obtaining Root Privileges!");
                        return States.FAILED;
                    }
                }
            } catch (Exception e) {
                Log.e (getPackageName(), "Error!, " + e.getMessage());
                e.printStackTrace();
                return States.FAILED;
            }

            return States.FAILED;
        }

        @Override
        protected void onPreExecute() {
            prevState = MainActivity.state;
            MainActivity.state = States.INTRANSITION;
        }

        @Override
        protected void onProgressUpdate(String... strings) {
            updateDisplayShell(strings[0] + "\n");
        }

        //this gets called when our operation was completed.
        @Override
        protected void onPostExecute(States state) {
            MainActivity.state = state;
            updateUI(state);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void updateDisplayShell(String command) {
        TextView output = (TextView) findViewById(R.id.lbl_shell);
        output.setText("" + output.getText() + command);

        ScrollView sv = (ScrollView) findViewById(R.id.scrv_shell);
        sv.fullScroll(View.FOCUS_DOWN);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about){
            Intent aboutScreen = new Intent(this, About.class);
            startActivity(aboutScreen);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
