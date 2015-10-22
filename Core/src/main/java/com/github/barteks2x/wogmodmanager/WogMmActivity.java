package com.github.barteks2x.wogmodmanager;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.goofans.gootool.addins.Addin;
import com.goofans.gootool.model.Configuration;
import com.goofans.gootool.util.ProgressListener;
import com.goofans.gootool.wog.WorldOfGoo;

import org.askerov.dynamicgrid.DynamicGridView;

import java.io.IOException;


public class WogMmActivity extends ActionBarActivity {

  public static final String TAG = "WogMM";

  public Button installApkBtn;
  public Button installModsBtn;
  public Button changeOrder;

  private ProgressBar pb;

  private DynamicGridView modsGrid;
  private ModListDynamicGridViewAdapter modListAdapter;

  private TextView text;

  private CountDownTimer timer = new CountDownTimer(5000, 10000) {
    @Override
    public void onTick(long millisUntilFinished) {}

    @Override
    public void onFinish() {
      modsGrid.stopEditMode();
      changeOrder.setEnabled(true);
    }
  };
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_wog_mm);

    this.modsGrid = (DynamicGridView) findViewById(R.id.modsGrid);
    this.modsGrid.setAdapter(modListAdapter = new ModListDynamicGridViewAdapter(this, this.modsGrid));
    this.modsGrid.setEditModeEnabled(true);
    this.modsGrid.setWobbleInEditMode(false);
    this.modsGrid.setOnDragListener(new DynamicGridView.OnDragListener() {
      @Override
      public void onDragStarted(int position) {
        timer.cancel();
      }

      @Override
      public void onDragPositionsChanged(int oldPosition, int newPosition) {
      }
    });
    this.modsGrid.setOnDropListener(new DynamicGridView.OnDropListener() {
      @Override
      public void onActionDrop() {
        timer.start();
      }
    });

    this.pb = (ProgressBar) findViewById(R.id.installProgress);
    this.pb.setInterpolator(new LinearInterpolator());

    this.text = (TextView) findViewById(R.id.textView);

    this.installModsBtn = (Button) findViewById(R.id.installModsBtn);
    this.installModsBtn.setOnClickListener(new GoomodInstaller(this, pb, text, modsGrid));

    this.installApkBtn = (Button) findViewById(R.id.installApkBtn);
    this.installApkBtn.setOnClickListener(new ApkInstaller(this, pb, text));

    this.changeOrder = (Button) findViewById(R.id.changeOrderButton);
    this.changeOrder.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        modsGrid.startEditMode();
        timer.start();
        changeOrder.setEnabled(false);
      }
    });
    new AsyncTask<Void, ProgressData, Void>() {

      @Override
      protected Void doInBackground(Void... params) {
        WorldOfGoo.getTheInstance().init();

        return null;
      }

      @Override
      protected void onPostExecute(Void nothing) {
        enableButtons();

        pb.setVisibility(View.INVISIBLE);
        pb.setProgress(0);
        text.setText("");

        //TODO: DO IT IN BACKGROUND
        try {
          WorldOfGoo wog = WorldOfGoo.getTheInstance();

          wog.updateInstalledAddins();
          Configuration cfg = WorldOfGoo.getTheInstance().readConfiguration();

          for(Addin addin :wog.getAvailableAddins()) {
            boolean enabled = cfg.isEnabledAdddin(addin.getId());

            modListAdapter.add(new ModListDynamicGridViewAdapter.GoomodEntry(addin.getName(), addin.getId(), enabled));
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      protected void onPreExecute() {
        pb.setVisibility(View.VISIBLE);
        WoGInitData.setPackageManager(getPackageManager());
        WoGInitData.setContext(getApplicationContext());

        WoGInitData.setProgressListener(new ProgressListener() {
          private String stepName;

          @Override
          public void beginStep(String taskDescription, boolean progressAvailable) {
            stepName = taskDescription;
            if(!progressAvailable) {
              progressStep(0.5f);
            } else {
              progressStep(0);
            }
          }

          @Override
          public void progressStep(float percent) {
            publishProgress(new ProgressData(stepName, percent));
          }
        });
      }

      @Override
      protected void onProgressUpdate(ProgressData... i) {
        ProgressData pd = i[i.length - 1];
        pb.setProgress((int) (pd.progress * 100));
        text.setText(pd.name);

      }
    }.execute();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_wog_mm, menu);

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


  public void disableButtons() {
    installModsBtn.setEnabled(false);
    installApkBtn.setEnabled(false);
  }

  public void enableButtons() {
    installModsBtn.setEnabled(true);
    installApkBtn.setEnabled(true);
  }
}