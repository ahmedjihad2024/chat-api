package com.example.chat.common.extentions

import org.springframework.context.MessageSource
import org.springframework.context.NoSuchMessageException
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component

// Captures the application's MessageSource once Spring instantiates it, so the
// top-level String.tr() extension below can resolve i18n keys without every
// caller having to inject MessageSource.
@Component
class TranslationContext(messageSource: MessageSource) {

    init {
        source = messageSource
    }

    companion object {
        private lateinit var source: MessageSource

        internal fun resolve(key: String): String =
            try {
                source.getMessage(key, null, LocaleContextHolder.getLocale())
            } catch (_: NoSuchMessageException) {
                key
            }
    }
}

fun String.tr(): String = TranslationContext.resolve(this)
