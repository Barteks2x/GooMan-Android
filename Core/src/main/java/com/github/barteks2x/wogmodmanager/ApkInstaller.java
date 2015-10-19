package com.github.barteks2x.wogmodmanager;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.goofans.gootool.wog.WorldOfGoo;
import com.goofans.gootool.wog.WorldOfGooAndroid;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

import kellinwood.security.zipsigner.ProgressEvent;
import kellinwood.security.zipsigner.ProgressListener;
import kellinwood.security.zipsigner.ZipSigner;

import static com.github.barteks2x.wogmodmanager.WogMmActivity.TAG;

public class ApkInstaller implements View.OnClickListener {

  private final ProgressBar progress;
  private final WogMmActivity a;
  private TextView text;

  public ApkInstaller(WogMmActivity a, ProgressBar progress, TextView text) {
    this.progress = progress;
    this.a = a;
    this.text = text;
  }

  @Override
  public void onClick(View v) {
    a.disableButtons();
    new InstallModsTask(progress, a, text).execute();

  }

  private static final class InstallModsTask extends AsyncTask<Void, ProgressData, Boolean> {

    private final ProgressBar progress;
    private TextView text;
    private final WogMmActivity a;
    private PackageManager pkgMgr;

    private int taskNum = -1;
    private final int maxTask = 2;

    public InstallModsTask(ProgressBar progress, WogMmActivity act, TextView text) {
      this.progress = progress;
      this.text = text;

      this.pkgMgr = act.getPackageManager();
      this.a = act;
    }

    @Override
    protected void onPreExecute() {
      this.progress.setVisibility(View.VISIBLE);
      text.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPostExecute(Boolean b) {
      this.progress.setVisibility(View.INVISIBLE);
      text.setText("");
      a.enableButtons();
      if (!b) {
        return;
      }
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setDataAndType(Uri.fromFile(new File(WorldOfGooAndroid.get().DATA_DIR, "modded.apk")), "application/vnd.android.package-archive");
      a.startActivity(intent);
    }

    @Override
    protected Boolean doInBackground(Void... nothing) {
      taskNum++;
      setTaskProgress("Generating APK (progress bar not implemented yet)", 0);
      File srcDir = WorldOfGooAndroid.get().TEMP_MODDED_DIR;
      File zipFile = new File(WorldOfGooAndroid.get().DATA_DIR, "modded_unsigned.apk");
      if (!this.putIntoApk(srcDir, zipFile)) return false;


      taskNum++;
      setTaskProgress("Signing APK", 0);
      File signed = new File(WorldOfGooAndroid.get().DATA_DIR, "modded.apk");
      this.signApk(zipFile, signed);
      return true;
    }

    private boolean putIntoApk(File dir, File apkLoc) {
      try {
        IOUtils.zipDirContentWithZipBase(WorldOfGooAndroid.get().WOG_APK_FILE, dir, apkLoc);
      } catch (IOException e) {
        e.printStackTrace();
        return false;
      }
      return true;
    }

    private void signApk(File apk, File signed) {
      File unsigned = apk;
      try {
        ZipSigner signer = new ZipSigner();
        signer.addProgressListener(new ProgressListener() {
          @Override
          public void onProgress(ProgressEvent event) {
            setTaskProgress("Signing APK", event.getPercentDone() / 100.0d);
          }
        });
        signer.setKeymode(ZipSigner.MODE_AUTO);
        signer.loadKeys(ZipSigner.KEY_TESTKEY);
        signer.signZip(unsigned.getPath(), signed.getPath());
      } catch (ClassNotFoundException e) {
        Log.wtf(TAG, e);
      } catch (IllegalAccessException e) {
        Log.wtf(TAG, e);
      } catch (InstantiationException e) {
        Log.wtf(TAG, e);
      } catch (GeneralSecurityException e) {
        Log.wtf(TAG, e);
      } catch (IOException e) {
        Log.wtf(TAG, e);
      }
    }

    @Override
    protected void onProgressUpdate(ProgressData... i) {
      ProgressData pd = i[i.length-1];
      this.progress.setProgress((int)((pd.progress + taskNum) * 100 /maxTask));
      text.setText(pd.name);
    }

    private void setTaskProgress(String name, double p) {
      this.publishProgress(new ProgressData(name, p));
    }
  }
}
