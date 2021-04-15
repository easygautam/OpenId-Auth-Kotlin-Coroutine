package com.easygautam.openid

/**
 * @brief This file contains the android log implementations
 * @author Gautam Singh
 * @version 0v01 - First version
 * @date 12-11-2020
 * @copyright Copyright (c) 2020 easygautam
 * @file LogUtils.kt
 */

import android.util.Log
import net.openid.appauth.BuildConfig

/**
 * Object class to print logs
 */
object LogUtils {

    // Debug logs
    fun debug(tag: String? = "Easygautam", message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    // Error logs
    fun error(tag: String? = "Easygautam", message: String) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message)
        }
    }

}