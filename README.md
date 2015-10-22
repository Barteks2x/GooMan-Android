World of Goo Mod Manager for Android
=======================

This experimental project allows to install simple mods on android version of World of Goo WITHOUT ROOTED DEVICE.

Current state: Allows to install goomods from known directory (<emulated storage>/android/data/com.github.barteks2x.wogmodmanager/files/addins). Most (all?) goomods that work on PC can be installed..

Plans for the future
-----------
- Allow multiple modded/unmodded installation
- Backup saves from mods (without rooted device it is impossible to backup original World of Goo saves)
- Allow setting visible name of World of Goo installation (because I don't want to reverse engineer android resources format it will be limited to the length of the original name


How it works?
------------
On android all .apk files are readable for other programs, but they cannot be modified. This program copied the .apk file to a known location, extracts it (as it's simple .zip file), modifies some files, compresses it again into .apk, uses zipsigner library to sign the .apk and asks user to install it.
To actually make it possible to install the new .apk it's needed to modifiy AndroidManifest.xml which is xml file fonverted to a binary format. When installing mods manually via PC some programs are used to "decompile" and "recompile" the .xml file. There is no such tool for android so I needed to write a tool that replaces some text in this file.
The other difficuilty is Resources file - it contains the visible app name. In general case to change it most of the file would probably need to be modified. But if length of the name stays the same - the string can be simply replaced.

All level/ball files are unencrypted in the android version, but they have .mp3 appended to their name (which REALLY confuses file browsers and media players). 

Once files from .apk files are extracted - gootool code is used to install goomods. 

Limitations
-----------
 * It's impossible to access saves from the original installation on unrooted devices
 * Visible game name must have the the same or smaller length as the original (it is possible to change it, but it's  very hard)
 * On unrooted devices the modified game can't be installed automatically without user interaction
 * Modifying the .apk is quite slow - it may be an issue for those who frequently add/remove/enable/disable mods
