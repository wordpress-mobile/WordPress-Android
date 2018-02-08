#!/bin/sh

### Misc defines
# Commands
CMD_SETUP="setup"
CMD_TAKE="take"
CMD_PROCESS="process"
CMD_PREPARE="prepare"

# Config
APK=../WordPress/build/outputs/apk/WordPress-wasabi-debug.apk
AVD=Nexus_5X_API_25_SCREENSHOTS
RUN_DEV=All
LOG_FILE="/tmp/android-screenshot.log"
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

# Color/formatting support
OUTPUT_NORM="\033[0m"
OUTPUT_RED="\033[31m"
OUTPUT_GREEN="\033[32m"
OUTPUT_BOLD="\033[1m"

### Functions
# Show script usage, commands and options
function show_usage() {
    # Help message
    echo "Usage: $exeName command [options]"
    echo ""
    echo "   Available commands:"
    echo "      $CMD_SETUP:\tbrings up the required environment, with a local copy of the SDK and a dedicated device"
    echo "      $CMD_TAKE:\texecutes the app in the simulator and takes the screenshots"
    echo "      $CMD_PROCESS:\tprocesses the screenshots and generates the modded ones"
    echo "      $CMD_PREPARE:\tautomatically runs the $CMD_TAKE and $CMD_PROCESS commands"
    echo ""
    echo "   $CMD_SETUP command:"
    echo "   \tUsage: $exeName $CMD_SETUP"
    echo "   $CMD_TAKE command:"
    echo "   \tUsage: $exeName $CMD_TAKE [device-type] [avd-name] [apk-path]"
    echo "   $CMD_PROCESS command:"
    echo "   \tUsage: $exeName $CMD_PROCESS [device-type]"
    echo "   $CMD_PREPARE command:"
    echo "   \tUsage: $exeName $CMD_PREPARE [device-type] [avd-name] [apk-path]"
    echo ""
    echo "   Params:"
    echo "   \t - device-type:"
    echo "   \t         all: runs on all the available device sizes"
    echo "   \t         phone: runs on the phone size"
    echo "   \t         tab7: runs on the Tab7 size"
    echo "   \t         tab10: runs on the Tab10 size"
    echo "   \t - avd-name: the name of the simulator device"
    echo "   \t - apk-path: the path of the apk to use"
    echo ""
    echo "   Example: $exeName $CMD_TAKE"
    echo "   Example: $exeName $CMD_TAKE phone"
    echo "   Example: $exeName $CMD_PROCESS tab7"
    echo "   Example: $exeName $CMD_PREPARE all Android_Accelerated_x86"
    echo "   Example: $exeName $CMD_TAKE all Android_Accelerated_x86 ./app.apk"
    echo ""
    exit 1
}

# Show Helpers
function show_error_message() {
    message=$1
    echo "$OUTPUT_RED$message$OUTPUT_NORM"
    echo $message >> $LOG_FILE
}

function show_ok_message() {
    message=$1
    echo "$OUTPUT_GREEN$message$OUTPUT_NORM" 
    echo $message >> $LOG_FILE
}

function show_title_message() {
    message=$1
    echo "$OUTPUT_BOLD$message$OUTPUT_NORM" 
    echo $message >> $LOG_FILE
}

function show_message() {
    echo "$1" | tee -a $LOG_FILE
}

# Appends an init line to the log
function start_log() {
    dateTime=`date "+%d-%m-%Y - %H:%M:%S"`
    echo "$exeName started at $dateTime" >> $LOG_FILE
}

# Appends a closing line to the log
function stop_log() {
    dateTime=`date "+%d-%m-%Y - %H:%M:%S"`
    echo "$exeName terminated at $dateTime" >> $LOG_FILE
    echo "" >> $LOG_FILE
    echo "Log location: $LOG_FILE"
}

# Writes an error message and exits
function stop_on_error() {
    show_error_message "Operation failed. Aborting."
    show_error_message "See log for further details."
    stop_log
    exit 1
}

# Shows the current configuration
function show_config() {
  show_title_message "Configuration:"
  show_message "Device: $RUN_DEV"
  show_message "Emulator: $AVD"
  show_message "Package: $APK ($PKG)"
}

# Checks and setups the working dir
function require_dirs {
  if [ ! -d "$WORKING_DIR" ]; then
    show_message Creating working directory...
    mkdir "$WORKING_DIR" >> $LOG_FILE 2>&1 || stop_on_error
    show_message Done
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

# Setups the local sdk and configure the environment
function require_sdk() {
  if [ -z "$ANDROID_SDK_DIR" ]; then
    require_dirs

    if [ ! -d "$WORKING_DIR/sdk/tools" ]; then
      command -v wget >/dev/null 2>&1 || { show_message "wget command not installed"; stop_on_error; }

      show_message "Downloading the Android SDK..."
      mkdir -p "$WORKING_DIR/sdk" >> $LOG_FILE 2>&1 || stop_on_error
      wget -qO- $SDK_ZIP_URL | tar xf - -C "$WORKING_DIR/sdk" >> $LOG_FILE 2>&1 || stop_on_error
      show_message Done

      show_message "Downloading the Android platform (over 2GB so, this might take a while)"
      yes y | sdkmanager "platform-tools" "platforms;android-26" "system-images;android-25;google_apis;x86" "emulator" >> $LOG_FILE 2>&1 || stop_on_error
      show_message Done
    fi
  fi

  show_message "Setting up SDK..."
  export ANDROID_SDK_DIR="$WORKING_DIR/sdk"
  export PATH=$PATH:"$WORKING_DIR/sdk/tools":"$WORKING_DIR/sdk/tools/bin":"$WORKING_DIR/sdk/platform-tools/"
  show_message "Done"
}

# Creates the local emulator
function require_emu {
  avdmanager list avd -c | grep $AVD
  avdmissing=$?

  if [ $avdmissing = 1 ]; then
    show_message "Creating AVD..."
    echo no | avdmanager create avd -n $AVD -k "system-images;android-25;google_apis;x86" --tag "google_apis" >> $LOG_FILE 2>&1 || stop_on_error
  fi
}

# Setups the environment
function execute_setup() {
  show_title_message "Setting up the emulation environment..."
  require_dirs
  require_sdk
  require_emu
  show_message "Done!"
}

# Loads and checks the token for the magic login
# Also loads other configuration that is in the screenshot-config.sh file
function require_deeplink {
  if [ -f "screenshot-config.sh" ]; then
    . screenshot-config.sh
  fi

  if [ -z "$TOKEN_DEEPLINK" ]; then
    show_error_message "TOKEN_DEEPLINK variable is not set correctly. Make sure the file screenshot-config.sh is present and looks like this:"
    show_error_message ""
    show_error_message "#!/bin/sh"
    show_error_message "TOKEN_DEEPLINK=wordpress://magic-login?token=<secret login token>" 
    show_error_message ""
    stop_on_error
  fi

  if ! [[ "$TOKEN_DEEPLINK" =~ ^wordpress:\/\/magic-login\?token=* ]]; then
    show_error_message "TOKEN_DEEPLINK format is invalid.";
    stop_on_error
  fi
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

### Script main
exeName=$(basename "$0" ".sh")

# Params check
if [ "$#" -lt 1 ] || [ -z $1 ]; then
  show_usage
fi

if [ "$1" == $CMD_SETUP ] && [ "$#" -gt 1 ]; then
  show_usage
fi

if [ "$1" == $CMD_PROCESS ] && [ "$#" -gt 2 ]; then
  show_usage
fi

if [ "$#" -gt 4 ]; then
  show_usage
fi

start_log
# Load deeplink and configuration
require_deeplink

# Load params
CMD=$1
if ! [ -z $2 ]; then
  RUN_DEV=$2
fi
if ! [ -z $3 ]; then
  AVD=$3
fi 
if ! [ -z $4 ]; then
  APK=$4
fi

# Update user
show_config

# Launch command
if [ $CMD == $CMD_SETUP ]; then
  execute_setup 
else 
  show_usage
fi
stop_log
exit 0

require_font
require_imagemagick

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

