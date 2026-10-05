package com.yimu.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yimu.ai.data.AssetItem
import com.yimu.ai.data.AssetSummary
import com.yimu.ai.data.BillItem
import com.yimu.ai.data.CategoryExpense
import com.yimu.ai.data.SpendingSummary
import com.yimu.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    summary: SpendingSummary?,
    recentBills: List<BillItem>,
    isDecrypting: Boolean,
    onRefresh: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "一木账本透视",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !isDecrypting) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        if (summary == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "暂未加载一木记账数据",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "请先前往【设置】页面输入你的一木记账用户ID，并确认已在手机上进行了本地备份。",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onNavigateToSettings,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                        ) {
                            Text("前往配置用户ID")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. 资产与净资产总览卡片（首要展示）
                summary.assetSummary?.let { assets ->
                    item {
                        NetWorthCard(assets = assets, onNavigateToChat = onNavigateToChat)
                    }

                    if (assets.accounts.isNotEmpty()) {
                        item {
                            Text(
                                "账户资产分布 (${assets.accounts.size}个账户)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    assets.accounts.forEach { account ->
                                        AccountBalanceRow(account)
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. 收支流水看板
                item {
                    Text(
                        "日常收支概览",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                item {
                    SpendingOverviewCard(summary = summary)
                }

                // 3. 当暂无流水时的温馨提示引导
                if (summary.billCount == 0) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "当前备份包暂无收支流水",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "当前解密成功识别了 7 个资产账户。如果您在一木记账中其他账本记过流水，请在一木记账中确认当前账本并重新导出备份；您也可以直接点击底部【AI顾问】向小米大模型咨询资产负债结构或发送小票进行智能识别记账！",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. 分类支出排行榜
                if (summary.categoryRanking.isNotEmpty()) {
                    item {
                        Text(
                            "支出分类排行榜",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                summary.categoryRanking.take(6).forEach { cat ->
                                    CategoryRow(cat)
                                }
                            }
                        }
                    }
                }

                // 5. 近期账单流水
                if (recentBills.isNotEmpty()) {
                    item {
                        Text(
                            "近期账单记录 (${recentBills.size}笔)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    items(recentBills.take(20)) { bill ->
                        BillRow(bill)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun NetWorthCard(
    assets: AssetSummary,
    onNavigateToChat: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "个人净资产 (净值)",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp
                    )
                }
                Button(
                    onClick = onNavigateToChat,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI财务诊断", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "¥%.2f".format(assets.netAssets),
                color = if (assets.netAssets >= 0) Color.White else Color(0xFFF87171),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF334155))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("总资产 (资金/投资)", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "¥%.2f".format(assets.totalAssets),
                        color = Color(0xFF4ADE80),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
                Column {
                    Text("总负债 (信贷/借款)", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "¥%.2f".format(assets.totalLiabilities),
                        color = Color(0xFFF87171),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
                Column {
                    Text("账户总数", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${assets.accounts.size} 个",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountBalanceRow(account: AssetItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = account.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            if (account.groupName.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = account.groupName,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        Text(
            text = "¥%.2f".format(account.balance),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (account.isDebt) Color(0xFFDC2626) else Color(0xFF16A34A)
        )
    }
}

@Composable
private fun SpendingOverviewCard(summary: SpendingSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("本期总支出", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "¥%.2f".format(summary.totalExpense),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Column {
                Text("本期总收入", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "¥%.2f".format(summary.totalIncome),
                    color = IncomeGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Column {
                Text("收支结余", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "¥%.2f".format(summary.balance),
                    color = if (summary.balance >= 0) TextPrimary else ExpenseRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(cat: CategoryExpense) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(cat.categoryName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text(
                "¥%.2f (%.1f%%)".format(cat.amount, cat.percentage * 100),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { cat.percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = BrandPrimary,
            trackColor = Color(0xFFE2E8F0)
        )
    }
}

@Composable
private fun BillRow(bill: BillItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${bill.parentCategoryName} · ${bill.childCategoryName}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = bill.time,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    if (!bill.remark.isNullOrBlank()) {
                        Text(
                            text = " | ${bill.remark}",
                            fontSize = 12.sp,
                            color = BrandPrimary,
                            maxLines = 1
                        )
                    }
                }
            }

            Text(
                text = (if (bill.isExpense) "-" else "+") + "¥%.2f".format(bill.cost),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (bill.isExpense) TextPrimary else IncomeGreen
            )
        }
    }
}
