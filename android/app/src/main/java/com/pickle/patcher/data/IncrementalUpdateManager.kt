package com.pickle.patcher.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Manages incremental updates: fetches a manifest.json from GitHub Releases,
 * compares file hashes against a local libs/ cache, and downloads only changed
 * .so files. This avoids re-downloading the full 28MB bundle on every update.
 */
object IncrementalUpdateManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class Manifest(
        val version: String = "",
        val game: String = "cs16client",
        val abi: String = "arm64-v8a",
        val files: List<ManifestEntry> = emptyList(),
    )

    @Serializable
    data class ManifestEntry(
        val name: String = "",
        val asset: String = "",
        val path: String = "",
        val target: String = "",
        val sha256: String = "",
        val size: Long = 0,
        val required: Boolean = true,
        val description: String = "",
    )

    data class UpdateResult(
        val changed: List<ManifestEntry>,
        val unchanged: List<ManifestEntry>,
        val totalBytes: Long,
    )

    /**
     * Fetch the manifest.json from a release asset.
     * arm64-v8a uses manifest.json, armeabi-v7a uses manifest-v7a.json.
     */
    suspend fun fetchManifest(repo: String, tag: String, abi: String): Manifest? {
        val manifestName = if (abi == "armeabi-v7a") "manifest-v7a.json" else "manifest.json"
        val url = "https://github.com/$repo/releases/download/$tag/$manifestName"
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "cs16-amxx-patcher")
                .header("Accept", "application/octet-stream")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                json.decodeFromString<Manifest>(body)
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Load the local manifest from libs/ directory.
     */
    fun loadLocalManifest(libsDir: File, abi: String): Manifest? {
        val manifestFile = File(libsDir, "$abi/manifest.json")
        if (!manifestFile.exists()) return null
        return try {
            json.decodeFromString<Manifest>(manifestFile.readText())
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Save the local manifest to libs/ directory.
     */
    fun saveLocalManifest(libsDir: File, abi: String, manifest: Manifest) {
        val manifestFile = File(libsDir, "$abi/manifest.json")
        manifestFile.parentFile?.mkdirs()
        manifestFile.writeText(json.encodeToString(Manifest.serializer(), manifest))
    }

    /**
     * Compare remote manifest against local to find changed files.
     * Returns entries that need downloading (new or changed hashes).
     */
    fun diff(remote: Manifest, local: Manifest?): UpdateResult {
        if (local == null) {
            // No local manifest — need everything
            val totalBytes = remote.files.sumOf { it.size }
            return UpdateResult(changed = remote.files, unchanged = emptyList(), totalBytes = totalBytes)
        }

        val localMap = local.files.associateBy { it.asset.ifBlank { it.name } }
        val changed = mutableListOf<ManifestEntry>()
        val unchanged = mutableListOf<ManifestEntry>()

        for (entry in remote.files) {
            val key = entry.asset.ifBlank { entry.name }
            val localEntry = localMap[key]
            if (localEntry == null || localEntry.sha256 != entry.sha256) {
                changed.add(entry)
            } else {
                unchanged.add(entry)
            }
        }

        val totalBytes = changed.sumOf { it.size }
        return UpdateResult(changed = changed, unchanged = unchanged, totalBytes = totalBytes)
    }

    /**
     * Download only changed files from the release.
     * Each file is downloaded as: <repo>/releases/download/<tag>/<asset-name>
     * (uploaded as individual release assets by gen-manifest.py)
     */
    suspend fun downloadChanged(
        repo: String,
        tag: String,
        changed: List<ManifestEntry>,
        libsDir: File,
        abi: String,
        onFileStart: (index: Int, entry: ManifestEntry) -> Unit = { _, _ -> },
        onFileProgress: (index: Int, entry: ManifestEntry, fileProgress: Float) -> Unit = { _, _, _ -> },
        onProgress: (downloaded: Int, total: Int, bytesWritten: Long) -> Unit = { _, _, _ -> },
    ) {
        val targetDir = File(libsDir, abi)
        targetDir.mkdirs()

        for ((index, entry) in changed.withIndex()) {
            onFileStart(index, entry)

            // Download using ABI-prefixed asset name
            val assetName = entry.asset.ifBlank { entry.name }
            val url = "https://github.com/$repo/releases/download/$tag/$assetName"

            val destFile = File(targetDir, assetName)
            destFile.parentFile?.mkdirs()

            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "cs16-amxx-patcher")
                .header("Accept", "application/octet-stream")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Download failed for $assetName: ${resp.code}")
                }
                val body = resp.body ?: throw IllegalStateException("Empty body for $assetName")
                val expectedSize = entry.size.takeIf { it > 0 }
                    ?: body.contentLength().takeIf { it > 0 }
                    ?: 0L
                var written = 0L
                body.byteStream().use { input ->
                    destFile.outputStream().use { output ->
                        val buffer = ByteArray(65536)
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                            written += n
                            if (expectedSize > 0) {
                                onFileProgress(index, entry, (written.toFloat() / expectedSize).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
                destFile.setExecutable(true)

                // Verify SHA-256
                val actualHash = sha256File(destFile)
                if (actualHash != entry.sha256) {
                    destFile.delete()
                    throw IllegalStateException(
                        "Hash mismatch for $assetName: expected ${entry.sha256}, got $actualHash"
                    )
                }
            }

            onProgress(index + 1, changed.size, changed.take(index + 1).sumOf { it.size })
        }
    }

    /**
     * Build a Bundle-compatible in-memory file map from libs/ directory.
     * This allows the existing ZipRepacker to work without modification —
     * it receives the same Map<String, ByteArray> it always has.
     */
    fun loadBundleFiles(libsDir: File, abi: String, manifest: Manifest): Map<String, ByteArray> {
        val targetDir = File(libsDir, abi)
        val files = HashMap<String, ByteArray>()
        for (entry in manifest.files) {
            val assetName = entry.asset.ifBlank { entry.name }
            val file = File(targetDir, assetName)
            if (file.exists()) {
                files[entry.target] = file.readBytes()
            }
        }
        return files
    }

    private fun sha256File(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(65536)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
