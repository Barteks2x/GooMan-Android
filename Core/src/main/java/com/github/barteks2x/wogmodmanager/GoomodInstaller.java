package com.github.barteks2x.wogmodmanager;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.goofans.gootool.addins.Addin;
import com.goofans.gootool.addins.AddinFactory;
import com.goofans.gootool.addins.AddinFormatException;
import com.goofans.gootool.addins.AddinInstaller;
import com.goofans.gootool.model.Configuration;
import com.goofans.gootool.util.ProgressListener;
import com.goofans.gootool.wog.ConfigurationWriterTask;
import com.goofans.gootool.wog.WorldOfGoo;
import com.goofans.gootool.wog.WorldOfGooAndroid;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GoomodInstaller implements View.OnClickListener {
  private WogMmActivity wogMmActivity;
  private ProgressBar pb;
  private TextView text;
  private GridView modsGrid;

  public GoomodInstaller(WogMmActivity wogMmActivity, ProgressBar pb, TextView text, GridView modsGrid) {
    this.wogMmActivity = wogMmActivity;
    this.pb = pb;
    this.text = text;
    this.modsGrid = modsGrid;
  }

  @Override
  public void onClick(View v) {
    new AsyncTask<Void, ProgressData, Void>() {
      private Configuration cfg;

      @Override
      protected void onPreExecute() {
        wogMmActivity.disableButtons();
        pb.setVisibility(View.VISIBLE);
        text.setVisibility(View.VISIBLE);

        try {
          cfg = WorldOfGoo.getTheInstance().readConfiguration();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

        cfg.setWatermark("Modded using GooMan");

        ModListDynamicGridViewAdapter a = (ModListDynamicGridViewAdapter) modsGrid.getAdapter();

        for(int i = 0; i < a.getCount(); i++) {
          ModListDynamicGridViewAdapter.GoomodEntry entry = (ModListDynamicGridViewAdapter.GoomodEntry) a.getItem(i);
          if(entry.isEnabled())
            cfg.enableAddin(entry.getId());
        }
      }

      @Override
      protected void onPostExecute(Void nothing) {
        wogMmActivity.enableButtons();
        pb.setVisibility(View.INVISIBLE);
        text.setText("");
      }
      @Override
      protected Void doInBackground(Void... params) {
        ConfigurationWriterTask cwt = new ConfigurationWriterTask(cfg);
        cwt.addListener(new ProgressListener() {
          String task = "";
          @Override
          public void beginStep(String taskDescription, boolean progressAvailable) {
            publishProgress(new ProgressData(task = taskDescription, progressAvailable ? 0 : 0.5f));
          }

          @Override
          public void progressStep(float percent) {
            publishProgress(new ProgressData(task, percent));
          }
        });
        try {
          cwt.run();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        return null;
      }

      @Override
      protected void onProgressUpdate(ProgressData... i) {
        ProgressData pd = i[i.length-1];
        pb.setProgress((int) (pd.progress * 100));
        text.setText(pd.name);
      }
    }.execute();
  }
}
