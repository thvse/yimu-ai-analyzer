package com.yimu.ai.ai

import com.yimu.ai.data.BillItem
import com.yimu.ai.data.SpendingSummary

object PromptEngine {

    /**
     * 构建包含真实账本上下文的系统提示词
     */
    fun buildSystemPrompt(summary: SpendingSummary?, recentBills: List<BillItem>): String {
        val sb = StringBuilder()
        sb.appendLine("你是一名专业、温和且敏锐的私人财务健康顾问与记账AI助手。")
        sb.appendLine("你正在为用户分析其在【一木记账】软件中的个人账本数据。")
        sb.appendLine()
        sb.appendLine("### 当前用户的账本核心统计摘要：")

        if (summary != null) {
            sb.appendLine("- 总支出：¥%.2f".format(summary.totalExpense))
            sb.appendLine("- 总收入：¥%.2f".format(summary.totalIncome))
            sb.appendLine("- 账面结余：¥%.2f".format(summary.balance))
            sb.appendLine("- 累计记账笔数：%d 笔".format(summary.billCount))
            sb.appendLine()
            sb.appendLine("### 各项分类支出排行：")
            summary.categoryRanking.take(8).forEachIndexed { index, cat ->
                sb.appendLine("${index + 1}. ${cat.categoryName}：¥%.2f (占比 %.1f%%)".format(cat.amount, cat.percentage * 100))
            }
        } else {
            sb.appendLine("（当前暂未加载账本数据，请引导用户先输入一木记账的用户ID完成解密）")
        }

        if (recentBills.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("### 最近记账明细（精选前 15 笔）：")
            recentBills.take(15).forEach { b ->
                val typeStr = if (b.isExpense) "支出" else "收入"
                val remarkStr = if (!b.remark.isNullOrBlank()) " (${b.remark})" else ""
                sb.appendLine("- [${b.time}] $typeStr ¥%.2f | 分类: ${b.parentCategoryName}->${b.childCategoryName} | 账户: ${b.assetName}$remarkStr".format(b.cost))
            }
        }

        sb.appendLine()
        sb.appendLine("### 你的分析与回复原则：")
        sb.appendLine("1. 严格基于上述真实的账本数据回答用户的提问，当用户询问开销、比例、习惯时给出具体数字。")
        sb.appendLine("2. 主动发现消费结构中的异常点（如餐饮占比过高、某项突发大额开销、恩格尔系数偏高）。")
        sb.appendLine("3. 给出切实可行、不生硬的省钱与预算建议。")
        sb.appendLine("4. 回答条理清晰，多使用清晰的 Markdown 列表和重点加粗。语言亲切自然。")
        sb.appendLine("5. 若用户发送了账单截图、购物小票或发票图片，利用你的多模态视觉能力自动识别消费金额、商家和项目明细，并推荐一木记账适配的一级/二级分类及记账建议。")

        return sb.toString()
    }
}
