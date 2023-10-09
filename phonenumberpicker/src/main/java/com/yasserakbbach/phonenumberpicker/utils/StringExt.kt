package com.yasserakbbach.phonenumberpicker.utils

fun String?.clearSpaces(): String? {
    return this?.replace("\\s+", "")
}