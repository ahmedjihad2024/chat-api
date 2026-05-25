package com.example.chat.common.phone

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * Bean Validation constraint backed by libphonenumber. Null/blank values pass (defer to
 * @NotBlank), so pair it with @NotBlank on required fields. Validation runs after Jackson
 * deserialization, by which point [E164PhoneDeserializer] has already normalized the value.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [PhoneValidator::class])
annotation class ValidPhone(
    val message: String = "{validation.invalid_phone}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class PhoneValidator : ConstraintValidator<ValidPhone, String?> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean =
        value.isNullOrBlank() || PhoneNumbers.isValid(value)
}
