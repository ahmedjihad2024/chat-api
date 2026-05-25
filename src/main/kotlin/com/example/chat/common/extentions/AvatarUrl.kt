package com.example.chat.common.extentions

import org.springframework.web.servlet.support.ServletUriComponentsBuilder

/**
 * Builds the absolute public URL for an avatar file (e.g. "https://host/avatars/<file>"),
 * deriving scheme/host from the current request. Returns null when the receiver (the stored
 * filename) is null. Must be called on an HTTP request thread.
 */
fun String?.toAvatarUrl(): String? =
    this?.let {
        ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/avatars/$it")
            .toUriString()
    }
