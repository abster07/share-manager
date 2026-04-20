package com.share_manager.ui.accounts

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.share_manager.data.model.Account
import com.share_manager.ui.components.GoldButton
import com.share_manager.ui.components.SectionHeader
import com.share_manager.ui.theme.*

@Composable
fun AccountsScreen(viewModel: AccountsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var deleteTarget by remember { mutableStateOf<Account?>(null) }

    LaunchedEffect(state.successMessage, state.errorMessage) {
        if (state.successMessage != null || state.errorMessage != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = "Accounts",
                    subtitle = "${state.accounts.size} account${if (state.accounts.size != 1) "s" else ""} saved"
                )
                GoldButton(
                    text = "Add",
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)) }
                )
            }

            Spacer(Modifier.height(16.dp))

            // Snackbar message
            AnimatedVisibility(visible = state.successMessage != null || state.errorMessage != null) {
                val isError = state.errorMessage != null
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isError) Color(0xFF3B1F1F) else Color(0xFF064E3B))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                        null,
                        tint = if (isError) Red400 else Green400,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        state.errorMessage ?: state.successMessage ?: "",
                        color = if (isError) Red400 else Green400,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            if (state.accounts.isEmpty()) {
                EmptyAccountsPlaceholder { showAddDialog = true }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.accounts, key = { it.id }) { account ->
                        AccountCard(
                            account = account,
                            onEdit = { editingAccount = account },
                            onDelete = { deleteTarget = account }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Add/Edit Dialog
    if (showAddDialog || editingAccount != null) {
        AccountDialog(
            existing = editingAccount,
            onDismiss = { showAddDialog = false; editingAccount = null },
            onSave = { account ->
                if (editingAccount != null) viewModel.updateAccount(account)
                else viewModel.saveAccount(account)
                showAddDialog = false
                editingAccount = null
            }
        )
    }

    // Delete confirmation
    deleteTarget?.let { acc ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = Surface,
            title = { Text("Delete Account", color = OnSurface) },
            text = { Text("Remove \"${acc.name}\"? This cannot be undone.", color = Color(0xFF94A3B8)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAccount(acc); deleteTarget = null }) {
                    Text("Delete", color = Red400)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }
}

@Composable
private fun AccountCard(account: Account, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Outline, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Navy600),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    account.name.take(1).uppercase(),
                    color = Gold400,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        account.name,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface,
                        fontSize = 15.sp
                    )
                    if (account.isForeignEmployment) {
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
                    account.boid,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace
                )
                if (account.crn.isNotEmpty()) {
                    Text("CRN: ${account.crn}", fontSize = 11.sp, color = Color(0xFF475569))
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, null, tint = Gold400, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = Red400, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyAccountsPlaceholder(onAdd: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.AccountBox,
                null,
                tint = Color(0xFF2D3F5E),
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("No Accounts Yet", fontWeight = FontWeight.Bold, color = Color(0xFF475569), fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text("Add your BOID accounts to check results", color = Color(0xFF334155), fontSize = 13.sp)
            Spacer(Modifier.height(24.dp))
            GoldButton(text = "Add First Account", onClick = onAdd)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDialog(
    existing: Account?,
    onDismiss: () -> Unit,
    onSave: (Account) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var boid by remember { mutableStateOf(existing?.boid ?: "") }
    var crn by remember { mutableStateOf(existing?.crn ?: "") }
    var pin by remember { mutableStateOf(existing?.transactionPin ?: "") }
    var isFE by remember { mutableStateOf(existing?.isForeignEmployment ?: false) }
    var pinVisible by remember { mutableStateOf(false) }

    val boidError = boid.isNotEmpty() && boid.length != 16

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Navy800,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                if (existing != null) "Edit Account" else "Add Account",
                color = OnSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name / Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogFieldColors()
                )

                // BOID
                OutlinedTextField(
                    value = boid,
                    onValueChange = { v -> if (v.length <= 16) boid = v.filter { c -> c.isDigit() } },
                    label = { Text("BOID (16 digits) *") },
                    singleLine = true,
                    isError = boidError,
                    supportingText = {
                        if (boidError) Text("BOID must be exactly 16 digits", color = Red400)
                        else Text("${boid.length}/16", color = Color(0xFF64748B))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogFieldColors()
                )

                // CRN (optional)
                OutlinedTextField(
                    value = crn,
                    onValueChange = { crn = it },
                    label = { Text("CRN (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogFieldColors()
                )

                // Transaction PIN (optional, encrypted)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("Transaction PIN (optional)") },
                    singleLine = true,
                    visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { pinVisible = !pinVisible }) {
                            Icon(
                                if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null,
                                tint = Color(0xFF64748B)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogFieldColors()
                )

                // Foreign Employment Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Navy700)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Foreign Employment", color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("For FE-specific IPOs only", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                    Switch(
                        checked = isFE,
                        onCheckedChange = { isFE = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Navy900,
                            checkedTrackColor = Gold400
                        )
                    )
                }
            }
        },
        confirmButton = {
            GoldButton(
                text = if (existing != null) "Update" else "Save",
                enabled = name.isNotBlank() && boid.length == 16,
                onClick = {
                    onSave(
                        (existing ?: Account()).copy(
                            name = name.trim(),
                            boid = boid.trim(),
                            crn = crn.trim(),
                            transactionPin = pin,
                            isForeignEmployment = isFE
                        )
                    )
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF64748B))
            }
        }
    )
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Gold400,
    unfocusedBorderColor = Outline,
    focusedLabelColor = Gold400,
    unfocusedLabelColor = Color(0xFF64748B),
    focusedTextColor = OnSurface,
    unfocusedTextColor = OnSurface,
    cursorColor = Gold400,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent
)

// Extension to allow copy on base Account with id=0 default
private fun Account() = Account(id = 0, name = "", boid = "")
