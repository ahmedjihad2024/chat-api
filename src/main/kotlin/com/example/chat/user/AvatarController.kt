package com.example.chat.user

import com.example.chat.common.exception.ApiException
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

/**
 * Publicly serves profile pictures by filename. This path is permit-all in
 * SecurityConfig: anyone with the (unguessable, UUID-named) link can view the
 * image, which is the normal model for avatars and lets `<img src>` work without
 * a token. The filename changes on every upload, so responses are cached long-term.
 */
@Hidden
@RestController
@RequestMapping("/avatars")
class AvatarController(
    private val avatarStorage: AvatarStorage,
) {
    @GetMapping("/{filename:.+}")
    fun get(
        @PathVariable filename: String,
    ): ResponseEntity<Resource> {
        val avatar = avatarStorage.load(filename)
            ?: throw ApiException.NotFound("error.user.avatar_not_found")
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(avatar.contentType))
            .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic())
            .body(avatar.resource)
    }
}
