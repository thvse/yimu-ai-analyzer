package com.yimu.ai.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.yimu.ai.data.AiActionData
import com.yimu.ai.data.ChatMessage
import com.yimu.ai.ui.components.MarkdownView
import com.yimu.ai.ui.theme.*
import com.yimu.ai.utils.ImageHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    onSendMessage: (text: String, imageUri: String?, imageBase64: String?) -> Unit,
    onSpeakText: (String) -> Unit,
    currentlySpeakingText: String?,
    onExecuteAddBill: (AiActionData.AddBill, ChatMessage) -> Unit = { _, _ -> },
    onExecuteUpdateCategory: (AiActionData.UpdateCategory, ChatMessage) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var pendingImageUri by remember { mutableStateOf<String?>(null) }
    var pendingImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingImageBase64 by remember { mutableStateOf<String?>(null) }

    // 常用快捷提问与智能行动胶囊
    val suggestions = listOf(
        "🍔 记一笔25元的午餐外卖",
        "🚕 记一笔18元的打车支出",
        "💰 记一笔8000元工资收入",
        "🔄 把刚才那笔红包改成餐饮",
        "📊 深度诊断我当前的消费结构",
        "📸 帮我识别这张消费小票/账单",
        "💡 给出3个最有效的个性化省钱建议"
    )

    // 相册选图 Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val (bmp, b64) = ImageHelper.processImageUri(context, uri)
            if (bmp != null && b64 != null) {
                pendingImageUri = uri.toString()
                pendingImageBitmap = bmp
                pendingImageBase64 = b64
                Toast.makeText(context, "已载入图片，可配合问题直接发送", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "图片读取失败，请重试", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 语音识别结果 Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                inputText = if (inputText.isBlank()) spoken else "$inputText $spoken"
            }
        }
    }

    // 录音权限申请 Launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出您的问题或记账指令...")
            }
            try {
                speechLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "系统未找到语音输入组件", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "需要录音权限来进行语音输入", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(BrandPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = BrandPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("AI 智能记账顾问", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text("可自然语言记账 · 重分类 · 图文识别", fontSize = 11.sp, color = BrandPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .navigationBarsPadding()
                    .imePadding()
                    .animateContentSize()
            ) {
                // 快捷提问与智能行动胶囊
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestions.forEach { suggestion ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = BackgroundLight,
                            border = ButtonDefaults.outlinedButtonBorder,
                            modifier = Modifier.clickable(enabled = !isLoading) {
                                onSendMessage(suggestion, null, null)
                            }
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // 待发送图片缩略图预览栏
                AnimatedVisibility(
                    visible = pendingImageBitmap != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (pendingImageBitmap != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BackgroundLight)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        bitmap = pendingImageBitmap!!.asImageBitmap(),
                                        contentDescription = "待发送小票",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("已选发票/账单小票图片", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text("点击发送将连同问题一并提交给 MiMo 视觉分析", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                                IconButton(onClick = {
                                    pendingImageUri = null
                                    pendingImageBitmap = null
                                    pendingImageBase64 = null
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "取消选择", tint = ExpenseRed)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderLight)

                // 底部输入控制栏 (图片 + 语音 + 文本 + 发送)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. 发送图片按钮 (小票/发票/账单)
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "选择小票图片",
                            tint = if (pendingImageBitmap != null) BrandPrimary else TextSecondary
                        )
                    }

                    // 2. 语音输入按钮 (麦克风)
                    IconButton(
                        onClick = {
                            val permission = Manifest.permission.RECORD_AUDIO
                            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出您的问题或记账指令...")
                                }
                                try {
                                    speechLauncher.launch(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "未找到语音输入组件", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                audioPermissionLauncher.launch(permission)
                            }
                        },
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "语音输入",
                            tint = TextSecondary
                        )
                    }

                    // 3. 文本输入框
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("说句话直接记账，如“午饭微信付了25”...", fontSize = 13.sp) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = BackgroundLight,
                            focusedContainerColor = BackgroundLight,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = BrandPrimary
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    val canSend = (inputText.isNotBlank() || pendingImageBase64 != null) && !isLoading

                    // 4. 发送按钮
                    IconButton(
                        onClick = {
                            val text = inputText.trim()
                            val uri = pendingImageUri
                            val b64 = pendingImageBase64
                            if (canSend) {
                                inputText = ""
                                pendingImageUri = null
                                pendingImageBitmap = null
                                pendingImageBase64 = null
                                onSendMessage(text, uri, b64)
                            }
                        },
                        enabled = canSend,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (canSend) BrandPrimary else Color(0xFFE2E8F0))
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "发送",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                ChatMessageBubble(
                    message = message,
                    isSpeaking = currentlySpeakingText == message.text,
                    onSpeakText = { onSpeakText(message.text) },
                    onExecuteAddBill = { action -> onExecuteAddBill(action, message) },
                    onExecuteUpdateCategory = { action -> onExecuteUpdateCategory(action, message) }
                )
            }

            if (isLoading) {
                item {
                    AiThinkingCard()
                }
            }
        }
    }
}

/**
 * 丝滑脉冲思考动效卡片
 */
@Composable
private fun AiThinkingCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking_anim")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary.copy(alpha = alphaAnim))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "AI 正在理解账务语义 / 执行记账推理中...",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    isSpeaking: Boolean,
    onSpeakText: () -> Unit,
    onExecuteAddBill: (AiActionData.AddBill) -> Unit,
    onExecuteUpdateCategory: (AiActionData.UpdateCategory) -> Unit
) {
    val context = LocalContext.current
    val isUser = message.isUser

    val imageBitmap = remember(message.imageUri) {
        if (!message.imageUri.isNullOrBlank()) {
            ImageHelper.loadThumbnail(context, message.imageUri)
        } else null
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) BrandPrimary else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isUser) 1.dp else 2.dp),
            modifier = Modifier
                .then(
                    if (isUser) Modifier.widthIn(max = 300.dp)
                    else Modifier.fillMaxWidth(0.96f) // AI 消息留出充裕空间展示表格与操作卡片
                )
                .animateContentSize()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // 如果消息带有小票或图片附件，先渲染缩略图
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap.asImageBitmap(),
                        contentDescription = "账单图片",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    if (message.text.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // 核心渲染区：用户文本直接渲染，AI 消息使用 MarkdownView 渲染
                if (message.text.isNotBlank()) {
                    if (isUser) {
                        Text(
                            text = message.text,
                            color = Color.White,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    } else {
                        MarkdownView(
                            content = message.text,
                            textColor = TextPrimary
                        )
                    }
                }

                // 核心交互：AI 新增记账行动确认卡片
                if (message.action is AiActionData.AddBill) {
                    val addAction = message.action
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI 记账快捷入库确认", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF166534))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("入账金额", fontSize = 12.sp, color = Color(0xFF4B5563))
                                Text("¥%.2f".format(addAction.cost), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("记账分类", fontSize = 12.sp, color = Color(0xFF4B5563))
                                Text("${addAction.parentCategoryName} · ${addAction.childCategoryName}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("扣款账户", fontSize = 12.sp, color = Color(0xFF4B5563))
                                Text(addAction.assetName, fontSize = 13.sp, color = TextPrimary)
                            }
                            if (addAction.remark.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("记账备注", fontSize = 12.sp, color = Color(0xFF4B5563))
                                    Text(addAction.remark, fontSize = 13.sp, color = BrandPrimary)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            if (!message.actionExecuted) {
                                Button(
                                    onClick = { onExecuteAddBill(addAction) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("一键确认保存至一木账本", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFDCFCE7)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("已成功保存入库！账本已即时同步", fontSize = 12.sp, color = Color(0xFF166534), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 核心交互：AI 重新分类行动确认卡片
                if (message.action is AiActionData.UpdateCategory) {
                    val updateAction = message.action
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA5B4FC))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI 账单重新分类确认", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF3730A3))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("将对应账单重新归类为：", fontSize = 12.sp, color = Color(0xFF4B5563))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${updateAction.parentCategoryName} · ${updateAction.childCategoryName}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4F46E5)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (!message.actionExecuted) {
                                Button(
                                    onClick = { onExecuteUpdateCategory(updateAction) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("确认修改并同步账本数据库", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFE0E7FF)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4338CA), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("账单分类已更新并已同步数据库", fontSize = 12.sp, color = Color(0xFF3730A3), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // AI 回复附带语音播放按钮
                if (!isUser && message.text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSpeaking) BrandPrimary.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                            modifier = Modifier.clickable { onSpeakText() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = if (isSpeaking) "停止朗读" else "语音朗读",
                                    tint = if (isSpeaking) ExpenseRed else BrandPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSpeaking) "停止朗读" else "语音朗读",
                                    fontSize = 11.sp,
                                    color = if (isSpeaking) ExpenseRed else BrandPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
