package com.yimu.ai.crypto

import android.os.Environment
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File

object BackupDecryptor {

    /**
     * 一木记账默认备份目录
     */
    fun getDefaultYimuBackupDir(): File {
        val docDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        return File(docDir, "一木记账")
    }

    /**
     * 扫描查找最新的一木备份 zip 包
     */
    fun findLatestBackupFile(dir: File = getDefaultYimuBackupDir()): File? {
        if (!dir.exists() || !dir.isDirectory) return null
        return dir.listFiles { file ->
            file.isFile && file.name.endsWith(".zip", ignoreCase = true)
        }?.maxByOrNull { it.lastModified() }
    }

    /**
     * 使用用户数字 ID（密码）解密备份包中的 Custom.db
     * @param zipFile 备份 zip 文件
     * @param destDir 解压目标目录（通常为 context.filesDir 或内部 cacheDir）
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

            val zip = ZipFile(zipFile)
            if (zip.isEncrypted) {
                zip.setPassword(userId.trim().toCharArray())
            }

            // 查找是否存在 Custom.db
            val fileHeader = zip.getFileHeader("Custom.db")
                ?: throw IllegalStateException("备份包中未找到 Custom.db 核心数据库文件！")

            // 解压到目标目录
            if (!destDir.exists()) destDir.mkdirs()
            zip.extractFile(fileHeader, destDir.absolutePath)

            val extractedDb = File(destDir, "Custom.db")
            if (!extractedDb.exists()) {
                throw IllegalStateException("解压完成但未找到目标文件，可能密码(用户ID)不正确！")
            }
            extractedDb
        }
    }

    /**
     * 将编辑后的 Custom.db 按一木记账同款 AES-256 算法重新打包回 zip
     * 重新打包后的文件可以直接放入 /sdcard/Documents/一木记账/ 中供一木记账一键恢复
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
