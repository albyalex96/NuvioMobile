package com.nuvio.app.features.downloads

import com.nuvio.app.features.plugins.cryptointerop.CCCrypt
import com.nuvio.app.features.plugins.cryptointerop.kCCAlgorithmAES
import com.nuvio.app.features.plugins.cryptointerop.kCCDecrypt
import com.nuvio.app.features.plugins.cryptointerop.kCCOptionPKCS7Padding
import com.nuvio.app.features.plugins.cryptointerop.kCCSuccess
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.download_failed
import nuvio.composeapp.generated.resources.downloads_error_finalize_file_failed
import nuvio.composeapp.generated.resources.downloads_error_open_partial_file_failed
import nuvio.composeapp.generated.resources.downloads_error_partial_file_not_open
import nuvio.composeapp.generated.resources.downloads_error_write_partial_file_failed
import nuvio.composeapp.generated.resources.network_request_failed_http
import org.jetbrains.compose.resources.getString
import platform.Foundation.NSError
import platform.Foundation.NSDate
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequestReloadIgnoringLocalCacheData
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDataDelegateProtocol
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.posix.FILE
import platform.posix.fclose
import platform.posix.fflush
import platform.posix.fopen
import platform.posix.fwrite

private const val DOWNLOAD_REQUEST_TIMEOUT_SECONDS = 60.0
private const val DOWNLOAD_RESOURCE_TIMEOUT_SECONDS = 24.0 * 60.0 * 60.0
private const val PROGRESS_MIN_INTERVAL_SECONDS = 0.5
private const val PROGRESS_MIN_BYTE_DELTA = 512L * 1024L

private val backgroundSessionCompletionHandlers = mutableMapOf<String, () -> Unit>()

fun handleDownloadsBackgroundEvents(
    identifier: String,
    completionHandler: () -> Unit,
) {
    backgroundSessionCompletionHandlers[identifier] = completionHandler
}

fun pauseDownloadsForAppBackground() {
    DownloadsRepository.pauseActiveDownloads()
}

@OptIn(ExperimentalForeignApi::class)
internal actual object DownloadsPlatformDownloader {
    actual fun start(
        request: DownloadPlatformRequest,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
        onSuccess: (localFileUri: String, totalBytes: Long?) -> Unit,
        onFailure: (message: String) -> Unit,
    ): DownloadsTaskHandle {
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.Default)
        val handle = IosDownloadsTaskHandle(job)

        scope.launch {
            val downloadsDirectory = downloadsDirectoryPath()
            val destinationPath = "$downloadsDirectory/${request.destinationFileName}"
            val tempPath = "$downloadsDirectory/${request.destinationFileName}.part"

            try {
                var resumeFromBytes = fileSizeOrNull(tempPath)?.coerceAtLeast(0L) ?: 0L

                var attemptedRangeRequest = resumeFromBytes > 0L
                var result = performDownloadRequest(
                    request = request,
                    rangeStart = if (attemptedRangeRequest) resumeFromBytes else null,
                    resumeFromBytes = resumeFromBytes,
                    tempPath = tempPath,
                    handle = handle,
                    onProgress = onProgress,
                )

                if (attemptedRangeRequest && result.statusCode == 416) {
                    removePathIfExists(tempPath)
                    resumeFromBytes = 0L
                    attemptedRangeRequest = false
                    result = performDownloadRequest(
                        request = request,
                        rangeStart = null,
                        resumeFromBytes = 0L,
                        tempPath = tempPath,
                        handle = handle,
                        onProgress = onProgress,
                    )
                }

                if (result.statusCode !in 200..299) {
                    error(runBlocking { getString(Res.string.network_request_failed_http, result.statusCode) })
                }

                val isPartialResume = attemptedRangeRequest && result.statusCode == 206 && resumeFromBytes > 0L
                val startingBytes = if (isPartialResume) resumeFromBytes else 0L
                val totalBytes = resolveTotalBytes(
                    startingBytes = startingBytes,
                    isPartialResume = isPartialResume,
                    contentRangeHeader = result.contentRange,
                    contentLength = result.contentLength,
                )

                removePathIfExists(destinationPath)
                val moved = NSFileManager.defaultManager.moveItemAtPath(
                    srcPath = tempPath,
                    toPath = destinationPath,
                    error = null,
                )
                if (!moved) {
                    error(runBlocking { getString(Res.string.downloads_error_finalize_file_failed) })
                }

                val localFileUri = NSURL.fileURLWithPath(destinationPath).absoluteString ?: "file://$destinationPath"
                val finalSize = fileSizeOrNull(destinationPath)
                onSuccess(localFileUri, totalBytes ?: finalSize)
            } catch (_: CancellationException) {
                handle.cancelNativeTask()
            } catch (error: Throwable) {
                onFailure(error.message ?: runBlocking { getString(Res.string.download_failed) })
            }
        }

        return handle
    }

    actual fun removeFile(localFileUri: String?): Boolean {
        if (localFileUri.isNullOrBlank()) return false
        val path = localFileUri.toLocalPath() ?: return false
        if (NSFileManager.defaultManager.fileExistsAtPath(path)) {
            return removePathIfExists(path)
        }

        val fileName = path.substringAfterLast('/').takeIf { it.isNotBlank() } ?: return false
        return removePathIfExists("${downloadsDirectoryPath()}/$fileName")
    }

    actual fun removePartialFile(destinationFileName: String): Boolean {
        val tempPath = "${downloadsDirectoryPath()}/$destinationFileName.part"
        return removePathIfExists(tempPath)
    }

    actual fun resolveLocalFileUri(localFileUri: String?, destinationFileName: String): String? {
        localFileUri?.toLocalPath()
            ?.takeIf { NSFileManager.defaultManager.fileExistsAtPath(it) }
            ?.let { path ->
                return NSURL.fileURLWithPath(path).absoluteString ?: "file://$path"
            }

        val fileName = destinationFileName.trim().takeIf { it.isNotBlank() }
            ?: localFileUri?.toLocalPath()?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: return null
        val currentPath = "${downloadsDirectoryPath()}/$fileName"
        return if (NSFileManager.defaultManager.fileExistsAtPath(currentPath)) {
            NSURL.fileURLWithPath(currentPath).absoluteString ?: "file://$currentPath"
        } else {
            null
        }
    }

    actual fun fetchUrlAsString(url: String, headers: Map<String, String>): String? {
        return try {
            val nativeUrl = NSURL(string = url) ?: return null
            val request = NSMutableURLRequest(
                uRL = nativeUrl,
                cachePolicy = NSURLRequestReloadIgnoringLocalCacheData,
                timeoutInterval = 30.0,
            )
            request.setHTTPMethod("GET")
            headers.forEach { (key, value) ->
                request.setValue(value, forHTTPHeaderField = key)
            }

            val semaphore = dispatch_semaphore_create(0)
            var result: String? = null

            val task = NSURLSession.sharedSession.dataTaskWithRequest(request) { data, _, error ->
                if (error == null && data != null) {
                    result = NSString.create(data, NSUTF8StringEncoding) as? String
                }
                dispatch_semaphore_signal(semaphore)
            }
            task.resume()
            dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER)
            result
        } catch (_: Exception) {
            null
        }
    }

    actual fun fetchUrlAsBytes(url: String, headers: Map<String, String>): ByteArray? {
        return try {
            val nativeUrl = NSURL(string = url) ?: return null
            val request = NSMutableURLRequest(
                uRL = nativeUrl,
                cachePolicy = NSURLRequestReloadIgnoringLocalCacheData,
                timeoutInterval = 30.0,
            )
            request.setHTTPMethod("GET")
            headers.forEach { (key, value) ->
                request.setValue(value, forHTTPHeaderField = key)
            }

            val semaphore = dispatch_semaphore_create(0)
            var result: ByteArray? = null

            val task = NSURLSession.sharedSession.dataTaskWithRequest(request) { data, _, error ->
                if (error == null && data != null) {
                    result = data.toByteArray()
                }
                dispatch_semaphore_signal(semaphore)
            }
            task.resume()
            dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER)
            result
        } catch (_: Exception) {
            null
        }
    }

    actual fun probeHlsContentType(url: String, headers: Map<String, String>): Boolean {
        return try {
            val nativeUrl = NSURL(string = url) ?: return false
            val request = NSMutableURLRequest(
                uRL = nativeUrl,
                cachePolicy = NSURLRequestReloadIgnoringLocalCacheData,
                timeoutInterval = 15.0,
            )
            request.setHTTPMethod("HEAD")
            headers.forEach { (key, value) ->
                request.setValue(value, forHTTPHeaderField = key)
            }

            val semaphore = dispatch_semaphore_create(0)
            var isHls = false

            val task = NSURLSession.sharedSession.dataTaskWithRequest(request) { _, response, error ->
                if (error == null) {
                    val httpResponse = response as? NSHTTPURLResponse
                    val contentType = httpResponse?.valueForHTTPHeaderField("Content-Type")
                    isHls = HlsPlaylistParser.isHlsContentType(contentType)
                }
                dispatch_semaphore_signal(semaphore)
            }
            task.resume()
            dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER)
            isHls
        } catch (_: Exception) {
            false
        }
    }

    actual fun downloadHlsSegments(
        context: HlsDownloadContext,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
        onSuccess: (localFileUri: String, totalBytes: Long?) -> Unit,
        onFailure: (message: String) -> Unit,
    ): DownloadsTaskHandle {
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.Default)
        val handle = IosDownloadsTaskHandle(job)

        scope.launch {
            val downloadsDirectory = downloadsDirectoryPath()
            val destinationPath = "$downloadsDirectory/${context.destinationFileName}"
            val tempPath = "$downloadsDirectory/${context.destinationFileName}.hls.part"
            var totalDownloaded = 0L

            try {
                removePathIfExists(tempPath)

                var isFirstWrite = true
                if (context.mapInitSegment != null) {
                    val file = fopen(tempPath, "wb")
                    if (file != null) {
                        context.mapInitSegment.usePinned { pinned ->
                            fwrite(pinned.addressOf(0), 1.convert(), context.mapInitSegment.size.convert(), file)
                        }
                        fflush(file)
                        fclose(file)
                        totalDownloaded += context.mapInitSegment.size.toLong()
                        onProgress(totalDownloaded, null)
                    }
                    isFirstWrite = false
                }

                for ((index, spec) in context.segments.withIndex()) {
                    ensureActive()

                    val segBytes = fetchUrlAsBytes(spec.url, context.sourceHeaders)
                        ?: error(runBlocking { getString(Res.string.downloads_error_empty_body) })

                    val decrypted = if (spec.keyIndex != null && spec.keyIndex < context.keyDataList.size) {
                        val key = context.keyDataList[spec.keyIndex]
                        val iv = context.keyIvList.getOrNull(spec.keyIndex)
                            ?: deriveIvFromIndex(index)
                        segBytes.aes128CbcDecrypt(key, iv)
                    } else {
                        segBytes
                    }

                    val file = fopen(tempPath, if (isFirstWrite) "wb" else "ab")
                    if (file != null) {
                        decrypted.usePinned { pinned ->
                            fwrite(pinned.addressOf(0), 1.convert(), decrypted.size.convert(), file)
                        }
                        fflush(file)
                        fclose(file)
                        totalDownloaded += decrypted.size.toLong()
                        onProgress(totalDownloaded, null)
                    }
                    isFirstWrite = false
                }

                val isFmp4 = context.mapInitSegment != null
                val finalPath: String = if (isFmp4) {
                    removePathIfExists(destinationPath)
                    val moved = NSFileManager.defaultManager.moveItemAtPath(
                        srcPath = tempPath,
                        toPath = destinationPath,
                        error = null,
                    )
                    if (!moved) {
                        error(runBlocking { getString(Res.string.downloads_error_finalize_file_failed) })
                    }
                    destinationPath
                } else {
                    val mp4Path = destinationPath.removeSuffix(".mp4") + "_converted.mp4"
                    if (remuxTsToMp4(tempPath, mp4Path)) {
                        removePathIfExists(tempPath)
                        mp4Path
                    } else {
                        val tsPath = destinationPath.removeSuffix(".mp4") + ".ts"
                        removePathIfExists(tsPath)
                        val moved = NSFileManager.defaultManager.moveItemAtPath(
                            srcPath = tempPath,
                            toPath = tsPath,
                            error = null,
                        )
                        if (!moved) {
                            error(runBlocking { getString(Res.string.downloads_error_finalize_file_failed) })
                        }
                        tsPath
                    }
                }

                val localFileUri = NSURL.fileURLWithPath(finalPath).absoluteString ?: "file://$finalPath"
                val finalSize = fileSizeOrNull(finalPath)
                onSuccess(localFileUri, finalSize)
            } catch (_: CancellationException) {
                handle.cancelNativeTask()
            } catch (error: Throwable) {
                onFailure(error.message ?: runBlocking { getString(Res.string.download_failed) })
            }
        }

        return handle
    }
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun ByteArray.aes128CbcDecrypt(key: ByteArray, iv: ByteArray): ByteArray {
    val dataOut = ByteArray(size + 16)
    var dataOutMoved = 0UL

    memScoped {
        val moved = alloc<kotlinx.cinterop.size_tVar>()
        key.usePinned { pinnedKey ->
            iv.usePinned { pinnedIv ->
                this@aes128CbcDecrypt.usePinned { pinnedData ->
                    dataOut.usePinned { pinnedDataOut ->
                        val status = CCCrypt(
                            op = kCCDecrypt,
                            alg = kCCAlgorithmAES,
                            options = kCCOptionPKCS7Padding,
                            key = pinnedKey.addressOf(0),
                            keyLength = key.size.toULong(),
                            iv = pinnedIv.addressOf(0),
                            dataIn = pinnedData.addressOf(0),
                            dataInLength = this@aes128CbcDecrypt.size.toULong(),
                            dataOut = pinnedDataOut.addressOf(0),
                            dataOutAvailable = dataOut.size.toULong(),
                            dataOutMoved = moved.ptr,
                        )
                        if (status == kCCSuccess) {
                            dataOutMoved = moved.value
                        } else {
                            error("CCCrypt AES-128-CBC decrypt failed with status: $status")
                        }
                    }
                }
            }
        }
    }

    return dataOut.copyOf(dataOutMoved.toInt())
}

internal actual fun remuxTsToMp4(inputPath: String, outputPath: String): Boolean {
    return false
}

@OptIn(ExperimentalForeignApi::class)
private fun deriveIvFromIndex(index: Int): ByteArray {
    val iv = ByteArray(16)
    var idx = index
    for (i in 15 downTo 0) {
        iv[i] = (idx and 0xFF).toByte()
        idx = idx ushr 8
    }
    return iv
}

private class IosDownloadsTaskHandle(
    private val job: Job,
) : DownloadsTaskHandle {
    private var task: NSURLSessionTask? = null
    private var session: NSURLSession? = null

    fun attach(task: NSURLSessionTask, session: NSURLSession) {
        this.task = task
        this.session = session
    }

    override fun cancel() {
        cancelNativeTask()
        job.cancel()
    }

    fun cancelNativeTask() {
        task?.cancel()
        session?.invalidateAndCancel()
        task = null
        session = null
    }
}

private data class IosDownloadResult(
    val statusCode: Int,
    val contentRange: String?,
    val contentLength: Long?,
)

@OptIn(ExperimentalForeignApi::class)
private class IosDownloadDelegate(
    private val attemptedRangeRequest: Boolean,
    private val resumeFromBytes: Long,
    private val tempPath: String,
    private val onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
) : NSObject(), NSURLSessionDataDelegateProtocol {
    private val completion = CompletableDeferred<IosDownloadResult>()
    private var result: IosDownloadResult? = null
    private var fileError: Throwable? = null
    private var outputFile: CPointer<FILE>? = null
    private var startingBytesForResponse = 0L
    private var bytesWrittenForResponse = 0L
    private var totalBytesForResponse: Long? = null
    private var lastProgressBytes = -1L
    private var lastProgressTimestampSeconds = 0.0

    suspend fun awaitCompletion(): IosDownloadResult = completion.await()

    override fun URLSession(
        session: NSURLSession,
        dataTask: NSURLSessionDataTask,
        didReceiveResponse: NSURLResponse,
        completionHandler: (Long) -> Unit,
    ) {
        val httpResponse = didReceiveResponse as? NSHTTPURLResponse
        val statusCode = httpResponse?.statusCode?.toInt() ?: 200
        val nextResult = IosDownloadResult(
            statusCode = statusCode,
            contentRange = httpResponse?.valueForHTTPHeaderField("Content-Range"),
            contentLength = httpResponse
                ?.valueForHTTPHeaderField("Content-Length")
                ?.toLongOrNull()
                ?.takeIf { it > 0L },
        )
        result = nextResult

        if (statusCode in 200..299) {
            val isPartialResume = attemptedRangeRequest && statusCode == 206 && resumeFromBytes > 0L
            startingBytesForResponse = if (isPartialResume) resumeFromBytes else 0L
            bytesWrittenForResponse = 0L
            totalBytesForResponse = resolveTotalBytes(
                startingBytes = startingBytesForResponse,
                isPartialResume = isPartialResume,
                contentRangeHeader = nextResult.contentRange,
                contentLength = nextResult.contentLength,
            )

            outputFile = fopen(tempPath, if (isPartialResume) "ab" else "wb") ?: run {
                fileError = IllegalStateException(runBlocking { getString(Res.string.downloads_error_open_partial_file_failed) })
                null
            }

            reportProgress(startingBytesForResponse, totalBytesForResponse)
        }

        completionHandler(1L)
    }

    override fun URLSession(
        session: NSURLSession,
        dataTask: NSURLSessionDataTask,
        didReceiveData: NSData,
    ) {
        if (fileError != null) return

        val file = outputFile ?: run {
            fileError = IllegalStateException(runBlocking { getString(Res.string.downloads_error_partial_file_not_open) })
            return
        }

        val bytesToWrite = didReceiveData.length.toLong()
        val wrote = fwrite(
            didReceiveData.bytes,
            1.convert(),
            bytesToWrite.convert(),
            file,
        ).toLong()
        if (wrote != bytesToWrite) {
            fileError = IllegalStateException(runBlocking { getString(Res.string.downloads_error_write_partial_file_failed) })
            return
        }
        fflush(file)

        bytesWrittenForResponse += bytesToWrite
        reportProgress(
            downloadedBytes = startingBytesForResponse + bytesWrittenForResponse,
            totalBytes = totalBytesForResponse,
        )
    }

    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        didCompleteWithError: NSError?,
    ) {
        closeOutputFile()

        if (didCompleteWithError != null) {
            completion.completeExceptionally(
                IllegalStateException(didCompleteWithError.localizedDescription),
            )
            return
        }

        val error = fileError
        if (error != null) {
            completion.completeExceptionally(error)
            return
        }

        completion.complete(result ?: task.response.toDownloadResult())
    }

    override fun URLSessionDidFinishEventsForBackgroundURLSession(session: NSURLSession) {
        val identifier = session.configuration.identifier ?: return
        backgroundSessionCompletionHandlers.remove(identifier)?.invoke()
    }

    private fun closeOutputFile() {
        outputFile?.let { file ->
            fflush(file)
            fclose(file)
        }
        outputFile = null
    }

    private fun reportProgress(
        downloadedBytes: Long,
        totalBytes: Long?,
    ) {
        val normalizedDownloadedBytes = downloadedBytes.coerceAtLeast(0L)
        val now = NSDate().timeIntervalSince1970
        val byteDelta = normalizedDownloadedBytes - lastProgressBytes
        val timeDelta = now - lastProgressTimestampSeconds
        val reachedEnd = totalBytes != null && normalizedDownloadedBytes >= totalBytes

        if (
            lastProgressBytes >= 0L &&
            !reachedEnd &&
            byteDelta < PROGRESS_MIN_BYTE_DELTA &&
            timeDelta < PROGRESS_MIN_INTERVAL_SECONDS
        ) {
            return
        }

        lastProgressBytes = normalizedDownloadedBytes
        lastProgressTimestampSeconds = now
        onProgress(normalizedDownloadedBytes, totalBytes)
    }
}

private fun NSURLResponse?.toDownloadResult(): IosDownloadResult {
    val httpResponse = this as? NSHTTPURLResponse
    return IosDownloadResult(
        statusCode = httpResponse?.statusCode?.toInt() ?: 200,
        contentRange = httpResponse?.valueForHTTPHeaderField("Content-Range"),
        contentLength = httpResponse
            ?.valueForHTTPHeaderField("Content-Length")
            ?.toLongOrNull()
            ?.takeIf { it > 0L },
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun downloadsDirectoryPath(): String {
    val root = NSHomeDirectory().trimEnd('/')
    val path = "$root/Documents/nuvio_downloads"
    NSFileManager.defaultManager.createDirectoryAtPath(
        path = path,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    return path
}

@OptIn(ExperimentalForeignApi::class)
private fun removePathIfExists(path: String): Boolean {
    if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return true
    return NSFileManager.defaultManager.removeItemAtPath(path, null)
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun performDownloadRequest(
    request: DownloadPlatformRequest,
    rangeStart: Long?,
    resumeFromBytes: Long,
    tempPath: String,
    handle: IosDownloadsTaskHandle,
    onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
): IosDownloadResult {
    val url = NSURL(string = request.sourceUrl)
    val nativeRequest = NSMutableURLRequest(
        uRL = url,
        cachePolicy = NSURLRequestReloadIgnoringLocalCacheData,
        timeoutInterval = DOWNLOAD_REQUEST_TIMEOUT_SECONDS,
    )
    nativeRequest.setHTTPMethod("GET")
    nativeRequest.setAllowsCellularAccess(true)
    nativeRequest.setAllowsExpensiveNetworkAccess(true)
    nativeRequest.setAllowsConstrainedNetworkAccess(true)
    request.sourceHeaders.forEach { (key, value) ->
        nativeRequest.setValue(value, forHTTPHeaderField = key)
    }
    if (rangeStart != null && rangeStart > 0L) {
        nativeRequest.setValue("bytes=$rangeStart-", forHTTPHeaderField = "Range")
    }

    val delegate = IosDownloadDelegate(
        attemptedRangeRequest = rangeStart != null && rangeStart > 0L,
        resumeFromBytes = resumeFromBytes,
        tempPath = tempPath,
        onProgress = onProgress,
    )
    val configuration = NSURLSessionConfiguration.defaultSessionConfiguration().apply {
        timeoutIntervalForRequest = DOWNLOAD_REQUEST_TIMEOUT_SECONDS
        timeoutIntervalForResource = DOWNLOAD_RESOURCE_TIMEOUT_SECONDS
        waitsForConnectivity = true
        allowsCellularAccess = true
        allowsExpensiveNetworkAccess = true
        allowsConstrainedNetworkAccess = true
    }
    val session = NSURLSession.sessionWithConfiguration(
        configuration = configuration,
        delegate = delegate,
        delegateQueue = NSOperationQueue().apply {
            maxConcurrentOperationCount = 1
        },
    )
    val task = session.dataTaskWithRequest(nativeRequest)

    handle.attach(task, session)
    onProgress(resumeFromBytes.coerceAtLeast(0L), null)
    task.resume()

    return try {
        delegate.awaitCompletion()
    } finally {
        session.finishTasksAndInvalidate()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun fileSizeOrNull(path: String): Long? {
    val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(path, error = null)
    val value = attrs?.get("NSFileSize")
    return when (value) {
        is Long -> value
        is Number -> value.toLong()
        else -> null
    }
}

private fun String.toLocalPath(): String? {
    val value = trim()
    if (value.startsWith("file:")) {
        return NSURL(string = value).path ?: value.removePrefix("file://")
    }
    return value.takeIf { it.isNotBlank() }
}

private fun resolveTotalBytes(
    startingBytes: Long,
    isPartialResume: Boolean,
    contentRangeHeader: String?,
    contentLength: Long?,
): Long? {
    parseContentRangeTotal(contentRangeHeader)?.let { return it }
    val normalizedLength = contentLength?.takeIf { it > 0L } ?: return null
    return if (isPartialResume && startingBytes > 0L) {
        startingBytes + normalizedLength
    } else {
        normalizedLength
    }
}

private fun parseContentRangeTotal(headerValue: String?): Long? {
    val value = headerValue?.trim().orEmpty()
    if (value.isBlank()) return null
    val slashIndex = value.lastIndexOf('/')
    if (slashIndex == -1 || slashIndex == value.lastIndex) return null
    val totalPart = value.substring(slashIndex + 1).trim()
    if (totalPart == "*") return null
    return totalPart.toLongOrNull()?.takeIf { it > 0L }
}
