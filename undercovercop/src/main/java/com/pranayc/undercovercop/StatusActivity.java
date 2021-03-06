package com.pranayc.undercovercop;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class StatusActivity extends Activity implements View.OnClickListener, AndroidHandler.HandlerIx
{
    private ServiceConnection serviceConnection;
    private Messenger messenger;
    private static final AndroidHandler HANDLER = new AndroidHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.status);
        HANDLER.setInstance(this);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        if(checkInstallStatus())
        {
            // Check Config Status
            sendToService(64, null);
            System.out.println("Binding Service...");
        }
    }

    private void sendToService(final int what, final Bundle bundle)
    {
        if(serviceConnection==null || messenger == null)
        {
            serviceConnection = new ServiceConnection()
            {
                @Override
                public void onServiceConnected(final ComponentName name, final IBinder service)
                {
                    messenger = new Messenger(service);
                    System.out.println("Service is bound");
                    final Message msg = Message.obtain(null, what);
                    if(bundle!=null)
                    {
                        msg.setData(bundle);
                    }
                    msg.replyTo = new Messenger(HANDLER);

                    try
                    {
                        messenger.send(msg);
                    }
                    catch (RemoteException e)
                    {
                        e.printStackTrace();
                        doUnbind();
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name)
                {
                    doUnbind();
                }
            };
        }
        else
        {
            final Message msg = Message.obtain(null, what);
            if(bundle!=null)
            {
                msg.setData(bundle);
            }

            msg.replyTo = new Messenger(HANDLER);

            try
            {
                messenger.send(msg);
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
                doUnbind();
            }
        }

        final Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.pranayc.undercover", "com.pranayc.undercover.ConfigService"));
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private boolean checkInstallStatus()
    {
        final TextView installedStatus = (TextView) findViewById(R.id.installed_status);
        final Button installButton = (Button) findViewById(R.id.install_button);
        if(installedStatus!=null && installButton!=null)
        {
            final boolean isUndercoverAvailable = Utility.isAppInstalled(this, Utility.UNDERCOVER_PACKAGE);

            if(isUndercoverAvailable)
            {
                installedStatus.setText(R.string.undercover_present);
                installButton.setVisibility(View.GONE);
                return true;
            }
            else
            {
                installedStatus.setText(R.string.undercover_absent);
                installButton.setVisibility(View.VISIBLE);
                return false;
            }
        }
        return false;
    }

    @Override
    public void onClick(View v)
    {
        startActivityForResult(new Intent(this, ConfigActivity.class), 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode==100 && resultCode==1)
        {
            String username = data.getStringExtra("UserName");
            String password = data.getStringExtra("PassWord");
            final Bundle bundle = new Bundle();
            bundle.putString("UserName", username);
            bundle.putString("PassWord", password);

            sendToService(786, bundle);
        }
    }

    @Override
    public void handleMessage(final Message msg)
    {
        System.out.println("Reply from Service: " + msg.what);
        switch(msg.what)
        {
            case 0:
            {
                TextView tv = (TextView) findViewById(R.id.config_status);
                Button btn = (Button) findViewById(R.id.config_button);
                if(tv!=null && btn!=null)
                {
                    tv.setText(R.string.config_absent);
                    btn.setText(R.string.configbtn_absent);
                    btn.setOnClickListener(StatusActivity.this);
                }
            }
            break;
            case 1:
            {
                TextView tv = (TextView) findViewById(R.id.config_status);
                Button btn = (Button) findViewById(R.id.config_button);
                if(tv!=null && btn!=null)
                {
                    tv.setText(R.string.config_present);
                    btn.setText(R.string.configbtn_present);
                    btn.setOnClickListener(StatusActivity.this);
                }
            }
            break;
        }
    }

    @Override
    protected void onStop()
    {
        doUnbind();
        super.onStop();
    }

    private void doUnbind()
    {
        try
        {
            if(serviceConnection!=null)
            {
                unbindService(serviceConnection);
            }
        }
        catch (Exception e)
        {
            Log.d(Utility.TAG, "Cannot unbind service", e);
        }
        serviceConnection = null;
        messenger = null;
    }
}
