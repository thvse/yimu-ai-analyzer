package com.yimu.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    var selectedCategoryTab by remember { mutableIntStateOf(0) } // 0: 支出分类, 1: 收入分类
    var showAllCategories by remember { mutableStateOf(false) }
    var selectedBillFilter by remember { mutableIntStateOf(0) } // 0: 全部, 1: 支出, 2: 收入, 3: 转账

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BrandPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Analytics,
                                contentDescription = null,
                                tint = BrandPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "一木账本透视全景",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                            Text(
                                "多维分类透视 · 资产负债看板",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onPickBackupFile, enabled = !isDecrypting) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "手动选择备份包", tint = BrandPrimary)
                    }
                    IconButton(onClick = onRefresh, enabled = !isDecrypting) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
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
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(BrandPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = BrandPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "暂未加载一木记账数据",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "支持自动检测一木记账的备份包，或手动选取手机中的 Custom.db / zip 备份包完成毫秒级本地解密与透视。",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
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
                        modifier = Modifier.fillMaxWidth().animateContentSize(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "已挂载: ${summary.backupFileName.ifBlank { "Custom.db" }}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        maxLines = 1
                                    )
                                }
                                if (summary.backupFileModified.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "备份时间: ${summary.backupFileModified}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            TextButton(
                                onClick = onPickBackupFile,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("换包", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // 1. 资产与净资产总览卡片 (黑金商务卡)
                summary.assetSummary?.let { assets ->
                    item {
                        NetWorthCard(assets = assets, onNavigateToChat = onNavigateToChat)
                    }

                    if (assets.accounts.isNotEmpty()) {
                        item {
                            AccountAssetsList(assets = assets)
                        }
                    }
                }

                // 2. 日常收支多维仪表盘
                item {
                    SpendingOverviewCard(summary = summary)
                }

                // 3. 分类全景深度透视中心 (支出 vs 收入)
                item {
                    CategoryAnalyticsSection(
                        summary = summary,
                        selectedTab = selectedCategoryTab,
                        onTabChanged = { selectedCategoryTab = it },
                        showAll = showAllCategories,
                        onToggleShowAll = { showAllCategories = !showAllCategories }
                    )
                }

                // 4. 近期账单流水记录 (可切换 全部/支出/收入/转账)
                item {
                    BillsTimelineSection(
                        recentBills = recentBills,
                        selectedFilter = selectedBillFilter,
                        onFilterChanged = { selectedBillFilter = it },
                        onNavigateToChat = onNavigateToChat
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

/**
 * 资产总览黑金卡片
 */
@Composable
private fun NetWorthCard(
    assets: AssetSummary,
    onNavigateToChat: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "个人净资产 (净值)",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Button(
                    onClick = onNavigateToChat,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI深度诊断", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "¥%.2f".format(assets.netAssets),
                color = if (assets.netAssets >= 0) Color.White else Color(0xFFF87171),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF334155), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("总资产 (资金/理财)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "¥%.2f".format(assets.totalAssets),
                        color = Color(0xFF4ADE80),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
                Column {
                    Text("总负债 (信贷/借款)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "¥%.2f".format(assets.totalLiabilities),
                        color = Color(0xFFF87171),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
                Column {
                    Text("账户数", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${assets.accounts.size} 个",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * 账户分布折叠卡片
 */
@Composable
private fun AccountAssetsList(assets: AssetSummary) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "账户资产分布 (${assets.accounts.size}个账户)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(if (expanded) "收起" else "查看全部", fontSize = 12.sp, color = BrandPrimary)
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val displayAccounts = if (expanded) assets.accounts else assets.accounts.take(3)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                displayAccounts.forEach { account ->
                    AccountBalanceRow(account)
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (account.isDebt) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (account.isDebt) Icons.Default.CreditCard else Icons.Default.Savings,
                    contentDescription = null,
                    tint = if (account.isDebt) Color(0xFFDC2626) else Color(0xFF16A34A),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = account.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                if (account.groupName.isNotBlank()) {
                    Text(
                        text = account.groupName,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Text(
            text = "¥%.2f".format(account.balance),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (account.isDebt) Color(0xFFDC2626) else Color(0xFF16A34A)
        )
    }
}

/**
 * 核心收支概览卡片
 */
@Composable
private fun SpendingOverviewCard(summary: SpendingSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                "收支核心数据",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("总支出", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "¥%.2f".format(summary.totalExpense),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Column {
                    Text("总收入", color = TextSecondary, fontSize = 12.sp)
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
                    Text("记账笔数", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${summary.billCount} 笔",
                        color = BrandPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * 分类全景透视卡片 (支持支出与收入切换、进度条与详细排行榜)
 */
@Composable
private fun CategoryAnalyticsSection(
    summary: SpendingSummary,
    selectedTab: Int,
    onTabChanged: (Int) -> Unit,
    showAll: Boolean,
    onToggleShowAll: () -> Unit
) {
    val activeRankings = if (selectedTab == 0) summary.categoryRanking else summary.incomeRanking
    val totalAmount = if (selectedTab == 0) summary.totalExpense else summary.totalIncome

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // 顶栏 Tab 切换 (支出分类 vs 收入分类)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PieChart,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "分类全景透视",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // 支出/收入切换按钮组
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        TabPill(
                            label = "支出 (${summary.categoryRanking.size})",
                            isSelected = selectedTab == 0,
                            onClick = { onTabChanged(0) }
                        )
                        TabPill(
                            label = "收入 (${summary.incomeRanking.size})",
                            isSelected = selectedTab == 1,
                            onClick = { onTabChanged(1) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (activeRankings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (selectedTab == 0) "暂无支出分类数据" else "暂无收入分类数据",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            } else {
                // 1. 多色块比例进度条
                MultiColorProportionBar(rankings = activeRankings.take(5))

                Spacer(modifier = Modifier.height(16.dp))

                // 2. 分类排行榜明细列表
                val displayList = if (showAll) activeRankings else activeRankings.take(4)
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    displayList.forEach { cat ->
                        CategoryDetailRow(cat = cat, isIncomeTab = selectedTab == 1)
                    }
                }

                // 展开全部/收起按钮
                if (activeRankings.size > 4) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TextButton(onClick = onToggleShowAll) {
                            Text(
                                if (showAll) "收起部分分类" else "展开全部分类 (共 ${activeRankings.size} 个)",
                                fontSize = 13.sp,
                                color = BrandPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                if (showAll) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = BrandPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) BrandPrimary else Color.Transparent,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else TextSecondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/**
 * 漂亮的彩色分类占比条
 */
@Composable
private fun MultiColorProportionBar(rankings: List<CategoryExpense>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFFE2E8F0))
    ) {
        rankings.forEach { cat ->
            if (cat.percentage > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(cat.percentage.coerceAtLeast(0.02f))
                        .background(getCategoryColor(cat.categoryName))
                )
            }
        }
    }
}

/**
 * 单个分类卡片行（带图标、动效进度条、金额及占比）
 */
@Composable
private fun CategoryDetailRow(cat: CategoryExpense, isIncomeTab: Boolean) {
    val animProgress by animateFloatAsState(
        targetValue = cat.percentage,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "cat_progress"
    )
    val catColor = getCategoryColor(cat.categoryName)
    val catIcon = getCategoryIcon(cat.categoryName)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(catColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = catIcon,
                        contentDescription = null,
                        tint = catColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        cat.categoryName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        "${cat.count} 笔记账",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "¥%.2f".format(cat.amount),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncomeTab) IncomeGreen else TextPrimary
                )
                Text(
                    "%.1f%%".format(cat.percentage * 100),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { animProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = catColor,
            trackColor = Color(0xFFF1F5F9)
        )
    }
}

/**
 * 近期流水记录区
 */
@Composable
private fun BillsTimelineSection(
    recentBills: List<BillItem>,
    selectedFilter: Int,
    onFilterChanged: (Int) -> Unit,
    onNavigateToChat: () -> Unit
) {
    val filteredBills = remember(recentBills, selectedFilter) {
        when (selectedFilter) {
            1 -> recentBills.filter { it.isExpense }
            2 -> recentBills.filter { it.isIncome }
            3 -> recentBills.filter { it.isTransfer }
            else -> recentBills
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "近期账单记录",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // 筛选胶囊
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("全部", "支出", "收入", "转账").forEachIndexed { idx, label ->
                        FilterChip(
                            selected = selectedFilter == idx,
                            onClick = { onFilterChanged(idx) },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = BrandPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredBills.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无匹配流水明细", fontSize = 13.sp, color = TextSecondary)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filteredBills.take(25).forEach { bill ->
                        BillTimelineRow(bill)
                    }
                }
            }
        }
    }
}

@Composable
private fun BillTimelineRow(bill: BillItem) {
    val catColor = getCategoryColor(bill.parentCategoryName)
    val catIcon = getCategoryIcon(bill.parentCategoryName)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(catColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (bill.isTransfer) Icons.Default.SwapHoriz else catIcon,
                        contentDescription = null,
                        tint = catColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (bill.isTransfer) "内部转账" else "${bill.parentCategoryName} · ${bill.childCategoryName}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFE2E8F0)
                        ) {
                            Text(
                                text = bill.recordMethodName,
                                fontSize = 9.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = bill.time,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = " | ${bill.assetName}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        if (!bill.remark.isNullOrBlank()) {
                            Text(
                                text = " · ${bill.remark}",
                                fontSize = 11.sp,
                                color = BrandPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            val prefix = if (bill.isExpense) "-" else if (bill.isIncome) "+" else ""
            val color = if (bill.isExpense) TextPrimary else if (bill.isIncome) IncomeGreen else Color(0xFF2563EB)
            Text(
                text = prefix + "¥%.2f".format(bill.cost),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * 分类专属色盘
 */
fun getCategoryColor(name: String): Color {
    return when {
        name.contains("餐饮") || name.contains("食品") || name.contains("外卖") || name.contains("吃") -> Color(0xFFF97316)
        name.contains("交通") || name.contains("出行") || name.contains("打车") || name.contains("地铁") -> Color(0xFF3B82F6)
        name.contains("购物") || name.contains("消费") || name.contains("服饰") || name.contains("买") -> Color(0xFFEC4899)
        name.contains("居家") || name.contains("生活") || name.contains("日用") || name.contains("房租") -> Color(0xFF10B981)
        name.contains("娱乐") || name.contains("休闲") || name.contains("游戏") || name.contains("电影") -> Color(0xFF6366F1)
        name.contains("医疗") || name.contains("健康") || name.contains("药") -> Color(0xFFF43F5E)
        name.contains("人情") || name.contains("送礼") || name.contains("礼金") -> Color(0xFFF59E0B)
        name.contains("教育") || name.contains("文化") || name.contains("书") -> Color(0xFF0D9488)
        name.contains("收入") || name.contains("工资") || name.contains("奖金") || name.contains("理财") -> Color(0xFF059669)
        name.contains("转账") -> Color(0xFF06B6D4)
        else -> Color(0xFF64748B)
    }
}

/**
 * 分类专属图标
 */
fun getCategoryIcon(name: String): ImageVector {
    return when {
        name.contains("餐饮") || name.contains("食品") || name.contains("外卖") || name.contains("吃") -> Icons.Default.Restaurant
        name.contains("交通") || name.contains("出行") || name.contains("打车") || name.contains("地铁") -> Icons.Default.DirectionsCar
        name.contains("购物") || name.contains("消费") || name.contains("服饰") || name.contains("买") -> Icons.Default.ShoppingBag
        name.contains("居家") || name.contains("生活") || name.contains("日用") || name.contains("房租") -> Icons.Default.Home
        name.contains("娱乐") || name.contains("休闲") || name.contains("游戏") || name.contains("电影") -> Icons.Default.SportsEsports
        name.contains("医疗") || name.contains("健康") || name.contains("药") -> Icons.Default.LocalHospital
        name.contains("人情") || name.contains("送礼") || name.contains("礼金") -> Icons.Default.CardGiftcard
        name.contains("教育") || name.contains("文化") || name.contains("书") -> Icons.Default.School
        name.contains("收入") || name.contains("工资") || name.contains("奖金") || name.contains("理财") -> Icons.Default.Paid
        name.contains("转账") -> Icons.Default.SwapHoriz
        else -> Icons.Default.Category
    }
}
