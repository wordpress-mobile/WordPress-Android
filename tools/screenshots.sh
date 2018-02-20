#!/bin/sh

### Misc defines
# Commands
CMD_TAKE="take"
CMD_PROCESS="process"
CMD_PRODUCE="produce"

# Config
APK=../WordPress/build/outputs/apk/wasabi/debug/WordPress-wasabi-debug.apk
DEV_NAME=Nexus_5X_API_25_SCREENSHOTS
RUN_DEV=ALL
LOG_FILE="/tmp/android-screenshot.log"
WORKING_DIR=./autoscreenshot
TAKE_DIR=$WORKING_DIR/orig
PROD_DIR=$WORKING_DIR/final

DEVICES=(PHONE TAB7 TAB10)
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

PHONE_COORDS_LOGIN1="420 1100"
PHONE_COORDS_LOGIN2="220 385"
PHONE_COORDS_LOGIN3="735 680"
PHONE_COORDS_LOGIN4="425 700"
PHONE_COORDS_LOGIN5="250 470"
PHONE_COORDS_LOGIN6="735 680"

TAB7_COORDS_LOGIN1="453 1078"
TAB7_COORDS_LOGIN2="237 377"
TAB7_COORDS_LOGIN3="793 666"
TAB7_COORDS_LOGIN4="459 686"
TAB7_COORDS_LOGIN5="270 460"
TAB7_COORDS_LOGIN6="793 666"

TAB10_COORDS_LOGIN1="621 1130"
TAB10_COORDS_LOGIN2="325 396"
TAB10_COORDS_LOGIN3="1086 698"
TAB10_COORDS_LOGIN4="628 719"
TAB10_COORDS_LOGIN5="369 483"
TAB10_COORDS_LOGIN6="1086 698"

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

TEXT_SIZE_default=70
TEXT_SIZE_en_US=80
TEXT_SIZE_el_GR=70
TEXT_SIZE_it_IT=80

PHONE_TEMPLATE=android-phone-template2.png
PHONE_OFFSET="+121+532"
TAB7_TEMPLATE=android-tab7-template2.png
TAB7_OFFSET="+145+552"
TAB10_TEMPLATE=android-tab10-template2.png
TAB10_OFFSET="+148+622"

# Langs
LANG_FILE="exported-language-codes.csv"
LangGlotPress=("en-us" "ar" "az" "bg" "cs" "cy" "da" "de" "el" "en-au" "en-ca" "en-gb" "es" "es-cl" "es-co" "es-ve" "eu" "fi" \
"fr" "gd" "gl" "hi" "he" "hr" "hu" "id" "is" "it" "ja" "ka" "ko" "lv" "lt" "mk" "ms" "nb" \
"nl" "pl" "pt" "pt-br" "ro" "ru" "sk" "sl" "sq" "sr" "sv" "th" "tr" \
"uk" "uz" "vi" "zh" "zh-cn" "zh-tw")
LangGooglePlay=("en-US" "ar" "az-AZ" "bg" "cs-CZ" "cy" "da-DK" "de-DE" "el-GR" "en-AU" "en-CA" "en-GB" "es-ES" "es-rCL" "es-rVE" "es-rCO" "eu-ES" "fi-FI" \
"fr-FR" "gd-GB" "gl-ES" "hi-IN" "iw-IL" "hr" "hu" "id" "is-IS" "it-IT" "ja-JP" "ka-IN" "ko-KR" "lv" "lt" "mk" "ms" "nb-NO" \
"nl-NL" "pl-PL" "pt-PT" "pt-BR" "ro" "ru-RU" "sk" "sl" "sq" "sr" "sv-SE" "th" "tr-TR" \
"uk" "uz" "vi" "zh-CN" "zh-CN" "zh-TW")

# Color/formatting support
OUTPUT_NORM="\033[0m"
OUTPUT_RED="\033[31m"
OUTPUT_GREEN="\033[32m"
OUTPUT_YELLOW="\033[33m"
OUTPUT_BOLD="\033[1m"

### Functions
# Show script usage, commands and options
function show_usage() {
    # Help message
    echo "Usage: $exeName command [options]"
    echo ""
    echo "   Available commands:"
    echo "      $CMD_TAKE:\texecutes the app in the simulator and takes the screenshots"
    echo "      $CMD_PROCESS:\tprocesses the screenshots and generates the modded ones"
    echo "      $CMD_PRODUCE:\tautomatically runs the $CMD_TAKE and $CMD_PROCESS commands"
    echo ""
    echo "   $CMD_TAKE command:"
    echo "   \tUsage: $exeName $CMD_TAKE [device] [device-name] [apk-path]"
    echo "   $CMD_PROCESS command:"
    echo "   \tUsage: $exeName $CMD_PROCESS [device]"
    echo "   $CMD_PRODUCE command:"
    echo "   \tUsage: $exeName $CMD_PRODUCE [device] [device-name] [apk-path]"
    echo ""
    echo "   Params:"
    echo "   \t - device:"
    echo "   \t         emu-all: runs on all the available emulated device sizes"
    echo "   \t         emu-phone: runs on the emulated phone size"
    echo "   \t         emu-tab7: runs on the emulated Tab7 size"
    echo "   \t         emu-tab10: runs on the emulated Tab10 size"
    echo "   \t         dev-phone: runs on the selected phone device"
    echo "   \t         dev-tab7: runs on the selected Tab7 device"
    echo "   \t         dev-tab10: runs on the selected Tab10 device"
    echo "   \t - device-name: the name of the device to use (avd for simulated devices, id for other devices"
    echo "   \t - apk-path: the path of the apk to use"
    echo ""
    echo "   Example: $exeName $CMD_TAKE"
    echo "   Example: $exeName $CMD_TAKE emu-phone"
    echo "   Example: $exeName $CMD_PROCESS emu-tab7"
    echo "   Example: $exeName $CMD_PRODUCE emu-all Android_Accelerated_x86"
    echo "   Example: $exeName $CMD_TAKE emu-all Android_Accelerated_x86 ./app.apk"
    echo ""
    exit 1
}

# Show Helpers
function show_error_message() {
    message=$1
    echo "$OUTPUT_RED$message$OUTPUT_NORM"
    echo $message >> $LOG_FILE
}


function show_warning_message() {
    message=$1
    echo "$OUTPUT_YELLOW$message$OUTPUT_NORM"
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
  if [ $USE_EMU -eq 1 ]; then
    show_message "Device: $RUN_DEV (emulated)"
    show_message "Emulator: $DEV_NAME"
  else
    show_message "Device: $DEV_NAME"
  fi 
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

# Checks and setups the working dir for taking the screenshots
function require_image_dirs() {
  compDir=$1/$2/$3  
  require_dirs

  if [ ! -d "$compDir" ]; then
    show_message "Creating screenshot directory..."
    mkdir -p "$compDir" >> $LOG_FILE 2>&1 || stop_on_error
    show_message "Done"
  fi 
}

# Checks the availability of the promo font
function require_font {
  if [ ! -f "$FONT_FILE" ]; then
    show_message "Font file is missing."
    require_dirs

    show_message "Downloading..."
    command -v wget >/dev/null 2>&1 || { show_error_message "wget command not installed. Please, install it!"; stop_on_error; }
    wget $FONT_ZIP_URL -O "$WORKING_DIR/noto.zip" >> $LOG_FILE 2>&1 || stop_on_error
    show_message "Done"

    show_message "Unzipping..."
    unzip "$WORKING_DIR/noto.zip" -d "$WORKING_DIR/$FONT_DIR/" >> $LOG_FILE 2>&1 || stop_on_error
    show_message "Done"
  fi
}

# Checks for ImageMagick
function require_imagemagick {
  which magick &>/dev/null
  immissing=$?

  if [ $immissing = 1 ]; then
    show_message "Installing ImageMagick..."
    exec brew install imagemagick
  fi
}

# Checks the required AVD exists 
function check_avd() {
  avdmanager list avd -c | grep $DEV_NAME
  avdmissing=$?

  if [ $avdmissing = 1 ]; then
    show_error_message "Emulator $DEV_NAME doesn't exist."
    show_message "Available devices are:"
    avdmanager list avd -c
    stop_on_error
  fi
}

# Checks the required device exists
function check_real_dev() {
  adb devices | grep $DEV_NAME
  devmissing=$?

  if [ $devmissing = 1 ]; then
    show_error_message "Device $DEV_NAME doesn't exist."
    show_message "Available devices are:"
    adb devices
    stop_on_error
  fi
}

# Loads and checks configuration that is in the screenshot-config.sh file
function require_config {
  if [ -f "screenshots-config.sh" ]; then
    . screenshots-config.sh
  fi

  if [ -z "$LOGIN_USERNAME" ] || [ -z "$LOGIN_PASSWORD" ]; then
    show_error_message "LOGIN_USERNAME variable is not set correctly. Make sure the file screenshots-config.sh is present and the login data is filled"
    show_error_message ""
    stop_on_error
  fi
}

# Tries to start the selected emulator
function start_emu {
  show_message "Starting emulator..." 
  device=$1
  device_app_height=$device\_APP_HEIGHT
  device_nav_height=$device\_NAV_HEIGHT
  device_skin_height=$((${!device_app_height}+${!device_nav_height}))
  device_skin_width=$device\_SKIN_WIDTH
  device_skin=${!device_skin_width}'x'$device_skin_height
  $ANDROID_SDK_DIR/tools/emulator -verbose -no-boot-anim -timezone "Europe/UTC" -avd $DEV_NAME -skin $device_skin -qemu -lcd-density $LCD_DPI >> $LOG_FILE 2>&1 || stop_on_error &
  wait 5
  show_message Done
}

# Waits for the emu to start
function wait_emu {
  show_message "Waiting for device boot..." 
  adb $ADB_PARAMS wait-for-device

  # poll and wait until device has booted
  A=$(adb $ADB_PARAMS shell getprop sys.boot_completed | tr -d '\r')
  while [ "$A" != "1" ]; do
    sleep 2
    A=$(adb $ADB_PARAMS shell getprop sys.boot_completed | tr -d '\r')
  done
  show_message "Done"
}

# Kills any running emu
function kill_emus {
  show_message "Killing emulators..."
  adb -e devices | grep emulator | cut -f1 | while read line; do adb -e -s $line emu kill; done
	wait 10
  show_message "Done"
}

# Unistalls previous app instances
function uninstall {
  show_message "Uninstalling any previous app instances..." 
  adb $ADB_PARAMS shell pm uninstall $PKG >> $LOG_FILE 2>&1 || stop_on_error
  show_message "Done"
}

# Install the selected app
function install {
  show_message "Installing app..." 
  adb $ADB_PARAMS install -r $APK >> $LOG_FILE 2>&1 || stop_on_error
  show_message "Done"
}

# Exeutes the taps for the login steps
function login_tap() {
  login_offest=$1
  fast_tap_on ${!login_coords} $login_offest
}

# Tries to login via the magic link
function login {
  show_message "Logging in..." 

  if [ $USE_EMU -eq 0 ]; then
   dev_h=$device\_APP_HEIGHT
   dev_w=$device\_SKIN_WIDTH
   adb $ADB_PARAMS shell wm size ${!dev_w}x${!dev_h} >> $LOG_FILE 2>&1 || stop_on_error
   adb $ADB_PARAMS shell wm density $LCD_DPI >> $LOG_FILE 2>&1 || stop_on_error
   wait 5
  fi
  
  device=$1
  login_coords=$device\_COORDS_LOGIN1
  login_tap
  login_coords=$device\_COORDS_LOGIN2
  login_tap
  login_tap
  show_message "Setting username..."
  adb $ADB_PARAMS shell input text $LOGIN_USERNAME >> $LOG_FILE 2>&1 || stop_on_error
  login_coords=$device\_COORDS_LOGIN3
  login_tap
  login_coords=$device\_COORDS_LOGIN4 
  login_tap 100
  login_coords=$device\_COORDS_LOGIN5
  login_tap
  show_message "Setting password..."
  adb $ADB_PARAMS shell input text $LOGIN_PASSWORD >> $LOG_FILE 2>&1 || stop_on_error
  login_coords=$device\_COORDS_LOGIN6
  login_tap

  wait 5 # wait for app to finish logging in
  
  if [ $USE_EMU -eq 0 ]; then
    adb $ADB_PARAMS shell wm size reset >> $LOG_FILE 2>&1 || stop_on_error
    adb $ADB_PARAMS shell wm density reset >> $LOG_FILE 2>&1 || stop_on_error
    wait 5
  fi

  show_message "Done"
}

# Starts the app
function start_app {
  show_message "Starting app..." 
  adb $ADB_PARAMS shell am start -n $PKG/$ACTIVITY_LAUNCH >> $LOG_FILE 2>&1 || stop_on_error
  wait 5 # wait for app to finish start up
  show_message "Done"
}

# Kills the running app
function kill_app {
  show_message "Killing the app..."
  adb $ADB_PARAMS shell am force-stop $PKG >> $LOG_FILE 2>&1 || stop_on_error
  show_message "Done"
}

# Tapping on the provided coordinates
function tap_on {
  show_message "Tapping on $1x$2..."
  adb $ADB_PARAMS shell input tap $1 $2 >> $LOG_FILE 2>&1 || stop_on_error
  wait 10
  show_message "Done"
}

# Tapping on the provided coordinates
function fast_tap_on {
  if [ x$3 == x ]; then
    fty=$2
  else
    fty=$(($2+$3))
  fi
  show_message "Tapping on $1x$fty..."
  adb $ADB_PARAMS shell input tap $1 $fty >> $LOG_FILE 2>&1 || stop_on_error
  wait 3
  show_message "Done"
}

# Simple wait
function wait() {
  #echo -n Waiting for $1 seconds...
  sleep $1
  #echo Done
}

# Takes the actual screenshot
function screenshot() {
  show_message "Taking screenshot with name $1..."
  adb $ADB_PARAMS shell screencap -p /sdcard/$1.png >> $LOG_FILE 2>&1 || stop_on_error
  show_message  "Pulling the file to $TAKE_DIR/$2/$3/$1..."
  adb $ADB_PARAMS pull /sdcard/$1.png $TAKE_DIR/$2/$3/$1.png >> $LOG_FILE 2>&1 || stop_on_error
  show_message "Done"
}

# Takes the screenshot
function take_screenshot() {
  show_message "Taking image for $1 $2 $3"
  locdevice=$1
  loc=${2/-/_} # replace the - with _
  screen=$3
  fn=wpandroid_$locdevice\_$loc\_$screen
  
  screenshot $fn $1 $2
  show_message "Screenshot at: $TAKE_DIR/$1/$2/$fn.png"
}

# Process the screenshot adding the promo text
function process_screenshot() {
  show_message "Processing image for $1 $2 $3"
  device=$1
  loc=${2/-/_} # replace the - with _
  screen=$3
  fn=wpandroid_$device\_$loc\_$screen
  
  tn=$TAKE_DIR/$1/$2/$fn
  fn=$PROD_DIR/$1/$2/$fn

  text=TEXT_$loc\_$screen
  
  # Check text
  if [ -z "${!text}" ]; then
    show_warning_message "No traslation for $loc $screen: using default language"
    text=TEXT_en_US\_$screen
    size=TEXT_SIZE_en_US
    warningCount=$((warningCount+1))
  else
    size=TEXT_SIZE_$loc
  fi 

  # Check size
  if [ -z ${!size} ]; then
    size=TEXT_SIZE_default
  fi

  # Check screenshot file and process
  if [ -f $tn.png ]; then
    if [ $USE_EMU -eq 0 ]; then
      dev_target_height=$device\_APP\_HEIGHT
      dev_target_width=$device\_SKIN\_WIDTH
      magick $tn.png -resize ${!dev_target_width}x${!dev_target_height}\! $fn\_r.png || warningCount=$((warningCount+1))
      tn=$fn\_r
    fi

    template=$device\_TEMPLATE
    offset=$device\_OFFSET
 
    magick $tn.png -crop 0x$APP_HEIGHT+0+0 $fn\_cropped.png || warningCount=$((warningCount+1))
    magick ${!template} $fn\_cropped.png -geometry ${!offset} -composite $fn\_comp1.png || warningCount=$((warningCount+1))
    magick $fn\_comp1.png -gravity north -pointsize ${!size} -font $FONT_FILE -draw "fill white text 0,$TEXT_OFFSET_Y \"${!text}\"" $fn\_final.png || warningCount=$((warningCount+1))

    if [ $USE_EMU -eq 0 ]; then
      rm $fn\_cropped.png $fn\_comp1.png $fn\_r.png
    else
      rm $fn\_cropped.png $fn\_comp1.png
    fi

    show_message "Image ready: $fn\_final.png"
  else
    show_warning_message "No screenshot for $device, $loc, $screen (file: $tn.jpg): skipping it."
    warningCount=$((warningCount+1))
  fi
}

# Sets the required locale
function locale() {
  show_message "Preparing to set locale..."
  adb $ADB_PARAMS root >> $LOG_FILE 2>&1 || stop_on_error
  show_message "Done"
  show_message "Setting locale to $1..."
  adb $ADB_PARAMS shell "setprop ro.product.locale $1; setprop persist.sys.locale $1; stop; sleep 5; start" >> $LOG_FILE 2>&1 || stop_on_error
	wait 10
	wait_emu
	wait 10
}

# Sets the "Geeky" time 
function geekytime() {
  show_message "Setting geeky time..."
  adb $ADB_PARAMS root >> $LOG_FILE 2>&1 || stop_on_error
  adb $ADB_PARAMS shell "date -u 0101$GEEKY_TIME\2017.00 ; am broadcast -a android.intent.action.TIME_SET" >> $LOG_FILE 2>&1 || stop_on_error
  if [ $USE_EMU -eq 1 ]; then
    adb $ADB_PARAMS unroot >> $LOG_FILE 2>&1 || stop_on_error
  fi
}

# Checks the apk package exists
function check_apk() {
  if [ ! -f $APK ]; then
    show_error_message "Apk file not found at $APK"
    stop_on_error
  fi
}

# Evaluate the device configuration
function eval_dev() {
  RUN_DEV=`echo "$RUN_DEV" | awk '{print toupper($0)}'`
  
  if [[ $RUN_DEV == EMU-* ]]; then
    USE_EMU=1
    RUN_DEV=${RUN_DEV#EMU-}
  elif [[ $RUN_DEV == DEV-* ]]; then
    USE_EMU=0
    RUN_DEV=${RUN_DEV#DEV-}
  else
    show_usage
  fi
}

# Checks the device to be valid
function check_device() {
  if [ $USE_EMU -eq 1 ] && [ $RUN_DEV != "ALL" ] && [[ ! " ${DEVICES[@]} " =~ " ${RUN_DEV} " ]]; then
    show_error_message "Unknown emulated device $RUN_DEV"
    stop_on_error
  fi
}

# Evaluate proper tap coords for the current device
function evalute_device_coords() {
  # Retrieving screen size and density from the device
  screen_size_string=`adb $ADB_PARAMS shell wm size | grep "Physical" | cut -d: -f2`
  screen_size=$(echo $screen_size_string | tr "x" "\n")
  screen_size_arr=($screen_size)

  # Evaluating scale factors
  width_mult=$(echo "scale=2; ${screen_size_arr[0]}/$PHONE_SKIN_WIDTH" | bc)
  height_mult=$(echo "scale=2; ${screen_size_arr[1]}/$PHONE_APP_HEIGHT" | bc)
}

# Checks for a command
function check_command() {
  cmdut=$1
  command -v $cmdut >/dev/null 2>&1 || { show_message "$cmdut command not available. Please, assure the SDK is installed and the path in screenshot-config.sh is correct."; stop_on_error; }
}

# checks the emulator is available
function check_emulator() {
  PATH=$PATH:$ANDROID_SDK_DIR:$ANDROID_SDK_DIR/tools/:$ANDROID_SDK_DIR/tools/bin/:$ANDROID_SDK_DIR/platform-tools/
  
  check_command avdmanager
  check_command adb
  check_command emulator

  # Update vars
  if [ -z $ANDROID_SDK_DIR ]; then
    EMU_PATH="$(which emulator)"
    ANDROID_TOOL_DIR="$(dirname "${EMU_PATH}")"
    ANDROID_SDK_DIR="${ANDROID_TOOL_DIR%/*}"
  fi
}

# Checks that parameters are meaningful
function check_params() {
  check_apk
  check_device
  check_emulator
}

# Gets the promo strings
function download_promo_strings() {
  # This function will download strings from GlotPress
  # In this version this is simulated and strings are embedded in the code
  TEXT_en_US_MYSITE="Manage your site\neverywhere you go"
  TEXT_en_US_READER="Enjoy your\nfavourite sites"
  TEXT_en_US_NOTIFS="Get notified\nin real-time"

  TEXT_el_GR_MYSITE="Έλεγξε τον ιστότοπο σου\nαπ'οπουδήποτε βρίσκεσαι"
  TEXT_el_GR_READER="Απόλαυσε τις\nαγαπημένες σου σελίδες"
  TEXT_el_GR_NOTIFS="Ειδοποιήσεις σε\nπραγματικό χρόνο"

  TEXT_it_IT_MYSITE="Gestisci il tuo sito\novunque tu sia"
  TEXT_it_IT_READER="Leggi i tuoi\nsiti preferiti"
  TEXT_it_IT_NOTIFS="Rimani aggiornato\nin tempo reale"
}

# Translates language codes
function glotPress_googlePlay() {
  lang=$1
  index=0
  for data in "${LangGlotPress[@]}"; do
    if [ $data == $lang ]; then
      return $index
    fi
    index=$((index+1))
  done

  return 255
}

# Processes the screenshot for the provided device
function process_device() {
  device=$1

  for line in $(cat $LANG_FILE); do
    cod=$(echo $line|cut -d "," -f1|tr -d " ")
    glotPress_googlePlay $cod
    locIdx=$?

    if [ $locIdx -lt 255 ]; then
      locLang=${LangGooglePlay[$locIdx]}
      require_image_dirs $PROD_DIR $device $locLang

      for screen in ${SCREENS[*]}; do
        process_screenshot $device $locLang $screen
      done

    else
      show_warning_message "Skipping language $cod as no Google Play pair was found."
      warningCount=$((warningCount+1))
    fi
  done
}

# Takes the screenshot for the provided device
function screenshot_device() {
  device=$1
  
  if [ $USE_EMU -eq 1 ]; then
    check_avd 
    ADB_PARAMS="-e"
    kill_emus
    start_emu $device
    wait_emu
  else
    ADB_PARAMS="-s $DEV_NAME"
    check_real_dev 
    evalute_device_coords
  fi

  uninstall
  install
  start_app
  login $device

  kill_app # kill the app so when restarting we don't have any first-time launch effects like promo dialogs and such

  start_app

  kill_app # kill the app so when restarting we don't have any first-time launch effects like promo dialogs and such

  for line in $(cat $LANG_FILE); do
    cod=$(echo $line|cut -d "," -f1|tr -d " ")
    glotPress_googlePlay $cod
    locIdx=$?

    if [ $locIdx -lt 255 ]; then
      locLang=${LangGooglePlay[$locIdx]}
      require_image_dirs $TAKE_DIR $device $locLang 

      locale $locLang

      start_app

      for screen in ${SCREENS[*]}; do
        coords=$device\_COORDS_$screen
        if [ $USE_EMU -eq 0 ]; then
          a1=(${!coords})
          tap1x=$(echo "scale=0; ${a1[0]}*$width_mult" | bc)
          tap1y=$(echo "scale=0; ${a1[1]}*$height_mult" | bc)
          tap_coords="${tap1x%.*} ${tap1y%.*}"
          tap_on $tap_coords
        else
          tap_on ${!coords}
        fi

        geekytime
        take_screenshot $device $locLang $screen
      done

    else
      show_warning_message "Skipping language $cod as no Google Play pair was found."
      warningCount=$((warningCount+1))
    fi
  done
}

# Takes the required screenshots
function execute_take() {
  check_params
  show_title_message "Starting the screenshot taker process..."
  warningCount=0
  if [ $RUN_DEV == "ALL" ]; then
    for device in ${DEVICES[*]}; do
      screenshot_device $device
    done
  else 
    screenshot_device $RUN_DEV
  fi

  if [ $CMD == $CMD_TAKE ]; then
    if [ $warningCount -eq 0 ]; then
      show_ok_message "Done!"
    else
      show_ok_message "Done (with $warningCount warning(s))!"
    fi
  fi
}

# Processes the screenshots
function execute_process() {
  require_font
  require_imagemagick
  
  download_promo_strings

  show_title_message "Starting the screenshot processor..."
  if [ $CMD == $CMD_PROCESS ]; then
    warningCount=0
  fi

  if [ $RUN_DEV == "ALL" ]; then
    for device in ${DEVICES[*]}; do
      process_device $device
    done
  else 
    process_device $RUN_DEV
  fi

  if [ $warningCount -eq 0 ]; then
    show_ok_message "Done!"
  else
    show_ok_message "Done (with $warningCount warning(s))!"
  fi
}

### Script main
exeName=$(basename "$0" ".sh")

# Params check
if [ "$#" -lt 1 ] || [ -z $1 ]; then
  show_usage
fi

if [ "$1" == $CMD_PROCESS ] && [ "$#" -gt 2 ]; then
  show_usage
fi

if [ "$#" -gt 4 ]; then
  show_usage
fi

start_log
# Load configuration
require_config

# Load params
CMD=$1
if ! [ -z $2 ]; then
  RUN_DEV=$2
fi
if ! [ -z $3 ]; then
  DEV_NAME=$3
fi 
if ! [ -z $4 ]; then
  APK=$4
fi

# Evaluate the configuration
eval_dev
show_config

# Launch command
if [ $CMD == $CMD_TAKE ]; then
  execute_take
elif [ $CMD == $CMD_PROCESS ]; then
  execute_process
elif [ $CMD == $CMD_PRODUCE ]; then
  execute_take
  execute_process
else 
  show_usage
fi
stop_log
exit 0





