package com.example.chat.common.phone

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil

/**
 * Thin wrapper over Google libphonenumber. Numbers are always parsed in international
 * mode (no default region), so a valid number must carry its own country code — this
 * keeps the API region-agnostic and enforces E.164-style input.
 */
object PhoneNumbers {

    private val util: PhoneNumberUtil = PhoneNumberUtil.getInstance()

    /** True when [raw] parses and is a valid number for its country code. */
    fun isValid(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        return try {
            util.isValidNumber(util.parse(raw, null))
        } catch (_: NumberParseException) {
            false
        }
    }

    /**
     * Returns the canonical E.164 form (e.g. "+201234567890") when [raw] is a valid
     * number, otherwise returns [raw] unchanged so downstream validation can reject it.
     */
    fun normalizeOrSelf(raw: String?): String? {
        if (raw.isNullOrBlank()) return raw
        return try {
            val parsed = util.parse(raw, null)
            if (util.isValidNumber(parsed)) {
                util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
            } else {
                raw
            }
        } catch (_: NumberParseException) {
            raw
        }
    }
}
