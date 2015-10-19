package com.github.barteks2x.wogmodmanager;

import android.util.Log;

import com.goofans.gootool.util.ProgressListener;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import kellinwood.zipio.ZioEntry;
import kellinwood.zipio.ZipInput;
import kellinwood.zipio.ZipOutput;

public class IOUtils {
  public static final void extractZip(File f, File to, ProgressListener pl) {
    byte[] buffer = new byte[1024];

    try {

      //create output directory is not exists
      File folder = to;
      if (!folder.exists()) {
        folder.mkdir();
      }

      final long size = getUncompressedZipSize(f);
      long unpackedBytes = 0;
      pl.progressStep(0.08f);
      //get the zip file content
      ZipInputStream zis =
              new ZipInputStream(new FileInputStream(f));
      //get the zipped file list entry
      ZipEntry ze;
      while ((ze = zis.getNextEntry()) != null) {

        String fileName = ze.getName();
        File newFile = new File(to + File.separator + fileName);

        System.out.println("file unzip : " + newFile.getAbsoluteFile());

        //create all non exists folders
        //else you will hit FileNotFoundException for compressed folder
        new File(newFile.getParent()).mkdirs();

        FileOutputStream fos = new FileOutputStream(newFile);

        int len;
        while ((len = zis.read(buffer)) > 0) {
          fos.write(buffer, 0, len);
          unpackedBytes += len;
          pl.progressStep((unpackedBytes / (float)size) * 0.9F + 0.1F);
        }

        fos.close();
      }

      zis.closeEntry();
      zis.close();

      System.out.println("Done");

    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  private static long getUncompressedZipSize(File f) throws IOException {
    //get the zip file content
    ZipInputStream zis =
            new ZipInputStream(new FileInputStream(f));
    ZipEntry e;
    long s = 0;
    while((e = zis.getNextEntry()) != null) {
      s += e.getSize();
    }
    return s;
  }


  public static InputStream getResource(String path) {
    try {
      return WoGInitData.getContext().getAssets().open(path);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Uses given ZipBase as source zip file for zipio library and replaces content of existing files with files in soedified location.
   *
   */
  public static void zipDirContentWithZipBase(File baseZip, File sourceDir, File outFile) throws IOException {
    //Hacked to support writing World of Goo APK...
    ZipInput zin = ZipInput.read(baseZip.getPath());
    ZipOutput zout = new ZipOutput(outFile);

    Set<File> filesToAdd = getAllFilesToAdd(sourceDir);

    Map<String, ZioEntry> zinEntries = zin.getEntries();

    ZioEntry baseEntry = zinEntries.get("assets/res/islands/island1.xml.mp3").getClonedEntry("UNNAMED");
    Iterator<Map.Entry<String, ZioEntry>> it = zinEntries.entrySet().iterator();
    //write entries that are there already
    while(it.hasNext()) {
      Map.Entry<String, ZioEntry> entry = it.next();
      File f = new File(sourceDir, entry.getKey()).getCanonicalFile();
      if(!f.exists()) {
        Log.w(WogMmActivity.TAG, "File doesn't exist, skipping: " + f);
        Assert.that(!filesToAdd.contains(f));
        continue;
      }
      Log.i(WogMmActivity.TAG, "Zipping file: " + f);
      InputStream in = new BufferedInputStream(new FileInputStream(f));
      OutputStream os = entry.getValue().getOutputStream();
      writeInToOut(in, os);
      os.close();
      zout.write(entry.getValue());
      in.close();
      filesToAdd.remove(f);
      it.remove();
      //HACK HACK HACK!
      //Force zipio to delete current OutputStream by creating a new one
      //It will still neave the ZioEntry in memory, but the big ByteArrayOutputStream will be deleted
      entry.getValue().getOutputStream();
    }
    //new entries will be assets, so take example asset
    //zipio will copy properties of example file like zip specification version, compression method etc...
    //they can't be set in any other way, except reflection hacks


    int stripLength = sourceDir.getCanonicalPath().length() + 1;
    //add all other files
    for(File f : filesToAdd) {
      ZioEntry e = baseEntry.getClonedEntry(f.getPath().substring(stripLength));
      InputStream in = new BufferedInputStream(new FileInputStream(f));
      OutputStream os = e.getOutputStream();
      writeInToOut(in, os);
      os.close();
      zout.write(e);
      in.close();
      //HACK. Just in case the ZioEntry is stored SOMEWHERE:
      e.getOutputStream();
    }

    zout.close();
    zin.close();
  }

  private static Set<File> getAllFilesToAdd(File sourceDir) throws IOException {
    Set<File> set = new HashSet<>();
    scanDirectoryRecursive(sourceDir, set);
    return set;
  }

  private static void scanDirectoryRecursive(File dir, Set<File> set) throws IOException {
    for(File f : dir.listFiles()) {
      if(f.isDirectory()) {
        scanDirectoryRecursive(f, set);
        continue;
      }
      set.add(f.getCanonicalFile());
    }
  }

  private static void addFilesFromDir(File sourceDir, ZipOutputStream out, File startPath) throws IOException {
    Assert.that(sourceDir.getPath().startsWith(startPath.getPath()));

    byte[] buf = new byte[10*1024];
    for(File f : sourceDir.listFiles()) {
      if (f.isDirectory()) {
        addFilesFromDir(f, out, startPath);
        continue;
      }
      System.out.println("Adding file to zip: " + f);
      out.putNextEntry(new ZipEntry(f.getPath().substring(startPath.getPath().length() + 1)));

      InputStream in = new BufferedInputStream(new FileInputStream(f));
      int l;
      while ((l = in.read(buf)) != -1) {
        out.write(buf, 0, l);
      }
      in.close();
      out.closeEntry();
    }
  }

  public static void writeInToOut(InputStream in, OutputStream out) throws IOException {
    byte[] buf = new byte[16*1024];
    int l;
    while((l = in.read(buf)) != -1) {
      out.write(buf, 0, l);
    }
  }


  public static List<String> getLines(File file) throws FileNotFoundException {
    Scanner s = new Scanner(file);
    List<String> l = new ArrayList<>();
    while(s.hasNextLine()) {
      l.add(s.nextLine());
    }
    s.close();
    return l;
  }
}
