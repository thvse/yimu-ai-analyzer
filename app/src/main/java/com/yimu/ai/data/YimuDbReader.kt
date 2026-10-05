package com.yimu.ai.data

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class YimuDbReader(private val dbFile: File) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private fun openDb(): SQLiteDatabase {
        if (!dbFile.exists()) {
            throw IllegalStateException("数据库文件不存在: ${dbFile.absolutePath}")
        }
        return SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE
        )
    }

    /**
     * 大小写不敏感的安全获取 Cursor 列索引扩展函数，彻底杜绝因驼峰/小写差异导致获取不到列名的问题
     */
    private fun Cursor.col(name: String): Int {
        val exact = getColumnIndex(name)
        if (exact >= 0) return exact
        val lower = getColumnIndex(name.lowercase())
        if (lower >= 0) return lower
        for (i in 0 until columnCount) {
            if (getColumnName(i).equals(name, ignoreCase = true)) {
                return i
            }
        }
        return -1
    }

    /**
     * 获取原生数据库 bill 表总行数（用于诊断）
     */
    fun getRawBillCount(): Int {
        val db = openDb()
        return try {
            val cursor = db.rawQuery("SELECT count(*) FROM bill", null)
            val cnt = if (cursor.moveToFirst()) cursor.getInt(0) else 0
            cursor.close()
            cnt
        } catch (e: Exception) {
            e.printStackTrace()
            0
        } finally {
            db.close()
        }
    }

    /**
     * 获取所有一级分类字典
     */
    fun getParentCategories(): Map<Long, String> {
        val db = openDb()
        val result = mutableMapOf<Long, String>()
        try {
            val cursor = db.rawQuery("SELECT categoryid, categoryname FROM parentcategory", null)
            val idIdx = cursor.col("categoryid")
            val nameIdx = cursor.col("categoryname")
            while (cursor.moveToNext()) {
                val id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
                val name = if (nameIdx >= 0) (cursor.getString(nameIdx) ?: "") else ""
                if (id != 0L && name.isNotBlank()) {
                    result[id] = name
                }
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }
        return result
    }

    /**
     * 获取所有二级分类字典
     */
    fun getChildCategories(): Map<Long, String> {
        val db = openDb()
        val result = mutableMapOf<Long, String>()
        try {
            val cursor = db.rawQuery("SELECT categoryid, categoryname FROM childcategory", null)
            val idIdx = cursor.col("categoryid")
            val nameIdx = cursor.col("categoryname")
            while (cursor.moveToNext()) {
                val id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
                val name = if (nameIdx >= 0) (cursor.getString(nameIdx) ?: "") else ""
                if (id != 0L && name.isNotBlank()) {
                    result[id] = name
                }
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }
        return result
    }

    /**
     * 获取所有账户/资产字典
     */
    fun getAssets(): Map<Long, String> {
        val db = openDb()
        val result = mutableMapOf<Long, String>()
        try {
            val cursor = db.rawQuery("SELECT assetid, assetname FROM asset", null)
            val idIdx = cursor.col("assetid")
            val nameIdx = cursor.col("assetname")
            while (cursor.moveToNext()) {
                val id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
                val name = if (nameIdx >= 0) (cursor.getString(nameIdx) ?: "") else ""
                if (id != 0L && name.isNotBlank()) {
                    result[id] = name
                }
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }
        return result
    }

    /**
     * 获取资产与负债结构摘要
     */
    fun getAssetSummary(): AssetSummary {
        val db = openDb()
        val accounts = mutableListOf<AssetItem>()
        var totalPositive = 0.0
        var totalNegative = 0.0

        try {
            val cursor = db.rawQuery(
                "SELECT * FROM asset WHERE (delete_lpcolumn != 1 OR delete_lpcolumn IS NULL) AND (hide != 1 OR hide IS NULL) ORDER BY positionweight ASC",
                null
            )
            val idIdx = cursor.col("assetid")
            val nameIdx = cursor.col("assetname")
            val numIdx = cursor.col("assetnumber")
            val grpIdx = cursor.col("groupname")
            val typeIdx = cursor.col("assettype")

            while (cursor.moveToNext()) {
                val id = if (idIdx >= 0) cursor.getLong(idIdx) else cursor.getLong(0)
                val name = if (nameIdx >= 0) (cursor.getString(nameIdx) ?: "账户") else "账户"
                val balance = if (numIdx >= 0) cursor.getDouble(numIdx) else 0.0
                val grp = if (grpIdx >= 0) (cursor.getString(grpIdx) ?: "") else ""
                val type = if (typeIdx >= 0) cursor.getInt(typeIdx) else 1

                if (balance >= 0) {
                    totalPositive += balance
                } else {
                    totalNegative += balance
                }

                accounts.add(
                    AssetItem(
                        id = id,
                        name = name,
                        balance = balance,
                        groupName = grp,
                        assetType = type
                    )
                )
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }

        return AssetSummary(
            totalAssets = totalPositive,
            totalLiabilities = totalNegative,
            netAssets = totalPositive + totalNegative,
            accounts = accounts
        )
    }

    /**
     * 分页查询账单明细（合并 bill 与 transfer 表）
     */
    fun getBills(limit: Int = 100, offset: Int = 0): List<BillItem> {
        val db = openDb()
        val parentCats = getParentCategories()
        val childCats = getChildCategories()
        val assets = getAssets()
        val list = mutableListOf<BillItem>()

        try {
            // 1. 查询常规收支流水 (bill)
            val billQuery = """
                SELECT * FROM bill
                WHERE delete_lpcolumn != 1 OR delete_lpcolumn IS NULL
                ORDER BY time DESC, id DESC
                LIMIT $limit OFFSET $offset
            """.trimIndent()

            val cursor = db.rawQuery(billQuery, null)
            val idIdx = cursor.col("id")
            val costIdx = cursor.col("cost")
            val billTypeIdx = cursor.col("billtype")
            val timeIdx = cursor.col("time")
            val remarkIdx = cursor.col("remark")
            val parentCatIdx = cursor.col("parentcategoryid")
            val childCatIdx = cursor.col("childcategoryid")
            val assetIdIdx = cursor.col("assetid")

            while (cursor.moveToNext()) {
                val id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
                val cost = if (costIdx >= 0) cursor.getDouble(costIdx) else 0.0
                val billType = if (billTypeIdx >= 0) cursor.getInt(billTypeIdx) else 0
                val timeLong = if (timeIdx >= 0) cursor.getLong(timeIdx) else 0L
                val timeStr = if (timeLong > 0) {
                    try {
                        dateFormat.format(Date(timeLong))
                    } catch (e: Exception) {
                        timeLong.toString()
                    }
                } else ""
                val remark = if (remarkIdx >= 0) cursor.getString(remarkIdx) else null
                val parentId = if (parentCatIdx >= 0) cursor.getLong(parentCatIdx) else 0L
                val childId = if (childCatIdx >= 0) cursor.getLong(childCatIdx) else 0L
                val assetId = if (assetIdIdx >= 0) cursor.getLong(assetIdIdx) else 0L

                list.add(
                    BillItem(
                        id = id,
                        cost = cost,
                        billType = billType,
                        time = timeStr,
                        remark = remark,
                        parentCategoryId = parentId,
                        parentCategoryName = parentCats[parentId] ?: "默认",
                        childCategoryId = childId,
                        childCategoryName = childCats[childId] ?: "其他",
                        assetId = assetId,
                        assetName = assets[assetId] ?: "账户"
                    )
                )
            }
            cursor.close()

            // 2. 查询转账记录 (transfer 表)
            try {
                val transferCursor = db.rawQuery(
                    "SELECT * FROM transfer WHERE delete_lpcolumn != 1 OR delete_lpcolumn IS NULL ORDER BY time DESC LIMIT $limit",
                    null
                )
                val tIdIdx = transferCursor.col("id")
                val tCostIdx = transferCursor.col("cost")
                val tTimeIdx = transferCursor.col("time")
                val tRemarkIdx = transferCursor.col("remark")
                val tFromIdx = transferCursor.col("fromassetid")
                val tToIdx = transferCursor.col("toassetid")

                while (transferCursor.moveToNext()) {
                    val id = if (tIdIdx >= 0) transferCursor.getLong(tIdIdx) else 0L
                    val cost = if (tCostIdx >= 0) transferCursor.getDouble(tCostIdx) else 0.0
                    val timeLong = if (tTimeIdx >= 0) transferCursor.getLong(tTimeIdx) else 0L
                    val timeStr = if (timeLong > 0) {
                        try { dateFormat.format(Date(timeLong)) } catch (e: Exception) { timeLong.toString() }
                    } else ""
                    val remark = if (tRemarkIdx >= 0) transferCursor.getString(tRemarkIdx) else null
                    val fromId = if (tFromIdx >= 0) transferCursor.getLong(tFromIdx) else 0L
                    val toId = if (tToIdx >= 0) transferCursor.getLong(tToIdx) else 0L
                    val fromName = assets[fromId] ?: "账户"
                    val toName = assets[toId] ?: "账户"

                    list.add(
                        BillItem(
                            id = id + 1_000_000_000L, // 偏移避免 ID 冲突
                            cost = cost,
                            billType = 2, // 2: 转账
                            time = timeStr,
                            remark = remark ?: "转账至 $toName",
                            parentCategoryId = 0L,
                            parentCategoryName = "转账",
                            childCategoryId = 0L,
                            childCategoryName = "$fromName ➔ $toName",
                            assetId = fromId,
                            assetName = fromName
                        )
                    )
                }
                transferCursor.close()
            } catch (e: Exception) {
                // transfer 表可能不存在或为空，忽略
            }

            // 按时间倒序排序
            list.sortByDescending { it.time }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }
        return list
    }

    /**
     * 计算统计概要（总支出、总收入、结余、分类排名、资产概况）
     */
    fun getMonthlySummary(backupName: String = "", backupModifiedTime: String = ""): SpendingSummary {
        val db = openDb()
        val parentCats = getParentCategories()
        val assetSummary = getAssetSummary()
        val rawCount = getRawBillCount()

        var totalExpense = 0.0
        var totalIncome = 0.0
        var count = 0
        val categoryMap = mutableMapOf<String, Double>()

        try {
            val cursor = db.rawQuery(
                "SELECT cost, billtype, parentcategoryid FROM bill WHERE delete_lpcolumn != 1 OR delete_lpcolumn IS NULL",
                null
            )
            val costIdx = cursor.col("cost")
            val typeIdx = cursor.col("billtype")
            val pCatIdx = cursor.col("parentcategoryid")

            while (cursor.moveToNext()) {
                val cost = if (costIdx >= 0) cursor.getDouble(costIdx) else 0.0
                val type = if (typeIdx >= 0) cursor.getInt(typeIdx) else 0
                val pId = if (pCatIdx >= 0) cursor.getLong(pCatIdx) else 0L
                val catName = parentCats[pId] ?: "其他"

                count++
                if (type == 0) { // 支出
                    totalExpense += cost
                    categoryMap[catName] = (categoryMap[catName] ?: 0.0) + cost
                } else if (type == 1) { // 收入
                    totalIncome += cost
                }
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }

        // 计算分类百分比排序
        val rankings = categoryMap.entries
            .sortedByDescending { it.value }
            .map { (name, amount) ->
                val pct = if (totalExpense > 0) (amount / totalExpense).toFloat() else 0f
                CategoryExpense(categoryName = name, amount = amount, percentage = pct)
            }

        return SpendingSummary(
            totalExpense = totalExpense,
            totalIncome = totalIncome,
            balance = totalIncome - totalExpense,
            billCount = count,
            categoryRanking = rankings,
            assetSummary = assetSummary,
            rawBillCount = rawCount,
            backupFileName = backupName,
            backupFileModified = backupModifiedTime
        )
    }

    /**
     * 更新指定账单的一级/二级分类
     */
    fun updateBillCategory(billId: Long, parentCategoryId: Long, childCategoryId: Long): Boolean {
        val db = openDb()
        return try {
            val sql = "UPDATE bill SET parentcategoryid = ?, childcategoryid = ? WHERE id = ?"
            db.execSQL(sql, arrayOf<Any>(parentCategoryId, childCategoryId, billId))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db.close()
        }
    }
}
