package com.yimu.ai.crypto

import android.content.Context
import android.net.Uri
import android.os.Environment
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File

object BackupDecryptor {

    /**
     * 扫描查找所有可能存放一木备份的目录
     */
    fun getCandidateBackupDirs(): List<File> {
        val dirs = mutableListOf<File>()
        try {
            val docDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            dirs.add(File(docDir, "一木记账"))
            dirs.add(docDir)
        } catch (e: Exception) {}

        try {
            val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dirs.add(File(dlDir, "一木记账"))
            dirs.add(dlDir)
        } catch (e: Exception) {}

        try {
            val root = Environment.getExternalStorageDirectory()
            dirs.add(File(root, "Documents/一木记账"))
            dirs.add(File(root, "Download/一木记账"))
            dirs.add(File(root, "一木记账"))
        } catch (e: Exception) {}

        return dirs.distinctBy { it.absolutePath }
    }

    /**
     * 获取手机中扫描到的所有一木备份文件，按修改时间倒序排列
     */
    fun findAllBackupFiles(): List<File> {
        val results = mutableListOf<File>()
        for (dir in getCandidateBackupDirs()) {
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles { file ->
                    file.isFile && file.name.endsWith(".zip", ignoreCase = true)
                }?.let { results.addAll(it) }
            }
        }
        return results.distinctBy { it.absolutePath }
            .sortedByDescending { it.lastModified() }
    }

    /**
     * 扫描查找最新的备份 zip 包
     */
    fun findLatestBackupFile(): File? {
        return findAllBackupFiles().firstOrNull()
    }

    /**
     * 将用户通过系统文件选择器 (SAF) 选中的文件复制到应用缓存临时文件
     */
    fun copyUriToTempFile(context: Context, uri: Uri): File {
        val destFile = File(context.cacheDir, "selected_backup.zip")
        if (destFile.exists()) {
            destFile.delete()
        }
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("无法打开所选文件流")
        return destFile
    }

    /**
     * 使用用户数字 ID（密码）解密备份包中的 Custom.db
     * @param zipFile 备份 zip 文件
     * @param destDir 解压目标目录
     * @param userId 一木记账用户数字 ID
     */
    fun decryptCustomDb(zipFile: File, destDir: File, userId: String): Result<File> {
        return runCatching {
            if (!zipFile.exists()) {
                throw IllegalArgumentException("备份文件不存在: ${zipFile.absolutePath}")
            }
            if (userId.isBlank()) {
                throw IllegalArgumentException("用户 ID 不能为空，解密需要一木记账的用户数字 ID 作为密码！")
            }

            if (!destDir.exists()) destDir.mkdirs()

            // 彻底清理旧数据库文件及 WAL 临时文件，避免 SQLite 句柄或缓存冲突
            val extractedDb = File(destDir, "Custom.db")
            val walFile = File(destDir, "Custom.db-wal")
            val shmFile = File(destDir, "Custom.db-shm")
            val journalFile = File(destDir, "Custom.db-journal")

            try { if (extractedDb.exists()) extractedDb.delete() } catch (e: Exception) {}
            try { if (walFile.exists()) walFile.delete() } catch (e: Exception) {}
            try { if (shmFile.exists()) shmFile.delete() } catch (e: Exception) {}
            try { if (journalFile.exists()) journalFile.delete() } catch (e: Exception) {}

            val zip = ZipFile(zipFile)
            if (zip.isEncrypted) {
                zip.setPassword(userId.trim().toCharArray())
            }

            // 查找是否存在 Custom.db
            val fileHeader = zip.getFileHeader("Custom.db")
                ?: throw IllegalStateException("备份包中未找到 Custom.db 核心数据库文件！")

            // 解压到目标目录
            zip.extractFile(fileHeader, destDir.absolutePath)

            if (!extractedDb.exists() || extractedDb.length() == 0L) {
                throw IllegalStateException("解压完成但未找到目标 Custom.db，请确认密码(用户ID)是否正确！")
            }
            extractedDb
        }
    }

    /**
     * 重新打包为一木记账同款 AES-256 备份包
     */
    fun repackCustomDb(dbFile: File, destZipFile: File, userId: String): Result<File> {
        return runCatching {
            if (!dbFile.exists()) {
                throw IllegalArgumentException("数据库文件不存在: ${dbFile.absolutePath}")
            }

            if (destZipFile.exists()) {
                destZipFile.delete()
            }

            val zip = ZipFile(destZipFile, userId.trim().toCharArray())
            val parameters = ZipParameters().apply {
                isEncryptFiles = true
                encryptionMethod = EncryptionMethod.AES
                aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
            }

            zip.addFile(dbFile, parameters)
            destZipFile
        }
    }
}
