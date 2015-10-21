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
import com.goofans.gootool.wog.WorldOfGoo;

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
      private List<String> mods = new ArrayList<String>();
      @Override
      protected void onPreExecute() {
        wogMmActivity.disableButtons();
        pb.setVisibility(View.VISIBLE);
        text.setVisibility(View.VISIBLE);

        ModListDynamicGridViewAdapter a = (ModListDynamicGridViewAdapter) modsGrid.getAdapter();

        for(int i = 0; i < a.getCount(); i++) {
          ModListDynamicGridViewAdapter.GoomodEntry entry = (ModListDynamicGridViewAdapter.GoomodEntry) a.getItem(i);
          if(entry.isEnabled())
            mods.add(entry.getName());
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

        install();
        return null;
      }

      public boolean install() {
        this.publishProgress(new ProgressData("Installing goomods", 0.0));

        try {
          File loc = WorldOfGoo.getTheInstance().getAddinsDir();
          List<File> goomods = getGoomodsList(loc);

          Log.w(WogMmActivity.TAG, "Location: " + loc);
          int max = goomods.size();
          int i = 0;
          for(File f : goomods) {
            Addin addin = AddinFactory.loadAddin(f);
            this.publishProgress(new ProgressData("Installing " + addin.getName(), i/(double)max));
            AddinInstaller.installAddin(addin);

            i++;
          }

        } catch (AddinFormatException e) {
          throw new RuntimeException(e);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        this.publishProgress(new ProgressData("Installing goomods", 1));
        return true;
      }

      private List<File> getGoomodsList(File dir) throws FileNotFoundException {

        File listTxt = new File(dir, "goomod-list.txt");
        writeGoomodsFile(mods, listTxt);
        if(listTxt.exists() && listTxt.isFile()) {
          List<String> mods = null;
          try {
            mods = IOUtils.getLines(listTxt);
          } catch (FileNotFoundException e) {
            Log.wtf(WogMmActivity.TAG, "goomod-list.txt - HeisenFile?");
          }
          List<File> fileList = new ArrayList<>(mods.size());
          for(String str : mods) {
            File mod = new File(dir, str);
            if(!mod.exists()) {
              throw new FileNotFoundException(mod.getPath());
            }
            fileList.add(mod);
          }
          return fileList;
        }

        List<File> list = new ArrayList<File>();
        for(File f : dir.listFiles()) {
          if(f.getName().endsWith(".goomod") && f.isFile()) {
            list.add(f);
          }
        }
        return list;
      }

      private void writeGoomodsFile(List<String> mods, File f) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(f);
        for(String str : mods) {
          pw.println(str);
        }
        pw.close();
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
