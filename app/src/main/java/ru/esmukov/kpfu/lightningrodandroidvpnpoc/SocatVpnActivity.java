package ru.esmukov.kpfu.lightningrodandroidvpnpoc;

import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;


public class SocatVpnActivity extends ActionBarActivity {
    private TextView mServerConfiguration;

    private enum Layout {
        WAITING_CONSENT, NO_CONSENT, HAVE_CONSENT
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.form);
        mServerConfiguration = (TextView) findViewById(R.id.configuration);

        obtainVpnConsent();
    }

    @Override
    protected void onResume() {
        super.onResume();

        LinearLayout haveConsentLayout = (LinearLayout) this.findViewById(R.id.have_consent_layout);
        if (haveConsentLayout.getVisibility() == LinearLayout.VISIBLE) {
            // check if we still have the consent
            obtainVpnConsent();
        }
    }

    private boolean obtainVpnConsent() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            stopService(new Intent(this, BroadcastsListenerService.class));
            drawLayouts(Layout.WAITING_CONSENT);
            startActivityForResult(intent, 0);
            return false;
        } else {
            drawLayouts(Layout.HAVE_CONSENT);
            onActivityResult(0, RESULT_OK, null);
            return true;
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (result == RESULT_OK) {
            drawLayouts(Layout.HAVE_CONSENT);
            startService(new Intent(this, BroadcastsListenerService.class));
        } else {
            drawLayouts(Layout.NO_CONSENT);
            stopService(new Intent(this, BroadcastsListenerService.class));
        }
    }

    public void manualConnect(View v) {
        if (!obtainVpnConsent())
            return;

        String prefix = getPackageName();
        Intent intent = new Intent(this, SocatVpnService.class)
                .putExtra(prefix + ".CONFIGURATION", mServerConfiguration.getText().toString());
        startService(intent);
    }

    public void stopAndQuit(View v) {
        stopService(new Intent(this, BroadcastsListenerService.class));

        // VpnService won't stop until after it is unbound, so we
        // notify it via the following broadcast that it should stop
        sendBroadcast(new Intent(getPackageName() + ".intent.VpnDisconnect"));
        stopService(new Intent(this, SocatVpnService.class));

        finishAffinity(); // close all activities
    }

    public void retryConsent(View v) {
        obtainVpnConsent();
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

    private void drawLayouts(Layout layout) {
        LinearLayout haveConsentLayout = (LinearLayout) this.findViewById(R.id.have_consent_layout);
        LinearLayout noConsentLayout = (LinearLayout) this.findViewById(R.id.no_consent_layout);
        LinearLayout waitingConsentLayout = (LinearLayout) this.findViewById(R.id.waiting_consent_layout);

        haveConsentLayout.setVisibility(LinearLayout.GONE);
        noConsentLayout.setVisibility(LinearLayout.GONE);
        waitingConsentLayout.setVisibility(LinearLayout.GONE);

        LinearLayout target = null;
        switch (layout) {
            case HAVE_CONSENT:
                target = haveConsentLayout;
                break;
            case NO_CONSENT:
                target = noConsentLayout;
                break;
            case WAITING_CONSENT:
                target = waitingConsentLayout;
                break;
        }
        target.setVisibility(LinearLayout.VISIBLE);
    }
}
