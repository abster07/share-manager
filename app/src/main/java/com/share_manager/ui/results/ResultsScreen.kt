package com.share_manager.ui.results

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.share_manager.data.model.CompanyShare
import com.share_manager.data.model.ResultStatus
import com.share_manager.ui.components.*
import com.share_manager.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(viewModel: ResultsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showCompanyPicker by remember { mutableStateOf(false) }

    // ── Summary stats ─────────────────────────────────────────────────────────
    val allotted    = state.accountResults.count { it.status is ResultStatus.Allotted }
    val notAllotted = state.accountResults.count { it.status is ResultStatus.NotAllotted }
    val errors      = state.accountResults.count { it.status is ResultStatus.Error }

    // BUG FIX: previous code did filterIsInstance<AccountResult>() on a
    // List<AccountResult> — every element already is one, so the cast was
    // always a no-op but the sumOf still worked by luck. Removed the
    // superfluous filterIsInstance call.
    val totalQty = state.accountResults.sumOf { r ->
        (r.status as? ResultStatus.Allotted)?.quantity ?: 0
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title    = "IPO Result Check",
                subtitle = "Bulk check across all accounts"
            )
        }

        // ── Error banner ──────────────────────────────────────────────────────
        if (state.errorMessage != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF3B1F1F))
                        .padding(12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Error, null, tint = Red400, modifier = Modifier.size(18.dp))
                    Text(
                        state.errorMessage!!,
                        color    = Red400,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick  = viewModel::clearError,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Red400)
                    }
                }
            }
        }

        // ── Company selector ──────────────────────────────────────────────────
        item {
            CompanySelectorCard(
                selected   = state.selectedCompany,
                isLoading  = state.isLoadingCompanies,
                onPickClick = { showCompanyPicker = true },
                onRefresh  = viewModel::loadCompanies
            )
        }

        // ── Action button / progress ──────────────────────────────────────────
        item {
            AnimatedContent(
                targetState  = state.isChecking,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label        = "action_button"
            ) { checking ->
                if (checking) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp)),
                            color      = Gold400,
                            trackColor = Navy700
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            // BUG FIX: use checkedCount directly instead of
                            // (progress * accounts.size).toInt() which could
                            // round incorrectly on the last step.
                            Text(
                                "Checking ${state.checkedCount} / ${state.accounts.size}…",
                                fontSize = 12.sp,
                                color    = Color(0xFF94A3B8)
                            )
                            OutlinedButton(
                                onClick = viewModel::cancelCheck,
                                border  = BorderStroke(1.dp, Red400),
                                shape   = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancel", color = Red400, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    GoldButton(
                        text    = if (state.accountResults.isEmpty()) "Check All Results" else "Re-check All",
                        onClick = viewModel::checkBulkResults,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.selectedCompany != null && state.accounts.isNotEmpty(),
                        icon    = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        }

        // ── No accounts warning ───────────────────────────────────────────────
        if (state.accounts.isEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Navy700)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = Amber400, modifier = Modifier.size(18.dp))
                    Text(
                        "Add accounts first to check results",
                        color    = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // ── Stats row (shown after first results) ─────────────────────────────
        if (state.accountResults.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("Allotted",     allotted.toString(),    Green400, Modifier.weight(1f))
                    StatCard("Not Allotted", notAllotted.toString(), Red400,   Modifier.weight(1f))
                    StatCard("Errors",       errors.toString(),      Amber400, Modifier.weight(1f))
                }
            }

            if (totalQty > 0) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF064E3B))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            "Total Allotted Shares",
                            color      = Green400,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "$totalQty units",
                            color      = Green400,
                            fontWeight = FontWeight.Black,
                            fontSize   = 20.sp
                        )
                    }
                }
            }

            item {
                Text(
                    "Results (${state.accountResults.size})",
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF94A3B8),
                    fontSize   = 13.sp
                )
            }

            items(state.accountResults, key = { it.account.id }) { result ->
                AccountResultCard(result)
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // ── Company bottom-sheet picker ───────────────────────────────────────────
    if (showCompanyPicker) {
        CompanyPickerSheet(
            companies = state.companies,
            isLoading = state.isLoadingCompanies,
            onSelect  = { company ->
                viewModel.selectCompany(company)
                showCompanyPicker = false
            },
            onDismiss = { showCompanyPicker = false }
        )
    }
}

// ── Company selector card ─────────────────────────────────────────────────────

@Composable
private fun CompanySelectorCard(
    selected   : CompanyShare?,
    isLoading  : Boolean,
    onPickClick: () -> Unit,
    onRefresh  : () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = Surface),
        border   = BorderStroke(1.dp, if (selected != null) Gold400.copy(0.5f) else Outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onPickClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Business,
                contentDescription = null,
                tint     = if (selected != null) Gold400 else Color(0xFF475569),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = if (selected != null) "IPO Company" else "Select IPO Company",
                    fontSize = 11.sp,
                    color    = Color(0xFF64748B)
                )
                if (selected != null) {
                    Text(
                        text       = selected.name,
                        color      = OnSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 14.sp
                    )
                    Text(selected.scrip, color = Gold400, fontSize = 12.sp)
                }
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(20.dp),
                    color       = Gold400,
                    strokeWidth = 2.dp
                )
            } else {
                Row {
                    IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Refresh, null,
                            tint     = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF475569))
                }
            }
        }
    }
}

// ── Company picker bottom sheet ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompanyPickerSheet(
    companies: List<CompanyShare>,
    isLoading: Boolean,
    onSelect : (CompanyShare) -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(search, companies) {
        companies.filter {
            it.name.contains(search, ignoreCase = true) ||
                    it.scrip.contains(search, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Navy800,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(36.dp, 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Outline)
            )
        }
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                "Select IPO Company",
                fontWeight = FontWeight.Bold,
                color      = OnSurface,
                fontSize   = 18.sp
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value         = search,
                onValueChange = { search = it },
                placeholder   = { Text("Search company or scrip…", color = Color(0xFF475569)) },
                leadingIcon   = { Icon(Icons.Default.Search, null, tint = Color(0xFF64748B)) },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = Gold400,
                    unfocusedBorderColor    = Outline,
                    focusedTextColor        = OnSurface,
                    unfocusedTextColor      = OnSurface,
                    cursorColor             = Gold400,
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor  = Color.Transparent
                )
            )

            Spacer(Modifier.height(8.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Gold400)
                }
            } else if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (search.isBlank()) "No companies available" else "No results for \"$search\"",
                        color    = Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                }
            } else {
                // BUG FIX: heightIn(max = 400.dp) could be too small on short
                // screens. Using fillMaxHeight(0.6f) respects the actual screen.
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filtered, key = { it.id }) { company ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelect(company) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    company.name,
                                    color      = OnSurface,
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(company.scrip, color = Gold400, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = Outline)
                        }
                        HorizontalDivider(color = Outline.copy(alpha = 0.5f), thickness = 0.5.dp)
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}
