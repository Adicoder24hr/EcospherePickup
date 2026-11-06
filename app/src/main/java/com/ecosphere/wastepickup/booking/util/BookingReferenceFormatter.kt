package com.ecosphere.wastepickup.booking.util

import java.util.Locale

object BookingReferenceFormatter {
    fun format(id: String): String =
        "ECO-${id.filter { it.isLetterOrDigit() }.take(8).uppercase(Locale.ROOT)}"
}
