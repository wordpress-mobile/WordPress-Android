#!/bin/bash

APK=../WordPress/build/outputs/apk/WordPress-wasabi-debug.apk

AVD=Nexus_5X_API_25_SCREENSHOTS

WORKING_DIR=./autoscreenshot

DEVICES=(PHONE TAB7 TAB10)
LOCALES=(en-US el-GR it-IT)
SCREENS=(MYSITE READER NOTIFS)

GEEKY_TIME='0830'

FONT_DIR=noto
FONT_FILE=$WORKING_DIR/$FONT_DIR/NotoSerif-Bold.ttf
FONT_ZIP_URL='https://fonts.google.com/download?family=Noto%20Serif'

SDK_ZIP_URL='https://dl.google.com/android/repository/sdk-tools-darwin-3859397.zip'

PHONE_APP_HEIGHT=1388
#PHONE_NAV_HEIGHT=96
PHONE_NAV_HEIGHT=0
PHONE_SKIN_WIDTH=840

SKIN_HEIGHT=$(($PHONE_APP_HEIGHT+$PHONE_NAV_HEIGHT))
SKIN=$PHONE_SKIN_WIDTH'x'$PHONE_SKIN_HEIGHT

TAB7_APP_HEIGHT=1368
#TAB7_NAV_HEIGHT=96
TAB7_NAV_HEIGHT=0
TAB7_SKIN_WIDTH=910

TAB10_APP_HEIGHT=1426
#TAB10_NAV_HEIGHT=96
TAB10_NAV_HEIGHT=0
TAB10_SKIN_WIDTH=1242

LCD_DPI=320

ADB_PARAMS="-e"

PKG_RELEASE=org.wordpress.android
PKG_DEBUG=org.wordpress.android.beta
PKG=$PKG_DEBUG

ACTIVITY_LAUNCH=org.wordpress.android.ui.WPLaunchActivity

PHONE_COORDS_MYSITE="100 100"
PHONE_COORDS_READER="300 100"
PHONE_COORDS_NOTIFS="700 100"

TAB7_COORDS_MYSITE="110 100"
TAB7_COORDS_READER="330 100"
TAB7_COORDS_NOTIFS="800 100"

TAB10_COORDS_MYSITE="300 100"
TAB10_COORDS_READER="520 100"
TAB10_COORDS_NOTIFS="930 100"

TEXT_OFFSET_Y=58

TEXT_SIZE_en_US=80
TEXT_en_US_MYSITE="Manage your site\neverywhere you go"
TEXT_en_US_READER="Enjoy your\nfavourite sites"
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

function require_dirs {
  if [ ! -d "$WORKING_DIR" ]; then
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
    require_dirs

    if [ ! -d "$WORKING_DIR/sdk/tools" ]; then
      echo -n Downloading the Android SDK...
      mkdir -p "$WORKING_DIR/sdk"
      wget -qO- $SDK_ZIP_URL | tar xf - -C "$WORKING_DIR/sdk" #&>/dev/null
      echo Done

      echo 'Downloading the Android platform (over 2GB so, this might take a while)'
      yes y | sdkmanager "platform-tools" "platforms;android-26" "system-images;android-25;google_apis;x86" "emulator" &>/dev/null
      echo Done
    fi

    echo Setting up SDK...
    export ANDROID_SDK_DIR="$WORKING_DIR/sdk"
    export PATH=$PATH:"$WORKING_DIR/sdk/tools":"$WORKING_DIR/sdk/tools/bin":"$WORKING_DIR/sdk/platform-tools/"
    echo Done
  fi

  export PATH=$PATH:$ANDROID_SDK_DIR/tools:$ANDROID_SDK_DIR/tools/bin
}

function require_emu {
  avdmanager list avd -c | grep $AVD
  avdmissing=$?

  if [ $avdmissing = 1 ]; then
    echo Creating AVD
    echo no | avdmanager create avd -n $AVD -k "system-images;android-25;google_apis;x86" --tag "google_apis" &>/dev/null
  fi
}

function require_deeplink {
  if [ -f "tokendeeplink.sh" ]; then
    . tokendeeplink.sh
  fi

  if [ -z "$TOKEN_DEEPLINK" ]; then
    echo 'TOKEN_DEEPLINK variable is not set correctly. Make sure the file tokendeeplink.sh is present and looks like this:'
    echo 
    echo '#!/bin/sh'
    echo 'TOKEN_DEEPLINK=wordpress://magic-login?token=<secret login token>' 
    exit 1
  fi

  if ! [[ "$TOKEN_DEEPLINK" =~ ^wordpress:\/\/magic-login\?token=* ]]; then
    echo "TOKEN_DEEPLINK format is invalid.";
    exit 1
  fi;
}

function start_emu {
  echo -n Starting emulator... 
  device=$1
  device_app_height=$device\_APP_HEIGHT
  device_nav_height=$device\_NAV_HEIGHT
  device_skin_height=$((${!device_app_height}+${!device_nav_height}))
  device_skin_width=$device\_SKIN_WIDTH
  device_skin=${!device_skin_width}'x'$device_skin_height
  $ANDROID_SDK_DIR/tools/emulator -verbose -no-boot-anim -timezone "Europe/UTC" -avd $AVD -skin $device_skin -qemu -lcd-density $LCD_DPI &>/dev/null &
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

function kill_emus {
  echo -n Killing emulators...
  adb -e devices | grep emulator | cut -f1 | while read line; do adb -e -s $line emu kill; done
	wait 10
  echo Done
}

function uninstall {
  echo -n Uninstalling any previous app instances... 
  adb $ADB_PARAMS shell pm uninstall $PKG &>/dev/null
  echo Done
}

function install {
  echo -n Installing app... 
  adb $ADB_PARAMS install -r $APK &>/dev/null
  echo Done
}

function login {
  echo -n Logging in via magiclink... 
  adb $ADB_PARAMS shell am start -W -a android.intent.action.VIEW -d $TOKEN_DEEPLINK $PKG &>/dev/null
  wait 5 # wait for app to finish logging in
  echo Done
}

function start_app {
  echo -n Starting app... 
  adb $ADB_PARAMS shell am start -n $PKG/$ACTIVITY_LAUNCH &>/dev/null
  wait 5 # wait for app to finish start up
  echo Done
}

function kill_app {
  echo -n Killing the app...
  adb $ADB_PARAMS shell am force-stop $PKG &>/dev/null
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

function geekytime() {
  echo Setting geeky time
  adb $ADB_PARAMS root &>/dev/null
  adb $ADB_PARAMS shell "date -u 0101$GEEKY_TIME\2017.00 ; am broadcast -a android.intent.action.TIME_SET" &>/dev/null
  adb $ADB_PARAMS unroot &>/dev/null
}

require_deeplink
require_font
require_imagemagick
require_sdk
require_emu

for device in ${DEVICES[*]}; do
  kill_emus
  start_emu $device
  wait_emu
  uninstall
  install
  login

  kill_app # kill the app so when restarting we don't have any first-time launch effects like promo dialogs and such

  start_app

  kill_app # kill the app so when restarting we don't have any first-time launch effects like promo dialogs and such

  for loc in ${LOCALES[*]}; do
    locale $loc

    start_app

    for screen in ${SCREENS[*]}; do
      coords=$device\_COORDS_$screen
      tap_on ${!coords}

      geekytime
      produce $device $loc $screen
    done
  done
done

