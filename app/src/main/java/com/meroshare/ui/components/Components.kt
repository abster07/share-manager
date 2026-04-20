package com.meroshare.ui.components

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meroshare.data.model.AccountResult
import com.meroshare.data.model.ResultStatus
import com.meroshare.ui.theme.*

@Composable
fun StatusBadge(status: ResultStatus) {
    val (bg, fg, icon, label) = when (status) {
        is ResultStatus.Pending  -> Quadruple(Navy700, Color(0xFF94A3B8), Icons.Default.HourglassEmpty, "Pending")
        is ResultStatus.Loading  -> Quadruple(Navy700, Amber400, Icons.Default.Sync, "Checking…")
        is ResultStatus.Allotted -> Quadruple(Color(0xFF064E3B), Green400, Icons.Default.CheckCircle, "Allotted ✓")
        is ResultStatus.NotAllotted -> Quadruple(Color(0xFF3B1F1F), Red400, Icons.Default.Cancel, "Not Allotted")
        is ResultStatus.Error    -> Quadruple(Color(0xFF3B2A0A), Amber400, Icons.Default.Warning, "Error")
    }

    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "rotation"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(14.dp)
        )
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        result.account.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (result.account.isForeignEmployment) {
                        Text(
                            "FE",
                            fontSize = 9.sp,
                            color = Gold400,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Navy600)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    result.account.boid,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                // Show message if allotted or not allotted
                val msg = when (val s = result.status) {
                    is ResultStatus.Allotted    -> "Quantity: ${s.quantity}"
                    is ResultStatus.NotAllotted -> if (s.message.length > 50) s.message.take(50) + "…" else s.message
                    is ResultStatus.Error       -> s.message
                    else -> null
                }
                if (msg != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(msg, fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }
            StatusBadge(result.status)
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = OnSurface
        )
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
        }
    }
}

@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Gold400,
            contentColor = Navy900,
            disabledContainerColor = Navy700,
            disabledContentColor = Color(0xFF64748B)
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

@Composable
fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Navy700)
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

// Helper data class
data class Quadruple<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
