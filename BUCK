import re

### Helper functions

# List all jars matching globfile
def get_all_jars(globfile):
    jar_deps = []
    for jarfile in glob([globfile]):
        name = 'jars__' + re.sub(r'^.*/([^/]+)\.jar$', r'\1', jarfile)
        jar_deps.append(':' + name)
        prebuilt_jar(
            name = name,
            binary_jar = jarfile,
        )
    return jar_deps

# Generate the build config values from the gradle.properties file
def get_build_config_values(filename):
    values = ['String APP_PN_KEY = "org.wordpress.android.playstore"']
    for line in open(filename).readlines():
        if line.startswith('wp.'):
            key, value = line.strip().replace(" ", "").split("=")
            key = key.replace("wp.", "").upper().replace(".", "_")
            values.append('String %s = "%s"' % (key, value))
    return values

### Inject versionCode and versionName in the AndroidManifest.xml file

genrule(
    name = 'processed_manifest',
    srcs = [
        'tools/inject_version_in_manifest.py',
        'WordPress/src/main/AndroidManifest.xml',
        'WordPress/build.gradle'
        ],
    cmd = 'tools/inject_version_in_manifest.py WordPress/src/main/AndroidManifest.xml WordPress/build.gradle > $OUT',
    out = 'AndroidManifest.xml',
)

### Define aar and jar dependencies / auto-generated

remote_file(
    name = 'appcompat-v7-file',
    out = 'appcompat-v7.aar',
    url = 'mvn:com.android.support:appcompat-v7:aar:23.1.0',
    sha1 = '0ec99fae8716984ec56489fb45d1ae752724bae7',
)
android_prebuilt_aar(
    name = 'appcompat-v7',
    aar = ':appcompat-v7-file',
)
remote_file(
    name = 'cardview-v7-file',
    out = 'cardview-v7.aar',
    url = 'mvn:com.android.support:cardview-v7:aar:23.1.0',
    sha1 = '9c0994ace3fd3339ea99521e66570adb5df3d5b2',
)
android_prebuilt_aar(
    name = 'cardview-v7',
    aar = ':cardview-v7-file',
)
remote_file(
    name = 'design-file',
    out = 'design.aar',
    url = 'mvn:com.android.support:design:aar:23.1.0',
    sha1 = '88a6c3ea503e2740c2091e8d5112d383a21f05fe',
)
android_prebuilt_aar(
    name = 'design',
    aar = ':design-file',
)
remote_file(
    name = 'recyclerview-v7-file',
    out = 'recyclerview-v7.aar',
    url = 'mvn:com.android.support:recyclerview-v7:aar:23.1.0',
    sha1 = '9baf22ce2d5c1162365bfce00766e47ebd768fbc',
)
android_prebuilt_aar(
    name = 'recyclerview-v7',
    aar = ':recyclerview-v7-file',
)
remote_file(
    name = 'support-v13-file',
    out = 'support-v13.aar',
    url = 'mvn:com.android.support:support-v13:aar:23.1.0',
    sha1 = '77e34b6545e8594b102bf97c50c57071f161f88f',
)
android_prebuilt_aar(
    name = 'support-v13',
    aar = ':support-v13-file',
)
remote_file(
    name = 'support-v4-file',
    out = 'support-v4.aar',
    url = 'mvn:com.android.support:support-v4:aar:23.1.0',
    sha1 = '8820cb60b2eb5c707be237476fef1b78aa3ccdbe',
)
android_prebuilt_aar(
    name = 'support-v4',
    aar = ':support-v4-file',
)
remote_file(
    name = 'wpcomrest-file',
    out = 'wpcomrest.aar',
    url = 'mvn:com.automattic:rest:aar:1.0.2',
    sha1 = 'e632a6f347ae2ae39a01b22c5b2dd8f44d0116da',
)
android_prebuilt_aar(
    name = 'wpcomrest',
    aar = ':wpcomrest-file',
)
remote_file(
    name = 'tracks-file',
    out = 'tracks.aar',
    url = 'mvn:com.automattic:tracks:aar:1.1.0',
    sha1 = '2dbad8d69e7d118b6930115040d284f2319be497',
)
android_prebuilt_aar(
    name = 'tracks',
    aar = ':tracks-file',
)
remote_file(
    name = 'crashlytics-file',
    out = 'crashlytics.aar',
    url = 'mvn:com.crashlytics.sdk.android:crashlytics:aar:2.2.2',
    sha1 = 'b0b2570cfe1a36d8f4b9680ad62d2aabe51dc2f7',
)
android_prebuilt_aar(
    name = 'crashlytics',
    aar = ':crashlytics-file',
)
remote_file(
    name = 'photoview-file',
    out = 'photoview.aar',
    url = 'mvn:com.github.chrisbanes.photoview:library:aar:1.2.4',
    sha1 = '8abf92fe0df72a97547a172474dcd538722a5371',
)
android_prebuilt_aar(
    name = 'photoview',
    aar = ':photoview-file',
)
remote_file(
    name = 'play-services-gcm-file',
    out = 'play-services-gcm.aar',
    url = 'mvn:com.google.android.gms:play-services-gcm:aar:8.1.0',
    sha1 = 'd86d8b4c502c6169773b27496290ed5c0f294d73',
)
android_prebuilt_aar(
    name = 'play-services-gcm',
    aar = ':play-services-gcm-file',
)
remote_file(
    name = 'play-services-base-file',
    out = 'play-services-base.aar',
    url = 'mvn:com.google.android.gms:play-services-base:aar:8.1.0',
    sha1 = '6ec5b3f737b28a64818b5d245d839e2290994a49',
)
android_prebuilt_aar(
    name = 'play-services-basement',
    aar = ':play-services-basement-file',
)
remote_file(
    name = 'play-services-basement-file',
    out = 'play-services-basement.aar',
    url = 'mvn:com.google.android.gms:play-services-basement:aar:8.1.0',
    sha1 = '997dfcce730a948ff7a59d20fa38161a7d513720',
)
android_prebuilt_aar(
    name = 'play-services-base',
    aar = ':play-services-base-file',
)
remote_file(
    name = 'gson-file',
    out = 'gson.jar',
    url = 'mvn:com.google.code.gson:gson:jar:2.2.2',
    sha1 = '1f96456ca233dec780aa224bff076d8e8bca3908',
)
prebuilt_jar(
    name = 'gson',
    binary_jar = ':gson-file',
)
remote_file(
    name = 'helpshift-file',
    out = 'helpshift.aar',
    url = 'mvn:com.helpshift:android-aar:aar:3.12.0',
    sha1 = '113f25994931714e120a675d8abe9703aeab4699',
)
android_prebuilt_aar(
    name = 'helpshift',
    aar = ':helpshift-file',
)
remote_file(
    name = 'volley-file',
    out = 'volley.aar',
    url = 'mvn:com.mcxiaoke.volley:library:aar:1.0.18',
    sha1 = 'eb970d6cf6ae79345692431dc029e26828096758',
)
android_prebuilt_aar(
    name = 'volley',
    aar = ':volley-file',
)
remote_file(
    name = 'mixpanel-android-file',
    out = 'mixpanel-android.aar',
    url = 'mvn:com.mixpanel.android:mixpanel-android:aar:4.6.4',
    sha1 = '2ae3e05fdb0008a5cd0364a63ab03bd99b6b6205',
)
android_prebuilt_aar(
    name = 'mixpanel-android',
    aar = ':mixpanel-android-file',
)
remote_file(
    name = 'simperium-file',
    out = 'simperium.aar',
    url = 'mvn:com.simperium.android:simperium:aar:0.6.6',
    sha1 = '46a189de8974bdbf16ee20fc0cdd99f666650dff',
)
android_prebuilt_aar(
    name = 'simperium',
    aar = ':simperium-file',
)
remote_file(
    name = 'tagsoup-file',
    out = 'tagsoup.jar',
    url = 'mvn:org.ccil.cowan.tagsoup:tagsoup:jar:1.2.1',
    sha1 = '5584627487e984c03456266d3f8802eb85a9ce97',
)
prebuilt_jar(
    name = 'tagsoup',
    binary_jar = ':tagsoup-file',
)
remote_file(
    name = 'drag-sort-listview-file',
    out = 'drag-sort-listview.aar',
    url = 'mvn:org.wordpress:drag-sort-listview:aar:0.6.1',
    sha1 = '238699f638a40b9850d7dfabe65ffdf93cd9bfa2',
)
android_prebuilt_aar(
    name = 'drag-sort-listview',
    aar = ':drag-sort-listview-file',
)
remote_file(
    name = 'emailchecker-file',
    out = 'emailchecker.aar',
    url = 'mvn:org.wordpress:emailchecker:aar:0.3',
    sha1 = 'd5f9d7dbb36560357b4894495366bd80303d031d',
)
android_prebuilt_aar(
    name = 'emailchecker',
    aar = ':emailchecker-file',
)
remote_file(
    name = 'graphview-file',
    out = 'graphview.aar',
    url = 'mvn:org.wordpress:graphview:aar:3.4.0',
    sha1 = '710db0f26a101d4dc6042601e122469d5fcc1439',
)
android_prebuilt_aar(
    name = 'graphview',
    aar = ':graphview-file',
)
remote_file(
    name = 'mediapicker-file',
    out = 'mediapicker.aar',
    url = 'mvn:org.wordpress:mediapicker:aar:1.2.4',
    sha1 = 'ce142db12fb37e6f0d0dc08d76aeb2feb8ac711b',
)
android_prebuilt_aar(
    name = 'mediapicker',
    aar = ':mediapicker-file',
)
remote_file(
    name = 'passcodelock-file',
    out = 'passcodelock.aar',
    url = 'mvn:org.wordpress:passcodelock:aar:1.0.0',
    sha1 = 'b203d519db2f6ec0507fd1cb46e3f001da4db10e',
)
android_prebuilt_aar(
    name = 'passcodelock',
    aar = ':passcodelock-file',
)
remote_file(
    name = 'persistentedittext-file',
    out = 'persistentedittext.aar',
    url = 'mvn:org.wordpress:persistentedittext:aar:1.0.1',
    sha1 = 'ed8c682b51d2bdf70bf4dc879b92bc676422a1e8',
)
android_prebuilt_aar(
    name = 'persistentedittext',
    aar = ':persistentedittext-file',
)
remote_file(
    name = 'slidinguppanel-file',
    out = 'slidinguppanel.aar',
    url = 'mvn:org.wordpress:slidinguppanel:aar:1.0.0',
    sha1 = '225937b13cd93277379dbd5168206706a0f049a7',
)
android_prebuilt_aar(
    name = 'slidinguppanel',
    aar = ':slidinguppanel-file',
)

## Manually created

remote_file(
    name = 'androidpinning-file',
    out = 'androidpinning.aar',
    url = 'mvn:org.thoughtcrime.ssl.pinning:AndroidPinning:aar:1.0.0',
    sha1 = '01a3bcfa0b90580c3119f0eb2a620560b6e99495',
)
android_prebuilt_aar(
    name = 'androidpinning',
    aar = ':androidpinning-file',
)
remote_file(
    name = 'fabric-file',
    out = 'fabric.aar',
    url = 'mvn:io.fabric.sdk.android:fabric:aar:1.2.0',
    sha1 = '3ccb675269c6fc7b002bba0a97318d0109c4e3ae',
)
android_prebuilt_aar(
    name = 'fabric',
    aar = ':fabric-file',
)
remote_file(
    name = 'commons-lang-file',
    out = 'commons-lang.jar',
    url = 'mvn:commons-lang:commons-lang:jar:2.6',
    sha1 = '0ce1edb914c94ebc388f086c6827e8bdeec71ac2',
)
prebuilt_jar(
    name = 'commons-lang',
    binary_jar = ':commons-lang-file'
)
remote_file(
    name = 'support-annotations-file',
    out = 'support-annotations.jar',
    url = 'mvn:com.android.support:support-annotations:jar:23.1.0',
    sha1 = '92e3fc113ec3ee36b64603a38857b95700025633',
)
prebuilt_jar(
    name = 'support-annotations',
    binary_jar = ':support-annotations-file'
)
remote_file(
    name = 'eventbus-file',
    out = 'eventbus.jar',
    url = 'mvn:de.greenrobot:eventbus:jar:2.4.0',
    sha1 = 'ddd166d01b3158d1c00576d29f7ed15c030df719',
)
prebuilt_jar(
    name = 'eventbus',
    binary_jar = ':eventbus-file',
)
remote_file(
    name = 'androidasync-file',
    out = 'androidasync.jar',
    url = 'mvn:com.koushikdutta.async:androidasync:jar:2.1.3',
    sha1 = '52aed89a155265a48984ecc06aa8dec12674edad',
)
prebuilt_jar(
    name = 'androidasync',
    binary_jar = ':androidasync-file',
)

### WordPressUtils

android_build_config(
    name = 'build-config-utils',
    package = 'org.wordpress.android.util',
)

android_resource(
    name = 'wpandroid-utils-res',
    package = 'org.wordpress.android.util',
    res = 'libs/utils/WordPressUtils/src/main/res',
)

android_library(
    name = 'wpandroid-utils',
    srcs = glob(['libs/utils/WordPressUtils/src/main/java/**/*.java']),
    deps = [
        ':support-v4',
        ':wpandroid-utils-res',
        ':build-config-utils',
        ':volley',
        ':commons-lang',
    ]
)

### WordPressEditor

android_resource(
    name = 'wpandroid-editor-res',
    package = 'org.wordpress.android.editor',
    res = 'libs/editor/WordPressEditor/src/main/res',
    # TODO: exclude fonts assets = 'libs/editor/WordPressEditor/src/main/assets',
)

android_library(
    name = 'wpandroid-editor',
    srcs = glob(['libs/editor/WordPressEditor/src/main/java/**/*.java']),
    deps = [
        ':volley',
        ':support-v4',
        ':wpandroid-utils',
        ':wpanalytics',
        ':wpandroid-editor-res',
        ':appcompat-v7',
        ':support-annotations',
    ]
)

### WordPressAnalytics

android_library(
    name = 'wpanalytics',
    srcs = glob(['libs/analytics/WordPressAnalytics/src/main/java/**/*.java']),
    deps = [
        ':wpandroid-utils',
        ':mixpanel-android',
        ':tracks',
    ]
)

### WordPressNetworking

android_library(
    name = 'wpnetworking',
    srcs = glob(['libs/networking/WordPressNetworking/src/main/java/**/*.java']),
    deps = [
        ':wpcomrest',
        ':wpandroid-utils',
        ':volley',
    ]
)

### Main app

keystore(
    name = 'debug_keystore',
    store = 'keystore/debug.keystore',
    properties = 'keystore/debug.keystore.properties',
)

android_resource(
    name = 'zres', # when buck merge resources, it sort them by name,
                   # the last one override previous
    package = 'org.wordpress.android',
    res = 'WordPress/src/main/res',
    assets = 'WordPress/src/main/assets',
    deps = [
        ':appcompat-v7',
        ':design',
        ':passcodelock',
        ':wpandroid-utils',
        ':wpandroid-utils-res',
        ':wpandroid-editor-res',
        ':drag-sort-listview',
        ':mediapicker',
        ':cardview-v7',
    ]
)

android_build_config(
    name = 'build-config',
    package = 'org.wordpress.android',
    values = get_build_config_values('WordPress/gradle.properties'),
)

android_library(
    name = 'main-lib',
    srcs = glob(['WordPress/src/main/java/org/**/*.java']),
    deps = [
        ':wpandroid-editor-res',
        ':zres',
        ':volley',
        ':appcompat-v7',
        ':support-v13',
        ':support-v4',
        ':design',
        ':recyclerview-v7',
        ':cardview-v7',
        ':persistentedittext',
        ':wpandroid-utils',
        ':wpandroid-editor',
        ':wpcomrest',
        ':wpanalytics',
        ':graphview',
        ':build-config',
        ':drag-sort-listview',
        ':simperium',
        ':mediapicker',
        ':slidinguppanel',
        ':passcodelock',
        ':wpnetworking',
        ':helpshift',
        ':emailchecker',
        ':crashlytics',
        ':fabric',
        ':photoview',
        ':androidpinning',
        ':support-annotations',
        ':commons-lang',
        ':eventbus',
        ':gson',
        ':tagsoup',
        ':play-services-gcm',
        ':play-services-base',
        ':play-services-basement',
        ':androidasync',
    ],
)

android_binary(
    name = 'wpandroid',
    manifest = ':processed_manifest',
    keystore = ':debug_keystore',
    package_type = 'debug',
    deps = [
        ':main-lib',
    ],
)
