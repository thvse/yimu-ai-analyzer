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
     * 判定某一分类是否为“收入”类别
     * 一木记账默认：
     * parentCategoryId == 9 为系统“收入”大类
     * 其余所有预设分类（1~8、99）均为“支出”
     * 兼容用户自定义分类：若分类名称含有“收入”、“工资”、“奖金”、“报销”、“补贴”等字样，亦判定为收入
     */
    fun isIncomeCategory(parentCategoryId: Long, parentCategoryName: String, childCategoryName: String = ""): Boolean {
        if (parentCategoryId == 9L) return true
        if (parentCategoryName.contains("收入") || parentCategoryName.contains("工资") || parentCategoryName.contains("奖金") || parentCategoryName.contains("报销")) return true
        if (childCategoryName.contains("收入") || childCategoryName.contains("工资") || childCategoryName.contains("奖金") || childCategoryName.contains("报销")) return true
        return false
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
     * 获取所有一级分类字典 (id -> name)
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
     * 获取所有二级分类字典 (id -> name)
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
     * 获取所有账户/资产字典 (id -> name)
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
     * 分页查询账单明细（合并 bill 与 transfer 表，准确判定支出/收入/转账）
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
            val rawBillTypeIdx = cursor.col("billtype")
            val timeIdx = cursor.col("time")
            val remarkIdx = cursor.col("remark")
            val parentCatIdx = cursor.col("parentcategoryid")
            val childCatIdx = cursor.col("childcategoryid")
            val assetIdIdx = cursor.col("assetid")

            while (cursor.moveToNext()) {
                val id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
                val cost = if (costIdx >= 0) cursor.getDouble(costIdx) else 0.0
                val rawMethod = if (rawBillTypeIdx >= 0) cursor.getInt(rawBillTypeIdx) else 1
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

                val parentName = parentCats[parentId] ?: "默认分类"
                val childName = childCats[childId] ?: "常规"

                // 核心修复：依据真实分类判断收支类型
                val isInc = isIncomeCategory(parentId, parentName, childName)
                val normalizedType = if (isInc) 1 else 0 // 0: 支出, 1: 收入

                list.add(
                    BillItem(
                        id = id,
                        cost = cost,
                        billType = normalizedType,
                        time = timeStr,
                        remark = remark,
                        parentCategoryId = parentId,
                        parentCategoryName = parentName,
                        childCategoryId = childId,
                        childCategoryName = childName,
                        assetId = assetId,
                        assetName = assets[assetId] ?: "账户",
                        recordMethod = rawMethod
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
                            remark = remark ?: "内部转账: $fromName ➔ $toName",
                            parentCategoryId = 0L,
                            parentCategoryName = "转账",
                            childCategoryId = 0L,
                            childCategoryName = "$fromName ➔ $toName",
                            assetId = fromId,
                            assetName = fromName,
                            recordMethod = 1
                        )
                    )
                }
                transferCursor.close()
            } catch (e: Exception) {
                // transfer 表可能为空或不存在，忽略
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
     * 计算统计概要（总支出、总收入、结余、支出/收入分类排行榜、资产概况）
     */
    fun getMonthlySummary(backupName: String = "", backupModifiedTime: String = ""): SpendingSummary {
        val db = openDb()
        val parentCats = getParentCategories()
        val childCats = getChildCategories()
        val assetSummary = getAssetSummary()
        val rawCount = getRawBillCount()

        var totalExpense = 0.0
        var totalIncome = 0.0
        var count = 0

        val expenseCategoryMap = mutableMapOf<String, Double>()
        val expenseCategoryCount = mutableMapOf<String, Int>()
        val expenseCategoryIdMap = mutableMapOf<String, Long>()

        val incomeCategoryMap = mutableMapOf<String, Double>()
        val incomeCategoryCount = mutableMapOf<String, Int>()
        val incomeCategoryIdMap = mutableMapOf<String, Long>()

        try {
            val cursor = db.rawQuery(
                "SELECT cost, parentcategoryid, childcategoryid FROM bill WHERE delete_lpcolumn != 1 OR delete_lpcolumn IS NULL",
                null
            )
            val costIdx = cursor.col("cost")
            val pCatIdx = cursor.col("parentcategoryid")
            val cCatIdx = cursor.col("childcategoryid")

            while (cursor.moveToNext()) {
                val cost = if (costIdx >= 0) cursor.getDouble(costIdx) else 0.0
                val pId = if (pCatIdx >= 0) cursor.getLong(pCatIdx) else 0L
                val cId = if (cCatIdx >= 0) cursor.getLong(cCatIdx) else 0L

                val pName = parentCats[pId] ?: "默认分类"
                val cName = childCats[cId] ?: "常规"

                count++
                val isInc = isIncomeCategory(pId, pName, cName)

                if (isInc) {
                    totalIncome += cost
                    incomeCategoryMap[pName] = (incomeCategoryMap[pName] ?: 0.0) + cost
                    incomeCategoryCount[pName] = (incomeCategoryCount[pName] ?: 0) + 1
                    incomeCategoryIdMap[pName] = pId
                } else {
                    totalExpense += cost
                    expenseCategoryMap[pName] = (expenseCategoryMap[pName] ?: 0.0) + cost
                    expenseCategoryCount[pName] = (expenseCategoryCount[pName] ?: 0) + 1
                    expenseCategoryIdMap[pName] = pId
                }
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }

        // 支出分类百分比排行
        val expenseRankings = expenseCategoryMap.entries
            .sortedByDescending { it.value }
            .map { (name, amount) ->
                val pct = if (totalExpense > 0) (amount / totalExpense).toFloat() else 0f
                CategoryExpense(
                    categoryId = expenseCategoryIdMap[name] ?: 0L,
                    categoryName = name,
                    amount = amount,
                    percentage = pct,
                    count = expenseCategoryCount[name] ?: 0,
                    isIncome = false
                )
            }

        // 收入分类百分比排行
        val incomeRankings = incomeCategoryMap.entries
            .sortedByDescending { it.value }
            .map { (name, amount) ->
                val pct = if (totalIncome > 0) (amount / totalIncome).toFloat() else 0f
                CategoryExpense(
                    categoryId = incomeCategoryIdMap[name] ?: 0L,
                    categoryName = name,
                    amount = amount,
                    percentage = pct,
                    count = incomeCategoryCount[name] ?: 0,
                    isIncome = true
                )
            }

        val allCats = getAllCategoriesWithStats()

        return SpendingSummary(
            totalExpense = totalExpense,
            totalIncome = totalIncome,
            balance = totalIncome - totalExpense,
            billCount = count,
            categoryRanking = expenseRankings,
            incomeRanking = incomeRankings,
            assetSummary = assetSummary,
            rawBillCount = rawCount,
            backupFileName = backupName,
            backupFileModified = backupModifiedTime,
            allCategories = allCats
        )
    }

    /**
     * 获取一木记账所有一级与二级分类树状列表及当前账本的消费统计
     */
    fun getAllCategoriesWithStats(): List<FullCategory> {
        val db = openDb()
        val list = mutableListOf<FullCategory>()
        try {
            // 1. 查询账单统计 (按一级和二级汇总)
            val parentSpent = mutableMapOf<Long, Double>()
            val parentCount = mutableMapOf<Long, Int>()
            val childSpent = mutableMapOf<Long, Double>()
            val childCount = mutableMapOf<Long, Int>()

            val billCursor = db.rawQuery(
                "SELECT cost, parentcategoryid, childcategoryid FROM bill WHERE delete_lpcolumn != 1 OR delete_lpcolumn IS NULL",
                null
            )
            val costCol = billCursor.col("cost")
            val pCol = billCursor.col("parentcategoryid")
            val cCol = billCursor.col("childcategoryid")

            while (billCursor.moveToNext()) {
                val cost = if (costCol >= 0) billCursor.getDouble(costCol) else 0.0
                val pId = if (pCol >= 0) billCursor.getLong(pCol) else 0L
                val cId = if (cCol >= 0) billCursor.getLong(cCol) else 0L

                parentSpent[pId] = (parentSpent[pId] ?: 0.0) + cost
                parentCount[pId] = (parentCount[pId] ?: 0) + 1

                childSpent[cId] = (childSpent[cId] ?: 0.0) + cost
                childCount[cId] = (childCount[cId] ?: 0) + 1
            }
            billCursor.close()

            // 2. 查询所有二级分类，按 parentCategoryId 分组
            val childMap = mutableMapOf<Long, MutableList<ChildCategoryItem>>()
            val childCursor = db.rawQuery(
                "SELECT categoryid, parentcategoryid, categoryname FROM childcategory ORDER BY positionweight ASC, categoryid ASC",
                null
            )
            val cidCol = childCursor.col("categoryid")
            val cPidCol = childCursor.col("parentcategoryid")
            val cNameCol = childCursor.col("categoryname")

            while (childCursor.moveToNext()) {
                val cid = if (cidCol >= 0) childCursor.getLong(cidCol) else 0L
                val pid = if (cPidCol >= 0) childCursor.getLong(cPidCol) else 0L
                val name = if (cNameCol >= 0) (childCursor.getString(cNameCol) ?: "") else ""

                val cItem = ChildCategoryItem(
                    id = cid,
                    parentId = pid,
                    name = name,
                    spentAmount = childSpent[cid] ?: 0.0,
                    billCount = childCount[cid] ?: 0
                )
                childMap.getOrPut(pid) { mutableListOf() }.add(cItem)
            }
            childCursor.close()

            // 3. 查询所有一级分类
            val parentCursor = db.rawQuery(
                "SELECT categoryid, categoryname FROM parentcategory ORDER BY positionweight ASC, categoryid ASC",
                null
            )
            val pidCol = parentCursor.col("categoryid")
            val pNameCol = parentCursor.col("categoryname")

            while (parentCursor.moveToNext()) {
                val pid = if (pidCol >= 0) parentCursor.getLong(pidCol) else 0L
                val name = if (pNameCol >= 0) (parentCursor.getString(pNameCol) ?: "") else ""
                val isInc = isIncomeCategory(pid, name, "")

                val children = childMap[pid] ?: emptyList()
                list.add(
                    FullCategory(
                        id = pid,
                        name = name,
                        isIncome = isInc,
                        spentAmount = parentSpent[pid] ?: 0.0,
                        billCount = parentCount[pid] ?: 0,
                        children = children
                    )
                )
            }
            parentCursor.close()

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }
        return list
    }

    /**
     * 向一木记账数据库写入/新增一笔记账流水
     */
    fun insertBill(
        cost: Double,
        parentCategoryId: Long,
        childCategoryId: Long,
        assetId: Long,
        remark: String? = null,
        timestamp: Long = System.currentTimeMillis(),
        userId: Long = 1467948L
    ): Long {
        val db = openDb()
        return try {
            var maxId = 0L
            val c = db.rawQuery("SELECT max(id), max(billid) FROM bill", null)
            if (c.moveToFirst()) {
                maxId = maxOf(c.getLong(0), c.getLong(1))
            }
            c.close()
            val newId = if (maxId > 0) maxId + 1 else 1L

            val values = android.content.ContentValues().apply {
                put("id", newId)
                put("billid", newId)
                put("userid", userId)
                put("bookid", 1L)
                put("billtype", 1) // 1 = 手动记账
                put("cost", cost)
                put("parentcategoryid", parentCategoryId)
                put("childcategoryid", childCategoryId)
                put("assetid", assetId)
                put("remark", remark ?: "")
                put("time", timestamp)
                put("recordtime", timestamp)
                put("updatetime", timestamp)
                put("delete_lpcolumn", 0)
            }
            val rowId = db.insert("bill", null, values)

            // 同步调整账户余额
            if (rowId != -1L && assetId > 0) {
                val isInc = isIncomeCategory(parentCategoryId, "", "")
                val delta = if (isInc) cost else -cost
                db.execSQL("UPDATE asset SET assetnumber = assetnumber + $delta, updatetime = $timestamp WHERE assetid = $assetId")
            }
            newId
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        } finally {
            db.close()
        }
    }

    /**
     * 更新指定账单的一级/二级分类
     */
    fun updateBillCategory(billId: Long, parentCategoryId: Long, childCategoryId: Long): Boolean {
        val db = openDb()
        return try {
            val now = System.currentTimeMillis()
            val values = android.content.ContentValues().apply {
                put("parentcategoryid", parentCategoryId)
                put("childcategoryid", childCategoryId)
                put("updatetime", now)
            }
            val rows = db.update("bill", values, "id = ?", arrayOf(billId.toString()))
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db.close()
        }
    }

    /**
     * 软删除指定账单
     */
    fun deleteBill(billId: Long): Boolean {
        val db = openDb()
        return try {
            val now = System.currentTimeMillis()
            val values = android.content.ContentValues().apply {
                put("delete_lpcolumn", 1)
                put("updatetime", now)
            }
            val rows = db.update("bill", values, "id = ?", arrayOf(billId.toString()))
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db.close()
        }
    }
}
