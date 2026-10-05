package com.yimu.ai.data

data class BillItem(
    val id: Long,
    val cost: Double,
    val billType: Int, // 0: 支出, 1: 收入, 2: 转账
    val time: String, // 记账时间字符串，例如 "2024-05-01 12:30:00"
    val remark: String?,
    val parentCategoryId: Long,
    val parentCategoryName: String,
    val childCategoryId: Long,
    val childCategoryName: String,
    val assetId: Long,
    val assetName: String
) {
    val isExpense: Boolean get() = billType == 0
    val isIncome: Boolean get() = billType == 1
}

data class CategoryItem(
    val id: Long,
    val name: String,
    val type: Int, // 0: 支出, 1: 收入
    val parentId: Long = 0
)

data class AssetItem(
    val id: Long,
    val name: String,
    val balance: Double,
    val groupName: String = "",
    val assetType: Int = 1 // 1: 资金, 2: 负债/信用, 4: 投资
) {
    val isDebt: Boolean get() = balance < 0 || assetType == 2
}

data class AssetSummary(
    val totalAssets: Double,
    val totalLiabilities: Double,
    val netAssets: Double,
    val accounts: List<AssetItem>
)

data class SpendingSummary(
    val totalExpense: Double,
    val totalIncome: Double,
    val balance: Double,
    val billCount: Int,
    val categoryRanking: List<CategoryExpense>,
    val assetSummary: AssetSummary? = null,
    val rawBillCount: Int = 0,
    val backupFileName: String = "",
    val backupFileModified: String = ""
)

data class CategoryExpense(
    val categoryName: String,
    val amount: Double,
    val percentage: Float
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val imageUri: String? = null, // 本地图片 URI
    val imageBase64: String? = null, // Base64 编码，用于传输给多模态大模型
    val timestamp: Long = System.currentTimeMillis()
)
