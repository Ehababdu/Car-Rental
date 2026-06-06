package com.example.accounting

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.*

// Styling helper tokens
val AccountingPrimary = Color(0xFF673AB7) // Indigo dark purple
val AccountingSurface = Color(0xFFF3E5F5) // Soft purple-grey background
val ProfitGreen = Color(0xFF2E7D32)
val DeficitRed = Color(0xFFC62828)

// ----------------------------------------------------------------------------------
// 1. MAIN ACCOUNTING DASHBOARD SCREEN
// ----------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountingDashboardScreen(
    viewModel: AccountingViewModel
) {
    var selectedSubTab by remember { mutableStateOf("treasury") }
    val notification by viewModel.uiNotification.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Quick Toast notifications handler
    LaunchedEffect(notification) {
        notification?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearNotification()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCFBFE))
    ) {
        // Accounting Sub-Tab Header Bar
        ScrollableTabRow(
            selectedTabIndex = when (selectedSubTab) {
                "treasury" -> 0
                "partners" -> 1
                "ledger_tree" -> 2
                "vouchers" -> 3
                "statements" -> 4
                else -> 0
            },
            containerColor = Color(0xFFEFEBEF),
            contentColor = AccountingPrimary,
            edgePadding = 8.dp
        ) {
            Tab(
                selected = selectedSubTab == "treasury",
                onClick = { selectedSubTab = "treasury" },
                text = { Text("الصناديق والبنوك", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedSubTab == "partners",
                onClick = { selectedSubTab = "partners" },
                text = { Text("العملاء والموردون", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Filled.People, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedSubTab == "ledger_tree",
                onClick = { selectedSubTab = "ledger_tree" },
                text = { Text("شجرة الحسابات", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Filled.AccountTree, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedSubTab == "vouchers",
                onClick = { selectedSubTab = "vouchers" },
                text = { Text("القيود والسندات", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Filled.Receipt, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedSubTab == "statements",
                onClick = { selectedSubTab = "statements" },
                text = { Text("كشف حساب", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Filled.Book, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        Divider(color = Color(0xFFE3E0E4), thickness = 1.dp)

        // Selected Subscreen Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedSubTab) {
                "treasury" -> TreasuryDashboardView(viewModel)
                "partners" -> HelpersLedgerView(viewModel)
                "ledger_tree" -> ChartOfAccountsView(viewModel)
                "vouchers" -> VouchersJournalView(viewModel)
                "statements" -> AccountStatementView(viewModel)
            }
        }
    }
}

// ----------------------------------------------------------------------------------
// 2. CHART OF ACCOUNTS SUB-SCREEN (الخصوم، الأصول، الإيرادات، المصروفات)
// ----------------------------------------------------------------------------------

@Composable
fun ChartOfAccountsView(viewModel: AccountingViewModel) {
    val accountsList by viewModel.accounts.collectAsState(initial = emptyList())
    var showAddAccountDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (accountsList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccountingPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "دليل وشجرة الحسابات المالية اللامركزية",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccountingPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(accountsList) { account ->
                    AccountGridItem(account)
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showAddAccountDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = AccountingPrimary,
            contentColor = Color.White
        ) {
            Icon(Icons.Filled.Add, contentDescription = "إضافة حساب")
        }

        if (showAddAccountDialog) {
            AddAccountDialog(
                accountsList = accountsList,
                onDismiss = { showAddAccountDialog = false },
                onAddClick = { code, name, type, parentId, isGroup ->
                    viewModel.addAccount(code, name, type, parentId, isGroup)
                    showAddAccountDialog = false
                }
            )
        }
    }
}

@Composable
fun AccountGridItem(account: Account) {
    val indent = if (account.parentId != null) (account.code.length - 1) * 12 else 0
    val cardBg = if (account.isGroup) Color(0xFFF3E8FF) else Color(0xFFFCFAFD)
    val textColor = if (account.isGroup) AccountingPrimary else Color(0xFF333333)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent.dp)
            .border(
                1.dp,
                if (account.isGroup) Color(0xFFE1BEE7) else Color(0xFFECE9ED),
                RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (account.isGroup) Icons.Filled.Folder else Icons.Filled.Description,
                    contentDescription = null,
                    tint = if (account.isGroup) AccountingPrimary else Color(0xFF7E57C2),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = account.name,
                        fontWeight = if (account.isGroup) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                        color = textColor
                    )
                    Text(
                        text = "رمز: ${account.code} • ${getTypeAr(account.type)}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
            if (!account.isGroup) {
                Text(
                    text = String.format(Locale.US, "%,.2f د.ل", account.balance),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = getBalanceColor(account.balance, account.type)
                )
            } else {
                Text(
                    text = "مجموعة فرعية",
                    fontSize = 11.sp,
                    color = AccountingPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ----------------------------------------------------------------------------------
// 3. JOURNAL VOUCHERS SCREEN (قيود اليومية المتزنة)
// ----------------------------------------------------------------------------------

@Composable
fun VouchersJournalView(viewModel: AccountingViewModel) {
    val vouchersList by viewModel.vouchers.collectAsState(initial = emptyList())
    var showVoucherEditor by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (vouchersList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.Balance,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "سجل قيود اليومية ودفتر اليومية خالي حالياً.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "سجل سندات الصرف والقبض والقيود المستندية",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccountingPrimary
                    )
                }
                items(vouchersList) { voucher ->
                    VoucherInvoiceCard(voucher, onDelete = { viewModel.deleteVoucher(voucher.id) })
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Add Post button
        ExtendedFloatingActionButton(
            onClick = { showVoucherEditor = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = AccountingPrimary,
            contentColor = Color.White
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("إضافة سند / قيد متزن", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        if (showVoucherEditor) {
            VoucherEditorDialog(
                viewModel = viewModel,
                onDismiss = { showVoucherEditor = false }
            )
        }
    }
}

@Composable
fun VoucherInvoiceCard(voucher: Voucher, onDelete: () -> Unit) {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    val displayDate = sdf.format(Date(voucher.date))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFEBE5EC), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "سند رقم: ${voucher.voucherNo}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = AccountingPrimary
                    )
                    Text(text = displayDate, fontSize = 11.sp, color = Color.Gray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%,.2f د.ل", voucher.totalAmount),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfitGreen
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = DeficitRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Divider(color = Color(0xFFF1EEF2), modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "البيان: ${voucher.description}",
                fontSize = 12.sp,
                color = Color.DarkGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ----------------------------------------------------------------------------------
// 4. ACCOUNT STATEMENT VIEW (كشف حساب الأستاذ العام لأي حساب)
// ----------------------------------------------------------------------------------

@Composable
fun AccountStatementView(viewModel: AccountingViewModel) {
    val accountsList by viewModel.accounts.collectAsState(initial = emptyList())
    val allLines by viewModel.voucherLines.collectAsState(initial = emptyList())
    val vouchersAll by viewModel.vouchers.collectAsState(initial = emptyList())

    val directAccounts = accountsList.filter { !it.isGroup }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var showDropdown by remember { mutableStateOf(false) }

    // Statement items generator
    val statementLines = remember(selectedAccount, allLines, vouchersAll) {
        if (selectedAccount == null) emptyList()
        else {
            var runningBal = 0.0
            val multiplier = when (selectedAccount!!.type) {
                AccountType.ASSET, AccountType.EXPENSE -> 1.0
                AccountType.LIABILITY, AccountType.EQUITY, AccountType.REVENUE -> -1.0
            }
            allLines.filter { it.accountId == selectedAccount!!.id }.reversed().map { line ->
                val v = vouchersAll.find { it.id == line.voucherId }
                runningBal += (line.debit - line.credit) * multiplier
                StatementRecord(
                    date = v?.date ?: 0L,
                    voucherNo = v?.voucherNo ?: "-",
                    memo = line.memo.ifEmpty { v?.description } ?: "",
                    debit = line.debit,
                    credit = line.credit,
                    runningBalance = runningBal
                )
            }.reversed()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "دفاتر الأستاذ العام وكشوفات الحسابات التفصيلية",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = AccountingPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Dropdown triggering Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDropdown = true }
                .border(1.dp, Color(0xFFECE2ED), RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedAccount?.let { "${it.name} (${it.code})" } ?: "اختر الحساب المالي للمعاينة...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedAccount != null) Color.Black else Color.Gray
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = AccountingPrimary)
            }
        }

        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false },
            modifier = Modifier.fillMaxWidth(0.85f).heightIn(max = 240.dp)
        ) {
            directAccounts.forEach { acc ->
                DropdownMenuItem(
                    text = { Text("${acc.name} (${acc.code})", fontSize = 13.sp) },
                    onClick = {
                        selectedAccount = acc
                        showDropdown = false
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedAccount == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("يرجى اختيار حساب من القائمة أعلى لإظهار كشف حساب المتكامل.", color = Color.Gray, fontSize = 13.sp)
            }
        } else if (statementLines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد قيود مسجلة لهذا الحساب تحت الدورة الحالية.", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEAE5EC), RoundedCornerShape(4.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("البيان والسند", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                        Text("مدين (+)", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text("دائن (-)", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text("الرصيد", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                    }
                }
                items(statementLines) { line ->
                    Divider(color = Color(0xFFF1EFF2))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(2f)) {
                            Text(text = line.memo, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(text = "سند ${line.voucherNo}", fontSize = 9.sp, color = Color.Gray)
                        }
                        Text(
                            text = if (line.debit > 0) String.format(Locale.US, "%,.1f", line.debit) else "-",
                            fontSize = 11.sp,
                            color = ProfitGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = if (line.credit > 0) String.format(Locale.US, "%,.1f", line.credit) else "-",
                            fontSize = 11.sp,
                            color = DeficitRed,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = String.format(Locale.US, "%,.1f", line.runningBalance),
                            fontSize = 11.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.2f),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

data class StatementRecord(
    val date: Long,
    val voucherNo: String,
    val memo: String,
    val debit: Double,
    val credit: Double,
    val runningBalance: Double
)

// ----------------------------------------------------------------------------------
// 5. CLIENT / SUPPLIER AUXILIARY LEDGERS VIEW (حسابات العملاء والموردين المساعدة)
// ----------------------------------------------------------------------------------

@Composable
fun HelpersLedgerView(viewModel: AccountingViewModel) {
    val partnersList by viewModel.partners.collectAsState(initial = emptyList())
    val accountsList by viewModel.accounts.collectAsState(initial = emptyList())
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "CUSTOMER", "SUPPLIER"
    
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedPartnerForTx by remember { mutableStateOf<AccountPartner?>(null) }

    // Computations
    val customersCount = partnersList.count { it.type == "CUSTOMER" }
    val suppliersCount = partnersList.count { it.type == "SUPPLIER" }
    val totalCustomerReceivables = partnersList.filter { it.type == "CUSTOMER" }.sumOf { it.balance }
    val totalSupplierPayables = partnersList.filter { it.type == "SUPPLIER" }.sumOf { it.balance }

    // Filtered lists
    val filteredPartners = partnersList.filter { partner ->
        val matchesSearch = partner.name.contains(searchQuery, ignoreCase = true) || partner.phone.contains(searchQuery)
        val matchesType = when (selectedFilter) {
            "CUSTOMER" -> partner.type == "CUSTOMER"
            "SUPPLIER" -> partner.type == "SUPPLIER"
            else -> true
        }
        matchesSearch && matchesType
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Metrics at the top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("رصيد العملاء (الزبائن)", fontSize = 11.sp, color = Color(0xFF3F51B5), fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format(Locale.US, "%,.2f د.ل", totalCustomerReceivables),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (totalCustomerReceivables >= 0) ProfitGreen else DeficitRed
                        )
                        Text("$customersCount عميل مسجل", fontSize = 9.sp, color = Color.Gray)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("مستحقات الموردين", fontSize = 11.sp, color = Color(0xFFEF6C00), fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format(Locale.US, "%,.2f د.ل", totalSupplierPayables),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (totalSupplierPayables >= 0) ProfitGreen else DeficitRed
                        )
                        Text("$suppliersCount مورد مسجل", fontSize = 9.sp, color = Color.Gray)
                    }
                }
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("بحث عن جهة بدليل شركاء الأعمال...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccountingPrimary,
                    unfocusedBorderColor = Color(0xFFE2DFE4)
                )
            )

            // Category choice chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("ALL", "كل الحسابات", Icons.Filled.People),
                    Triple("CUSTOMER", "الزبائن والعملاء", Icons.Filled.Person),
                    Triple("SUPPLIER", "الموردين والشركات", Icons.Filled.Business)
                ).forEach { (code, label, icon) ->
                    val isSelected = selectedFilter == code
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = code },
                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccountingPrimary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            if (filteredPartners.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) "لا توجد أي جهات مسجلة تحت هذا التصنيف." else "لا توجد نتائج بحث مطابقة.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredPartners) { partner ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFEEEAEF), RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (partner.type == "CUSTOMER") Color(0xFFE8EAF6) else Color(0xFFFFF3E0))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (partner.type == "CUSTOMER") "عميل" else "مورد",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (partner.type == "CUSTOMER") Color(0xFF3F51B5) else Color(0xFFEF6C00)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = partner.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text(text = partner.phone, fontSize = 11.sp, color = Color.Gray)
                                        if (partner.notes.isNotEmpty()) {
                                            Text(text = partner.notes, fontSize = 10.sp, color = Color.DarkGray)
                                        }
                                    }
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = String.format(Locale.US, "%,.2f د.ل", partner.balance),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (partner.balance >= 0) ProfitGreen else DeficitRed
                                        )
                                        Text(
                                            text = if (partner.balance >= 0) "رصيد له" else "رصيد عليه",
                                            fontSize = 9.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    
                                    // Cash posting quick action
                                    IconButton(
                                        onClick = { selectedPartnerForTx = partner },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF3E5F5))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Paid,
                                            contentDescription = "عملية مالية",
                                            tint = AccountingPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add partner fab
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = AccountingPrimary,
            contentColor = Color.White
        ) {
            Icon(Icons.Filled.PersonAdd, contentDescription = null)
        }

        if (showAddDialog) {
            AddPartnerDialog(
                onDismiss = { showAddDialog = false },
                onAddClick = { name, phone, type, notes ->
                    viewModel.addPartner(name, phone, type, notes)
                    showAddDialog = false
                }
            )
        }

        selectedPartnerForTx?.let { partner ->
            QuickPartnerTransactionDialog(
                partner = partner,
                accountsList = accountsList,
                onDismiss = { selectedPartnerForTx = null },
                onSave = { partnerId, amount, isPayment, safeOrBankId, notes ->
                    viewModel.recordPartnerTransaction(partnerId, amount, isPayment, safeOrBankId, notes)
                    selectedPartnerForTx = null
                }
            )
        }
    }
}

// ----------------------------------------------------------------------------------
// 6. CUSTOM TAILORING SIZING & MEASUREMENTS WORKSPACE SCREEN (التطريز والمقاسات)
// ----------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementsSizingScreen(
    viewModel: AccountingViewModel
) {
    val list by viewModel.measurements.collectAsState(initial = emptyList())
    val feedback by viewModel.uiNotification.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var showAddDiag by remember { mutableStateOf(false) }
    var selectedProfileForDetails by remember { mutableStateOf<TailorMeasurement?>(null) }

    LaunchedEffect(feedback) {
        feedback?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearNotification()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("قسيمة القياسات والتفصيل", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AccountingPrimary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDiag = true },
                containerColor = AccountingPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "إضافة مقاس")
            }
        }
    ) { paddingVals ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .background(Color(0xFFF9F7FA))
        ) {
            if (list.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.SquareFoot, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد بطاقات مقاسات وتطريز مسجلة حتى الآن.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(list) { profile ->
                        MeasurementCardItem(
                            profile = profile,
                            onDelete = { viewModel.removeMeasurement(profile) },
                            onSelect = { selectedProfileForDetails = profile }
                        )
                    }
                }
            }

            if (showAddDiag) {
                AddMeasurementDialog(
                    onDismiss = { showAddDiag = false },
                    onSave = { name, phone, neck, shoulder, chest, waist, sleeve, totalLen, notes ->
                        viewModel.addMeasurement(name, phone, neck, shoulder, chest, waist, sleeve, totalLen, notes)
                        showAddDiag = false
                    }
                )
            }

            if (selectedProfileForDetails != null) {
                MeasurementDetailsDialog(
                    profile = selectedProfileForDetails!!,
                    onDismiss = { selectedProfileForDetails = null }
                )
            }
        }
    }
}

@Composable
fun MeasurementCardItem(
    profile: TailorMeasurement,
    onDelete: () -> Unit,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .border(1.dp, Color(0xFFF1EEF4), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFEDE7F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.SquareFoot, contentDescription = null, tint = AccountingPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = profile.customerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "رقم: ${profile.phone}", fontSize = 11.sp, color = Color.Gray)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "رؤية القياسات ◀",
                    fontSize = 11.sp,
                    color = AccountingPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "مسح", tint = DeficitRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------------
// LOCALIZED HELPER PARSERS
// ----------------------------------------------------------------------------------

fun getTypeAr(type: AccountType): String {
    return when (type) {
        AccountType.ASSET -> "أصل"
        AccountType.LIABILITY -> "التزام"
        AccountType.EQUITY -> "حقوق ملكية"
        AccountType.REVENUE -> "إيراد"
        AccountType.EXPENSE -> "مصروف"
    }
}

fun getBalanceColor(balance: Double, type: AccountType): Color {
    val isNetPositive = when (type) {
        AccountType.ASSET, AccountType.EXPENSE -> balance >= 0
        AccountType.LIABILITY, AccountType.EQUITY, AccountType.REVENUE -> balance >= 0
    }
    return if (isNetPositive) ProfitGreen else DeficitRed
}

// ----------------------------------------------------------------------------------
// DIALOG MODALS FOR ENTRY SUB-SCREENS
// ----------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountDialog(
    accountsList: List<Account>,
    onDismiss: () -> Unit,
    onAddClick: (code: String, name: String, type: AccountType, parentId: Int?, isGroup: Boolean) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.ASSET) }
    var parentId by remember { mutableStateOf<Int?>(null) }
    var isGroup by remember { mutableStateOf(false) }

    var dropdownOpen by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("إضافة حساب مالي لشجرة الدفاتر", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AccountingPrimary)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("رمز الحساب (مثل 1104)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الحساب (الصندوق الفرعي)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Account Type Selector Row
                Text("نوع الحساب الرئيسي:", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AccountType.values().forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(getTypeAr(t), fontSize = 10.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Parent account dropdown selection
                Text("الحساب الأب (إن وجد):", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                val possibleParents = accountsList.filter { it.isGroup }
                OutlinedCard(
                    onClick = { dropdownOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = parentId?.let { pid -> possibleParents.find { it.id == pid }?.name } ?: "--- بدون أب (حساب رئيسي) ---",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                DropdownMenu(expanded = dropdownOpen, onDismissRequest = { dropdownOpen = false }) {
                    DropdownMenuItem(text = { Text("بلا (حساب مالي رئيسي)") }, onClick = { parentId = null; dropdownOpen = false })
                    possibleParents.forEach { p ->
                        DropdownMenuItem(text = { Text(p.name) }, onClick = { parentId = p.id; dropdownOpen = false })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Checkbox(checked = isGroup, onCheckedChange = { isGroup = it })
                    Text("حساب تجميعي (Group) لا يقبل قيود مباشرة", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Button(
                        onClick = { onAddClick(code, name, type, parentId, isGroup) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccountingPrimary)
                    ) {
                        Text("حفظ")
                    }
                }
            }
        }
    }
}

@Composable
fun AddPartnerDialog(
    onDismiss: () -> Unit,
    onAddClick: (name: String, phone: String, type: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("CUSTOMER") }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("تسجيل جهة ذمة (عميل / مورد)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AccountingPrimary)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الكامل للجهة") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    RadioButton(selected = type == "CUSTOMER", onClick = { type = "CUSTOMER" })
                    Text("عميل (ذمم مدينة)", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = type == "SUPPLIER", onClick = { type = "SUPPLIER" })
                    Text("مورد (ذمم دائنة)", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Button(
                        onClick = { onAddClick(name, phone, type, notes) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccountingPrimary)
                    ) {
                        Text("إضافة")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VoucherEditorDialog(
    viewModel: AccountingViewModel,
    onDismiss: () -> Unit
) {
    val accountsList by viewModel.accounts.collectAsState(initial = emptyList())
    val directAccounts = accountsList.filter { !it.isGroup }

    var voucherNo by remember { mutableStateOf("SND-${1000 + (Math.random() * 8999).toInt()}") }
    var description by remember { mutableStateOf("") }
    var voucherType by remember { mutableStateOf("JOURNAL") }

    // List of dynamic lines: accountId, debitString, creditString, memo
    val linesState = remember { mutableStateListOf(
        VoucherEntryLine(0, "", "", ""),
        VoucherEntryLine(0, "", "", "")
    ) }

    val totalDebits = linesState.sumOf { it.debit.toDoubleOrNull() ?: 0.0 }
    val totalCredits = linesState.sumOf { it.credit.toDoubleOrNull() ?: 0.0 }
    val difference = totalDebits - totalCredits
    val isBalanced = Math.abs(difference) < 0.01 && totalDebits > 0.0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text("ترحيل قيد يومية مستندي متكامل", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AccountingPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = voucherNo,
                        onValueChange = { voucherNo = it },
                        label = { Text("رقم القيد") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("البيان الرئيسي") },
                        modifier = Modifier.weight(1.5f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Table area for double entries
                Text("أطراف القيد المزدوج:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(linesState.size) { index ->
                        val entryLine = linesState[index]
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFBF9FC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("الطرف #${index + 1}", fontSize = 11.sp, color = AccountingPrimary, fontWeight = FontWeight.Bold)
                                    if (linesState.size > 2) {
                                        Text(
                                            "إزالة",
                                            fontSize = 11.sp,
                                            color = DeficitRed,
                                            modifier = Modifier.clickable { linesState.removeAt(index) }
                                        )
                                    }
                                }

                                var accountDropdownOpen by remember { mutableStateOf(false) }
                                OutlinedCard(
                                    onClick = { accountDropdownOpen = true },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = directAccounts.find { it.id == entryLine.accountId }?.let { "${it.name} (${it.code})" } ?: "اختر حساب محاسبي...",
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                                DropdownMenu(expanded = accountDropdownOpen, onDismissRequest = { accountDropdownOpen = false }) {
                                    directAccounts.forEach { acc ->
                                        DropdownMenuItem(
                                            text = { Text("${acc.name} (${acc.code})", fontSize = 12.sp) },
                                            onClick = {
                                                linesState[index] = entryLine.copy(accountId = acc.id)
                                                accountDropdownOpen = false
                                            }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = entryLine.debit,
                                        onValueChange = { linesState[index] = entryLine.copy(debit = it) },
                                        label = { Text("مدين (+)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = entryLine.credit,
                                        onValueChange = { linesState[index] = entryLine.copy(credit = it) },
                                        label = { Text("دائن (-)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = entryLine.memo,
                                        onValueChange = { linesState[index] = entryLine.copy(memo = it) },
                                        label = { Text("شرح فرعي") },
                                        modifier = Modifier.weight(1.5f)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        TextButton(
                            onClick = { linesState.add(VoucherEntryLine(0, "", "", "")) },
                            colors = ButtonDefaults.textButtonColors(contentColor = AccountingPrimary)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة سطر قيد إضافي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Divider(color = Color(0xFFF1EFF2), modifier = Modifier.padding(vertical = 8.dp))

                // Audit panel showing Debits vs Credits
                Surface(
                    color = if (isBalanced) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("إجمالي المدين: ${String.format(Locale.US, "%,.2f", totalDebits)}", fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                            Text("إجمالي الدائن: ${String.format(Locale.US, "%,.2f", totalCredits)}", fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isBalanced) "حالة القيد: ميزان متزن ✓" else "حالة القيد: غير متزن ✗ الفارق: ${String.format(Locale.US, "%,.2f", difference)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBalanced) ProfitGreen else DeficitRed
                            )
                            Icon(
                                imageVector = if (isBalanced) Icons.Filled.CheckCircle else Icons.Filled.Error,
                                contentDescription = null,
                                tint = if (isBalanced) ProfitGreen else DeficitRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Button(
                        enabled = isBalanced,
                        onClick = {
                            val rawLines = linesState.map {
                                it.accountId to Triple(
                                    it.debit.toDoubleOrNull() ?: 0.0,
                                    it.credit.toDoubleOrNull() ?: 0.0,
                                    it.memo
                                )
                            }
                            viewModel.postVoucher(voucherNo, voucherType, description, System.currentTimeMillis(), rawLines)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isBalanced) ProfitGreen else AccountingPrimary)
                    ) {
                        Text("ترحيل القيد المزدوج")
                    }
                }
            }
        }
    }
}

data class VoucherEntryLine(
    val accountId: Int,
    val debit: String,
    val credit: String,
    val memo: String
)

@Composable
fun AddMeasurementDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, neck: Double, shoulder: Double, chest: Double, waist: Double, sleeve: Double, length: Double, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var neck by remember { mutableStateOf("") }
    var shoulder by remember { mutableStateOf("") }
    var chest by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    var sleeve by remember { mutableStateOf("") }
    var length by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "تسجيل بطاقة تصميم وقياسات جديدة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = AccountingPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم العميل") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("القياسات الفنية للتصميم (بالسنتيمتر):", fontSize = 11.sp, color = AccountingPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = neck, onValueChange = { neck = it }, label = { Text("الرقبة") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = shoulder, onValueChange = { shoulder = it }, label = { Text("الأكتاف") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = chest, onValueChange = { chest = it }, label = { Text("الصدر") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = waist, onValueChange = { waist = it }, label = { Text("الوسط") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = sleeve, onValueChange = { sleeve = it }, label = { Text("الكم") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = length, onValueChange = { length = it }, label = { Text("الطول") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("مواصفات وتطريزات خاصة") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Button(
                        onClick = {
                            onSave(
                                name,
                                phone,
                                neck.toDoubleOrNull() ?: 0.0,
                                shoulder.toDoubleOrNull() ?: 0.0,
                                chest.toDoubleOrNull() ?: 0.0,
                                waist.toDoubleOrNull() ?: 0.0,
                                sleeve.toDoubleOrNull() ?: 0.0,
                                length.toDoubleOrNull() ?: 0.0,
                                notes
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccountingPrimary)
                    ) {
                        Text("حفظ البطاقة")
                    }
                }
            }
        }
    }
}

@Composable
fun MeasurementDetailsDialog(
    profile: TailorMeasurement,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("تفاصيل بطاقة قياسات العميل", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccountingPrimary)
                Spacer(modifier = Modifier.height(14.dp))

                Text(profile.customerName, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text("هاتف: ${profile.phone}", fontSize = 11.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(16.dp))

                // Technical measurements grid
                TableMetrics(profile)

                if (profile.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("المواصفات المطلوبة والتطريز:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Surface(
                        color = Color(0xFFF9F7FA),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).border(1.dp, Color(0xFFEDEAF0), RoundedCornerShape(6.dp))
                    ) {
                        Text(profile.notes, fontSize = 12.sp, modifier = Modifier.padding(10.dp), color = Color.DarkGray)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccountingPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إغلاق البطاقة")
                }
            }
        }
    }
}

@Composable
fun TableMetrics(profile: TailorMeasurement) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFEDE8F1), RoundedCornerShape(8.dp))
            .background(Color(0xFFFCFAFD))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            MetricBadge(label = "الرقبة", value = "${profile.neck} سم")
            Divider(modifier = Modifier.width(1.dp).height(24.dp))
            MetricBadge(label = "الأكتاف", value = "${profile.shoulder} سم")
            Divider(modifier = Modifier.width(1.dp).height(24.dp))
            MetricBadge(label = "الصدر", value = "${profile.chest} سم")
        }
        Divider(color = Color(0xFFEDE8F1))
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            MetricBadge(label = "الوسط", value = "${profile.waist} سم")
            Divider(modifier = Modifier.width(1.dp).height(24.dp))
            MetricBadge(label = "الكم", value = "${profile.sleeve} سم")
            Divider(modifier = Modifier.width(1.dp).height(24.dp))
            MetricBadge(label = "الطول", value = "${profile.length} سم")
        }
    }
}

@Composable
fun MetricBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        Text(text = label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccountingPrimary)
    }
}

// ==================================================================================
// TREASURY / SAFES & BANKS EXTENSIONS, DASHBOARD & DIRECT POSTING Modal Forms
// ==================================================================================

// Extension matches to safely identify Safe (صندوق) or Bank (بنك) accounts
fun Account.isSafe(): Boolean {
    val lowercaseName = name.lowercase()
    return lowercaseName.contains("صندوق") || lowercaseName.contains("الخزينة") || lowercaseName.contains("كاش") || lowercaseName.contains("خزينة") || code == "1101"
}

fun Account.isBank(): Boolean {
    val lowercaseName = name.lowercase()
    return lowercaseName.contains("بنك") || lowercaseName.contains("مصرف") || lowercaseName.contains("حساب بنكي") || lowercaseName.contains("سداد") || code == "1102"
}

@Composable
fun TreasuryDashboardView(viewModel: AccountingViewModel) {
    val accountsList by viewModel.accounts.collectAsState(initial = emptyList())
    
    // Categorize accounts cleanly
    val safes = accountsList.filter { it.isSafe() }
    val banks = accountsList.filter { it.isBank() }
    val postingAccounts = accountsList.filter { !it.isGroup }

    val totalSafesBalance = safes.sumOf { it.balance }
    val totalBanksBalance = banks.sumOf { it.balance }
    val totalLiquidity = totalSafesBalance + totalBanksBalance

    // Dialog trigger states
    var activeQuickTxType by remember { mutableStateOf<String?>(null) } // "RECEIPT", "PAYMENT"
    var showTransferDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Grand consolidated liquidity overview card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AccountingPrimary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "رصيد السيولة المتوفرة بالخزائن والحسابات الإجمالية",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(Locale.US, "%,.2f د.ل", totalLiquidity),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.White.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("السيولة الكاش (الصناديق)", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                        Text(String.format(Locale.US, "%,.2f د.ل", totalSafesBalance), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Divider(modifier = Modifier.width(1.dp).height(24.dp), color = Color.White.copy(alpha = 0.2f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("الحسابات بالبنوك والمصارف", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                        Text(String.format(Locale.US, "%,.2f د.ل", totalBanksBalance), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Action Buttons for simplified cashier bookings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { activeQuickTxType = "RECEIPT" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                contentPadding = PaddingValues(vertical = 10.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = null, modifier = Modifier.size(15.dp))
                    Text("قبض نقدية", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
            
            Button(
                onClick = { activeQuickTxType = "PAYMENT" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = DeficitRed),
                contentPadding = PaddingValues(vertical = 10.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = null, modifier = Modifier.size(15.dp))
                    Text("صرف نقدية", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Button(
                onClick = { showTransferDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                contentPadding = PaddingValues(vertical = 10.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.SyncAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                    Text("تحويل نقدية", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // List Safes & Banks
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Safes Header
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Payments, contentDescription = null, tint = AccountingPrimary, modifier = Modifier.size(18.dp))
                    Text("الصناديق والعهد المالية (الكاش)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                }
            }
            
            if (safes.isEmpty()) {
                item {
                    Text("لا توجد حسابات مسجلة كصندوق كاش.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(start = 12.dp))
                }
            } else {
                items(safes) { safe ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFEEEAEF), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(safe.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text("رمز الحساب الدفتري: ${safe.code}", fontSize = 10.sp, color = Color.Gray)
                            }
                            Text(
                                text = String.format(Locale.US, "%,.2f د.ل", safe.balance),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AccountingPrimary
                            )
                        }
                    }
                }
            }

            // Banks Header
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = AccountingPrimary, modifier = Modifier.size(18.dp))
                    Text("الحسابات المصرفية والبنوك", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                }
            }
            
            if (banks.isEmpty()) {
                item {
                    Text("لا توجد حسابات مخصصة كحساب مصرفي.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(start = 12.dp))
                }
            } else {
                items(banks) { bank ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFEEEAEF), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(bank.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text("رمز الحساب الجاري: ${bank.code}", fontSize = 10.sp, color = Color.Gray)
                            }
                            Text(
                                text = String.format(Locale.US, "%,.2f د.ل", bank.balance),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AccountingPrimary
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal dialog trigger logic
    activeQuickTxType?.let { type ->
        val isPayment = type == "PAYMENT"
        QuickReceiptPaymentDialog(
            accountsList = postingAccounts,
            isPayment = isPayment,
            onDismiss = { activeQuickTxType = null },
            onSave = { safeId, targetId, amount, memo ->
                viewModel.recordQuickTransaction(safeId, targetId, amount, isPayment, memo)
                activeQuickTxType = null
            }
        )
    }

    if (showTransferDialog) {
        val selectableTreasuries = accountsList.filter { (it.isSafe() || it.isBank()) && !it.isGroup }
        QuickTransferDialog(
            accountsList = selectableTreasuries,
            onDismiss = { showTransferDialog = false },
            onSave = { srcId, destId, amount, memo ->
                viewModel.recordTransferTransaction(srcId, destId, amount, memo)
                showTransferDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickReceiptPaymentDialog(
    accountsList: List<Account>,
    isPayment: Boolean,
    onDismiss: () -> Unit,
    onSave: (safeAccountId: Int, targetAccountId: Int, amount: Double, memo: String) -> Unit
) {
    val title = if (isPayment) "إصدار سند صرف نقدي مبسط" else "إصدار سند قبض نقدي مبسط"
    
    var selectedTreasuryAccount by remember { mutableStateOf<Account?>(null) }
    var selectedTargetAccount by remember { mutableStateOf<Account?>(null) }
    var amountStr by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }

    var treasuryDropdownExpanded by remember { mutableStateOf(false) }
    var targetDropdownExpanded by remember { mutableStateOf(false) }

    val treasuries = accountsList.filter { it.isSafe() || it.isBank() }
    val targetLedgers = accountsList.filter { !it.isSafe() && !it.isBank() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AccountingPrimary,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Choice of safe/bank
                Column {
                    Text("الصندوق أو الحساب البنكي المستخدم:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { treasuryDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                        ) {
                            Text(
                                text = selectedTreasuryAccount?.name ?: "اختر الصندوق/البنك...",
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Right
                            )
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = treasuryDropdownExpanded,
                            onDismissRequest = { treasuryDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            treasuries.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (${String.format(Locale.US, "%,.2f", acc.balance)} د.ل)") },
                                    onClick = {
                                        selectedTreasuryAccount = acc
                                        treasuryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Choice of counter-party/target ledger account
                Column {
                    Text(
                        text = if (isPayment) "المستفيد (حساب المصروف المباشر):" else "المصدر المقابل (حساب الإيراد المباشر):",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { targetDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                        ) {
                            Text(
                                text = selectedTargetAccount?.name ?: "اختر طرف قيد المقابل...",
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Right
                            )
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = targetDropdownExpanded,
                            onDismissRequest = { targetDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            targetLedgers.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (${acc.code})") },
                                    onClick = {
                                        selectedTargetAccount = acc
                                        targetDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("المبلغ (د.ل)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Memo
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("البيان والسبب") },
                    placeholder = { Text(if (isPayment) "مثال: دفع فاتورة صيانة للسيارة" else "مثال: استلام دفعة مقدم تأجير") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    val treasuryId = selectedTreasuryAccount?.id
                    val targetId = selectedTargetAccount?.id
                    if (treasuryId != null && targetId != null && amount > 0) {
                        onSave(treasuryId, targetId, amount, memo.ifEmpty { title })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccountingPrimary)
            ) {
                Text("ترحيل المعاملة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickTransferDialog(
    accountsList: List<Account>,
    onDismiss: () -> Unit,
    onSave: (sourceAccountId: Int, destinationAccountId: Int, amount: Double, memo: String) -> Unit
) {
    var selectedSourceAccount by remember { mutableStateOf<Account?>(null) }
    var selectedDestinationAccount by remember { mutableStateOf<Account?>(null) }
    var amountStr by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }

    var sourceExpanded by remember { mutableStateOf(false) }
    var destExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تحويل نقدية (بين الصناديق والبنوك)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AccountingPrimary,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Source
                Column {
                    Text("الحساب المصدر (المرسل):", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { sourceExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                        ) {
                            Text(
                                text = selectedSourceAccount?.name ?: "اختر الحساب المصدر...",
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Right
                            )
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = sourceExpanded,
                            onDismissRequest = { sourceExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            accountsList.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (${String.format(Locale.US, "%,.2f", acc.balance)} د.ل)") },
                                    onClick = {
                                        selectedSourceAccount = acc
                                        sourceExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Destination
                Column {
                    Text("الحساب المستهدف (المودع فيه):", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { destExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                        ) {
                            Text(
                                text = selectedDestinationAccount?.name ?: "اختر الحساب المصرفي المستقبل...",
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Right
                            )
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = destExpanded,
                            onDismissRequest = { destExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            accountsList.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (${String.format(Locale.US, "%,.2f", acc.balance)} د.ل)") },
                                    onClick = {
                                        selectedDestinationAccount = acc
                                        destExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("المبلغ المحول (د.ل)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Memo
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("البيان وملاحظات التحويل") },
                    placeholder = { Text("مثال: إيداع الكاش اليومي بالبنك") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    val srcId = selectedSourceAccount?.id
                    val destId = selectedDestinationAccount?.id
                    if (srcId != null && destId != null && amount > 0 && srcId != destId) {
                        onSave(srcId, destId, amount, memo.ifEmpty { "تحويل نقدية بين الخزائن" })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccountingPrimary)
            ) {
                Text("إرسال وتأكيد التحويل")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPartnerTransactionDialog(
    partner: AccountPartner,
    accountsList: List<Account>,
    onDismiss: () -> Unit,
    onSave: (partnerId: Int, amount: Double, isPayment: Boolean, safeOrBankAccountId: Int, notes: String) -> Unit
) {
    val isCustomer = partner.type == "CUSTOMER"
    var isPayment by remember { mutableStateOf(!isCustomer) } // Default receipt for customer, payment for supplier
    var selectedTreasuryAccount by remember { mutableStateOf<Account?>(null) }
    var amountStr by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }

    var treasuryDropdownExpanded by remember { mutableStateOf(false) }

    val treasuries = accountsList.filter { (it.isSafe() || it.isBank()) && !it.isGroup }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "سند قيد مالي لـ: ${partner.name}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = AccountingPrimary,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Cash Flow Selection Button Row (قبض / صرف)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEEEEEE))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isPayment) ProfitGreen else Color.Transparent)
                            .clickable { isPayment = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "سند قبض (استلام كاش)",
                            color = if (!isPayment) Color.White else Color.DarkGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isPayment) DeficitRed else Color.Transparent)
                            .clickable { isPayment = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "سند صرف (دفع كاش)",
                            color = if (isPayment) Color.White else Color.DarkGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Choose Treasury
                Column {
                    Text("صندوق / بنك التسوية المالية:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { treasuryDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                        ) {
                            Text(
                                text = selectedTreasuryAccount?.name ?: "اختر صندوق التسوية لترحيل الكاش...",
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Right
                            )
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = treasuryDropdownExpanded,
                            onDismissRequest = { treasuryDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            treasuries.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (${String.format(Locale.US, "%,.2f", acc.balance)} د.ل)") },
                                    onClick = {
                                        selectedTreasuryAccount = acc
                                        treasuryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("المبلغ (د.ل)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Memo
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("البيان والملاحظات الدفترية") },
                    placeholder = { Text(if (isPayment) "تسديد حساب / توريد دفعة" else "تحصيل رصيد مستحق / استلام نقدي") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    val safeId = selectedTreasuryAccount?.id
                    if (safeId != null && amount > 0) {
                        val desc = memo.ifEmpty { if (isPayment) "دفع استحقاق مالي للجهة" else "تحصيل نقود من الجهة" }
                        onSave(partner.id, amount, isPayment, safeId, desc)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccountingPrimary)
            ) {
                Text("ترحيل وإثبات القيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
