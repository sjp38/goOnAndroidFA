package com.example.versioncheck;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {

    private static final String TAG = "versionCheck";

    private Process goProcess = null;

    private String goBinPath() {
        return getBaseContext().getFilesDir().getAbsolutePath()
                + "/verChecker.bin";
    }

    private void copyGoBinary() {
        String dstFile = goBinPath();
        /*
         * TODO: Check whether the binary is up-to-date
         */
        Log.d(TAG, "Copy Go binary from APK to " + dstFile);
        try {
            InputStream is = getAssets().open("go.bin");
            FileOutputStream fos = getBaseContext().openFileOutput(
                    "verChecker.bin", MODE_PRIVATE);
            byte[] buf = new byte[8192];
            int offset;
            while ((offset = is.read(buf)) > 0) {
                fos.write(buf, 0, offset);
            }
            is.close();
            fos.flush();
            fos.close();

            Log.d(TAG, "wrote out " + dstFile);
            Runtime.getRuntime().exec("chmod 0777 " + dstFile);
            Log.d(TAG, "did chmod 0777 on " + dstFile);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Log.d(TAG, "interrupted from sleep");
            }
            Runtime.getRuntime().exec("sync");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void startServer(View view) {
        Toast.makeText(getApplicationContext(), "start server",
                Toast.LENGTH_SHORT).show();

        copyGoBinary();

        if (goProcess != null) {
            goProcess.destroy();
            goProcess = null;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command(goBinPath());
            pb.redirectErrorStream(false);
            goProcess = pb.start();
            Log.d(TAG, "goProcess started");
            new CopyToAndroidLogThread("stderr", goProcess.getErrorStream())
                    .start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopServer(View view) {
        Toast.makeText(getApplicationContext(), "stop server",
                Toast.LENGTH_SHORT).show();

        if (goProcess != null) {
            goProcess.destroy();
            goProcess = null;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class CopyToAndroidLogThread extends Thread {
        private final BufferedReader mBufIn;
        private final String mStream;

        public CopyToAndroidLogThread(String stream, InputStream in) {
            mBufIn = new BufferedReader(new InputStreamReader(in));
            mStream = stream;
        }

        @Override
        public void run() {
            String tag = TAG + "/" + mStream + "-child";
            while (true) {
                String line = null;
                try {
                    line = mBufIn.readLine();
                } catch (IOException e) {
                    Log.d(tag, "Exception: " + e.toString());
                    return;
                }
                if (line == null) {
                    // EOF
                    return;
                }
                Log.d(tag, line);
            }
        }
    }
}
