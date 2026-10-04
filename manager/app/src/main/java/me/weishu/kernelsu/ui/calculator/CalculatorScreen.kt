package me.weishu.kernelsu.ui.calculator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

private val CalculatorBackground = Color(0xFFE6E8EB)
private val CalculatorKeyTop = Color(0xFFF5F6F7)
private val CalculatorKeyBottom = Color(0xFFD9DBDE)
private val CalculatorText = Color(0xFF111316)
private val CalculatorMutedText = Color(0xFF686B70)
private val CalculatorAccent = Color(0xFFFF8A20)

private enum class CalculatorMode { Standard, Scientific }

private data class ConverterCategory(
    val title: String,
    val symbol: String,
    val units: List<String>,
    val toBase: Map<String, Double> = emptyMap(),
    val fromBase: Map<String, Double> = emptyMap(),
    val temperature: Boolean = false,
)

private val converterCategories = listOf(
    ConverterCategory("汇率", "¥", listOf("CNY", "USD", "EUR", "JPY", "KRW"), mapOf("CNY" to 1.0, "USD" to 7.18, "EUR" to 8.38, "JPY" to 0.050, "KRW" to 0.0052), mapOf("CNY" to 1.0, "USD" to 1.0 / 7.18, "EUR" to 1.0 / 8.38, "JPY" to 20.0, "KRW" to 192.3)),
    ConverterCategory("房贷", "⌂", emptyList()),
    ConverterCategory("长度", "▰", listOf("mm", "cm", "m", "km", "in", "ft", "yd", "mi"), mapOf("mm" to 0.001, "cm" to 0.01, "m" to 1.0, "km" to 1000.0, "in" to 0.0254, "ft" to 0.3048, "yd" to 0.9144, "mi" to 1609.344), mapOf("mm" to 1000.0, "cm" to 100.0, "m" to 1.0, "km" to 0.001, "in" to 39.37007874, "ft" to 3.280839895, "yd" to 1.093613298, "mi" to 0.000621371)),
    ConverterCategory("面积", "▣", listOf("mm²", "cm²", "m²", "km²", "ha", "in²", "ft²", "acre"), mapOf("mm²" to 1e-6, "cm²" to 1e-4, "m²" to 1.0, "km²" to 1e6, "ha" to 1e4, "in²" to 0.00064516, "ft²" to 0.09290304, "acre" to 4046.8564224), mapOf("mm²" to 1e6, "cm²" to 1e4, "m²" to 1.0, "km²" to 1e-6, "ha" to 1e-4, "in²" to 1550.0031, "ft²" to 10.7639104, "acre" to 0.0002471054)),
    ConverterCategory("体积", "◇", listOf("mL", "L", "m³", "cm³", "gal", "qt"), mapOf("mL" to 0.001, "L" to 1.0, "m³" to 1000.0, "cm³" to 0.001, "gal" to 3.785411784, "qt" to 0.946352946), mapOf("mL" to 1000.0, "L" to 1.0, "m³" to 0.001, "cm³" to 1000.0, "gal" to 0.2641720524, "qt" to 1.056688209)),
    ConverterCategory("重量", "K", listOf("mg", "g", "kg", "t", "oz", "lb"), mapOf("mg" to 1e-6, "g" to 0.001, "kg" to 1.0, "t" to 1000.0, "oz" to 0.0283495231, "lb" to 0.45359237), mapOf("mg" to 1e6, "g" to 1000.0, "kg" to 1.0, "t" to 0.001, "oz" to 35.273962, "lb" to 2.20462262)),
    ConverterCategory("温度", "♨", listOf("°C", "°F", "K"), temperature = true),
    ConverterCategory("速度", "◉", listOf("m/s", "km/h", "mph", "kn"), mapOf("m/s" to 1.0, "km/h" to 1.0 / 3.6, "mph" to 0.44704, "kn" to 0.514444), mapOf("m/s" to 1.0, "km/h" to 3.6, "mph" to 2.236936292, "kn" to 1.943844492)),
    ConverterCategory("压强", "◍", listOf("Pa", "kPa", "MPa", "bar", "atm", "psi"), mapOf("Pa" to 1.0, "kPa" to 1000.0, "MPa" to 1e6, "bar" to 1e5, "atm" to 101325.0, "psi" to 6894.757293), mapOf("Pa" to 1.0, "kPa" to 0.001, "MPa" to 1e-6, "bar" to 1e-5, "atm" to 1.0 / 101325.0, "psi" to 1.0 / 6894.757293)),
    ConverterCategory("功率", "ϟ", listOf("W", "kW", "MW", "hp"), mapOf("W" to 1.0, "kW" to 1000.0, "MW" to 1e6, "hp" to 745.699872), mapOf("W" to 1.0, "kW" to 0.001, "MW" to 1e-6, "hp" to 1.0 / 745.699872)),
    ConverterCategory("进制", "01", listOf("DEC", "BIN", "OCT", "HEX")),
)

@Composable
fun CalculatorScreen(onUnlock: () -> Unit) {
    var calculatorMode by rememberSaveable { mutableStateOf(CalculatorMode.Standard.name) }
    var converterMode by rememberSaveable { mutableStateOf(false) }
    var expression by rememberSaveable { mutableStateOf("") }
    var display by rememberSaveable { mutableStateOf("0") }
    var justEvaluated by rememberSaveable { mutableStateOf(false) }
    var inverse by rememberSaveable { mutableStateOf(false) }
    var degree by rememberSaveable { mutableStateOf(true) }
    var menuExpanded by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<String>() }

    if (converterMode) {
        ConverterHomeScreen(onBack = { converterMode = false })
        return
    }

    val scientific = calculatorMode == CalculatorMode.Scientific.name

    fun append(value: String) {
        if (value == "AC") {
            expression = ""; display = "0"; justEvaluated = false; return
        }
        if (value == "⌫") {
            if (expression.isNotEmpty()) expression = expression.dropLast(1)
            display = expression.ifEmpty { "0" }
            justEvaluated = false
            return
        }
        if (value == "=") {
            if (expression.isBlank()) return
            val result = runCatching { evaluateExpression(expression, degree, inverse) }.getOrElse { return }
            history.add("$expression = $result")
            while (history.size > 30) history.removeAt(0)
            display = result
            expression = result
            justEvaluated = true
            return
        }
        if (value in listOf("+", "−", "×", "÷", "%", "^")) {
            if (expression.isEmpty()) return
            if (expression.last().toString() in listOf("+", "−", "×", "÷", "%", "^")) {
                expression = expression.dropLast(1) + value
            } else expression += value
            display = expression
            justEvaluated = false
            return
        }
        if (value == ".") {
            val token = expression.takeLastWhile { it.isDigit() || it == '.' }
            if (token.contains('.')) return
            if (expression.isEmpty() || expression.last().toString() in listOf("+", "−", "×", "÷", "%", "^", "(")) expression += "0."
            else expression += "."
            display = expression; justEvaluated = false; return
        }
        if (value == "!") {
            if (expression.isNotEmpty() && (expression.last().isDigit() || expression.last() == ')')) expression += "!"
            display = expression; justEvaluated = false; return
        }
        if (value == "π" || value == "e" || value == "(") {
            if (justEvaluated) { expression = ""; justEvaluated = false }
            expression += value; display = expression; return
        }
        if (value.endsWith("(")) {
            if (justEvaluated) { expression = ""; justEvaluated = false }
            expression += value; display = expression; return
        }
        if (justEvaluated) { expression = ""; justEvaluated = false }
        expression += value
        display = expression
        if (expression == "917813") { expression = ""; display = "0"; onUnlock() }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(CalculatorBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(72.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { }) {
                    Icon(Icons.Outlined.Fullscreen, null, tint = CalculatorText)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { calculatorMode = if (scientific) CalculatorMode.Standard.name else CalculatorMode.Scientific.name }) {
                    Icon(if (scientific) Icons.Outlined.Calculate else Icons.Outlined.Functions, null, tint = CalculatorText)
                }
                IconButton(onClick = { converterMode = true }) {
                    Icon(Icons.Outlined.GridView, null, tint = CalculatorText)
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Outlined.MoreVert, null, tint = CalculatorText) }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("计算历史") }, onClick = { menuExpanded = false; historyOpen = true })
                        DropdownMenuItem(text = { Text("清空历史") }, onClick = { history.clear(); menuExpanded = false })
                        DropdownMenuItem(text = { Text("换算") }, leadingIcon = { Icon(Icons.Outlined.SwapHoriz, null) }, onClick = { menuExpanded = false; converterMode = true })
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomEnd) {
                Text(
                    display,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = if (display.length > 12) 40.sp else 58.sp,
                    fontWeight = FontWeight.Light,
                    color = CalculatorText,
                )
            }
            Spacer(Modifier.height(10.dp))
            AnimatedContent(
                targetState = scientific,
                transitionSpec = {
                    (scaleIn(initialScale = 0.94f) + fadeIn()) togetherWith
                        (scaleOut(targetScale = 0.94f) + fadeOut())
                },
                label = "calculator-mode"
            ) { isScientific ->
                if (isScientific) {
                    ScientificPad(onKey = { key ->
                        when (key) {
                            "sin" -> append(if (inverse) "asin(" else "sin(")
                            "cos" -> append(if (inverse) "acos(" else "cos(")
                            "tan" -> append(if (inverse) "atan(" else "tan(")
                            "log" -> append("log(")
                            "ln" -> append("ln(")
                            "sqrt" -> append("sqrt(")
                            "inv" -> inverse = !inverse
                            "rad" -> degree = false
                            "deg" -> degree = true
                            else -> append(key)
                        }
                    })
                } else {
                    StandardPad(onKey = ::append)
                }
            }
        }
    }

    if (historyOpen) {
        AlertDialog(
            onDismissRequest = { historyOpen = false },
            confirmButton = {},
            title = { Text("计算历史") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (history.isEmpty()) Text("暂无记录", color = CalculatorMutedText)
                    else history.asReversed().forEach { Text(it, modifier = Modifier.padding(vertical = 4.dp)) }
                }
            },
        )
    }
}

@Composable
private fun StandardPad(onKey: (String) -> Unit) {
    val rows = listOf(
        listOf("AC", "%", "⌫", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "−"),
        listOf("1", "2", "3", "+"),
        listOf("00", "0", ".", "=")
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { key ->
                    GlassKey(
                        key,
                        Modifier.weight(1f),
                        highlighted = key == "=",
                        onClick = { onKey(key) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScientificPad(onKey: (String) -> Unit) {
    val rows = listOf(
        listOf("sin", "cos", "tan", "rad", "deg"),
        listOf("log", "ln", "(", ")", "inv"),
        listOf("!", "AC", "%", "⌫", "÷"),
        listOf("^", "7", "8", "9", "×"),
        listOf("sqrt", "4", "5", "6", "−"),
        listOf("π", "1", "2", "3", "+"),
        listOf("e", "00", "0", ".", "="),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    val active = key == "deg"
                    GlassKey(key, Modifier.weight(1f), highlighted = key == "=", active = active, onClick = { onKey(key) }, small = key in listOf("sin","cos","tan","rad","deg","log","ln","(",")","inv","sqrt","π","e","!","^"))
                }
            }
        }
    }
}

@Composable
private fun GlassKey(label: String, modifier: Modifier, highlighted: Boolean, active: Boolean = false, small: Boolean = false, onClick: () -> Unit) {
    val borderColor = Color.White.copy(alpha = 0.88f)
    val textColor = when {
        highlighted -> Color.White
        active -> CalculatorAccent
        else -> CalculatorText
    }
    val pressSource = remember { MutableInteractionSource() }
    val pressed by pressSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 650f),
        label = "calculator-key-scale"
    )
    val brush = if (highlighted) Brush.linearGradient(listOf(CalculatorAccent, Color(0xFFFFAA55))) else Brush.linearGradient(listOf(CalculatorKeyTop, CalculatorKeyBottom))
    Box(
        modifier
            .height(if (small) 58.dp else 72.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (pressed) 2f else 0f
            }
            .shadow(if (pressed) 2.dp else 8.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(brush)
            .border(1.dp, borderColor, CircleShape)
            .clickable(interactionSource = pressSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = if (small) 20.sp else 28.sp, fontWeight = if (highlighted) FontWeight.Medium else FontWeight.Normal, color = textColor, maxLines = 1)
    }
}

@Composable
private fun ConverterHomeScreen(onBack: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf(-1) }
    if (selected >= 0) {
        UnitConverterScreen(converterCategories[selected], onBack = { selected = -1 })
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().background(CalculatorBackground).padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(72.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null, tint = CalculatorText) }
            Text("换算", fontSize = 26.sp, fontWeight = FontWeight.Medium, color = CalculatorText)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
        ) {
            items(converterCategories) { category ->
                Box(
                    modifier = Modifier.height(126.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.94f)).border(1.dp, Color.White.copy(alpha = 0.98f), RoundedCornerShape(18.dp)).clickable { selected = converterCategories.indexOf(category) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF17191C)), contentAlignment = Alignment.Center) {
                            Text(category.symbol, color = Color.White, fontSize = if (category.symbol.length > 1) 15.sp else 25.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(category.title, fontSize = 18.sp, color = CalculatorText)
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitConverterScreen(category: ConverterCategory, onBack: () -> Unit) {
    var value by rememberSaveable { mutableStateOf("1") }
    var from by rememberSaveable { mutableStateOf(category.units.firstOrNull() ?: "CNY") }
    var to by rememberSaveable { mutableStateOf(category.units.getOrNull(1) ?: "USD") }
    val result = remember(value, from, to, category.title) { convertValue(value, from, to, category) }

    Column(Modifier.fillMaxSize().background(CalculatorBackground).padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(72.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null, tint = CalculatorText) }
            Text(category.title, fontSize = 26.sp, fontWeight = FontWeight.Medium, color = CalculatorText)
        }
        if (category.units.isEmpty()) {
            MortgageCalculator()
            return@Column
        }
        Text("输入值", color = CalculatorMutedText, fontSize = 14.sp)
        androidx.compose.material3.OutlinedTextField(value = value, onValueChange = { value = it }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        Spacer(Modifier.height(14.dp))
        UnitPicker("从", from, category.units) { from = it }
        Spacer(Modifier.height(10.dp))
        UnitPicker("到", to, category.units) { to = it }
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = .72f)).border(1.dp, Color.White, RoundedCornerShape(22.dp)).padding(20.dp)) {
            Column {
                Text("结果", color = CalculatorMutedText)
                Text(result, fontSize = 32.sp, color = CalculatorText, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun UnitPicker(label: String, selected: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        androidx.compose.material3.OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("$label：$selected"); Text("⌄") }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { expanded = false; onSelected(option) }) }
        }
    }
}

@Composable
private fun MortgageCalculator() {
    var principal by rememberSaveable { mutableStateOf("1000000") }
    var annual by rememberSaveable { mutableStateOf("3.45") }
    var years by rememberSaveable { mutableStateOf("30") }
    val payment = remember(principal, annual, years) {
        runCatching {
            val p = principal.toDouble()
            val r = annual.toDouble() / 1200.0
            val n = years.toDouble() * 12.0
            if (r == 0.0) p / n else p * r * Math.pow(1.0 + r, n) / (Math.pow(1.0 + r, n) - 1.0)
        }.getOrDefault(0.0)
    }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text("贷款计算", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        androidx.compose.material3.OutlinedTextField(principal, { principal = it }, label = { Text("贷款金额") }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        androidx.compose.material3.OutlinedTextField(annual, { annual = it }, label = { Text("年利率 (%)") }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        androidx.compose.material3.OutlinedTextField(years, { years = it }, label = { Text("年限") }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        Spacer(Modifier.height(18.dp))
        Text("等额本息月供：${"%.2f".format(payment)}", fontSize = 24.sp, color = CalculatorText)
    }
}

private fun convertValue(text: String, from: String, to: String, category: ConverterCategory): String {
    val x = text.toDoubleOrNull() ?: return "-"
    if (category.title == "进制") return convertRadixString(text.trim(), from, to)
    val result = when {
        category.title == "温度" -> when (from to to) {
            "°C" to "°F" -> x * 9 / 5 + 32
            "°F" to "°C" -> (x - 32) * 5 / 9
            "°C" to "K" -> x + 273.15
            "K" to "°C" -> x - 273.15
            "°F" to "K" -> (x - 32) * 5 / 9 + 273.15
            "K" to "°F" -> (x - 273.15) * 9 / 5 + 32
            else -> x
        }
        else -> {
            val base = x * (category.toBase[from] ?: 1.0)
            base * (category.fromBase[to] ?: 1.0)
        }
    }
    return if (result.isFinite()) BigDecimal.valueOf(result).stripTrailingZeros().toPlainString() else "-"
}

private fun convertRadixString(value: String, from: String, to: String): String {
    val radix = when (from) { "BIN" -> 2; "OCT" -> 8; "HEX" -> 16; else -> 10 }
    val decimal = value.removePrefix("0x").removePrefix("0X").toLongOrNull(radix) ?: return "-"
    return when (to) {
        "BIN" -> decimal.toString(2)
        "OCT" -> decimal.toString(8)
        "HEX" -> decimal.toString(16).uppercase()
        else -> decimal.toString()
    }
}

private fun evaluateExpression(input: String, degree: Boolean, inverse: Boolean): String {
    val parser = ExpressionParser(input.replace('−', '-').replace('×', '*').replace('÷', '/'), degree, inverse)
    val value = parser.parse()
    if (!value.isFinite()) error("Invalid")
    return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
}

private class ExpressionParser(private val text: String, private val degree: Boolean, private val inverse: Boolean) {
    private var index = 0

    fun parse(): Double {
        val result = parseAddSub()
        skipSpaces()
        if (index != text.length) error("Invalid expression")
        return result
    }

    private fun parseAddSub(): Double {
        var value = parseMulDiv()
        while (true) {
            skipSpaces()
            when {
                match('+') -> value += parseMulDiv()
                match('-') -> value -= parseMulDiv()
                else -> return value
            }
        }
    }

    private fun parseMulDiv(): Double {
        var value = parsePower()
        while (true) {
            skipSpaces()
            when {
                match('*') -> value *= parsePower()
                match('/') -> value /= parsePower()
                match('%') -> value %= parsePower()
                else -> return value
            }
        }
    }

    private fun parsePower(): Double {
        var value = parseUnary()
        if (match('^')) value = Math.pow(value, parsePower())
        return value
    }

    private fun parseUnary(): Double {
        skipSpaces()
        if (match('+')) return parseUnary()
        if (match('-')) return -parseUnary()
        var value = parsePrimary()
        while (match('!')) value = factorial(value)
        return value
    }

    private fun parsePrimary(): Double {
        skipSpaces()
        if (match('(')) {
            val value = parseAddSub()
            if (!match(')')) error("Missing )")
            return value
        }
        val start = index
        while (index < text.length && (text[index].isDigit() || text[index] == '.')) index++
        if (start != index) return text.substring(start, index).toDouble()
        if (index < text.length && (text[index] == 'π' || text[index] == 'e')) return if (text[index++] == 'π') PI else Math.E
        val nameStart = index
        while (index < text.length && text[index].isLetter()) index++
        if (nameStart == index) error("Expected number")
        val name = text.substring(nameStart, index)
        if (!match('(')) error("Missing (")
        val arg = parseAddSub()
        if (!match(')')) error("Missing )")
        return when (name) {
            "sin" -> if (degree) sin(Math.toRadians(arg)) else sin(arg)
            "cos" -> if (degree) cos(Math.toRadians(arg)) else cos(arg)
            "tan" -> if (degree) tan(Math.toRadians(arg)) else tan(arg)
            "asin" -> {
                val v = asin(arg); if (degree) Math.toDegrees(v) else v
            }
            "acos" -> {
                val v = acos(arg); if (degree) Math.toDegrees(v) else v
            }
            "atan" -> {
                val v = atan(arg); if (degree) Math.toDegrees(v) else v
            }
            "log" -> log10(arg)
            "ln" -> ln(arg)
            "sqrt" -> sqrt(arg)
            "abs" -> abs(arg)
            "inv" -> 1.0 / arg
            else -> error("Unknown function")
        }
    }

    private fun match(c: Char): Boolean {
        if (index < text.length && text[index] == c) { index++; return true }
        return false
    }
    private fun skipSpaces() { while (index < text.length && text[index].isWhitespace()) index++ }
}

private fun factorial(x: Double): Double {
    if (x < 0 || x != floor(x) || x > 170) error("Factorial")
    var result = 1.0
    var i = 2L
    while (i <= x.toLong()) { result *= i; i++ }
    return result
}

