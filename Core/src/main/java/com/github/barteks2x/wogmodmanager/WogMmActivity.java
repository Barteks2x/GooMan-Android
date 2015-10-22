package com.github.barteks2x.wogmodmanager;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.goofans.gootool.addins.Addin;
import com.goofans.gootool.addins.AddinFactory;
import com.goofans.gootool.addins.AddinFormatException;
import com.goofans.gootool.model.Configuration;
import com.goofans.gootool.util.ProgressListener;
import com.goofans.gootool.util.Utilities;
import com.goofans.gootool.wog.WorldOfGoo;
import com.goofans.gootool.wog.WorldOfGooAndroid;

import org.askerov.dynamicgrid.DynamicGridView;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;


public class WogMmActivity extends ActionBarActivity {

  public static final String TAG = "WogMM";
  private static final int FILE_SELECT_CODE = 0;

  public Button installApkBtn;
  public Button installModsBtn;
  public Button changeOrder;

  private Button addBtn, rmBtn;

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

    this.addBtn = (Button) findViewById(R.id.addBtn);
    this.addBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
          startActivityForResult(
                  Intent.createChooser(intent, "Select a File to Upload"),
                  FILE_SELECT_CODE);
        } catch (ActivityNotFoundException ex) {
          Toast.makeText(WogMmActivity.this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
      }
    });

    this.rmBtn = (Button) findViewById(R.id.rmBtn);

    this.changeOrder = (Button) findViewById(R.id.changeOrderButton);
    this.changeOrder.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        modsGrid.startEditMode();
        timer.start();
        changeOrder.setEnabled(false);
      }
    });
    new InitGoomanTask().execute();
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

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case FILE_SELECT_CODE:
        if (resultCode == RESULT_OK) {
          // Get the Uri of the selected file
          Uri uri = data.getData();
          Log.d(TAG, "File Uri: " + uri.toString());

          File file = IOUtils.getFile(this, uri);

          if(file == null) {
            Toast.makeText(this, "Couldn't open file", Toast.LENGTH_SHORT);
            return;
          }

          new AddAddinAsyncTask(file, (ModListDynamicGridViewAdapter) this.modsGrid.getAdapter(), this).execute((Void[])null);
        }
        break;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }


  public void disableButtons() {
    setButtonsEnabled(false);
  }

  public void enableButtons() {
    setButtonsEnabled(true);
  }

  private void setButtonsEnabled(boolean value) {
    installModsBtn.setEnabled(value);
    installApkBtn.setEnabled(value);
    rmBtn.setEnabled(value);
    addBtn.setEnabled(value);
    changeOrder.setEnabled(value);
  }

  private class AddAddinAsyncTask extends AsyncTask<Void, Void, String>{
    private File file;
    private ModListDynamicGridViewAdapter adapter;
    private Context context;

    public AddAddinAsyncTask(File file, ModListDynamicGridViewAdapter adapter, Context context) {

      this.file = file;
      this.adapter = adapter;
      this.context = context;
    }

    @Override
    protected String doInBackground(Void... params) {
      try {
        Addin a = AddinFactory.loadAddin(file);
        WorldOfGoo.getTheInstance().installAddin(file, a.getId(), false);
      } catch (AddinFormatException e) {
        Log.e(TAG, "Addin error", e);
        return "Invalid addin file";
      } catch (IOException e) {
        Log.e(TAG, "IO error", e);
        return "File read error";
      } catch(DuplicateAddinException ex) {
        Log.e(TAG, "Duplicate addin", ex);
        return "Addin already added";
      }
      return null;
    }

    @Override
    protected void onPostExecute(String result) {
      if(result != null) {
        Toast.makeText(context, result, Toast.LENGTH_LONG).show();
        return;
      }
      Set<String> alreadyInstalled = new HashSet<String>();
      for(int i = 0; i < adapter.getCount(); i++) {
        alreadyInstalled.add(((ModListDynamicGridViewAdapter.GoomodEntry) adapter.getItem(i)).getId());
      }
      for(Addin addin : WorldOfGoo.getAvailableAddins()) {
        if(!alreadyInstalled.contains(addin.getId())) {
          adapter.add(new ModListDynamicGridViewAdapter.GoomodEntry(addin.getName(), addin.getId(), false));
        }
      }
      Toast.makeText(context, "Successfully added addin. Don't forget to save changes", Toast.LENGTH_SHORT).show();
    }
  }

  private class InitGoomanTask extends AsyncTask<Void, ProgressData, Void> {

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

        for(Addin addin : wog.getAvailableAddins()) {
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
  }
}