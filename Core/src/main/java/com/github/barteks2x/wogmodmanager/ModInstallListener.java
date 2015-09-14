package com.github.barteks2x.wogmodmanager;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.Map;

import kellinwood.security.zipsigner.ProgressEvent;
import kellinwood.security.zipsigner.ProgressListener;
import kellinwood.security.zipsigner.ZipSigner;
import kellinwood.zipio.ZioEntry;
import kellinwood.zipio.ZipInput;
import kellinwood.zipio.ZipOutput;

import static com.github.barteks2x.wogmodmanager.WogMmActivity.*;
public class ModInstallListener implements View.OnClickListener {

    private final ProgressBar progress;
    private final Activity a;

    public ModInstallListener(Activity a, ProgressBar progress) {
        this.progress = progress;
        this.a=a;
    }
    @Override
    public void onClick(View v) {
        new InstallModsTask(progress, a).execute();
        Button theButton = (Button) v;
        theButton.setEnabled(false);

    }

    private static final class InstallModsTask extends AsyncTask<Void, Double, Boolean> {

        private final ProgressBar progress;
        private final Activity a;
        private PackageManager pkgMgr;

        private int taskNum = 0;
        private final int maxTask = 3;
        public InstallModsTask(ProgressBar progress, Activity act) {
            this.progress = progress;

            this.pkgMgr = act.getPackageManager();
            this.a = act;
        }

        @Override
        protected void onPreExecute() {
            this.progress.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Boolean b) {
            if(!b) {
                return;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "wog-mod-manager/wog-apk-signed.apk")), "application/vnd.android.package-archive");
            a.startActivity(intent);
        }

        @Override
        protected Boolean doInBackground(Void...nothing) {
            if(!this.copyApk()) return false; taskNum++;
            if(!this.modifyApk()) return false; taskNum++;
            this.signApk();
            return true;
        }

        private boolean copyApk() {
            PackageManager pm = pkgMgr;

            String srcDir = null;
            for (ApplicationInfo app : pm.getInstalledApplications(0)) {
                if (app.packageName.equals("com.twodboy.worldofgoofull")) {
                    Log.i(TAG, String.format("Found World of Goo apk in %s", app.sourceDir));
                    srcDir = app.sourceDir;
                }
            }
            if (srcDir == null) {
                Log.i(TAG, "World of Goo apk not found. Is it installed?");
                return false;
            }
            File src = new File(srcDir);

            File dest = new File(Environment.getExternalStorageDirectory(), "wog-mod-manager");

            if (dest.exists() && !dest.isDirectory()) {
                Log.i(TAG, "Destination location already exists and is not a directory. Select different location.");
                return false;
            }
            if (!dest.exists()) {
                dest.mkdirs();
                dest.mkdir();
            }
            File destFile = new File(dest, "wog-apk.apk");
            if (destFile.exists()) {
                if (destFile.isDirectory()) {
                    Log.i(TAG, String.format("%s already exists and is a directory. Cancelling...", destFile.getPath()));
                    return false;
                }
                Log.i(TAG, "%s already exists, skipping");
                this.setTaskProgress(1.00);
                return true;
            }
            try {
                copy(src, destFile);
            } catch (IOException e) {
                Log.e(TAG, "IOException when copying file: %s, cancelling...", e);
                return false;
            }
            if (destFile.exists())
                Log.i(TAG, "Successfully copied World of Goo apk file");
            else
                Log.i(TAG, "For some strange reason copying file went without error but the file doesn't exist.");
            return true;
        }

        private void copy(File src, File dst) throws IOException {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = new FileInputStream(src);
                dst.createNewFile();
                out = new FileOutputStream(dst);

                // Transfer bytes from in to out
                long copiedBytes = 0;
                long toCopy = src.length();
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                    copiedBytes += len;
                    this.setTaskProgress((double)copiedBytes/(double)toCopy);
                }
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        Log.wtf(TAG, "Exception when closing input stream");
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ex) {
                        Log.wtf(TAG, "Exception when closing input stream");
                    }
                }
            }
        }

        public boolean modifyApk() {
            File infile = new File(Environment.getExternalStorageDirectory(), "/wog-mod-manager/wog-apk.apk");
            File outF = new File(Environment.getExternalStorageDirectory(), "/wog-mod-manager/wog-apk-2.apk");

            //TODO: Extract the APK and then use GooTool code to modify files. then re-package it into APK

            //I really need some zip handling library better than kellinswood's zipio...
            try {
                ZipInput zin = ZipInput.read(infile.getPath());
                ZipOutput zout = new ZipOutput(outF);
                Map<String, ZioEntry> entries = zin.getEntries();
                int i = 0;
                for (ZioEntry e : entries.values()) {
                    if (e.getName().contains("AndroidManifest")) {
                        //modify AndroidManifest.xml to allow installing 2 copies of the game
                        InputStream in = e.getInputStream();
                        OutputStream os = e.getOutputStream();
                        modifyAxml(in, os);
                    }
                    if(e.getName().contains("resources.arsc")) {
                        //modify the resources file to change the visible name
                        InputStream in = e.getInputStream();
                        OutputStream os = e.getOutputStream();
                        modifyResources(in, os);
                    }

                    /*
                    //Test code
                    if(false && e.getName().contains("GoingUp.level.mp3")) {
                        File nfile = new File(Environment.getExternalStorageDirectory(), "/wog-mod-manager/GoingUp.level.mp3");
                        InputStream in = new FileInputStream(nfile);

                        OutputStream os = e.getOutputStream();
                        int r;
                        while((r = in.read()) != -1) {
                            os.write(r);
                        }
                    }*/
                    zout.write(e);
                    i++;
                    this.setTaskProgress((i / (double) entries.size())*0.9);
                }
                zout.close();
                zin.close();


            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            this.setTaskProgress(1.00);
            return true;
        }

        private void modifyResources(InputStream in, OutputStream os) throws IOException {
            if(!in.markSupported()) {
                in = new BufferedInputStream(in, 1024);
            }
            Assert.that(in.markSupported());
            int read;
            String toReplace =   "World of Goo";
            String replaceWith = "WoG Mod TEST";
            Assert.that(toReplace.length() == replaceWith.length(), "length of the new name must be the same as the original name");
            while((read = in.read()) != -1) {
                //assume that it contains only ASCII characters
                //it will unless it's some weird modified version that I won't support anyway
                boolean foundString = false;
                if(read == toReplace.charAt(0)) {
                    //assume we found if for now
                    foundString = true;
                    in.mark(toReplace.length());
                    for(int i = 1; i < toReplace.length(); i++) {
                        //if it's the end of the stream we will break here and reset the stream. We will eventually reach that point in the outer loop
                        if(in.read() != toReplace.charAt(i)) {
                            //we didn't really find it :(
                            foundString = false;
                            in.reset();
                            break;
                        }
                    }
                }
                if(foundString) {
                    //write it to output
                    for(int i = 0; i < replaceWith.length(); i++) {
                        os.write(replaceWith.charAt(i));
                    }
                } else {
                    os.write(read);
                }
            }
        }

        private void modifyAxml(InputStream in, OutputStream os) throws IOException {
            //this expects the original World of Goo Manifest.xml file. It probably won't work with any other file
            //
            Integer unknown1, fileSize, unknown3, textEndm4, preTextIntsNum, unknown6, unknown7, textStartm8, unknown9;
            unknown1 = readLeUint(in);
            fileSize = readLeUint(in);
            //System.out.println(fileSize);
            unknown3 = readLeUint(in);
            textEndm4 = readLeUint(in);
            //System.out.println(textEndm4);
            preTextIntsNum = readLeUint(in);
            unknown6 = readLeUint(in);
            unknown7 = readLeUint(in);
            textStartm8 = readLeUint(in);
            unknown9 = readLeUint(in);

            if (anyEqual(null, unknown1, fileSize, unknown3, textEndm4, preTextIntsNum, unknown6, unknown7, textStartm8, unknown9)) {
                throw new IOException("Unexpected end of file");
            }
            Integer[] preTextInts = new Integer[preTextIntsNum];

            for (int i = 0; i < preTextInts.length; i++) {
                preTextInts[i] = readLeUint(in);
            }
            if (anyEqual(null, preTextInts)) {
                throw new IOException("Unexpected end of file");
            }

            LeUtf16String[] strings = new LeUtf16String[preTextInts.length];

            for (int i = 0; i < preTextInts.length; i++) {
                strings[i] = readNextString(in);
            }

            byte unknown[] = readUntilEof(in);

            LeUtf16String[] newStrings = new LeUtf16String[preTextInts.length];

            int[] newPreText = new int[preTextInts.length];
            //characters
            int lenIncrease = 0;
            int changedStrings = 0;
            for (int i = 0; i < strings.length; i++) {
                newStrings[i] = strings[i];
                newPreText[i] = preTextInts[i] + lenIncrease * 2;
                //System.out.println(strings[i].toString());
                if (strings[i].toString().equals("com.twodboy.worldofgoofull")) {
                    lenIncrease += 8;
                    newStrings[i] = new LeUtf16String("com.twodboy.worldofgoofull.mod0000");
                    changedStrings++;
                }
                if (strings[i].toString().equals("World.Of.Goo")) {
                    lenIncrease += 8;
                    newStrings[i] = new LeUtf16String("World.Of.Goo.Mod0000");
                    changedStrings++;
                }
                if (strings[i].toString().equals(".WorldOfGooFull")) {
                    lenIncrease += "com.twodboy.worldofgoofull".length();
                    newStrings[i] = new LeUtf16String("com.twodboy.worldofgoofull.WorldOfGooFull");
                    changedStrings++;
                }
            }
            if (changedStrings != 3) {
                throw new IllegalStateException("Expected changing 3 strings. Changed " + changedStrings);
            }

            int bytesLenIncrease = lenIncrease * 2;

            int padBytes = 0;
            if ((bytesLenIncrease & 3) != 0) {
                padBytes = 4 - (lenIncrease & 3);
            }
            bytesLenIncrease += padBytes;
            System.out.println(padBytes);
            System.out.println(bytesLenIncrease);
            //write initial bytes
            writeLeUint(os, unknown1);
            writeLeUint(os, fileSize + bytesLenIncrease);
            writeLeUint(os, unknown3);
            writeLeUint(os, textEndm4 + bytesLenIncrease);
            writeLeUint(os, preTextIntsNum);
            writeLeUint(os, unknown6);
            writeLeUint(os, unknown7);
            writeLeUint(os, textStartm8);
            writeLeUint(os, unknown9);
            for (int i : newPreText) {
                writeLeUint(os, i);
            }
            for (LeUtf16String str : newStrings) {
                char[] ca = str.getRawData();
                System.out.println(str + ", " + str.length());
                for (char c : ca) {
                    writeChar(os, c);
                }
            }
            for (int i = 0; i < padBytes; i++) {
                os.write(0);
            }
            os.write(unknown);
        }

        private byte[] readUntilEof(InputStream in) throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int read;
            while ((read = in.read()) != -1) {
                bos.write(read);
            }
            return bos.toByteArray();
        }

        private LeUtf16String readNextString(InputStream in) throws IOException {
            char c = readChar(in);
            char[] arr = new char[((c & 0xFF) << 8 | c >>> 8) + 2];
            arr[0] = c;
            //c+1: read null
            for (int i = 0; i < ((c & 0xFF) << 8 | c >>> 8) + 1; i++) {
                arr[i + 1] = readChar(in);
            }
            return new LeUtf16String(arr);
        }

        private boolean anyEqual(Object o, Object... objs) {
            for (Object obj : objs) {
                if ((o == null && obj == null) || (o != null && o.equals(obj))) {
                    return true;
                }
            }
            return false;
        }

        private Integer readLeUint(InputStream in) throws IOException {
            int byte1 = in.read();
            if (byte1 == -1) {
                return null;
            }
            int byte2 = in.read();
            if (byte2 == -1) {
                return null;
            }
            int byte3 = in.read();
            if (byte3 == -1) {
                return null;
            }
            int byte4 = in.read();
            if (byte4 == -1) {
                return null;
            }

            int ret = byte1 | byte2 << 8 | byte3 << 16 | byte4 << 24;
            return ret;
        }

        private void writeLeUint(OutputStream out, int num) throws IOException {
            for (int i = 0; i < 4; i++) {
                out.write((num >>> (i * 8)) & 0xFF);
            }
        }

        private void writeChar(OutputStream out, char num) throws IOException {
            out.write(num >>> 8);
            out.write(num & 0xFF);
        }

        private Character readChar(InputStream in) throws IOException {
            int byte1 = in.read();
            if (byte1 == -1) {
                return null;
            }
            int byte2 = in.read();
            if (byte2 == -1) {
                return null;
            }
            char ret = (char) (byte1 << 8 | byte2);
            return ret;
        }

        private void signApk() {
            File unsigned = new File(Environment.getExternalStorageDirectory(), "/wog-mod-manager/wog-apk-2.apk");
            File signed = new File(Environment.getExternalStorageDirectory(), "/wog-mod-manager/wog-apk-signed.apk");
            try {
                ZipSigner signer = new ZipSigner();
                signer.addProgressListener(new ProgressListener() {
                    @Override
                    public void onProgress(ProgressEvent event) {
                        setTaskProgress(event.getPercentDone() / 100.0d);
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
        protected void onProgressUpdate(Double... i) {
            this.progress.setProgress((int)(i[i.length-1]*100));
        }

        private void setTaskProgress(double p) {
            this.publishProgress((p+taskNum)/maxTask);
        }
    }
}
