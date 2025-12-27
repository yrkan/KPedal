package io.github.kpedal.ui.screens.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kpedal.R
import io.github.kpedal.ui.theme.Theme

/**
 * Compact help screen optimized for Karoo 3 small display.
 */
@Composable
fun HelpScreen(
    onBack: () -> Unit
) {
    var expandedSection by remember { mutableStateOf<String?>("metrics") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(top = 24.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                color = Theme.colors.dim,
                fontSize = 16.sp,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onBack() }
                    .padding(end = 8.dp)
            )
            Text(
                text = stringResource(R.string.help),
                color = Theme.colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Divider()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Metrics
            SectionRow(
                title = stringResource(R.string.section_metrics),
                isExpanded = expandedSection == "metrics",
                onClick = { expandedSection = if (expandedSection == "metrics") null else "metrics" }
            )
            if (expandedSection == "metrics") {
                MetricsContent()
            }
            Divider()

            // Colors
            SectionRow(
                title = stringResource(R.string.section_colors),
                isExpanded = expandedSection == "colors",
                onClick = { expandedSection = if (expandedSection == "colors") null else "colors" }
            )
            if (expandedSection == "colors") {
                ColorsContent()
            }
            Divider()

            // Setup
            SectionRow(
                title = stringResource(R.string.section_setup),
                isExpanded = expandedSection == "setup",
                onClick = { expandedSection = if (expandedSection == "setup") null else "setup" }
            )
            if (expandedSection == "setup") {
                SetupContent()
            }
            Divider()

            // Tips
            SectionRow(
                title = stringResource(R.string.section_tips),
                isExpanded = expandedSection == "tips",
                onClick = { expandedSection = if (expandedSection == "tips") null else "tips" }
            )
            if (expandedSection == "tips") {
                TipsContent()
            }
            Divider()

            // FAQ
            SectionRow(
                title = stringResource(R.string.section_faq),
                isExpanded = expandedSection == "faq",
                onClick = { expandedSection = if (expandedSection == "faq") null else "faq" }
            )
            if (expandedSection == "faq") {
                FAQContent()
            }
            Divider()

            // Troubleshooting
            SectionRow(
                title = stringResource(R.string.section_troubleshooting),
                isExpanded = expandedSection == "trouble",
                onClick = { expandedSection = if (expandedSection == "trouble") null else "trouble" }
            )
            if (expandedSection == "trouble") {
                TroubleshootingContent()
            }
            Divider()

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Theme.colors.divider)
    )
}

@Composable
private fun SectionRow(
    title: String,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = Theme.colors.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = if (isExpanded) "−" else "+",
            color = if (isExpanded) Theme.colors.optimal else Theme.colors.dim,
            fontSize = 16.sp
        )
    }
}

// ========== METRICS ==========

@Composable
private fun MetricsContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.colors.surface)
            .padding(12.dp)
    ) {
        MetricItem(
            name = stringResource(R.string.balance_option),
            value = stringResource(R.string.help_balance_range),
            color = Theme.colors.attention,
            desc = stringResource(R.string.help_balance_desc),
            note = stringResource(R.string.help_balance_note)
        )
        Spacer(modifier = Modifier.height(12.dp))
        MetricItem(
            name = "TE",
            value = stringResource(R.string.help_te_range),
            color = Theme.colors.optimal,
            desc = stringResource(R.string.help_te_desc),
            note = stringResource(R.string.help_te_note)
        )
        Spacer(modifier = Modifier.height(12.dp))
        MetricItem(
            name = "PS",
            value = stringResource(R.string.help_ps_range),
            color = Theme.colors.problem,
            desc = stringResource(R.string.help_ps_desc),
            note = stringResource(R.string.help_ps_note)
        )
    }
}

@Composable
private fun MetricItem(
    name: String,
    value: String,
    color: Color,
    desc: String,
    note: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .width(3.dp)
                .height(14.dp)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    color = Theme.colors.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = desc,
                color = Theme.colors.dim,
                fontSize = 11.sp,
                lineHeight = 13.sp
            )
            Text(
                text = note,
                color = Theme.colors.muted,
                fontSize = 10.sp,
                lineHeight = 12.sp
            )
        }
    }
}

// ========== COLORS ==========

@Composable
private fun ColorsContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.colors.surface)
            .padding(12.dp)
    ) {
        ColorItem(Theme.colors.text, stringResource(R.string.color_white), stringResource(R.string.color_white_meaning))
        Spacer(modifier = Modifier.height(8.dp))
        ColorItem(Theme.colors.optimal, stringResource(R.string.color_green), stringResource(R.string.color_green_meaning))
        Spacer(modifier = Modifier.height(8.dp))
        ColorItem(Theme.colors.attention, stringResource(R.string.color_yellow), stringResource(R.string.color_yellow_meaning))
        Spacer(modifier = Modifier.height(8.dp))
        ColorItem(Theme.colors.problem, stringResource(R.string.color_red), stringResource(R.string.color_red_meaning))
    }
}

@Composable
private fun ColorItem(color: Color, name: String, meaning: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = name,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(50.dp)
        )
        Text(
            text = meaning,
            color = Theme.colors.dim,
            fontSize = 11.sp
        )
    }
}

// ========== SETUP ==========

@Composable
private fun SetupContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.colors.surface)
            .padding(12.dp)
    ) {
        SetupStep(1, stringResource(R.string.setup_step1))
        SetupStep(2, stringResource(R.string.setup_step2))
        SetupStep(3, stringResource(R.string.setup_step3))
        SetupStep(4, stringResource(R.string.setup_step4))
        SetupStep(5, stringResource(R.string.setup_step5))

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.setup_requires),
            color = Theme.colors.muted,
            fontSize = 10.sp,
            lineHeight = 12.sp
        )
    }
}

@Composable
private fun SetupStep(num: Int, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$num.",
            color = Theme.colors.optimal,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(16.dp)
        )
        Text(
            text = text,
            color = Theme.colors.text,
            fontSize = 11.sp
        )
    }
}

// ========== TIPS ==========

@Composable
private fun TipsContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.colors.surface)
            .padding(12.dp)
    ) {
        TipGroup(stringResource(R.string.balance_option), Theme.colors.attention, listOf(
            stringResource(R.string.tips_balance_1),
            stringResource(R.string.tips_balance_2),
            stringResource(R.string.tips_balance_3)
        ))
        Spacer(modifier = Modifier.height(10.dp))
        TipGroup("TE", Theme.colors.optimal, listOf(
            stringResource(R.string.tips_te_1),
            stringResource(R.string.tips_te_2),
            stringResource(R.string.tips_te_3)
        ))
        Spacer(modifier = Modifier.height(10.dp))
        TipGroup("PS", Theme.colors.problem, listOf(
            stringResource(R.string.tips_ps_1),
            stringResource(R.string.tips_ps_2),
            stringResource(R.string.tips_ps_3)
        ))
    }
}

@Composable
private fun TipGroup(title: String, color: Color, tips: List<String>) {
    Text(
        text = title,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
    )
    tips.forEach { tip ->
        Row(modifier = Modifier.padding(vertical = 2.dp)) {
            Text(
                text = "•",
                color = Theme.colors.dim,
                fontSize = 10.sp,
                modifier = Modifier.padding(end = 6.dp)
            )
            Text(
                text = tip,
                color = Theme.colors.text,
                fontSize = 10.sp
            )
        }
    }
}

// ========== FAQ ==========

@Composable
private fun FAQContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.colors.surface)
            .padding(12.dp)
    ) {
        FAQItem(
            q = stringResource(R.string.faq_te85_q),
            a = stringResource(R.string.faq_te85_a)
        )
        Spacer(modifier = Modifier.height(10.dp))
        FAQItem(
            q = stringResource(R.string.faq_balance_q),
            a = stringResource(R.string.faq_balance_a)
        )
        Spacer(modifier = Modifier.height(10.dp))
        FAQItem(
            q = stringResource(R.string.faq_jump_q),
            a = stringResource(R.string.faq_jump_a)
        )
        Spacer(modifier = Modifier.height(10.dp))
        FAQItem(
            q = stringResource(R.string.faq_compare_q),
            a = stringResource(R.string.faq_compare_a)
        )
    }
}

@Composable
private fun FAQItem(q: String, a: String) {
    Column {
        Text(
            text = stringResource(R.string.question_prefix, q),
            color = Theme.colors.text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 13.sp
        )
        Text(
            text = a,
            color = Theme.colors.dim,
            fontSize = 10.sp,
            lineHeight = 12.sp
        )
    }
}

// ========== TROUBLESHOOTING ==========

@Composable
private fun TroubleshootingContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.colors.surface)
            .padding(12.dp)
    ) {
        TroubleItem(
            problem = stringResource(R.string.trouble_nodata),
            fix = stringResource(R.string.trouble_nodata_fix)
        )
        Spacer(modifier = Modifier.height(10.dp))
        TroubleItem(
            problem = stringResource(R.string.trouble_balance_only),
            fix = stringResource(R.string.trouble_balance_only_fix)
        )
        Spacer(modifier = Modifier.height(10.dp))
        TroubleItem(
            problem = stringResource(R.string.trouble_stuck),
            fix = stringResource(R.string.trouble_stuck_fix)
        )
        Spacer(modifier = Modifier.height(10.dp))
        TroubleItem(
            problem = stringResource(R.string.trouble_crash),
            fix = stringResource(R.string.trouble_crash_fix)
        )
    }
}

@Composable
private fun TroubleItem(problem: String, fix: String) {
    Column {
        Text(
            text = problem,
            color = Theme.colors.problem,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 13.sp
        )
        Text(
            text = fix,
            color = Theme.colors.dim,
            fontSize = 10.sp,
            lineHeight = 12.sp
        )
    }
}
