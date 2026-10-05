package com.yimu.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.yimu.ai.data.*
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
    onNavigateToSettings: () -> Unit,
    onUpdateBillCategory: (billId: Long, parentCategoryId: Long, childCategoryId: Long) -> Unit = { _, _, _ -> }
) {
    var selectedCategoryTab by remember { mutableIntStateOf(0) } // 0: 支出透视, 1: 收入透视, 2: 一木全部分类(10大类/66子类)
    var selectedBillFilter by remember { mutableIntStateOf(0) } // 0: 全部, 1: 支出, 2: 收入, 3: 转账

    // 重新分类弹窗状态
    var billToReclassify by remember { mutableStateOf<BillItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                                "多维分类透视 · 智能记账交互",
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
                            "支持自动检测一木记账的备份包，或手动挑选手机中的 Custom.db / zip 备份包完成本地秒级解密与透视。",
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
                // 1. 当前所读取的备份文件状态栏
                item(key = "backup_status") {
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
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("换包", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // 2. 核心收支四宫格质感卡片
                item(key = "metric_cards") {
                    LedgerMetricsGrid(summary = summary)
                }

                // 3. 资产与净资产总览卡片 (黑金商务卡)
                summary.assetSummary?.let { assets ->
                    item(key = "net_worth_card") {
                        NetWorthCard(assets = assets, onNavigateToChat = onNavigateToChat)
                    }

                    if (assets.accounts.isNotEmpty()) {
                        item(key = "asset_accounts_card") {
                            AccountAssetsList(assets = assets)
                        }
                    }
                }

                // 4. 分类全景深度透视中心 (支出 vs 收入 vs 全部分类 10大类/66子类)
                item(key = "category_section") {
                    CategoryAnalyticsSection(
                        summary = summary,
                        selectedTab = selectedCategoryTab,
                        onTabChanged = { selectedCategoryTab = it }
                    )
                }

                // 5. 近期账单流水记录 (可切换 全部/支出/收入/转账，支持一键改分类)
                item(key = "bills_section") {
                    BillsTimelineSection(
                        recentBills = recentBills,
                        selectedFilter = selectedBillFilter,
                        onFilterChanged = { selectedBillFilter = it },
                        onReclassifyBill = { bill -> billToReclassify = bill }
                    )
                }

                item(key = "bottom_space") {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // 重新分类底部弹窗
    if (billToReclassify != null && summary != null) {
        ModalBottomSheet(
            onDismissRequest = { billToReclassify = null },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            CategoryPickerBottomSheetContent(
                bill = billToReclassify!!,
                allCategories = summary.allCategories,
                onSelectCategory = { parentId, childId ->
                    onUpdateBillCategory(billToReclassify!!.id, parentId, childId)
                    billToReclassify = null
                },
                onDismiss = { billToReclassify = null }
            )
        }
    }
}

/**
 * 现代金融四宫格收支指标卡片
 */
@Composable
private fun LedgerMetricsGrid(summary: SpendingSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 总支出卡片
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFEE2E2))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("本期总支出", fontSize = 12.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "¥%.2f".format(summary.totalExpense),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF991B1B)
                    )
                }
            }

            // 总收入卡片
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDCFCE7))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("本期总收入", fontSize = 12.sp, color = Color(0xFF166534), fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "¥%.2f".format(summary.totalIncome),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF166534)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 收支结余卡片
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("收支结余", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "¥%.2f".format(summary.balance),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.balance >= 0) Color(0xFF0F172A) else Color(0xFFDC2626)
                    )
                }
            }

            // 流水笔数卡片
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("累计记账笔数", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${summary.billCount} 笔",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary
                    )
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
                    Text("AI智能记账", fontSize = 12.sp)
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
 * 分类全景透视卡片 (支持支出、收入、以及一木记账 10 大分类 66 子分类全景矩阵)
 */
@Composable
private fun CategoryAnalyticsSection(
    summary: SpendingSummary,
    selectedTab: Int,
    onTabChanged: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // 顶栏 Tab 切换 (支出透视 / 收入透视 / 一木全部分类)
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
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 三维 Segmented Control
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F5F9)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
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
                    TabPill(
                        label = "全部系统分类 (${summary.allCategories.size}大类)",
                        isSelected = selectedTab == 2,
                        onClick = { onTabChanged(2) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedTab) {
                0 -> {
                    // 支出透视
                    if (summary.categoryRanking.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("暂无支出分类数据", fontSize = 13.sp, color = TextSecondary)
                        }
                    } else {
                        MultiColorProportionBar(rankings = summary.categoryRanking.take(5))
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            summary.categoryRanking.forEach { cat ->
                                CategoryDetailRow(cat = cat, isIncomeTab = false)
                            }
                        }
                    }
                }
                1 -> {
                    // 收入透视
                    if (summary.incomeRanking.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("暂无收入分类数据", fontSize = 13.sp, color = TextSecondary)
                        }
                    } else {
                        MultiColorProportionBar(rankings = summary.incomeRanking.take(5))
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            summary.incomeRanking.forEach { cat ->
                                CategoryDetailRow(cat = cat, isIncomeTab = true)
                            }
                        }
                    }
                }
                2 -> {
                    // 一木记账所有分类展示矩阵 (10 大类 / 66 子类)
                    AllCategoriesExplorer(categories = summary.allCategories)
                }
            }
        }
    }
}

@Composable
private fun TabPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) BrandPrimary else Color.Transparent,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else TextSecondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

/**
 * 完整分类知识库与全景矩阵视图 (展现所有 10 大类及 66 个子分类)
 */
@Composable
private fun AllCategoriesExplorer(categories: List<FullCategory>) {
    var expandedCatId by remember { mutableStateOf<Long?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "一木记账预设分类字典 (共 ${categories.size} 大类，包含完整子分类树)",
            fontSize = 12.sp,
            color = TextSecondary
        )

        categories.forEach { cat ->
            val isExpanded = expandedCatId == cat.id
            val catColor = getCategoryColor(cat.name)
            val catIcon = getCategoryIcon(cat.name)

            Card(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isExpanded) Color(0xFFF8FAFC) else Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isExpanded) catColor.copy(alpha = 0.5f) else Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedCatId = if (isExpanded) null else cat.id },
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
                                Icon(catIcon, contentDescription = null, tint = catColor, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(cat.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("${cat.children.size} 个二级子分类", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (cat.spentAmount > 0) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "¥%.2f".format(cat.spentAmount),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (cat.isIncome) IncomeGreen else TextPrimary
                                    )
                                    Text("${cat.billCount} 笔", fontSize = 10.sp, color = TextSecondary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Text("暂无支出", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Icon(
                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // 展开展示所有子分类
                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("子分类明细：", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(6.dp))

                        // 流式展示所有二级分类胶囊
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            cat.children.chunked(3).forEach { columnItems ->
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    columnItems.forEach { child ->
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (child.spentAmount > 0) catColor.copy(alpha = 0.12f) else Color(0xFFF1F5F9)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(child.name, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                                if (child.spentAmount > 0) {
                                                    Text("¥%.0f".format(child.spentAmount), fontSize = 10.sp, color = catColor, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
 * 近期流水记录区 (支持一键改分类)
 */
@Composable
private fun BillsTimelineSection(
    recentBills: List<BillItem>,
    selectedFilter: Int,
    onFilterChanged: (Int) -> Unit,
    onReclassifyBill: (BillItem) -> Unit
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
                    filteredBills.take(30).forEach { bill ->
                        BillTimelineRow(bill = bill, onReclassify = { onReclassifyBill(bill) })
                    }
                }
            }
        }
    }
}

@Composable
private fun BillTimelineRow(bill: BillItem, onReclassify: () -> Unit) {
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
                        // 可点击的分类标签 (点击直接改分类)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = catColor.copy(alpha = 0.1f),
                            modifier = Modifier.clickable { onReclassify() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (bill.isTransfer) "内部转账" else "${bill.parentCategoryName} · ${bill.childCategoryName}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = catColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Edit, contentDescription = "修改分类", tint = catColor, modifier = Modifier.size(10.dp))
                            }
                        }

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

                    Spacer(modifier = Modifier.height(3.dp))
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
 * 重新分类 BottomSheet 内容
 */
@Composable
private fun CategoryPickerBottomSheetContent(
    bill: BillItem,
    allCategories: List<FullCategory>,
    onSelectCategory: (parentCategoryId: Long, childCategoryId: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedParentId by remember { mutableStateOf(bill.parentCategoryId.takeIf { it != 0L } ?: (allCategories.firstOrNull()?.id ?: 1L)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("修改账单分类", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "账单金额: ¥%.2f | 当前: %s".format(bill.cost, "${bill.parentCategoryName}·${bill.childCategoryName}"),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "关闭", tint = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text("选择一级分类：", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))

        // 一级分类水平滚动选择
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allCategories.forEach { pCat ->
                val isSelected = selectedParentId == pCat.id
                val catColor = getCategoryColor(pCat.name)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) catColor else Color(0xFFF1F5F9),
                    modifier = Modifier.clickable { selectedParentId = pCat.id }
                ) {
                    Text(
                        text = pCat.name,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else TextPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("选择二级子分类并保存：", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))

        val currentParent = allCategories.find { it.id == selectedParentId }
        val children = currentParent?.children ?: emptyList()

        if (children.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                Text("该大类下暂无子分类，点击确认直接保存", fontSize = 12.sp, color = TextSecondary)
            }
            Button(
                onClick = { onSelectCategory(selectedParentId, 0L) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("保存为「${currentParent?.name ?: ""}」")
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                children.chunked(3).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { child ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onSelectCategory(selectedParentId, child.id)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = child.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                        if (rowItems.size < 3) {
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
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
