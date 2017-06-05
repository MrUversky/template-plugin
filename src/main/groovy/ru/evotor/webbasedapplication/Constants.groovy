package ru.evotor.webbasedapplication

import groovy.xml.Namespace

class Constants {
    public static final String ANDROID_ACTION_MAIN = "android.intent.action.MAIN"
    public static final String ANDROID_CATEGORY_DEFAULT = "android.intent.category.DEFAULT"
    public static final String ANDROID_CATEGORY_EVOTOR = "android.intent.category.EVOTOR"
    public static final String ANDROID_CATEGORY_SALES_SCREEN = "evo.v2.receipt.sell.receiptDiscount"
    public static final String ANDROID_CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER"
    public static final String APPLICATION_UUID = "app_uuid"
    public static final String GRANTS_FILE_POSTFIX = "_grants"
    public static final String INTEGRATION_POINT_MAIN_SCREEN = "MAIN_SCREEN"
    public static final String INTEGRATION_POINT_SALES_SCREEN = "SALES_SCREEN"
    public static final String BACKGROUND_COLOR_SALES_SCREEN = "ru.evotor.sales_screen.BACKGROUND_COLOR"
    public static final String BACKGROUND_COLOR_MAIN_SCREEN = "ru.evotor.launcher.BACKGROUND_COLOR"
    public static final Namespace namespace = new Namespace("http://schemas.android.com/apk/res/android", "android")
}