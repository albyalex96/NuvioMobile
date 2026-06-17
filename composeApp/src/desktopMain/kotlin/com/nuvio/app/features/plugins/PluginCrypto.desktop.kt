package com.nuvio.app.features.plugins

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.Signature
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

internal fun pluginDigest(algorithm: String, data: ByteArray): ByteArray {
    val md = MessageDigest.getInstance(algorithm)
    return md.digest(data)
}

internal fun pluginDigestHex(algorithm: String, data: String): String {
    val digest = pluginDigest(algorithm, data.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}

internal fun pluginHmac(algorithm: String, key: ByteArray, data: ByteArray): ByteArray {
    val mac = Mac.getInstance(algorithm)
    val keySpec = SecretKeySpec(key, algorithm)
    mac.init(keySpec)
    return mac.doFinal(data)
}

internal fun pluginHmacHex(algorithm: String, key: String, data: String): String {
    val hmac = pluginHmac(algorithm, key.toByteArray(Charsets.UTF_8), data.toByteArray(Charsets.UTF_8))
    return hmac.joinToString("") { "%02x".format(it) }
}

internal fun pluginBase64Encode(data: String): String =
    Base64.getEncoder().encodeToString(data.toByteArray(Charsets.UTF_8))

internal fun pluginBase64Decode(data: String): ByteArray =
    Base64.getDecoder().decode(data)

internal fun pluginUtf8ToHex(data: String): String =
    data.toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it) }

internal fun pluginHexToUtf8(data: String): String {
    val bytes = pluginHexToByteArray(data)
    return String(bytes, Charsets.UTF_8)
}

internal fun pluginHexToByteArray(data: String): ByteArray {
    val len = data.length
    val bytes = ByteArray(len / 2)
    for (i in bytes.indices) {
        bytes[i] = ((Character.digit(data[i * 2], 16) shl 4) + Character.digit(data[i * 2 + 1], 16)).toByte()
    }
    return bytes
}

internal fun pluginGetRandomValues(length: Int): ByteArray {
    val random = SecureRandom()
    val bytes = ByteArray(length)
    random.nextBytes(bytes)
    return bytes
}

internal fun pluginPbkdf2(
    password: ByteArray,
    salt: ByteArray,
    iterations: Int,
    keyLength: Int,
    algorithm: String,
): ByteArray {
    val spec = javax.crypto.spec.PBEKeySpec(
        String(password, Charsets.UTF_8).toCharArray(),
        salt,
        iterations,
        keyLength * 8,
    )
    val factory = javax.crypto.SecretKeyFactory.getInstance(algorithm)
    return factory.generateSecret(spec).encoded
}

internal fun pluginAesEncrypt(
    mode: String,
    key: ByteArray,
    iv: ByteArray,
    data: ByteArray,
): ByteArray {
    val normalizedMode = "AES/CBC/PKCS5Padding"
    val cipher = Cipher.getInstance(normalizedMode)
    val keySpec = SecretKeySpec(key, "AES")
    val ivSpec = IvParameterSpec(iv)
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
    return cipher.doFinal(data)
}

internal fun pluginAesDecrypt(
    mode: String,
    key: ByteArray,
    iv: ByteArray,
    data: ByteArray,
): ByteArray {
    val normalizedMode = "AES/CBC/PKCS5Padding"
    val cipher = Cipher.getInstance(normalizedMode)
    val keySpec = SecretKeySpec(key, "AES")
    val ivSpec = IvParameterSpec(iv)
    cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
    return cipher.doFinal(data)
}

internal fun pluginSign(algorithm: String, privateKey: ByteArray, data: ByteArray): ByteArray {
    val (keyAlgo, sigAlgo) = when (algorithm.uppercase()) {
        "RSASSA-PKCS1-V1_5-SHA256", "RSASSA-PKCS1-V1_5" -> "RSA" to "SHA256withRSA"
        "ECDSA-SHA256", "ECDSA" -> "EC" to "SHA256withECDSA"
        else -> "RSA" to "SHA256withRSA"
    }
    val factory = KeyFactory.getInstance(keyAlgo)
    val privKey = factory.generatePrivate(PKCS8EncodedKeySpec(privateKey))
    val sig = Signature.getInstance(sigAlgo)
    sig.initSign(privKey)
    sig.update(data)
    return sig.sign()
}

internal fun pluginVerify(algorithm: String, publicKey: ByteArray, signature: ByteArray, data: ByteArray): Boolean {
    val (keyAlgo, sigAlgo) = when (algorithm.uppercase()) {
        "RSASSA-PKCS1-V1_5-SHA256", "RSASSA-PKCS1-V1_5" -> "RSA" to "SHA256withRSA"
        "ECDSA-SHA256", "ECDSA" -> "EC" to "SHA256withECDSA"
        else -> "RSA" to "SHA256withRSA"
    }
    val factory = KeyFactory.getInstance(keyAlgo)
    val pubKey = factory.generatePublic(X509EncodedKeySpec(publicKey))
    val sig = Signature.getInstance(sigAlgo)
    sig.initVerify(pubKey)
    sig.update(data)
    return sig.verify(signature)
}
