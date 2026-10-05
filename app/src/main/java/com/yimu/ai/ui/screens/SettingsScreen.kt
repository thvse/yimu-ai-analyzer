package com.yimu.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yimu.ai.ui.theme.*

import androidx.compose.material.icons.filled.FolderOpen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userId: String,
    apiKey: String,
    apiUrl: String,
    modelName: String,
    autoVoice: Boolean,
    latestBackupFileName: String?,
    statusMessage: String?,
    isProcessing: Boolean,
    onSaveSettings: (userId: String, apiKey: String, apiUrl: String, modelName: String, autoVoice: Boolean) -> Unit,
    onTriggerDecrypt: () -> Unit,
    onPickBackupFile: () -> Unit
) {
    var inputUserId by remember(userId) { mutableStateOf(userId) }
    var inputApiKey by remember(apiKey) { mutableStateOf(apiKey) }
    var inputApiUrl by remember(apiUrl) { mutableStateOf(apiUrl) }
    var inputModelName by remember(modelName) { mutableStateOf(modelName) }
    var inputAutoVoice by remember(autoVoice) { mutableStateOf(autoVoice) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("配置与同步", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 一木记账备份同步卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = BrandPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("一木记账本地备份同步", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "检测到的最新备份文件：",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = latestBackupFileName ?: "未检测到备份文件（请在一木记账中执行一次“数据备份”）",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (latestBackupFileName != null) BrandPrimary else ExpenseRed
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = inputUserId,
                        onValueChange = { inputUserId = it },
                        label = { Text("一木记账用户数字ID（解密密码）") },
                        placeholder = { Text("例如：104523") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "提示：打开手机一木记账 -> 点击“我的” -> 点击头像进入个人信息即可看到该数字ID。",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPickBackupFile,
                            enabled = !isProcessing,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("手动挑选文件")
                        }

                        Button(
                            onClick = {
                                onSaveSettings(inputUserId, inputApiKey, inputApiUrl, inputModelName, inputAutoVoice)
                                onTriggerDecrypt()
                            },
                            enabled = inputUserId.isNotBlank() && !isProcessing,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("解密中...")
                            } else {
                                Text("扫描并同步")
                            }
                        }
                    }
                }
            }

            // AI 大模型 API 配置卡片 (支持 MiMo / DeepSeek / OpenAI 等)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = BrandPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI 大模型配置 (支持 MiMo/多模态)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("快速预设推荐：", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))

                    // 预设选择按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                inputApiUrl = "https://api.xiaomimimo.com/v1/chat/completions"
                                inputModelName = "mimo-v2.6-flash"
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("小米 MiMo", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                inputApiUrl = "https://api.deepseek.com/chat/completions"
                                inputModelName = "deepseek-chat"
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("DeepSeek", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                inputApiUrl = "https://api.openai.com/v1/chat/completions"
                                inputModelName = "gpt-4o"
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("OpenAI", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = inputApiKey,
                        onValueChange = { inputApiKey = it },
                        label = { Text("API Key (如小米 MiMo Token / DeepSeek Key)") },
                        placeholder = { Text("例如：sk-...") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputApiUrl,
                        onValueChange = { inputApiUrl = it },
                        label = { Text("API 端点地址 (Base URL)") },
                        placeholder = { Text("https://api.xiaomimimo.com/v1/chat/completions") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputModelName,
                        onValueChange = { inputModelName = it },
                        label = { Text("模型名称 (如 mimo-v2.6-flash, mimo-v2.6-pro)") },
                        placeholder = { Text("mimo-v2.6-flash") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onSaveSettings(inputUserId, inputApiKey, inputApiUrl, inputModelName, inputAutoVoice)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("保存大模型配置")
                    }
                }
            }

            // 语音互动卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = BrandPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("AI 回复自动语音朗读", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("接收到回复后通过系统语音引擎朗读", fontSize = 12.sp, color = TextSecondary)
                        }
                    }

                    Switch(
                        checked = inputAutoVoice,
                        onCheckedChange = {
                            inputAutoVoice = it
                            onSaveSettings(inputUserId, inputApiKey, inputApiUrl, inputModelName, it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandPrimary)
                    )
                }
            }

            // 状态反馈展示
            if (!statusMessage.isNullOrBlank()) {
                Surface(
                    color = BackgroundLight,
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusMessage,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}
