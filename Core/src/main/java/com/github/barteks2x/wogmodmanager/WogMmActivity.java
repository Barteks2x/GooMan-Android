package com.github.barteks2x.wogmodmanager;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
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

import com.goofans.gootool.util.ProgressListener;
import com.goofans.gootool.wog.WorldOfGoo;

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

  private Button upBtn, downBtn;

  private ProgressBar pb;

  private GridView modsGrid;

  private TextView text;

  //HACK
  public static int selected = -1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_wog_mm);

    this.modsGrid = (GridView) findViewById(R.id.modsGrid);

    this.pb = (ProgressBar) findViewById(R.id.installProgress);
    this.pb.setInterpolator(new LinearInterpolator());

    this.text = (TextView) findViewById(R.id.textView);

    this.cleanBtn = (Button) findViewById(R.id.cleanBtn);
    this.cleanBtn.setOnClickListener(new CleanTask(this, pb, text));

    this.installModsBtn = (Button) findViewById(R.id.installModsBtn);
    this.installModsBtn.setOnClickListener(new GoomodInstaller(this, pb, text, modsGrid));

    this.installApkBtn = (Button) findViewById(R.id.installApkBtn);
    this.installApkBtn.setOnClickListener(new ApkInstaller(this, pb, text));

    this.upBtn = (Button) findViewById(R.id.upBtn);
    this.downBtn = (Button) findViewById(R.id.downBtn);


    upBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (selected <= 1) {
          return;
        }
        ArrayAdapter<Map.Entry<String, String>> a = (ArrayAdapter<Map.Entry<String, String>>) modsGrid.getAdapter();
        Map.Entry<String, String> m = a.getItem(selected);
        a.remove(m);
        a.insert(m, selected - 1);
        selected--;
      }
    });

    downBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if(selected == -1 || selected < 1) {
          return;
        }
        ArrayAdapter<Map.Entry<String, String>> a = (ArrayAdapter<Map.Entry<String, String>>) modsGrid.getAdapter();
        if (selected == a.getCount()-1) {
          return;
        }
        Map.Entry<String, String> m = a.getItem(selected);
        a.remove(m);
        a.insert(m, selected + 1);
        selected++;
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
          ArrayAdapter<Map.Entry<String, String>> a = new ArrayAdapter<Map.Entry<String, String>>(WogMmActivity.this, android.R.layout.simple_list_item_1) {
            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
              final Map.Entry<String, String> item = getItem(position);
              TextView tv1;
              CheckBox cb;
              LinearLayout ll;

              if (convertView == null) {
                tv1 = new TextView(WogMmActivity.this);
                tv1.setTextSize(15);
                tv1.setSingleLine();
                tv1.setGravity(Gravity.LEFT);
                tv1.setPadding(5, 5, 5, 5);
                tv1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.FILL_PARENT,
                        (float) 3.0));


                ll = new LinearLayout(WogMmActivity.this);
                ll.setOrientation(LinearLayout.HORIZONTAL);
                ll.setPadding(5, 5, 5, 10);

                tv1.setText(item.getKey());

                cb = new CheckBox(getContext());
                cb.setChecked(Boolean.parseBoolean(item.getValue()));
                cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                  @Override
                  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    item.setValue(isChecked + "");
                  }
                });

                ll.addView(cb);
                ll.addView(tv1);

              } else {
                ll = (LinearLayout) convertView;
                tv1 = (TextView) ll.getChildAt(1);
                cb = (CheckBox) ll.getChildAt(0);
                cb.setChecked(Boolean.parseBoolean(item.getValue()));
                tv1.setText(item.getKey());
              }


              tv1.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                  selected = position;
                  return true;
                }
              });
              return ll;
            }
          };
          a.add(new StringPair("Mod List", "On"));
          modsGrid.setAdapter(a);
          int maxWidth = 0;
          for (File f : files) {
            String name = f.getName();
            int w = getTextWidth(name) + 20;
            if(w > maxWidth) {
              maxWidth = w;
            }
            if (!name.endsWith(".goomod")) {
              continue;
            }
            boolean enabled = enabledMods.contains(name);

            a.add(new StringPair(name, enabled+""));
          }
          modsGrid.setColumnWidth(maxWidth);
          modsGrid.setMinimumWidth(maxWidth);


        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      private int getTextWidth(String s) {
        Paint paint = new Paint();
        Rect bounds = new Rect();

        int text_width = 0;

        paint.setTypeface(Typeface.DEFAULT);// your preference here
        paint.setTextSize(15);// have this the same as your text size

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

  private static final class StringPair implements Map.Entry<String, String> {

    private String b;
    private final String a;

    public StringPair(String a, String b) {
      this.a = a;
      this.b = b;
    }
    @Override
    public String getKey() {
      return a;
    }

    @Override
    public String getValue() {
      return b;
    }

    @Override
    public String setValue(String object) {
      String x = b;
      this.b = object;
      return x;
    }
  }
}