package com.yimu.ai

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.yimu.ai.ai.AiChatClient
import com.yimu.ai.ai.PromptEngine
import com.yimu.ai.crypto.BackupDecryptor
import com.yimu.ai.data.BillItem
import com.yimu.ai.data.ChatMessage
import com.yimu.ai.data.SpendingSummary
import com.yimu.ai.data.YimuDbReader
import com.yimu.ai.ui.screens.ChatScreen
import com.yimu.ai.ui.screens.HomeScreen
import com.yimu.ai.ui.screens.SettingsScreen
import com.yimu.ai.ui.theme.BrandPrimary
import com.yimu.ai.ui.theme.YimuAiTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {

    private val prefs by lazy { getSharedPreferences("yimu_ai_prefs", Context.MODE_PRIVATE) }
    private var dbReader: YimuDbReader? = null
    private val chatClient by lazy {
        AiChatClient(
            apiKey = prefs.getString("api_key", "") ?: "",
            baseUrl = prefs.getString("api_url", "https://api.deepseek.com/chat/completions") ?: ""
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestStoragePermissions()

        setContent {
            YimuAiTheme {
                MainApp()
            }
        }
    }

    private fun checkAndRequestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        } else {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val needed = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, needed.toTypedArray(), 100)
            }
        }
    }

    @Composable
    private fun MainApp() {
        var selectedTab by remember { mutableIntStateOf(0) }

        var userId by remember { mutableStateOf(prefs.getString("user_id", "") ?: "") }
        var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }
        var apiUrl by remember { mutableStateOf(prefs.getString("api_url", "https://api.deepseek.com/chat/completions") ?: "") }

        var summary by remember { mutableStateOf<SpendingSummary?>(null) }
        var recentBills by remember { mutableStateOf<List<BillItem>>(emptyList()) }
        val chatMessages = remember { mutableStateListOf<ChatMessage>() }

        var isDecrypting by remember { mutableStateOf(false) }
        var isAiThinking by remember { mutableStateOf(false) }
        var statusMessage by remember { mutableStateOf<String?>(null) }
        var latestBackupName by remember { mutableStateOf<String?>(null) }

        // 尝试自动解密并读取最新备份
        fun doDecryptAndLoad() {
            if (userId.isBlank()) {
                statusMessage = "请先配置一木记账的用户数字ID。"
                return
            }

            val latestZip = BackupDecryptor.findLatestBackupFile()
            if (latestZip == null) {
                statusMessage = "未在一木记账备份目录 (/sdcard/Documents/一木记账) 下检测到 .zip 备份包！"
                latestBackupName = null
                return
            }

            latestBackupName = "${latestZip.name} (${latestZip.length() / 1024} KB)"
            isDecrypting = true
            statusMessage = "正在解密 ${latestZip.name}..."

            lifecycleScope.launch(Dispatchers.IO) {
                val destDir = File(filesDir, "extracted_db")
                val decryptResult = BackupDecryptor.decryptCustomDb(latestZip, destDir, userId)

                withContext(Dispatchers.Main) {
                    isDecrypting = false
                    decryptResult.onSuccess { extractedDb ->
                        statusMessage = "解密成功！已就绪 Custom.db"
                        dbReader = YimuDbReader(extractedDb)
                        // 加载数据
                        val sum = dbReader!!.getMonthlySummary()
                        val bills = dbReader!!.getBills(limit = 30)
                        summary = sum
                        recentBills = bills

                        if (chatMessages.isEmpty()) {
                            chatMessages.add(
                                ChatMessage(
                                    text = "你好！我已经成功连接并解析了你的一木账本（共 ${sum.billCount} 笔记录，总支出 ¥%.2f）。你可以向我咨询任何消费结构、异常开销或财务优化建议！".format(sum.totalExpense),
                                    isUser = false
                                )
                            )
                        }
                    }.onFailure { err ->
                        statusMessage = "解密失败: ${err.message}"
                        Toast.makeText(this@MainActivity, statusMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // 初始化加载
        LaunchedEffect(Unit) {
            val latest = BackupDecryptor.findLatestBackupFile()
            if (latest != null) {
                latestBackupName = latest.name
            }
            if (userId.isNotBlank()) {
                doDecryptAndLoad()
            }
        }

        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "概览") },
                        label = { Text("概览") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = BrandPrimary, indicatorColor = BrandPrimary.copy(alpha = 0.1f))
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI顾问") },
                        label = { Text("AI顾问") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = BrandPrimary, indicatorColor = BrandPrimary.copy(alpha = 0.1f))
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "设置") },
                        label = { Text("设置") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = BrandPrimary, indicatorColor = BrandPrimary.copy(alpha = 0.1f))
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        summary = summary,
                        recentBills = recentBills,
                        isDecrypting = isDecrypting,
                        onRefresh = { doDecryptAndLoad() },
                        onNavigateToChat = { selectedTab = 1 },
                        onNavigateToSettings = { selectedTab = 2 }
                    )
                    1 -> ChatScreen(
                        messages = chatMessages,
                        isLoading = isAiThinking,
                        onSendMessage = { query ->
                            chatMessages.add(ChatMessage(text = query, isUser = true))
                            isAiThinking = true

                            lifecycleScope.launch {
                                val systemPrompt = PromptEngine.buildSystemPrompt(summary, recentBills)
                                val history = chatMessages.map { it.isUser to it.text }
                                val result = chatClient.sendMessage(systemPrompt, history)

                                isAiThinking = false
                                result.onSuccess { reply ->
                                    chatMessages.add(ChatMessage(text = reply, isUser = false))
                                }.onFailure { error ->
                                    chatMessages.add(
                                        ChatMessage(
                                            text = "请求失败: ${error.message}",
                                            isUser = false
                                        )
                                    )
                                }
                            }
                        }
                    )
                    2 -> SettingsScreen(
                        userId = userId,
                        apiKey = apiKey,
                        apiUrl = apiUrl,
                        latestBackupFileName = latestBackupName,
                        statusMessage = statusMessage,
                        isProcessing = isDecrypting,
                        onSaveSettings = { newUid, newKey, newUrl ->
                            userId = newUid
                            apiKey = newKey
                            apiUrl = newUrl
                            prefs.edit()
                                .putString("user_id", newUid)
                                .putString("api_key", newKey)
                                .putString("api_url", newUrl)
                                .apply()

                            chatClient.apiKey = newKey
                            chatClient.baseUrl = newUrl
                            Toast.makeText(this@MainActivity, "配置已保存", Toast.LENGTH_SHORT).show()
                        },
                        onTriggerDecrypt = {
                            doDecryptAndLoad()
                        }
                    )
                }
            }
        }
    }
}
