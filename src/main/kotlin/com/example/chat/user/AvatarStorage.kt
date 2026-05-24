package com.example.chat.user

import com.example.chat.common.exception.ApiException
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID

/** An avatar file resolved from disk, paired with the MIME type to serve it as. */
data class AvatarFile(val resource: Resource, val contentType: String)

/**
 * Stores and retrieves user profile pictures as plain files on the server's
 * local disk (under [avatarDir]).
 *
 * Uploads are validated by byte size and actual magic bytes (so a renamed file
 * can't masquerade as an image). The on-disk name is always a server-generated
 * UUID — the client-supplied filename is never trusted — which also closes off
 * path-traversal. Every resolved path is re-checked to stay within the base dir.
 */
@Component
class AvatarStorage(
    @Value($$"${app.avatar.dir:uploads/avatars}") private val avatarDir: String,
) {
    private val maxBytes = 5L * 1024 * 1024 // 5 MB — keep in sync with spring.servlet.multipart limits.
    private val extensionByType = mapOf(
        "image/png" to ".png",
        "image/jpeg" to ".jpg",
        "image/webp" to ".webp",
    )
    private val typeByExtension = mapOf(
        ".png" to "image/png",
        ".jpg" to "image/jpeg",
        ".webp" to "image/webp",
    )

    private lateinit var baseDir: Path

    @PostConstruct
    fun init() {
        baseDir = Paths.get(avatarDir).toAbsolutePath().normalize()
        Files.createDirectories(baseDir)
    }

    /** Validates [file] and writes it to disk, returning the generated filename (e.g. "<uuid>.png"). */
    fun store(file: MultipartFile): String {
        if (file.isEmpty) throw ApiException.BadRequest("error.user.avatar_empty")
        if (file.size > maxBytes) throw ApiException.BadRequest("error.user.avatar_too_large")

        val bytes = file.bytes
        val detectedType = detectImageType(bytes)
            ?: throw ApiException.BadRequest("error.user.avatar_invalid_type")
        val extension = extensionByType[detectedType]
            ?: throw ApiException.BadRequest("error.user.avatar_invalid_type")

        val filename = "${UUID.randomUUID()}$extension"
        val target = resolveWithinBase(filename)
            ?: throw ApiException.BadRequest("error.user.avatar_invalid_type")
        Files.newOutputStream(target).use { it.write(bytes) }
        return filename
    }

    /** Loads the avatar [filename] as a servable resource, or null if it's missing or the name is unsafe. */
    fun load(filename: String): AvatarFile? {
        val target = resolveWithinBase(filename) ?: return null
        if (!Files.exists(target)) return null
        val contentType = typeByExtension[extensionOf(filename)] ?: "application/octet-stream"
        return AvatarFile(UrlResource(target.toUri()), contentType)
    }

    /** Deletes the avatar [filename]; a no-op if it does not exist or the name is unsafe. */
    fun delete(filename: String) {
        val target = resolveWithinBase(filename) ?: return
        Files.deleteIfExists(target)
    }

    /** Resolves [filename] under [baseDir], returning null if it would escape the base directory. */
    private fun resolveWithinBase(filename: String): Path? {
        val resolved = baseDir.resolve(filename).normalize()
        return if (resolved.startsWith(baseDir)) resolved else null
    }

    private fun extensionOf(filename: String): String =
        filename.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it".lowercase() }

    /** Returns the image MIME type inferred from the leading bytes, or null if not a supported image. */
    private fun detectImageType(bytes: ByteArray): String? {
        if (bytes.size < 12) return null
        // PNG: 89 50 4E 47
        if (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
        ) {
            return "image/png"
        }
        // JPEG: FF D8 FF
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
            return "image/jpeg"
        }
        // WEBP: "RIFF" .... "WEBP"
        if (bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        ) {
            return "image/webp"
        }
        return null
    }
}
