import re

jar_deps = []
for jarfile in glob(['extlibs/*.jar']):
    name = 'jars__' + re.sub(r'^.*/([^/]+)\.jar$', r'\1', jarfile)
    jar_deps.append(':' + name)
    prebuilt_jar(
        name = name,
        binary_jar = jarfile,
    )

android_library(
    name = 'all-jars',
    exported_deps = jar_deps,
)

android_prebuilt_aar(
    name = 'appcompat',
    aar = 'extlibs/appcompat-v7-21.0.3.aar',
)

android_prebuilt_aar(
    name = 'cardview',
    aar = 'extlibs/cardview-v7-21.0.3.aar',
)

android_prebuilt_aar(
    name = 'mediapicker',
    aar = 'extlibs/mediapicker-1.1.4.aar',
)

android_prebuilt_aar(
    name = 'drag-sort-listview',
    aar = 'extlibs/drag-sort-listview-0.6.1.aar',
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
        ':wpandroid-utils-res',
        ':build-config-utils',
        ':all-jars', # support-v4 needed here
    ]
)

### WordPressEditor

android_resource(
    name = 'wpandroid-editor-res',
    package = 'org.wordpress.android.editor',
    res = 'libs/editor/WordPressEditor/src/main/res',
    assets = 'libs/editor/WordPressEditor/src/main/assets',
)

android_library(
    name = 'wpandroid-editor',
    srcs = glob(['libs/editor/WordPressEditor/src/main/java/**/*.java']),
    deps = [
        ':all-jars', # volley
        ':wpandroid-utils',
        ':wpanalytics',
        ':wpandroid-editor-res',
    ]
)

### PersistentEditText

android_resource(
    name = 'persistentedittext-res',
    package = 'org.wordpress.persistentedittext',
    res = 'libs/persistentedittext/PersistentEditText/src/main/res',
)

android_library(
    name = 'persistentedittext',
    srcs = glob(['libs/persistentedittext/PersistentEditText/src/main/java/**/*.java']),
    deps = [
        ':persistentedittext-res',
    ]
)

### WordPressAnalytics

android_library(
    name = 'wpanalytics',
    srcs = glob(['libs/analytics/WordPressAnalytics/src/main/java/**/*.java']),
    deps = [
        ':wpandroid-utils',
        ':all-jars', # tracks needed
    ]
)

### Main app

keystore(
    name = 'debug_keystore',
    store = 'keystore/debug.keystore',
    properties = 'keystore/debug.keystore.properties',
)

android_resource(
    name = 'res',
    package = 'org.wordpress.android',
    res = 'WordPress/src/main/res',
    assets = 'WordPress/src/main/assets',
    deps = [
        ':appcompat',
        ':wpandroid-utils',
        ':wpandroid-utils-res',
        ':persistentedittext-res',
        ':wpandroid-editor-res',
        ':drag-sort-listview',
        ':mediapicker',
        ':cardview',
    ]
)

android_build_config(
    name = 'build-config',
    package = 'org.wordpress.android',
)

android_library(
    name = 'main-lib',
    srcs = glob(['WordPress/src/main/java/org/**/*.java']),
    deps = [
        ':all-jars',
        ':persistentedittext',
        ':wpandroid-utils',
        ':wpandroid-editor',
        ':build-config',
        ':res',
        ':drag-sort-listview',
    ],
)

android_binary(
    name = 'wpandroid',
    manifest = 'WordPress/src/main/AndroidManifest.xml',
    target = 'Google Inc.:Google APIs:21',
    keystore = ':debug_keystore',
    deps = [
        ':main-lib',
    ],
)
