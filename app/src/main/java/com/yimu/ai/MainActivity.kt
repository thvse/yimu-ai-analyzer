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
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import com.yimu.ai.data.AiActionData
import com.yimu.ai.data.BillItem
import com.yimu.ai.data.ChatMessage
import com.yimu.ai.data.SpendingSummary
import com.yimu.ai.data.YimuDbReader
import com.yimu.ai.ui.screens.ChatScreen
import com.yimu.ai.ui.screens.HomeScreen
import com.yimu.ai.ui.screens.SettingsScreen
import com.yimu.ai.ui.theme.BrandPrimary
import com.yimu.ai.ui.theme.YimuAiTheme
import com.yimu.ai.utils.ActionParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val prefs by lazy { getSharedPreferences("yimu_ai_prefs", Context.MODE_PRIVATE) }
    private var dbReader: YimuDbReader? = null

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val chatClient by lazy {
        AiChatClient(
            apiKey = prefs.getString("api_key", "") ?: "",
            baseUrl = prefs.getString("api_url", "https://api.xiaomimimo.com/v1/chat/completions") ?: "https://api.xiaomimimo.com/v1/chat/completions",
            model = prefs.getString("model_name", "mimo-v2.6-flash") ?: "mimo-v2.6-flash"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestStoragePermissions()
        initTextToSpeech()

        setContent {
            YimuAiTheme {
                MainApp()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initTextToSpeech() {
        try {
            tts = TextToSpeech(applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale.CHINESE)
                    if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                        isTtsReady = true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
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
        var apiUrl by remember {
            mutableStateOf(prefs.getString("api_url", "https://api.xiaomimimo.com/v1/chat/completions") ?: "https://api.xiaomimimo.com/v1/chat/completions")
        }
        var modelName by remember {
            mutableStateOf(prefs.getString("model_name", "mimo-v2.6-flash") ?: "mimo-v2.6-flash")
        }
        var autoVoice by remember {
            mutableStateOf(prefs.getBoolean("auto_voice", false))
        }

        var summary by remember { mutableStateOf<SpendingSummary?>(null) }
        var recentBills by remember { mutableStateOf<List<BillItem>>(emptyList()) }
        val chatMessages = remember { mutableStateListOf<ChatMessage>() }

        var isDecrypting by remember { mutableStateOf(false) }
        var isAiThinking by remember { mutableStateOf(false) }
        var statusMessage by remember { mutableStateOf<String?>(null) }
        var latestBackupName by remember { mutableStateOf<String?>(null) }

        var currentlySpeakingText by remember { mutableStateOf<String?>(null) }

        fun speakOrStop(text: String) {
            if (currentlySpeakingText == text) {
                tts?.stop()
                currentlySpeakingText = null
            } else {
                if (!isTtsReady) {
                    Toast.makeText(this@MainActivity, "正在加载系统语音服务...", Toast.LENGTH_SHORT).show()
                    return
                }
                tts?.stop()
                currentlySpeakingText = text
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        runOnUiThread { currentlySpeakingText = null }
                    }
                    override fun onError(utteranceId: String?) {
                        runOnUiThread { currentlySpeakingText = null }
                    }
                })
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "yimu_speech_id")
            }
        }

        fun reloadLedgerData(label: String? = null, modTime: String? = null) {
            val reader = dbReader ?: return
            val sum = reader.getMonthlySummary(
                backupName = label ?: (summary?.backupFileName ?: ""),
                backupModifiedTime = modTime ?: (summary?.backupFileModified ?: "")
            )
            val bills = reader.getBills(limit = 60)
            summary = sum
            recentBills = bills
        }

        // 解密并读取指定或最新的备份文件
        fun doDecryptAndLoad(specificZip: File? = null, label: String? = null) {
            if (userId.isBlank()) {
                statusMessage = "请先配置一木记账的用户数字ID。"
                selectedTab = 2
                return
            }

            val targetZip = specificZip ?: BackupDecryptor.findLatestBackupFile()
            if (targetZip == null) {
                statusMessage = "未自动检测到备份包，请点击【手动挑选文件】选择手机上的 .zip 备份！"
                latestBackupName = null
                return
            }

            val modTimeStr = try {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(targetZip.lastModified()))
            } catch (e: Exception) { "" }

            latestBackupName = "${label ?: targetZip.name} (${targetZip.length() / 1024} KB)"
            isDecrypting = true
            statusMessage = "正在解密 ${targetZip.name}..."

            lifecycleScope.launch(Dispatchers.IO) {
                val destDir = File(filesDir, "extracted_db")
                val decryptResult = BackupDecryptor.decryptCustomDb(targetZip, destDir, userId)

                withContext(Dispatchers.Main) {
                    isDecrypting = false
                    decryptResult.onSuccess { extractedDb ->
                        dbReader = YimuDbReader(extractedDb)
                        reloadLedgerData(label ?: targetZip.name, modTimeStr)
                        val sum = summary
                        val billCnt = sum?.billCount ?: 0
                        val accCnt = sum?.assetSummary?.accounts?.size ?: 0
                        statusMessage = "解密成功！包含 $billCnt 笔流水，$accCnt 个账户"
                        Toast.makeText(this@MainActivity, "同步成功: $billCnt 笔流水，$accCnt 个账户", Toast.LENGTH_SHORT).show()

                        if (chatMessages.isEmpty()) {
                            val assets = sum?.assetSummary
                            val assetDesc = if (assets != null && assets.accounts.isNotEmpty()) {
                                "，已识别到 ${assets.accounts.size} 个账户（净资产 ¥%.2f，总资产 ¥%.2f，总负债 ¥%.2f）".format(assets.netAssets, assets.totalAssets, assets.totalLiabilities)
                            } else ""
                            chatMessages.add(
                                ChatMessage(
                                    text = "你好！我已经成功连接并解析了你的一木记账数据（共 $billCnt 笔流水$assetDesc）。你可以直接说“午饭花了25”、“帮我记一笔打车18”，我会直接帮你记账入库；也可以让我帮你修改任何账单分类！",
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

        // 执行 AI 记账入库动作
        fun executeAddBill(action: AiActionData.AddBill, message: ChatMessage) {
            val reader = dbReader
            if (reader == null) {
                Toast.makeText(this@MainActivity, "请先在首页加载账本数据库", Toast.LENGTH_SHORT).show()
                return
            }

            val allCats = summary?.allCategories ?: reader.getAllCategoriesWithStats()
            val matchedParent = allCats.find { it.name.contains(action.parentCategoryName) || action.parentCategoryName.contains(it.name) }
                ?: allCats.find { it.name.contains("食品") || it.name.contains("餐饮") }
                ?: allCats.firstOrNull()
            val pId = matchedParent?.id ?: 7L

            val matchedChild = matchedParent?.children?.find { it.name.contains(action.childCategoryName) || action.childCategoryName.contains(it.name) }
                ?: matchedParent?.children?.firstOrNull()
            val cId = matchedChild?.id ?: (pId * 100 + 1)

            val accounts = summary?.assetSummary?.accounts ?: emptyList()
            val matchedAsset = accounts.find { it.name.contains(action.assetName) || action.assetName.contains(it.name) }
                ?: accounts.firstOrNull()
            val assetId = matchedAsset?.id ?: (accounts.firstOrNull()?.id ?: 1L)

            val newId = reader.insertBill(
                cost = action.cost,
                parentCategoryId = pId,
                childCategoryId = cId,
                assetId = assetId,
                remark = action.remark,
                userId = userId.toLongOrNull() ?: 1467948L
            )

            if (newId > 0) {
                message.actionExecuted = true
                reloadLedgerData()
                Toast.makeText(this@MainActivity, "已成功写入一木账本！¥%.2f 已入账".format(action.cost), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "写入数据库失败，请检查数据库权限", Toast.LENGTH_LONG).show()
            }
        }

        // 执行 AI 重新分类动作
        fun executeUpdateCategory(action: AiActionData.UpdateCategory, message: ChatMessage) {
            val reader = dbReader
            if (reader == null) {
                Toast.makeText(this@MainActivity, "请先在首页加载账本数据库", Toast.LENGTH_SHORT).show()
                return
            }

            val allCats = summary?.allCategories ?: reader.getAllCategoriesWithStats()
            val matchedParent = allCats.find { it.name.contains(action.parentCategoryName) || action.parentCategoryName.contains(it.name) }
                ?: allCats.firstOrNull()
            val pId = matchedParent?.id ?: 7L

            val matchedChild = matchedParent?.children?.find { it.name.contains(action.childCategoryName) || action.childCategoryName.contains(it.name) }
                ?: matchedParent?.children?.firstOrNull()
            val cId = matchedChild?.id ?: (pId * 100 + 1)

            val success = reader.updateBillCategory(action.billId, pId, cId)
            if (success) {
                message.actionExecuted = true
                reloadLedgerData()
                Toast.makeText(this@MainActivity, "已重新分类为「${matchedParent?.name} · ${matchedChild?.name}」！", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "更新账单分类失败", Toast.LENGTH_SHORT).show()
            }
        }

        // 手动直接修改账单分类
        fun updateBillCategoryDirect(billId: Long, parentId: Long, childId: Long) {
            val reader = dbReader ?: return
            val success = reader.updateBillCategory(billId, parentId, childId)
            if (success) {
                reloadLedgerData()
                Toast.makeText(this@MainActivity, "账单分类已更新！", Toast.LENGTH_SHORT).show()
            }
        }

        // 系统文件选择器 (SAF)
        val filePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri != null) {
                isDecrypting = true
                statusMessage = "正在读取所选备份文件..."
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val tempFile = BackupDecryptor.copyUriToTempFile(this@MainActivity, uri)
                        withContext(Dispatchers.Main) {
                            doDecryptAndLoad(specificZip = tempFile, label = "用户手动选择的文件")
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            isDecrypting = false
                            statusMessage = "读取文件失败: ${e.message}"
                            Toast.makeText(this@MainActivity, statusMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        fun launchFilePicker() {
            try {
                filePickerLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "无法启动文件选择器: ${e.message}", Toast.LENGTH_SHORT).show()
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
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width / 4 } + fadeIn(animationSpec = tween(220)))
                                .togetherWith(slideOutHorizontally { width -> -width / 4 } + fadeOut(animationSpec = tween(220)))
                        } else {
                            (slideInHorizontally { width -> -width / 4 } + fadeIn(animationSpec = tween(220)))
                                .togetherWith(slideOutHorizontally { width -> width / 4 } + fadeOut(animationSpec = tween(220)))
                        }
                    },
                    label = "tab_switch_transition",
                    modifier = Modifier.fillMaxSize()
                ) { tabIndex ->
                    when (tabIndex) {
                        0 -> HomeScreen(
                            summary = summary,
                            recentBills = recentBills,
                            isDecrypting = isDecrypting,
                            onRefresh = { doDecryptAndLoad() },
                            onPickBackupFile = { launchFilePicker() },
                            onNavigateToChat = { selectedTab = 1 },
                            onNavigateToSettings = { selectedTab = 2 },
                            onUpdateBillCategory = { billId, pId, cId ->
                                updateBillCategoryDirect(billId, pId, cId)
                            }
                        )
                        1 -> ChatScreen(
                            messages = chatMessages,
                            isLoading = isAiThinking,
                            currentlySpeakingText = currentlySpeakingText,
                            onSpeakText = { text -> speakOrStop(text) },
                            onExecuteAddBill = { action, msg -> executeAddBill(action, msg) },
                            onExecuteUpdateCategory = { action, msg -> executeUpdateCategory(action, msg) },
                            onSendMessage = { query, imgUri, imgB64 ->
                                chatMessages.add(
                                    ChatMessage(
                                        text = query,
                                        isUser = true,
                                        imageUri = imgUri,
                                        imageBase64 = imgB64
                                    )
                                )
                                isAiThinking = true

                                lifecycleScope.launch {
                                    val systemPrompt = PromptEngine.buildSystemPrompt(summary, recentBills)
                                    val history = chatMessages.toList()
                                    val result = chatClient.sendMessage(systemPrompt, history)

                                    isAiThinking = false
                                    result.onSuccess { reply ->
                                        val (cleanedText, action) = ActionParser.extractAction(reply)
                                        chatMessages.add(ChatMessage(text = cleanedText, isUser = false, action = action))
                                        if (autoVoice) {
                                            speakOrStop(cleanedText)
                                        }
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
                            modelName = modelName,
                            autoVoice = autoVoice,
                            latestBackupFileName = latestBackupName,
                            statusMessage = statusMessage,
                            isProcessing = isDecrypting,
                            onSaveSettings = { newUid, newKey, newUrl, newModel, newAutoVoice ->
                                userId = newUid
                                apiKey = newKey
                                apiUrl = newUrl
                                modelName = newModel
                                autoVoice = newAutoVoice

                                prefs.edit()
                                    .putString("user_id", newUid)
                                    .putString("api_key", newKey)
                                    .putString("api_url", newUrl)
                                    .putString("model_name", newModel)
                                    .putBoolean("auto_voice", newAutoVoice)
                                    .apply()

                                chatClient.apiKey = newKey
                                chatClient.baseUrl = newUrl
                                chatClient.model = newModel
                                Toast.makeText(this@MainActivity, "配置已保存", Toast.LENGTH_SHORT).show()
                            },
                            onTriggerDecrypt = {
                                doDecryptAndLoad()
                            },
                            onPickBackupFile = {
                                launchFilePicker()
                            }
                        )
                    }
                }
            }
        }
    }
}
