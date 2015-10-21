package com.github.barteks2x.wogmodmanager;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.goofans.gootool.util.ProgressListener;
import com.goofans.gootool.wog.WorldOfGoo;

import org.askerov.dynamicgrid.DynamicGridView;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class WogMmActivity extends ActionBarActivity {

  public static final String TAG = "WogMM";

  public Button installApkBtn;
  public Button cleanBtn;
  public Button installModsBtn;
  public Button changeOrder;

  private ProgressBar pb;

  private DynamicGridView modsGrid;
  private ModListDynamicGridViewAdapter modListAdapter;
  private HorizontalScrollView scrollView;

  private TextView text;

  private CountDownTimer timer = new CountDownTimer(5000, 10000) {
    @Override
    public void onTick(long millisUntilFinished) {}

    @Override
    public void onFinish() {
      modsGrid.stopEditMode();
    }
  };
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_wog_mm);

    this.scrollView = (HorizontalScrollView) findViewById(R.id.horizontalScrollView);

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

    this.scrollView = (HorizontalScrollView) findViewById(R.id.horizontalScrollView);
    this.pb = (ProgressBar) findViewById(R.id.installProgress);
    this.pb.setInterpolator(new LinearInterpolator());

    this.text = (TextView) findViewById(R.id.textView);

    this.cleanBtn = (Button) findViewById(R.id.cleanBtn);
    this.cleanBtn.setOnClickListener(new CleanTask(this, pb, text));

    this.installModsBtn = (Button) findViewById(R.id.installModsBtn);
    this.installModsBtn.setOnClickListener(new GoomodInstaller(this, pb, text, modsGrid));

    this.installApkBtn = (Button) findViewById(R.id.installApkBtn);
    this.installApkBtn.setOnClickListener(new ApkInstaller(this, pb, text));

    this.changeOrder = (Button) findViewById(R.id.changeOrderButton);
    changeOrder.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if(modsGrid.isEditMode()) {
          modsGrid.stopEditMode();
        } else {
          modsGrid.startEditMode();
          timer.start();
        }
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
          File loc = WorldOfGoo.getTheInstance().getAddinsDir();
          File txt = new File(loc, "goomod-list.txt");

          File[] files = loc.listFiles();
          if (!txt.exists()) {
            txt.createNewFile();

            PrintWriter pw = new PrintWriter(txt);
            for (File f : files) {
              if (f.getName().endsWith(".goomod"))
                pw.println(f.getName());
            }
            pw.close();
          }

          Set<String> enabledMods = new HashSet<String>(IOUtils.getLines(txt));

          int width = 0;
          for (File f : files) {
            String name = f.getName();
            if (!name.endsWith(".goomod")) {
              continue;
            }
            boolean enabled = enabledMods.contains(name);

            modListAdapter.add(new ModListDynamicGridViewAdapter.GoomodEntry(name, enabled));
            width = Math.max(width, getTextWidth(name));
          }
          modsGrid.setColumnWidth(width + 40);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      private int getTextWidth(String s) {
        Paint paint = new Paint();
        Rect bounds = new Rect();

        int text_width = 0;

        paint.setTypeface(Typeface.DEFAULT);// your preference here
        paint.setTextSize(16);// have this the same as your text size

        String text = s;

        paint.getTextBounds(text, 0, text.length(), bounds);

        text_width =  bounds.width();
        return text_width;
      }

      @Override
      protected void onPreExecute() {
        pb.setVisibility(View.VISIBLE);
        WoGInitData.setPackageManager(getPackageManager());
        WoGInitData.setContext(getApplicationContext());

        WoGInitData.setProgressListener(new ProgressListener() {
          private int step = -1;
          private String stepName;

          @Override
          public void beginStep(String taskDescription, boolean progressAvailable) {
            step++;
            stepName = taskDescription;
            progressStep(0);
          }

          @Override
          public void progressStep(float percent) {
            publishProgress(new ProgressData(stepName, percent + step));
          }
        });
      }

      @Override
      protected void onProgressUpdate(ProgressData... i) {
        ProgressData pd = i[i.length - 1];
        pb.setProgress((int) (pd.progress * 100 / 2));
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
    cleanBtn.setEnabled(false);
    installModsBtn.setEnabled(false);
    installApkBtn.setEnabled(false);
  }

  public void enableButtons() {
    cleanBtn.setEnabled(true);
    installModsBtn.setEnabled(true);
    installApkBtn.setEnabled(true);
  }
}