package com.yimu.ai.ai

import com.yimu.ai.data.BillItem
import com.yimu.ai.data.SpendingSummary

object PromptEngine {

    /**
     * 构建包含真实账本上下文与可执行动作指令的系统提示词
     */
    fun buildSystemPrompt(summary: SpendingSummary?, recentBills: List<BillItem>): String {
        val sb = StringBuilder()
        sb.appendLine("你是一名全能且敏锐的私人财务总监与记账AI助手。")
        sb.appendLine("你拥有直接对【一木记账】本地数据库进行【新增记账入库】与【历史账单重新分类】的系统级执行能力！")
        sb.appendLine()
        sb.appendLine("### 当前用户的账本核心统计摘要：")

        if (summary != null) {
            sb.appendLine("- 总支出：¥%.2f".format(summary.totalExpense))
            sb.appendLine("- 总收入：¥%.2f".format(summary.totalIncome))
            sb.appendLine("- 账面收支结余：¥%.2f".format(summary.balance))
            sb.appendLine("- 累计收支记账笔数：%d 笔".format(summary.billCount))

            summary.assetSummary?.let { assets ->
                sb.appendLine()
                sb.appendLine("### 用户当前可用账户与余额资产：")
                sb.appendLine("- 净资产：¥%.2f | 总资产：¥%.2f | 总负债：¥%.2f".format(assets.netAssets, assets.totalAssets, assets.totalLiabilities))
                assets.accounts.forEach { acc ->
                    val typeStr = if (acc.isDebt) "负债" else "资金"
                    sb.appendLine("  * [账户ID:${acc.id}] ${acc.name}: ¥%.2f ($typeStr)".format(acc.balance))
                }
            }

            if (summary.categoryRanking.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("### 各项分类支出排行（前 8 项）：")
                summary.categoryRanking.take(8).forEachIndexed { index, cat ->
                    sb.appendLine("${index + 1}. ${cat.categoryName}：¥%.2f (占比 %.1f%%，共 %d 笔)".format(cat.amount, cat.percentage * 100, cat.count))
                }
            }

            if (summary.incomeRanking.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("### 收入分类明细：")
                summary.incomeRanking.take(5).forEachIndexed { index, cat ->
                    sb.appendLine("${index + 1}. ${cat.categoryName}：¥%.2f (占比 %.1f%%，共 %d 笔)".format(cat.amount, cat.percentage * 100, cat.count))
                }
            }
        } else {
            sb.appendLine("（当前暂未加载账本数据，请引导用户先输入一木记账的用户ID完成解密）")
        }

        sb.appendLine()
        sb.appendLine("### 一木记账系统标准分类字典：")
        sb.appendLine("- 支出大类：食品餐饮 (粮油调味/请客吃饭/生鲜食品/休闲零食/外卖早餐午餐晚餐)、购物消费 (服饰运动/手机数码/生活日用/宠物用品/个护美妆)、居家生活 (房租还贷/水电煤/物业费/生活日用)、出行交通 (打车/公交地铁/加油/停车费/火车/飞机)、休闲娱乐 (游戏/电影/旅游/运动健身)、健康医疗 (买药/医院/保健)、文化教育 (学费/书报/培训)、送礼人情 (打赏/红包/礼物/请客)、其他")
        sb.appendLine("- 收入大类：收入 (工资/奖金/报销/补贴/兼职外快/礼金人情/理财盈利/中奖/其他)")

        if (recentBills.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("### 最近记账明细（带账单ID）：")
            recentBills.take(25).forEach { b ->
                val typeStr = if (b.isTransfer) "内部转账" else if (b.isExpense) "支出" else "收入"
                val remarkStr = if (!b.remark.isNullOrBlank()) " (${b.remark})" else ""
                sb.appendLine("- [ID:${b.id}] [${b.time}] $typeStr ¥%.2f | 分类: ${b.parentCategoryName}·${b.childCategoryName} | 账户: ${b.assetName}$remarkStr".format(b.cost))
            }
        }

        sb.appendLine()
        sb.appendLine("### 你的智能行动协议（非常重要）：")
        sb.appendLine("1. 【新增记账】：当用户说要增加一笔消费或收入（例如：“增加一笔5元的晚饭支出”、“记一笔支付宝15块打车”），你除了解释外，必须在回复末尾附带以下格式的代码块：")
        sb.appendLine("```action:add_bill")
        sb.appendLine("{\"cost\": 5.0, \"parentCategoryName\": \"食品餐饮\", \"childCategoryName\": \"请客吃饭\", \"assetName\": \"支付宝\", \"remark\": \"晚饭\"}")
        sb.appendLine("```")
        sb.appendLine("app 客户端会自动捕获此指令并立即为用户生成一键写入数据库入库卡片，直连写入账本！")
        sb.appendLine()
        sb.appendLine("2. 【重新分类】：当用户要求修改某笔账单分类（例如：“把刚才30块的红包改成餐饮”、“把第一笔改成居家生活”），你根据上方账单列表找到对应的账单 [ID:xxx]，并在回复末尾附带：")
        sb.appendLine("```action:update_bill")
        sb.appendLine("{\"billId\": 账单ID数字, \"parentCategoryName\": \"目标一级分类\", \"childCategoryName\": \"目标二级分类\"}")
        sb.appendLine("```")
        sb.appendLine("app 客户端会自动捕获并直接更新数据库中的该账单分类！")
        sb.appendLine()
        sb.appendLine("3. 回复请保持条理清晰、排版优雅（使用 Markdown 表格、粗体重点），不要让用户自己去其他软件手动记账，你就是能直接帮他记账并改分类的智能助手！")

        return sb.toString()
    }
}
