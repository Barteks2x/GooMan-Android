/*
 * Copyright (c) 2008, 2009, 2010 David C A Croft. All rights reserved. Your use of this computer software
 * is permitted only in accordance with the GooTool license agreement distributed with this file.
 */

package com.goofans.gootool.platform;

import net.infotrek.util.prefs.FilePreferencesFactory;

import com.goofans.gootool.util.Utilities;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.List;

/**
 * TODO: merge it with AndroidSupport
 * On android there is only one supported platform so a lot of code has been removed.
 *
 * Platform support abstraction class.
 * <p/>
 * Also handles setting up an alternative preferences store if -preferences &lt;file&gt; is set on command line.
 *
 * @author David Croft (davidc@goofans.com)
 * @version $Id: PlatformSupport.java 415 2010-09-09 19:05:48Z david $
 */
public abstract class PlatformSupport
{
  private static final Logger log = Logger.getLogger(PlatformSupport.class.getName());

  private static PlatformSupport support = new AndroidSupport();


  protected PlatformSupport()
  {
  }

  public static boolean preStartup(List<String> args)
  {
    String prefsFile = null;

    for (int i = 0; i < args.size(); i++) {
      String arg = args.get(i);

      /* File preferences via -preferences */
      if ("-preferences".equals(arg)) {
        if (i + 1 >= args.size()) {
          throw new RuntimeException("Must specify a filename when using -preferences");
        }
        args.remove(i);
        prefsFile = args.get(i);
        args.remove(i);
        i--;
      }
    }

    if (prefsFile != null) {
      System.setProperty("java.util.prefs.PreferencesFactory", FilePreferencesFactory.class.getName());
      System.setProperty(FilePreferencesFactory.SYSTEM_PROPERTY_FILE, prefsFile);
      log.info("Preferences will be stored in " + FilePreferencesFactory.getPreferencesFile());
    }
    else {
      log.finest("Preferences will be stored using system defaults (" + Preferences.systemRoot().getClass() + ")");
    }

    return true;//support.doPreStartup(args);
  }

  public static String[] getProfileSearchPaths()
  {
    return support.doGetProfileSearchPaths();
  }

  protected abstract String[] doGetProfileSearchPaths();

  public static File getToolStorageDirectory() throws IOException
  {
    File dir = support.doGetToolStorageDirectory();
    Utilities.mkdirsOrException(dir);
    return dir;
  }

  protected abstract File doGetToolStorageDirectory() throws IOException;
}
