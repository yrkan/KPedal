package io.github.kpedal.ui.screens.help

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kpedal.R
import io.github.kpedal.ui.theme.Theme

/**
 * Screen explaining why and how to sync to cloud.
 * Optimized for Karoo 3 small display (480x800).
 */
@Composable
fun CloudSyncHelpScreen(
    onBack: () -> Unit
) {
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
                text = stringResource(R.string.cloud_sync_help_title),
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
                .padding(horizontal = 12.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Hero section with cloud icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Theme.colors.optimal.copy(alpha = 0.08f))
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "☁",
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.cloud_sync_free),
                        color = Theme.colors.optimal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Why sync? - Benefits grid
            SectionTitle(stringResource(R.string.cloud_sync_why_title))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BenefitChip(
                    icon = "📱",
                    text = stringResource(R.string.cloud_sync_why_1),
                    modifier = Modifier.weight(1f)
                )
                BenefitChip(
                    icon = "📊",
                    text = stringResource(R.string.cloud_sync_why_2),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BenefitChip(
                    icon = "🏆",
                    text = stringResource(R.string.cloud_sync_why_3),
                    modifier = Modifier.weight(1f)
                )
                BenefitChip(
                    icon = "🔒",
                    text = stringResource(R.string.cloud_sync_why_4),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // How to link? - Steps
            SectionTitle(stringResource(R.string.cloud_sync_how_title))
            Spacer(modifier = Modifier.height(10.dp))

            StepItem(1, stringResource(R.string.cloud_sync_how_1), Theme.colors.optimal)
            StepConnector()
            StepItem(2, stringResource(R.string.cloud_sync_how_2), Theme.colors.optimal)
            StepConnector()
            StepItemWithLink(3, Theme.colors.attention)
            StepConnector()
            StepItem(4, stringResource(R.string.cloud_sync_how_4), Theme.colors.attention)
            StepConnector()
            StepItem(5, stringResource(R.string.cloud_sync_how_5), Theme.colors.optimal, isDone = true)

            Spacer(modifier = Modifier.height(20.dp))

            // What syncs?
            SectionTitle(stringResource(R.string.cloud_sync_what_syncs))
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Theme.colors.surface)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SyncItem("🚴", stringResource(R.string.cloud_sync_syncs_rides))
                SyncItem("⚡", stringResource(R.string.cloud_sync_syncs_metrics))
                SyncItem("🎯", stringResource(R.string.cloud_sync_syncs_drills))
                SyncItem("⚙", stringResource(R.string.cloud_sync_syncs_settings))

                Spacer(modifier = Modifier.height(4.dp))

                // Privacy note
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Theme.colors.attention.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔐",
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.cloud_sync_not_syncs),
                        color = Theme.colors.attention,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
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
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        color = Theme.colors.dim,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun BenefitChip(
    icon: String,
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Theme.colors.surface)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            color = Theme.colors.dim,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            lineHeight = 13.sp,
            maxLines = 2
        )
    }
}

@Composable
private fun StepItem(
    number: Int,
    text: String,
    color: Color,
    isDone: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Number circle
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isDone) color else color.copy(alpha = 0.15f))
                .then(
                    if (!isDone) Modifier.border(1.dp, color, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isDone) "✓" else number.toString(),
                color = if (isDone) Theme.colors.background else color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            color = Theme.colors.text,
            fontSize = 12.sp,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun StepConnector() {
    Box(
        modifier = Modifier
            .padding(start = 11.dp)
            .width(2.dp)
            .height(12.dp)
            .background(Theme.colors.divider)
    )
}

@Composable
private fun SyncItem(icon: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = icon,
            fontSize = 14.sp,
            modifier = Modifier.width(24.dp)
        )
        Text(
            text = text,
            color = Theme.colors.dim,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun StepItemWithLink(
    number: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Number circle
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
                .border(1.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.cloud_sync_how_3_prefix),
                color = Theme.colors.text,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "LINK.KPEDAL.COM",
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(color.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
