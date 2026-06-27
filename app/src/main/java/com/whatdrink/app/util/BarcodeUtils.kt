package com.whatdrink.app.util


fun normalizeBarcode(barcode: String): String {
    return when {
        barcode.length == 8  && barcode.all { it.isDigit() } -> "00000$barcode"
        barcode.length == 12 && barcode.all { it.isDigit() } -> "0$barcode"
        else -> barcode
    }
}