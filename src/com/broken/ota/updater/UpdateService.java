/*=========================================================================
 *
 *  PROJECT:  BrokenOs
 *            Team BrokenOs (http://brokenos.wix.com/main)
 *
 *  COPYRIGHT Copyright (C) 2014 BrokenOs http://brokenos.wix.com/main
 *            All rights reserved
 *
 *  LICENSE   http://www.gnu.org/licenses/gpl-2.0.html GNU/GPL
 *
 *  AUTHORS:     fronti90, mnazim, tchaari, kufikugel
 *  DESCRIPTION: BrokenOTA keeps our rom up to date
 *
 *=========================================================================
 */

package com.broken.ota.updater;

import android.content.Intent;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class UpdateService extends WakefulIntentService {
    private static final String TAG = "UpdateService";

    private static boolean mNoLog = true;

    public UpdateService(String name) {
        super(name);
    }

    public UpdateService() {
        super("BrokenOtaService");
    }

    /* (non-Javadoc)
     * @see com.commonsware.cwac.wakeful.WakefulIntentService#doWakefulWork(android.content.Intent)
     */
    @Override
    protected void doWakefulWork(Intent intent) {
       if (mNoLog == false) Log.d(TAG, "Broken OTA Update service called!");
       UpdateChecker otaChecker = new UpdateChecker();
       otaChecker.execute(getBaseContext());
    }

}
