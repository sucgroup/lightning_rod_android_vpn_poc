package ru.esmukov.kpfu.lightningrodandroidvpnpoc;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.net.VpnService;
import android.view.View;
import android.widget.TextView;


public class SocatVpnActivity extends ActionBarActivity implements View.OnClickListener {
    private TextView mServerConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.form);
        mServerConfiguration = (TextView) findViewById(R.id.configuration);
        findViewById(R.id.connect).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, 0);
        } else {
            onActivityResult(0, RESULT_OK, null);
        }
    }
    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (result == RESULT_OK) {
            String prefix = getPackageName();
            Intent intent = new Intent(this, SocatVpnService.class)
                    .putExtra(prefix + ".CONFIGURATION", mServerConfiguration.getText().toString());
            startService(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
