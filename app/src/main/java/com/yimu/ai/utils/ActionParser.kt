package com.yimu.ai.utils

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.yimu.ai.data.AiActionData

object ActionParser {
    private val gson = Gson()

    fun extractAction(rawText: String): Pair<String, AiActionData?> {
        val addBillRegex = Regex("```action:add_bill\\s*([\\s\\S]*?)```")
        val updateBillRegex = Regex("```action:update_bill\\s*([\\s\\S]*?)```")

        val addMatch = addBillRegex.find(rawText)
        if (addMatch != null) {
            val jsonStr = addMatch.groupValues[1].trim()
            val cleaned = rawText.replace(addMatch.value, "").trim()
            try {
                val json = gson.fromJson(jsonStr, JsonObject::class.java)
                val cost = json.get("cost")?.asDouble ?: 0.0
                val pName = json.get("parentCategoryName")?.asString ?: "食品餐饮"
                val cName = json.get("childCategoryName")?.asString ?: "请客吃饭"
                val assetName = json.get("assetName")?.asString ?: "微信钱包"
                val remark = json.get("remark")?.asString ?: ""
                val time = json.get("time")?.asString ?: ""

                val action = AiActionData.AddBill(
                    cost = cost,
                    parentCategoryId = 0L,
                    parentCategoryName = pName,
                    childCategoryId = 0L,
                    childCategoryName = cName,
                    assetId = 0L,
                    assetName = assetName,
                    remark = remark,
                    time = time
                )
                return Pair(cleaned, action)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val updateMatch = updateBillRegex.find(rawText)
        if (updateMatch != null) {
            val jsonStr = updateMatch.groupValues[1].trim()
            val cleaned = rawText.replace(updateMatch.value, "").trim()
            try {
                val json = gson.fromJson(jsonStr, JsonObject::class.java)
                val billId = json.get("billId")?.asLong ?: 0L
                val pName = json.get("parentCategoryName")?.asString ?: "食品餐饮"
                val cName = json.get("childCategoryName")?.asString ?: "餐饮"

                val action = AiActionData.UpdateCategory(
                    billId = billId,
                    parentCategoryId = 0L,
                    parentCategoryName = pName,
                    childCategoryId = 0L,
                    childCategoryName = cName
                )
                return Pair(cleaned, action)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return Pair(rawText, null)
    }
}
