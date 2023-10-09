package com.yasserakbbach.phonenumberpicker.utils

private const val CHAR_PLUS = "+"

fun CharSequence?.prependPlus(): String {
    return StringBuilder()
        .append(CHAR_PLUS)
        .append(this)
        .toString()
}

fun CharSequence?.startsWithPlus(): Boolean {
    return this?.startsWith(CHAR_PLUS) == true
}