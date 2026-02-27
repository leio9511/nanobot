package com.google.clawminium.kernel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("AgentKernel", "Boot completed, starting KernelService");
            Intent serviceIntent = new Intent(context, KernelService.class);
            context.startForegroundService(serviceIntent);
        }
    }
}
