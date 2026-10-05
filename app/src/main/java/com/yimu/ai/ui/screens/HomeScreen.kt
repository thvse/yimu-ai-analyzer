package com.yimu.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FolderOpen
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
    onPickBackupFile: () -> Unit,
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
                    IconButton(onClick = onPickBackupFile, enabled = !isDecrypting) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "手动选择备份包", tint = BrandPrimary)
                    }
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
                            "你可以直接手动选择手机上的备份 .zip 文件，或在设置中输入密码后自动扫描同步。",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = onPickBackupFile,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("选择备份文件")
                            }
                            Button(
                                onClick = onNavigateToSettings,
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("配置用户ID")
                            }
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
                // 当前所读取的备份文件状态栏
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "当前备份: ${summary.backupFileName.ifBlank { "已加载数据库" }}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                if (summary.backupFileModified.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "修改时间: ${summary.backupFileModified}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            TextButton(
                                onClick = onPickBackupFile,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("切换文件", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // 1. 资产与净资产总览卡片
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
                                        "当前备份包暂未包含收支流水 (${summary.billCount} 笔)",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "如果您刚刚在一木记账中导出了新备份，但这里仍显示 0，通常是因为该备份包保存在了其他目录（如 Download 下载目录），导致读取了旧备份。请点击右上角【📂 文件夹图标】手动选中您刚刚导出的新 zip 备份包即可！",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = onPickBackupFile,
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("立即手动挑选新备份文件", fontSize = 12.sp)
                                    }
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
            Column {
                Text("流水笔数", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${summary.billCount} 笔",
                    color = TextPrimary,
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

            val prefix = if (bill.isExpense) "-" else if (bill.isIncome) "+" else ""
            val color = if (bill.isExpense) TextPrimary else if (bill.isIncome) IncomeGreen else Color(0xFF2563EB)
            Text(
                text = prefix + "¥%.2f".format(bill.cost),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
