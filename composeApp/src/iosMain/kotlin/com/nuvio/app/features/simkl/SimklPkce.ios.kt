package com.nuvio.app.features.simkl

import com.nuvio.app.features.plugins.cryptointerop.CC_SHA256
import com.nuvio.app.features.plugins.cryptointerop.CC_SHA256_DIGEST_LENGTH
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import platform.Foundation.NSData

@OptIn(ExperimentalForeignApi::class)
internal actual fun simklSha256Base64Url(value: String): String {
    val input = value.encodeToByteArray(); val output = UByteArray(CC_SHA256_DIGEST_LENGTH.toInt())
    CC_SHA256(input.refTo(0), input.size.toUInt(), output.refTo(0))
    return NSData.create(bytes = output.refTo(0), length = output.size.toULong()).base64EncodedStringWithOptions(0u).replace('+', '-').replace('/', '_').replace("=", "")
}
