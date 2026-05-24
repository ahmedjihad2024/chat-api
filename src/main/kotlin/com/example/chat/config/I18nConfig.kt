package com.example.chat.config

import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver
import java.util.Locale

@Configuration
class I18nConfig {

    @Bean
    fun localeResolver(): LocaleResolver = AcceptHeaderLocaleResolver().apply {
        setDefaultLocale(Locale.ENGLISH)
        supportedLocales = listOf(Locale.ENGLISH, Locale.forLanguageTag("ar"))
    }

    @Bean
    fun validator(messageSource: MessageSource): LocalValidatorFactoryBean =
        LocalValidatorFactoryBean().apply {
            setValidationMessageSource(messageSource)
        }
}
