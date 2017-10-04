#!/bin/bash

# TODO: check for adb
# TODO: check for imagemagick

. tokendeeplink.sh

if [ -z "$TOKEN_DEEPLINK" ]; then
	echo "TOKEN_DEEPLINK variable is not set correctly. Make sure the file tokendeeplink.sh is present and properly sets the variable."; 
	exit 1
fi

if ! [[ "$TOKEN_DEEPLINK" =~ ^wordpress:\/\/magic-login\?token=* ]]; then
	echo "TOKEN_DEEPLINK format is invalid.";
	exit 1
fi;

AVD=Nexus_5X_API_25_SCREENSHOTS

WORKING_DIR=./autoscreenshot

FONT_DIR=noto
FONT_FILE=$WORKING_DIR/$FONT_DIR/NotoSerif-Bold.ttf
FONT_ZIP_URL='https://fonts.google.com/download?family=Noto%20Serif'

APP_HEIGHT=1388
#NAV_HEIGHT=96
NAV_HEIGHT=0
SKIN_WIDTH=840
SKIN_HEIGHT=$(($APP_HEIGHT+$NAV_HEIGHT))
SKIN=$SKIN_WIDTH'x'$SKIN_HEIGHT
LCD_DPI=320

ADB_PARAMS="-e"

APK=../WordPress/build/outputs/apk/WordPress-wasabi-debug.apk
PKG_RELEASE=org.wordpress.android
PKG_DEBUG=org.wordpress.android.beta
ACTIVITY_LAUNCH=org.wordpress.android.ui.WPLaunchActivity

COORDS_MYSITE="100 100"
COORDS_READER="300 100"
COORDS_NOTIFS="700 100"

TEXT_OFFSET_Y=58

TEXT_SIZE_en_US=80
TEXT_en_US_MYSITE="Manage your site\neverywhere you go"
TEXT_en_US_READER="Enjoy you\nfavorite sites"
TEXT_en_US_NOTIFS="Get notified\nin real-time"

TEXT_SIZE_el_GR=70
TEXT_el_GR_MYSITE="Έλεγξε τον ιστότοπο σου\nαπ'οπουδήποτε βρίσκεσαι"
TEXT_el_GR_READER="Απόλαυσε τις\nαγαπημένες σου σελίδες"
TEXT_el_GR_NOTIFS="Ειδοποιήσεις σε\nπραγματικό χρόνο"

TEXT_SIZE_it_IT=80
TEXT_it_IT_MYSITE="Gestisci il tuo sito\novunque tu sia"
TEXT_it_IT_READER="Leggi i tuoi\nsiti preferiti"
TEXT_it_IT_NOTIFS="Rimani aggiornato\nin tempo reale"

PHONE_TEMPLATE=android-phone-template2.png
PHONE_OFFSET="+121+532"
TAB7_TEMPLATE=android-tab7-template2.png
TAB7_OFFSET="+145+552"
TAB10_TEMPLATE=android-tab10-template2.png
TAB10_OFFSET="+148+622"

function require_emu {
  avdmanager list avd -c | grep $AVD
  avdmissing=$?

  if [ $avdmissing = 1 ]; then
    echo Creating AVD
    echo no | avdmanager create avd -n $AVD -k "system-images;android-25;google_apis;x86" &>/dev/null
  fi
}

function require_dirs {
  if [ ! -d "$WORDKING_DIR" ]; then
    echo -n Creating working directory...
    mkdir "$WORKING_DIR"
    echo Done
  fi 
}

function require_font {
  if [ ! -f "$FONT_FILE" ]; then
    echo Font file is missing.
    require_dirs

    echo -n Downloading...
    wget $FONT_ZIP_URL -O "$WORKING_DIR/noto.zip" &>/dev/null
    echo Done

    echo -n Unzipping...
    unzip "$WORKING_DIR/noto.zip" -d "$WORKING_DIR/$FONT_DIR/" &>/dev/null
    echo Done
  fi
}

function require_imagemagick {
  which magick &>/dev/null
  immissing=$?

  if [ $immissing = 1 ]; then
    echo Installing ImageMagick...
    exec brew install imagemagick
  fi
}

function require_sdk {
  if [ -z "$ANDROID_SDK_DIR" ]; then
    echo "ANDROID_SDK_DIR variable is not set correctly. That usually means that the Android sdk is not installed."; 
    exit 1
  fi

  export PATH=$PATH:$ANDROID_SDK_DIR/tools:$ANDROID_SDK_DIR/tools/bin
}

function start_emu {
  echo -n Starting emulator... 
  $ANDROID_SDK_DIR/tools/emulator -verbose -avd $AVD -skin $SKIN -qemu -lcd-density $LCD_DPI &>/dev/null &
  echo Done
}

function wait_emu {
  echo -n Waiting for device boot... 
  adb $ADB_PARAMS wait-for-device

  # poll and wait until device has booted
  A=$(adb $ADB_PARAMS shell getprop sys.boot_completed | tr -d '\r')
  while [ "$A" != "1" ]; do
    sleep 2
    A=$(adb $ADB_PARAMS shell getprop sys.boot_completed | tr -d '\r')
  done
  echo Done
}

function uninstall {
  echo -n Uninstalling any previous app instances... 
  adb $ADB_PARAMS shell pm uninstall $PKG_DEBUG &>/dev/null
  echo Done
}

function install {
  echo -n Installing app... 
  adb $ADB_PARAMS install -r $APK &>/dev/null
  echo Done
}

function login {
  echo -n Logging in via magiclink... 
  adb $ADB_PARAMS shell am start -W -a android.intent.action.VIEW -d $TOKEN_DEEPLINK $PKG_DEBUG &>/dev/null
  wait 5 # wait for app to finish logging in
  echo Done
}

function start_app {
  echo -n Starting app... 
  adb $ADB_PARAMS shell am start -n $PKG_DEBUG/$ACTIVITY_LAUNCH &>/dev/null
  wait 5 # wait for app to finish start up
  echo Done
}

function kill_app {
  echo -n Killing the app...
  adb $ADB_PARAMS shell am force-stop $PKG_DEBUG &>/dev/null
  echo Done
}

function tap_on {
  echo -n Tapping on $1x$2...
  adb $ADB_PARAMS shell input tap $1 $2 &>/dev/null
  wait 10
  echo Done
}

function wait() {
  #echo -n Waiting for $1 seconds...
  sleep $1
  #echo Done
}

function screenshot() {
  echo -n Taking screenshot with name $1...
  adb $ADB_PARAMS shell screencap -p /sdcard/$1.png &>/dev/null
  echo -n  pulling the file...
  adb $ADB_PARAMS pull /sdcard/$1.png ./$1.png &>/dev/null
  echo Done
}

function produce() {
  echo "Producing image for $1 $2 $3"
  device=$1
  loc=${2/-/_} # replace the - with _
  screen=$3
  fn=wpandroid_$device\_$loc\_$screen
  
  screenshot $fn
  magick $fn.png -crop 0x$APP_HEIGHT+0+0 $fn\_cropped.png
  template=$device\_TEMPLATE
  offset=$device\_OFFSET
  magick ${!template} $fn\_cropped.png -geometry ${!offset} -composite $fn\_comp1.png

  text=TEXT_$loc\_$screen
  size=TEXT_SIZE_$loc
  magick $fn\_comp1.png -gravity north -pointsize ${!size} -font $FONT_FILE -draw "fill white text 0,$TEXT_OFFSET_Y \"${!text}\"" $fn\_final.png

  rm $fn.png $fn\_cropped.png $fn\_comp1.png
  echo Image ready: $fn\_final.png
}

function locale() {
  echo -n Preparing to set locale...
  adb $ADB_PARAMS root &>/dev/null
  echo Done
  echo -n Setting locale to $1...
  adb $ADB_PARAMS shell "setprop ro.product.locale $1; setprop persist.sys.locale $1; stop; sleep 5; start" &>/dev/null
	wait 10
	wait_emu
	wait 10
}

require_font
require_imagemagick
require_sdk
require_emu
start_emu
wait_emu
uninstall
install
login

kill_app # kill the app so when restarting we don't have any first-time launch effects like promo dialogs and such

start_app

kill_app # kill the app so when restarting we don't have any first-time launch effects like promo dialogs and such

for device in PHONE #TAB7 TAB10
do
  for loc in en-US el-GR it-IT
  do
    locale $loc

    start_app

    for screen in MYSITE READER NOTIFS
    do
      coords=COORDS_$screen
      tap_on ${!coords}

      produce $device $loc $screen
    done
  done
done

