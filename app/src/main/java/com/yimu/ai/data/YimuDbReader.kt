package com.yimu.ai.data

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
            -1
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
            val cursor = db.rawQuery("SELECT categoryId, categoryName FROM parentcategory", null)
            val idIdx = cursor.getColumnIndex("categoryId")
            val nameIdx = cursor.getColumnIndex("categoryName")
            while (cursor.moveToNext()) {
                val id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
                val name = if (nameIdx >= 0) cursor.getString(nameIdx) else ""
                result[id] = name
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
            val cursor = db.rawQuery("SELECT categoryId, categoryName FROM childcategory", null)
            val idIdx = cursor.getColumnIndex("categoryId")
            val nameIdx = cursor.getColumnIndex("categoryName")
            while (cursor.moveToNext()) {
                val id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
                val name = if (nameIdx >= 0) cursor.getString(nameIdx) else ""
                result[id] = name
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
            val cursor = db.rawQuery("SELECT assetId, assetName FROM asset", null)
            val idIdx = cursor.getColumnIndex("assetId")
            val nameIdx = cursor.getColumnIndex("assetName")
            while (cursor.moveToNext()) {
                val id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
                val name = if (nameIdx >= 0) cursor.getString(nameIdx) else ""
                result[id] = name
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
                "SELECT assetId, assetName, assetNumber, groupName, assetType FROM asset WHERE (delete_lpcolumn != 1 OR delete_lpcolumn IS NULL) AND (hide != 1 OR hide IS NULL) ORDER BY positionWeight ASC",
                null
            )
            val idIdx = cursor.getColumnIndex("assetId")
            val nameIdx = cursor.getColumnIndex("assetName")
            val numIdx = cursor.getColumnIndex("assetNumber")
            val grpIdx = cursor.getColumnIndex("groupName")
            val typeIdx = cursor.getColumnIndex("assetType")

            while (cursor.moveToNext()) {
                val id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
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
     * 分页查询账单明细
     */
    fun getBills(limit: Int = 50, offset: Int = 0): List<BillItem> {
        val db = openDb()
        val parentCats = getParentCategories()
        val childCats = getChildCategories()
        val assets = getAssets()
        val list = mutableListOf<BillItem>()

        try {
            val query = """
                SELECT id, cost, billType, time, remark, parentCategoryId, childCategoryId, assetId
                FROM bill
                WHERE delete_lpcolumn != 1 OR delete_lpcolumn IS NULL
                ORDER BY time DESC, id DESC
                LIMIT $limit OFFSET $offset
            """.trimIndent()

            val cursor = db.rawQuery(query, null)
            val idIdx = cursor.getColumnIndex("id")
            val costIdx = cursor.getColumnIndex("cost")
            val billTypeIdx = cursor.getColumnIndex("billType")
            val timeIdx = cursor.getColumnIndex("time")
            val remarkIdx = cursor.getColumnIndex("remark")
            val parentCatIdx = cursor.getColumnIndex("parentCategoryId")
            val childCatIdx = cursor.getColumnIndex("childCategoryId")
            val assetIdIdx = cursor.getColumnIndex("assetId")

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
            val query = "SELECT cost, billType, parentCategoryId FROM bill WHERE delete_lpcolumn != 1 OR delete_lpcolumn IS NULL"
            val cursor = db.rawQuery(query, null)
            val costIdx = cursor.getColumnIndex("cost")
            val typeIdx = cursor.getColumnIndex("billType")
            val pCatIdx = cursor.getColumnIndex("parentCategoryId")

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
            val sql = "UPDATE bill SET parentCategoryId = ?, childCategoryId = ? WHERE id = ?"
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
