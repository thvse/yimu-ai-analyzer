package com.yimu.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yimu.ai.ui.theme.BrandPrimary
import com.yimu.ai.ui.theme.TextPrimary
import com.yimu.ai.ui.theme.TextSecondary

sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock()
    data class Blockquote(val text: String) : MarkdownBlock()
    data class BulletList(val items: List<String>) : MarkdownBlock()
    data class OrderedList(val items: List<Pair<String, String>>) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    object Divider : MarkdownBlock()
}

/**
 * 极简高效的 Markdown 原生 Compose 渲染器，支持：
 * 1. 一至四级标题 (H1-H4)
 * 2. Markdown 表格 (带网格边框、表头加粗底色及自适应排版)
 * 3. 引用块 (Blockquote)
 * 4. 水平分割线 (Divider)
 * 5. 无序及有序列表 (Bullet / Numbered lists)
 * 6. 代码块 (Code block)
 * 7. 行内粗体 (**bold**)、行内代码 (`code`)、斜体 (*italic*)
 */
@Composable
fun MarkdownView(
    content: String,
    modifier: Modifier = Modifier,
    textColor: Color = TextPrimary
) {
    val blocks = remember(content) { parseMarkdown(content) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> HeaderBlockView(block, textColor)
                is MarkdownBlock.Paragraph -> ParagraphBlockView(block.text, textColor)
                is MarkdownBlock.Table -> TableBlockView(block)
                is MarkdownBlock.Blockquote -> BlockquoteView(block.text)
                is MarkdownBlock.BulletList -> BulletListView(block.items, textColor)
                is MarkdownBlock.OrderedList -> OrderedListView(block.items, textColor)
                is MarkdownBlock.CodeBlock -> CodeBlockView(block)
                is MarkdownBlock.Divider -> {
                    HorizontalDivider(
                        color = Color(0xFFE2E8F0),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderBlockView(header: MarkdownBlock.Header, defaultColor: Color) {
    val (fontSize, fontWeight, topPadding) = when (header.level) {
        1 -> Triple(18.sp, FontWeight.Bold, 6.dp)
        2 -> Triple(16.sp, FontWeight.Bold, 4.dp)
        3 -> Triple(15.sp, FontWeight.SemiBold, 2.dp)
        else -> Triple(14.sp, FontWeight.SemiBold, 0.dp)
    }

    Column(modifier = Modifier.padding(top = topPadding)) {
        Text(
            text = parseInlineMarkdown(header.text),
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = if (header.level == 1) BrandPrimary else defaultColor,
            lineHeight = (fontSize.value * 1.3).sp
        )
        if (header.level <= 2) {
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
private fun ParagraphBlockView(text: String, defaultColor: Color) {
    Text(
        text = parseInlineMarkdown(text),
        fontSize = 14.sp,
        lineHeight = 22.sp,
        color = defaultColor
    )
}

@Composable
private fun BlockquoteView(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF1F5F9))
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(BrandPrimary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = parseInlineMarkdown(text),
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = Color(0xFF334155),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TableBlockView(table: MarkdownBlock.Table) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 表头
            if (table.headers.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    table.headers.forEachIndexed { index, title ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            contentAlignment = if (index == 0) Alignment.CenterStart else if (index == table.headers.lastIndex) Alignment.CenterEnd else Alignment.Center
                        ) {
                            Text(
                                text = parseInlineMarkdown(title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
            }

            // 数据行
            table.rows.forEachIndexed { rowIndex, row ->
                val rowBg = if (rowIndex % 2 == 1) Color(0xFFF8FAFC).copy(alpha = 0.5f) else Color.White
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEachIndexed { colIndex, cell ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            contentAlignment = if (colIndex == 0) Alignment.CenterStart else if (colIndex == row.lastIndex) Alignment.CenterEnd else Alignment.Center
                        ) {
                            Text(
                                text = parseInlineMarkdown(cell),
                                fontSize = 12.sp,
                                color = Color(0xFF334155),
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
                if (rowIndex != table.rows.lastIndex) {
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)
                }
            }
        }
    }
}

@Composable
private fun BulletListView(items: List<String>, defaultColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp, end = 8.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary)
                )
                Text(
                    text = parseInlineMarkdown(item),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = defaultColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OrderedListView(items: List<Pair<String, String>>, defaultColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { (indexStr, text) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "$indexStr.",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = BrandPrimary,
                    modifier = Modifier.padding(end = 6.dp, top = 1.dp)
                )
                Text(
                    text = parseInlineMarkdown(text),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = defaultColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CodeBlockView(block: MarkdownBlock.CodeBlock) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            if (block.language.isNotBlank()) {
                Text(
                    text = block.language.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = block.code,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Color(0xFFE2E8F0)
            )
        }
    }
}

fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = text.length
        while (i < len) {
            if (i + 1 < len && text[i] == '*' && text[i + 1] == '*') {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            } else if (i + 1 < len && text[i] == '_' && text[i + 1] == '_') {
                val end = text.indexOf("__", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            } else if (text[i] == '`') {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandPrimary,
                            background = Color(0x15000000)
                        )
                    ) {
                        append(" ${text.substring(i + 1, end)} ")
                    }
                    i = end + 1
                    continue
                }
            } else if (text[i] == '*' && (i == 0 || text[i - 1] != '*') && (i + 1 < len && text[i + 1] != '*')) {
                val end = text.indexOf('*', i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }
            append(text[i])
            i++
        }
    }
}

fun parseMarkdown(content: String): List<MarkdownBlock> {
    val lines = content.lines()
    val blocks = mutableListOf<MarkdownBlock>()
    var i = 0

    while (i < lines.size) {
        val line = lines[i].trim()
        if (line.isEmpty()) {
            i++
            continue
        }

        // 1. 标题 (#, ##, ###, ####)
        if (line.startsWith("#")) {
            val level = line.takeWhile { it == '#' }.length
            val headerText = line.drop(level).trim()
            blocks.add(MarkdownBlock.Header(level, headerText))
            i++
            continue
        }

        // 2. 分割线 (---, ***, ___)
        if (line == "---" || line == "***" || line == "___") {
            blocks.add(MarkdownBlock.Divider)
            i++
            continue
        }

        // 3. 引用块 (> ...)
        if (line.startsWith(">")) {
            val quoteLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                quoteLines.add(lines[i].trim().removePrefix(">").trim())
                i++
            }
            blocks.add(MarkdownBlock.Blockquote(quoteLines.joinToString("\n")))
            continue
        }

        // 4. 表格 (| col1 | col2 |)
        if (line.startsWith("|") && line.endsWith("|") && i + 1 < lines.size && lines[i + 1].contains("|") && lines[i + 1].contains("-")) {
            val headers = line.split("|").filter { it.isNotBlank() }.map { it.trim() }
            i += 2 // 跳过表头和分隔线 |---|---|
            val rows = mutableListOf<List<String>>()
            while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                val cells = lines[i].trim().split("|").filter { it.isNotBlank() }.map { it.trim() }
                rows.add(cells)
                i++
            }
            blocks.add(MarkdownBlock.Table(headers, rows))
            continue
        }

        // 5. 无序列表 (- , * , + )
        if (line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ")) {
            val items = mutableListOf<String>()
            while (i < lines.size) {
                val cur = lines[i].trim()
                if (cur.startsWith("- ") || cur.startsWith("* ") || cur.startsWith("+ ")) {
                    items.add(cur.substring(2).trim())
                    i++
                } else {
                    break
                }
            }
            blocks.add(MarkdownBlock.BulletList(items))
            continue
        }

        // 6. 有序列表 (1. , 2. )
        val orderedMatch = Regex("^(\\d+)\\.\\s+(.+)").find(line)
        if (orderedMatch != null) {
            val items = mutableListOf<Pair<String, String>>()
            while (i < lines.size) {
                val cur = lines[i].trim()
                val m = Regex("^(\\d+)\\.\\s+(.+)").find(cur)
                if (m != null) {
                    items.add(Pair(m.groupValues[1], m.groupValues[2]))
                    i++
                } else {
                    break
                }
            }
            blocks.add(MarkdownBlock.OrderedList(items))
            continue
        }

        // 7. 代码块 (```)
        if (line.startsWith("```")) {
            val lang = line.removePrefix("```").trim()
            i++
            val codeLines = mutableListOf<String>()
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size && lines[i].trim().startsWith("```")) {
                i++
            }
            blocks.add(MarkdownBlock.CodeBlock(lang, codeLines.joinToString("\n")))
            continue
        }

        // 8. 普通段落
        blocks.add(MarkdownBlock.Paragraph(line))
        i++
    }

    return blocks
}
