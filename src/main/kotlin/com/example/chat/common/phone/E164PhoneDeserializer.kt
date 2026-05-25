package com.example.chat.common.phone

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

/**
 * Normalizes an incoming phone string to canonical E.164 during JSON deserialization,
 * so every entry point (register, login, lookups, storage) sees the same value
 * regardless of how the client formatted it. Invalid input is passed through unchanged
 * for @ValidPhone to reject with a proper validation error.
 */
class E164PhoneDeserializer : JsonDeserializer<String?>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): String? =
        PhoneNumbers.normalizeOrSelf(p.valueAsString)
}
