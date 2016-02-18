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
 *  AUTHORS:     fronti90, blk_jack
 *  DESCRIPTION: BrokenCenter: manage your ROM
 *
 *=========================================================================
 */
package com.broken.center;

import com.broken.ota.R;
import com.broken.sizer.BrokenSizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.internal.util.broken.BuildInfo;
import com.broken.util.Shell;
import com.broken.util.Utils;

public class AboutBroken extends Fragment{

    private LinearLayout website;
    private LinearLayout source;
    private LinearLayout donate;
    private LinearLayout report;
    private String mStrDevice;
    private boolean su=false;
    private static final String FILENAME_PROC_VERSION = "/proc/version";
    private static final String LOG_TAG = "DeviceInfoSettings";
    public File path;
    public String zipfile;
    public String logfile;
    public String last_kmsgfile;
    public String kmsgfile;
    public String systemfile;
    Process superUser;
    DataOutputStream ds;
    byte[] buf = new byte[1024];
    Shell.SH shell;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.broken_about, container, false);
        return view;
    }

    private final View.OnClickListener mActionLayouts = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == website) {
                launchUrl("http://dysfunctionalroms.net");
            } else if (v == source) {
                launchUrl("http://github.com/BrokenROM");
            } else if (v == donate) {
                launchUrl("http://brokenos.wix.com/main#!donations/c1u32");
            } else if (v == report) {
                bugreport();
            }
        }
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //set LinearLayouts and onClickListeners

        website = (LinearLayout) getView().findViewById(R.id.broken_website);
        website.setOnClickListener(mActionLayouts);

        source = (LinearLayout) getView().findViewById(R.id.broken_source);
        source.setOnClickListener(mActionLayouts);

        donate = (LinearLayout) getView().findViewById(R.id.broken_donate);
        donate.setOnClickListener(mActionLayouts);

        report = (LinearLayout) getView().findViewById(R.id.broken_bugreport);
        report.setOnClickListener(mActionLayouts);
        // request su
        if (Utils.isSuEnabled()) {
            shell = new Shell().su;
            if (!su) {
                su = shell.runCommand("exit");
            }
        } else {
            shell = new Shell().sh;
        }
    }

    private void launchUrl(String url) {
        Uri uriUrl = Uri.parse(url);
        Intent donate = new Intent(Intent.ACTION_VIEW, uriUrl);
        getActivity().startActivity(donate);
    }

    private void toast(String text) {
        // easy toasts for all!
        Toast toast = Toast.makeText(getView().getContext(), text,
                Toast.LENGTH_SHORT);
        toast.show();
    }

    private boolean isCallable(Intent intent) {
        List<ResolveInfo> list = getActivity().getPackageManager().queryIntentActivities(
                intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }
    // bugreport
    private void bugreport() {
        // collect system information
        String kernel = getFormattedKernelVersion();
        // check if sdcard is available
        short state = Utils.sdAvailable();
        // initialize logfiles
        File extdir = Environment.getExternalStorageDirectory();
        path = new File(extdir.getAbsolutePath() + "/Broken/Bugreport");
        File savefile = new File(path + "/system.log");
        File logcat = new File(path + "/logcat.log");
        File last_kmsg = new File(path + "/last_kmsg.log");
        File kmsg = new File(path + "/kmsg.log");
        File zip = new File(Environment.getExternalStorageDirectory() + "/Broken/bugreport.zip");
        systemfile = savefile.toString();
        logfile = logcat.toString();
        last_kmsgfile = last_kmsg.toString();
        kmsgfile = kmsg.toString();
        zipfile = zip.toString();
        //cleanup old logs
        if (state == 2) {
            try {
                // create directory if it doesnt exist
                if (!path.exists()) path.mkdirs();
                // cleanup old logs
                if (savefile.exists()) savefile.delete();
                if (logcat.exists()) logcat.delete();
                if (zip.exists()) zip.delete();
                if (last_kmsg.exists()) last_kmsg.delete();
                if (kmsg.exists()) kmsg.delete();

                // create savefile and output lists to it
                FileWriter outstream = new FileWriter(savefile);
                BufferedWriter save = new BufferedWriter(outstream);
                save.write("Device: " + mStrDevice + '\n' + "Kernel: " + kernel);
                save.close();
                outstream.close();
                // get logcat and write to file
                shell.run("logcat -d -f " + logcat + " *:V\n");
                shell.run("cat /proc/last_kmsg > " + last_kmsgfile + "\n");
                shell.run("cat /proc/kmsg > " + kmsgfile + "\n");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                 // create zip file
                if (savefile.exists() && logcat.exists() && last_kmsg.exists() && kmsg.exists()) {
                    dialog(zip());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            toast(getResources().getString(R.string.sizer_message_sdnowrite));
        }
    }

    //get kernel information
    private static String getFormattedKernelVersion() {
        try {
            return formatKernelVersion(readLine(FILENAME_PROC_VERSION));

        } catch (IOException e) {
            Log.e(LOG_TAG,
                "IO Exception when getting kernel version for Device Info screen",
                e);

            return "Unavailable";
        }
    }

    public static String formatKernelVersion(String rawKernelVersion) {

        final String PROC_VERSION_REGEX =
            "Linux version (\\S+) " + /* group 1: "3.0.31-g6fb96c9" */
            "\\((\\S+?)\\) " +        /* group 2: "x@y.com" (kernel builder) */
            "(?:\\(gcc.+? \\)) " +    /* ignore: GCC version information */
            "(#\\d+) " +              /* group 3: "#1" */
            "(?:.*?)?" +              /* ignore: optional SMP, PREEMPT, and any CONFIG_FLAGS */
            "((Sun|Mon|Tue|Wed|Thu|Fri|Sat).+)"; /* group 4: "Thu Jun 28 11:02:39 PDT 2012" */

        Matcher m = Pattern.compile(PROC_VERSION_REGEX).matcher(rawKernelVersion);
        if (!m.matches()) {
            Log.e(LOG_TAG, "Regex did not match on /proc/version: " + rawKernelVersion);
            return "Unavailable";
        } else if (m.groupCount() < 4) {
            Log.e(LOG_TAG, "Regex match on /proc/version only returned " + m.groupCount()
                    + " groups");
            return "Unavailable";
        }
        return m.group(1) + " " +                 // 3.0.31-g6fb96c9
            m.group(2) + " " + m.group(3);
    }

    private static String readLine(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename), 256);
        try {
            return reader.readLine();
        } finally {
            reader.close();
        }
    }
    // zipping!
    private boolean zip() {
        String[] source = { systemfile, logfile, last_kmsgfile, kmsgfile };
        try {
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipfile));
            for (int i = 0; i < source.length; i++) {
                String file = source[i].substring(source[i].lastIndexOf("/"), source[i].length());
                FileInputStream in = new FileInputStream(source[i]);
                out.putNextEntry(new ZipEntry(file));
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.closeEntry();
                in.close();
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void dialog(boolean success) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        if (success) {
            alert.setMessage(R.string.report_infosuccess).setPositiveButton(R.string.ok,
                    new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int id) {
                            // action for ok
                            dialog.cancel();
                        }
                    });
        } else {
            alert.setMessage(R.string.report_infofail).setPositiveButton(R.string.ok,
                    new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int id) {
                            // action for ok
                            dialog.cancel();
                        }
                    });
        }
        alert.show();
    }
}
