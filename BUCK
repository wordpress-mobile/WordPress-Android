import re

### Rule to fetch dependencies, doesn't work on `android_prebuilt_aar`
### rules (no deps)

genrule(
    name = 'fetch_deps',
    srcs = [
        'tools/fetch_buck_dependencies.py',
    ],
    cmd = 'tools/fetch_buck_dependencies.py ../../../extlibs > $OUT',
    out = '../../extlibs/dependencies.log',
)

### Helper functions

# Macro to add fetch_deps as default dependency
def r_android_library(name, srcs=[], resources=[], exported_deps=[],
                      deps=[], visibility=[]):
    android_library(
        name = name,
        srcs = srcs,
        resources = resources,
        exported_deps = exported_deps,
        deps = [
          # This assumes this is where Guava is in your project.
          ':fetch_deps',
        ] + deps,
        visibility = visibility,
    )

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

### Define jar dependencies

r_android_library(
    name = 'all-jars',
    exported_deps = get_all_jars('extlibs/*.jar'),
)

### Define aar dependencies

android_prebuilt_aar(
    name = 'appcompat-v7',
    aar = 'extlibs/appcompat-v7.aar',
)

android_prebuilt_aar(
    name = 'android-support-v13',
    aar = 'extlibs/android-support-v13.aar',
)

android_prebuilt_aar(
    name = 'android-support-v4',
    aar = 'extlibs/android-support-v4.aar',
)

android_prebuilt_aar(
    name = 'cardview-v7',
    aar = 'extlibs/cardview-v7.aar',
)

android_prebuilt_aar(
    name = 'recyclerview-v7',
    aar = 'extlibs/recyclerview-v7.aar',
)

android_prebuilt_aar(
    name = 'mixpanel',
    aar = 'extlibs/mixpanel.aar',
)

android_prebuilt_aar(
    name = 'crashlytics',
    aar = 'extlibs/crashlytics.aar',
)

android_prebuilt_aar(
    name = 'fabric',
    aar = 'extlibs/fabric.aar',
)

android_prebuilt_aar(
    name = 'mediapicker',
    aar = 'extlibs/mediapicker.aar',
)

android_prebuilt_aar(
    name = 'drag-sort-listview',
    aar = 'extlibs/drag-sort-listview.aar',
)

android_prebuilt_aar(
    name = 'simperium',
    aar = 'extlibs/simperium.aar',
)

android_prebuilt_aar(
    name = 'undobar',
    aar = 'extlibs/undobar.aar',
)

android_prebuilt_aar(
    name = 'slidinguppanel',
    aar = 'extlibs/slidinguppanel.aar',
)

android_prebuilt_aar(
    name = 'passcodelock',
    aar = 'extlibs/passcodelock.aar',
)

android_prebuilt_aar(
    name = 'tracks',
    aar = 'extlibs/tracks.aar',
)

android_prebuilt_aar(
    name = 'emailchecker',
    aar = 'extlibs/emailchecker.aar',
)

android_prebuilt_aar(
    name = 'helpshift',
    aar = 'extlibs/helpshift.aar',
)

android_prebuilt_aar(
    name = 'photoview',
    aar = 'extlibs/photoview.aar',
)

android_prebuilt_aar(
    name = 'androidpinning',
    aar = 'extlibs/androidpinning.aar',
)

android_prebuilt_aar(
    name = 'floatingactionbutton',
    aar = 'extlibs/floatingactionbutton.aar',
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

r_android_library(
    name = 'wpandroid-utils',
    srcs = glob(['libs/utils/WordPressUtils/src/main/java/**/*.java']),
    deps = [
        ':android-support-v4',
        ':wpandroid-utils-res',
        ':build-config-utils',
        ':all-jars',
    ]
)

### WordPressEditor

android_resource(
    name = 'wpandroid-editor-res',
    package = 'org.wordpress.android.editor',
    res = 'libs/editor/WordPressEditor/src/main/res',
    assets = 'libs/editor/WordPressEditor/src/main/assets',
)

r_android_library(
    name = 'wpandroid-editor',
    srcs = glob(['libs/editor/WordPressEditor/src/main/java/**/*.java']),
    deps = [
        ':all-jars', # volley
        ':android-support-v4',
        ':wpandroid-utils',
        ':wpanalytics',
        ':wpandroid-editor-res',
        ':appcompat-v7',
    ]
)

### PersistentEditText

android_resource(
    name = 'persistentedittext-res',
    package = 'org.wordpress.persistentedittext',
    res = 'libs/persistentedittext/PersistentEditText/src/main/res',
)

r_android_library(
    name = 'persistentedittext',
    srcs = glob(['libs/persistentedittext/PersistentEditText/src/main/java/**/*.java']),
    deps = [
        ':persistentedittext-res',
    ]
)

### WPComRest

r_android_library(
    name = 'wpcomrest',
    srcs = glob(['libs/wpcomrest/WordPressComRest/src/main/java/**/*.java']),
    deps = [
        ':all-jars', # volley
    ]
)

### WPGraphView

r_android_library(
    name = 'wpgraphview',
    srcs = glob(['libs/graphview/WordPressGraphView/src/main/java/**/*.java']),
)


### WordPressAnalytics

r_android_library(
    name = 'wpanalytics',
    srcs = glob(['libs/analytics/WordPressAnalytics/src/main/java/**/*.java']),
    deps = [
        ':wpandroid-utils',
        ':mixpanel',
        ':tracks',
    ]
)

### WordPressNetworking

r_android_library(
    name = 'wpnetworking',
    srcs = glob(['libs/networking/WordPressNetworking/src/main/java/**/*.java']),
    deps = [
        ':wpcomrest',
        ':wpandroid-utils',
        ':all-jars', # volley
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
        ':wpandroid-utils',
        ':wpandroid-utils-res',
        ':persistentedittext-res',
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

r_android_library(
    name = 'main-lib',
    srcs = glob(['WordPress/src/main/java/org/**/*.java']),
    deps = [
        ':wpandroid-editor-res',
        ':zres',
        ':all-jars',
        ':appcompat-v7',
        ':android-support-v13',
        ':android-support-v4',
        ':recyclerview-v7',
        ':cardview-v7',
        ':persistentedittext',
        ':wpandroid-utils',
        ':wpandroid-editor',
        ':wpcomrest',
        ':wpanalytics',
        ':wpgraphview',
        ':build-config',
        ':drag-sort-listview',
        ':simperium',
        ':mediapicker',
        ':undobar',
        ':slidinguppanel',
        ':passcodelock',
        ':wpnetworking',
        ':helpshift',
        ':emailchecker',
        ':crashlytics',
        ':fabric',
        ':photoview',
        ':androidpinning',
        ':floatingactionbutton',
    ],
)

android_binary(
    name = 'wpandroid',
    manifest = ':processed_manifest',
    keystore = ':debug_keystore',
    package_type = 'debug',
    deps = [
        ':fetch_deps',
        ':main-lib',
    ],
)
