package com.share_manager.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.share_manager.data.model.AccountResult
import com.share_manager.data.model.ResultStatus
import com.share_manager.ui.theme.*

// ── Status badge ──────────────────────────────────────────────────────────────

@Composable
fun StatusBadge(status: ResultStatus) {
    data class BadgeStyle(
        val bg   : Color,
        val fg   : Color,
        val icon : ImageVector,
        val label: String
    )

    val style = when (status) {
        is ResultStatus.Pending     -> BadgeStyle(Navy700,          Color(0xFF94A3B8), Icons.Default.HourglassEmpty, "Pending")
        is ResultStatus.Loading     -> BadgeStyle(Navy700,          Amber400,          Icons.Default.Sync,           "Checking…")
        is ResultStatus.Allotted    -> BadgeStyle(Color(0xFF064E3B), Green400,         Icons.Default.CheckCircle,    "Allotted ✓")
        is ResultStatus.NotAllotted -> BadgeStyle(Color(0xFF3B1F1F), Red400,           Icons.Default.Cancel,         "Not Allotted")
        is ResultStatus.Error       -> BadgeStyle(Color(0xFF3B2A0A), Amber400,         Icons.Default.Warning,        "Error")
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(style.bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // BUG FIX: Previously the infinite rotation transition was created
        // unconditionally (for every status), wasting resources on Pending /
        // Allotted / etc.  Now the spinning animation is only active when the
        // status is Loading; all other statuses draw a static icon.
        if (status is ResultStatus.Loading) {
            val infiniteTransition = rememberInfiniteTransition(label = "spin")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue  = 360f,
                animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
                label        = "rotation"
            )
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                tint     = style.fg,
                modifier = Modifier
                    .size(14.dp)
                    .rotate(rotation)
            )
        } else {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                tint     = style.fg,
                modifier = Modifier.size(14.dp)
            )
        }

        Text(style.label, color = style.fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Account result card ───────────────────────────────────────────────────────

@Composable
fun AccountResultCard(result: AccountResult) {
    val cardBorder = when (result.status) {
        is ResultStatus.Allotted    -> Green400.copy(alpha = 0.5f)
        is ResultStatus.NotAllotted -> Red400.copy(alpha = 0.3f)
        is ResultStatus.Error       -> Amber400.copy(alpha = 0.3f)
        else                        -> Outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp)),
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text       = result.account.name,
                        style      = MaterialTheme.typography.titleSmall,
                        color      = OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (result.account.isForeignEmployment) {
                        Text(
                            text     = "FE",
                            fontSize = 9.sp,
                            color    = Gold400,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Navy600)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text       = result.account.boid,
                    fontSize   = 11.sp,
                    color      = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace
                )

                val msg = when (val s = result.status) {
                    is ResultStatus.Allotted    -> "Quantity: ${s.quantity}"
                    is ResultStatus.NotAllotted -> s.message.take(60).let { if (s.message.length > 60) "$it…" else it }
                    is ResultStatus.Error       -> s.message
                    else                        -> null
                }
                if (msg != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(msg, fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }

            Spacer(Modifier.width(8.dp))
            StatusBadge(result.status)
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Column {
        Text(
            text  = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = OnSurface
        )
        if (subtitle != null) {
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
            )
        }
    }
}

// ── Gold primary button ───────────────────────────────────────────────────────

@Composable
fun GoldButton(
    text    : String,
    onClick : () -> Unit,
    modifier: Modifier = Modifier,
    enabled : Boolean = true,
    icon    : @Composable (() -> Unit)? = null
) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = modifier.height(48.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = Gold400,
            contentColor           = Navy900,
            disabledContainerColor = Navy700,
            disabledContentColor   = Color(0xFF64748B)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontWeight = FontWeight.Bold)
    }
}

// ── Stat summary card ─────────────────────────────────────────────────────────

@Composable
fun StatCard(
    label   : String,
    value   : String,
    color   : Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = Navy700)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = color)
            Text(label, fontSize = 11.sp, color = Color(0xFF94A3B8))
        }
    }
}
