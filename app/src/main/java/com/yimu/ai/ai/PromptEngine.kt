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
            sb.appendLine("- 账面收支结余：¥%.2f".format(summary.balance))
            sb.appendLine("- 累计收支记账笔数：%d 笔".format(summary.billCount))

            summary.assetSummary?.let { assets ->
                sb.appendLine()
                sb.appendLine("### 用户当前资产与负债结构（重点）：")
                sb.appendLine("- 净资产：¥%.2f".format(assets.netAssets))
                sb.appendLine("- 总资产（资金/投资）：¥%.2f".format(assets.totalAssets))
                sb.appendLine("- 总负债（信贷/借款）：¥%.2f".format(assets.totalLiabilities))
                sb.appendLine("各账户余额明细：")
                assets.accounts.forEach { acc ->
                    val typeStr = if (acc.isDebt) "负债" else "资产"
                    val groupStr = if (acc.groupName.isNotBlank()) " [${acc.groupName}]" else ""
                    sb.appendLine("  * ${acc.name}$groupStr: ¥%.2f ($typeStr)".format(acc.balance))
                }
            }

            if (summary.categoryRanking.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("### 各项分类支出排行：")
                summary.categoryRanking.take(8).forEachIndexed { index, cat ->
                    sb.appendLine("${index + 1}. ${cat.categoryName}：¥%.2f (占比 %.1f%%)".format(cat.amount, cat.percentage * 100))
                }
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
        } else if (summary != null && summary.billCount == 0) {
            sb.appendLine()
            sb.appendLine("（注意：用户当前账本中尚未记录任何日常消费/收入流水，但已配置了资产账户与余额。你可以重点为用户分析资产负债比率、债务优化还款策略或资金流动性）")
        }

        sb.appendLine()
        sb.appendLine("### 你的分析与回复原则：")
        sb.appendLine("1. 严格基于上述真实的账本数据回答用户的提问，当用户询问财务、负债、资产时给出具体真实数字。")
        sb.appendLine("2. 资产负债健康度评估：结合正向资产与负债结构（如借呗、花呗、欠款），给出科学的负债偿还顺序与应急备用金建议。")
        sb.appendLine("3. 回答条理清晰，多使用清晰的 Markdown 列表和重点加粗。语言亲切自然、充满鼓励。")
        sb.appendLine("4. 若用户发送了账单截图、购物小票或发票图片，利用你的多模态视觉能力自动识别消费金额、商家和项目明细，并推荐一木记账适配的一级/二级分类及记账建议。")

        return sb.toString()
    }
}
