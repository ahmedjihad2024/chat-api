package com.example.chat.user.dto

import com.example.chat.common.phone.E164PhoneDeserializer
import com.example.chat.common.phone.ValidPhone
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import jakarta.validation.constraints.NotBlank

data class ChangePhoneRequest(
    @field:NotBlank(message = "{validation.cannot_be_blank}")
    @field:ValidPhone
    @field:JsonDeserialize(using = E164PhoneDeserializer::class)
    val newPhone: String,
)
