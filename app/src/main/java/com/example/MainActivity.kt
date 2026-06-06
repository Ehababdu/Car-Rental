package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.accounting.*
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: CarRentalViewModel = viewModel()
            val isDark by viewModel.isDarkMode.collectAsState()
            MyApplicationTheme(darkTheme = isDark) {
                // Wrap in CompositionLocalProvider to enforce Right-To-Left (RTL) Arabic layout
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        CarRentalAppMainScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarRentalAppMainScreen(
    viewModel: CarRentalViewModel = viewModel()
) {
    val accountingViewModel: AccountingViewModel = viewModel()
    var currentTab by remember { mutableStateOf("dashboard") }
    val notifications by viewModel.notifications.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                "android.permission.POST_NOTIFICATIONS"
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            if (!hasPermission) {
                permissionLauncher.launch("android.permission.POST_NOTIFICATIONS")
            }
        }
    }

    // Dialog state controllers
    var showAddCarDialog by remember { mutableStateOf(false) }
    var showAddBookingDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showManualTransactionDialog by remember { mutableStateOf(false) }
    var showRatingDialogForBooking by remember { mutableStateOf<Booking?>(null) }
    var selectedCarForDetails by remember { mutableStateOf<Car?>(null) }
    var preSelectedCarIdForBooking by remember { mutableStateOf<Int?>(null) }

    // Quick Notifications Toast Handler
    LaunchedEffect(notifications) {
        if (notifications.isNotEmpty()) {
            Toast.makeText(context, notifications.first(), Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_scaffold"),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_nav_bar"),
                containerColor = Color(0xFFF3F0F5),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == "dashboard",
                    onClick = { currentTab = "dashboard" },
                    icon = { Icon(Icons.Filled.Dashboard, contentDescription = "الرئيسية") },
                    label = { Text("الرئيسية", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandPrimary,
                        selectedTextColor = BrandPrimary,
                        indicatorColor = BrandSurface
                    )
                )
                NavigationBarItem(
                    selected = currentTab == "cars",
                    onClick = { currentTab = "cars" },
                    icon = { Icon(Icons.Filled.DirectionsCar, contentDescription = "السيارات") },
                    label = { Text("المركبات", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandPrimary,
                        selectedTextColor = BrandPrimary,
                        indicatorColor = BrandSurface
                    )
                )
                NavigationBarItem(
                    selected = currentTab == "bookings",
                    onClick = { currentTab = "bookings" },
                    icon = { Icon(Icons.Filled.Assignment, contentDescription = "الحجوزات") },
                    label = { Text("الحجوزات", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandPrimary,
                        selectedTextColor = BrandPrimary,
                        indicatorColor = BrandSurface
                    )
                )
                NavigationBarItem(
                    selected = currentTab == "accounting",
                    onClick = { currentTab = "accounting" },
                    icon = { Icon(Icons.Filled.AccountBalance, contentDescription = "المحاسبة") },
                    label = { Text("المحاسبة والقيود", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandPrimary,
                        selectedTextColor = BrandPrimary,
                        indicatorColor = BrandSurface
                    )
                )
                NavigationBarItem(
                    selected = currentTab == "profile",
                    onClick = { currentTab = "profile" },
                    icon = { Icon(Icons.Filled.Person, contentDescription = "الملف الشخصي") },
                    label = { Text("الملف الشخصي", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandPrimary,
                        selectedTextColor = BrandPrimary,
                        indicatorColor = BrandSurface
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BrandBackground)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                },
                label = "tab_switch"
            ) { targetTab ->
                when (targetTab) {
                    "dashboard" -> DashboardScreen(
                        viewModel = viewModel,
                        accountingViewModel = accountingViewModel,
                        onAddCarClick = { showAddCarDialog = true },
                        onReportsClick = { currentTab = "profile" },
                        onSafesClick = { currentTab = "accounting" },
                        onQuickBookingClick = { showAddBookingDialog = true }
                    )
                    "cars" -> CarsScreen(
                        viewModel = viewModel,
                        onAddNewCarClick = { showAddCarDialog = true },
                        onCarClick = { selectedCarForDetails = it }
                    )
                    "bookings" -> BookingsScreen(
                        viewModel = viewModel,
                        onAddNewBookingClick = { showAddBookingDialog = true },
                        onRateBookingClick = { showRatingDialogForBooking = it }
                    )
                    "accounting" -> AccountingDashboardScreen(viewModel = accountingViewModel)
                    "profile" -> UserProfileScreen(viewModel = viewModel)
                }
            }

            // NOTIFICATIONS OVERLAY (Quick logs notification badge drawer inside Dashboard or reports)
            if (notifications.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BrandSecondary.copy(alpha = 0.95f))
                        .border(1.dp, BrandPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { viewModel.clearNotifications() }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = "إشعار", tint = BrandOrange)
                        Text(
                            text = notifications.first(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = Color.LightGray)
                    }
                }
            }
        }
    }

    // DIALOGS CODES
    if (showAddCarDialog) {
        AddCarDialog(
            onDismiss = { showAddCarDialog = false },
            onSave = { type, plate, chassis, color, model, mileage, price, images ->
                viewModel.addNewCar(type, plate, chassis, color, model, mileage.toIntOrNull() ?: 0, price.toDoubleOrNull() ?: 0.0, images)
                showAddCarDialog = false
            }
        )
    }

    if (showAddBookingDialog) {
        val carsList by viewModel.cars.collectAsState()
        val accountsList by viewModel.accounts.collectAsState()
        AddBookingDialog(
            cars = carsList.filter { it.status == "متاحة" },
            accounts = accountsList,
            preSelectedCarId = preSelectedCarIdForBooking,
            onDismiss = { 
                showAddBookingDialog = false
                preSelectedCarIdForBooking = null
            },
            onSave = { carId, name, phone, pickup, returnDate, total, accountId ->
                viewModel.createBooking(carId, name, phone, pickup, returnDate, total, accountId)
                showAddBookingDialog = false
                preSelectedCarIdForBooking = null
            }
        )
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onSave = { name, type, balance ->
                viewModel.addFinancialAccount(name, type, balance.toDoubleOrNull() ?: 0.0)
                showAddAccountDialog = false
            }
        )
    }

    if (showManualTransactionDialog) {
        val accountsList by viewModel.accounts.collectAsState()
        ManualTransactionDialog(
            accounts = accountsList,
            onDismiss = { showManualTransactionDialog = false },
            onSave = { accountId, amount, type, desc ->
                viewModel.recordManualTransaction(accountId, amount.toDoubleOrNull() ?: 0.0, type, desc)
                showManualTransactionDialog = false
            }
        )
    }

    showRatingDialogForBooking?.let { booking ->
        val carsList by viewModel.cars.collectAsState()
        val car = carsList.find { it.id == booking.carId }
        val carLabel = car?.model ?: "المركبة"
        RatingDialog(
            targetLabel = "تقييم خدمة تأجير $carLabel للعميل ${booking.customerName}",
            onDismiss = { showRatingDialogForBooking = null },
            onRate = { rating, comment ->
                viewModel.rateService(booking.id, rating, comment)
                showRatingDialogForBooking = null
            }
        )
    }

    selectedCarForDetails?.let { car ->
        CarDetailsDialog(
            car = car,
            onDismiss = { selectedCarForDetails = null },
            onUpdateMileage = { newMileage ->
                viewModel.updateCarMileage(car, newMileage)
                selectedCarForDetails = car.copy(mileage = newMileage)
            },
            onRentClick = {
                preSelectedCarIdForBooking = car.id
                showAddBookingDialog = true
                selectedCarForDetails = null
            }
        )
    }
}

// 1. DASHBOARD SCREEN
@Composable
fun DashboardScreen(
    viewModel: CarRentalViewModel,
    accountingViewModel: com.example.accounting.AccountingViewModel,
    onAddCarClick: () -> Unit,
    onReportsClick: () -> Unit,
    onSafesClick: () -> Unit,
    onQuickBookingClick: () -> Unit
) {
    val carsList by viewModel.cars.collectAsState()
    val bookingsList by viewModel.bookings.collectAsState()
    val rentalAccountsList by viewModel.accounts.collectAsState()

    // Observe accounting system lists for real-time ledger integrations
    val accountingAccounts by accountingViewModel.accounts.collectAsState(initial = emptyList())
    val accountingPartners by accountingViewModel.partners.collectAsState(initial = emptyList())
    val accountingVouchers by accountingViewModel.vouchers.collectAsState(initial = emptyList())

    // 1. Real-time Car Availability computations
    val availableCarsCount = carsList.count { it.status == "متاحة" }
    val rentedCarsCount = carsList.count { it.status == "مؤجرة" }
    val maintenanceCarsCount = carsList.count { it.status == "صيانة" }
    val totalCarsCount = carsList.size

    // User Interactive state for real-time car availability filter
    var selectedAvailabilityFilter by remember { mutableStateOf("ALL") } // "ALL", "متاحة", "مؤجرة", "صيانة"

    val filteredCarsByAvailability = when (selectedAvailabilityFilter) {
        "متاحة" -> carsList.filter { it.status == "متاحة" }
        "مؤجرة" -> carsList.filter { it.status == "مؤجرة" }
        "صيانة" -> carsList.filter { it.status == "صيانة" }
        else -> carsList
    }

    // 2. Active rentals computation ("قيد التنفيذ" or "مؤكد")
    val activeBookings = bookingsList.filter { it.status == "قيد التنفيذ" || it.status == "مؤكد" }

    // 3. Outstanding accounting financials (Receivables, Payables, Treasury Cash)
    val totalCustomerReceivables = accountingPartners.filter { it.type == "CUSTOMER" }.sumOf { it.balance }
    val totalSupplierPayables = accountingPartners.filter { it.type == "SUPPLIER" }.sumOf { it.balance }
    
    // Consolidated Treasury Cash (unified from local accounts and ledger cash balances)
    val localTreasuryCash = rentalAccountsList.sumOf { it.balance }
    val generalLedgerLiquidity = accountingAccounts.filter { 
        !it.isGroup && (it.code.startsWith("11") || it.name.contains("صندوق") || it.name.contains("بنك") || it.name.contains("مصرف") || it.name.contains("خزينة")) 
    }.sumOf { it.balance }
    val consolidatedLiquidity = localTreasuryCash + generalLedgerLiquidity

    val sdf = remember { java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_scroll"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App header & Greeting
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(BrandPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.DirectionsCar,
                            contentDescription = "اللوجو",
                            tint = BrandPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "مرحباً بك في نظام",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrandAccent
                        )
                        Text(
                            text = "بوابة تأجير السيارات الذكية",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = BrandSecondary
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isDark by viewModel.isDarkMode.collectAsState()
                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .clip(CircleShape)
                            .background(BrandSurface)
                            .clickable { viewModel.toggleDarkMode() }
                            .testTag("theme_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "تبديل المظهر البصري",
                            tint = BrandPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .clip(CircleShape)
                            .background(BrandSurface)
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        String.format("") // Dummy placeholder
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "التنبيهات",
                            tint = BrandPrimary
                        )
                    }
                }
            }
        }

        // Consolidated Financial Liquidity Banner Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(BrandPrimary, BrandSecondary),
                            start = Offset(0f, 0f),
                            end = Offset(1000f, 1000f)
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "إجمالي السيولة الموحدة (الصناديق والبنوك)",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            Icons.Filled.AccountBalance,
                            contentDescription = "أرصدة مجمعة",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(java.util.Locale.US, "%,.2f د.ل", consolidatedLiquidity),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("خزينة التأجير المحلية", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                            Text(String.format(java.util.Locale.US, "%,.2f ر.س", localTreasuryCash), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Divider(modifier = Modifier.width(1.dp).height(28.dp), color = Color.White.copy(alpha = 0.2f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("أرصدة دفتر الأستاذ العام", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                            Text(String.format(java.util.Locale.US, "%,.2f د.ل", generalLedgerLiquidity), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // Quick Shortcuts Row Grid
        item {
            Column {
                Text(
                    text = "عمليات سريعة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BrandSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardActionButton(
                        label = "إضافة سيارة",
                        icon = Icons.Filled.Add,
                        tint = BrandPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = onAddCarClick
                    )
                    DashboardActionButton(
                        label = "إبرام عقد حجز",
                        icon = Icons.Filled.Handshake,
                        tint = BrandTeal,
                        modifier = Modifier.weight(1f),
                        onClick = onQuickBookingClick
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardActionButton(
                        label = "الخزائن والبنوك",
                        icon = Icons.Filled.AccountBalanceWallet,
                        tint = BrandIndigo,
                        modifier = Modifier.weight(1f),
                        onClick = onSafesClick
                    )
                    DashboardActionButton(
                        label = "التقارير والمقاسات",
                        icon = Icons.Filled.TrendingUp,
                        tint = BrandSecondary,
                        modifier = Modifier.weight(1f),
                        onClick = onReportsClick
                    )
                }
            }
        }

        // Real-time Maintenance Alerts Reminders Dashboard card
        val overdueMaintenanceCount = carsList.count { it.mileage >= it.lastServiceMileage + it.serviceThreshold }
        if (overdueMaintenanceCount > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Can click to view details or just general informational alert
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Red.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Notifications,
                                contentDescription = "تنبيه الصيانة",
                                tint = Color.Red,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تنبيه وقائي حرجة للصيانة",
                                color = Color(0xFF991B1B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "هناك عدد ($overdueMaintenanceCount) سيارات بالأسطول تجاوزت حد عداد الصيانة ومستحقة فورياً ومجدولة للغيار.",
                                color = Color(0xFF7F1D1D),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // ====================================================================
        // SECTION A: Real-time Car Availability
        // ====================================================================
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "حالة وحركة أسطول السيارات الفعلي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BrandSecondary
                    )
                    Text(
                        text = "$totalCarsCount سيارة إجمالاً",
                        fontSize = 11.sp,
                        color = BrandAccent,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Interactive Filters with color codes
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple("ALL", "الكل", Color(0xFFF3F4F6)),
                        Triple("متاحة", "متاحة ($availableCarsCount)", Color(0xFFE8F5E9)),
                        Triple("مؤجرة", "مؤجرة ($rentedCarsCount)", Color(0xFFFFF3E0)),
                        Triple("صيانة", "سيارة صيانة ($maintenanceCarsCount)", Color(0xFFFFEBEE))
                    ).forEach { (filterType, name, bgColor) ->
                        val isSelected = selectedAvailabilityFilter == filterType
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) BrandPrimary else bgColor)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) BrandPrimary else Color(0xFFE5E7EB),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedAvailabilityFilter = filterType }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else BrandSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                if (filteredCarsByAvailability.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BrandSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد أي سيارات حالياً تناسب هذا الفلتر.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("carousel_cars_availability"),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(filteredCarsByAvailability) { car ->
                            Card(
                                modifier = Modifier
                                    .width(200.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, Color(0xFFEEEAEF), RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Car Image inside Carousel
                                    val imageUrl = car.images.split(",").firstOrNull() ?: ""
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                    ) {
                                        if (imageUrl.isNotEmpty()) {
                                            AsyncImage(
                                                model = imageUrl,
                                                contentDescription = car.model,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0xFFF3F4F6)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Filled.DirectionsCar,
                                                    contentDescription = null,
                                                    tint = Color.LightGray,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }
                                        }

                                        // Status badge overlay
                                        val badgeColor = when (car.status) {
                                            "متاحة" -> BrandGreen
                                            "مؤجرة" -> BrandOrange
                                            else -> BrandRed
                                        }
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(badgeColor)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = car.status,
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Content details inside Carousel
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = car.model,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = BrandSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "لوحة: ${car.plateNo}",
                                            fontSize = 9.sp,
                                            color = BrandAccent
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "السعر اليومي:",
                                                fontSize = 9.sp,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = "${car.rentPricePerDay} ر.س",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp,
                                                color = BrandPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ====================================================================
        // SECTION B: Active Rentals & Timeline Countdown Tracker
        // ====================================================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "عقود التأجير النشطة والجارية",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BrandSecondary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandPrimary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${activeBookings.size} عقد نشط",
                        fontSize = 10.sp,
                        color = BrandPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (activeBookings.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد أي عقود تأجير نشطة قيد التنفيذ حالياً.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(activeBookings) { booking ->
                val associatedCar = carsList.find { it.id == booking.carId }
                
                // Timeline progress calculations
                val currentTime = System.currentTimeMillis()
                val totalDurationMs = (booking.returnDate - booking.pickupDate).coerceAtLeast(1L)
                val elapsedDurationMs = (currentTime - booking.pickupDate).coerceAtLeast(0L)
                val progressValue = (elapsedDurationMs.toDouble() / totalDurationMs.toDouble()).toFloat().coerceIn(0f, 1f)

                val remainingMs = (booking.returnDate - currentTime).coerceAtLeast(0L)
                val remainingDays = remainingMs / (24 * 3600 * 1000)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFEEEAEF), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(BrandPrimary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = BrandPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = booking.customerName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = BrandSecondary
                                    )
                                    Text(
                                        text = booking.customerPhone,
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BrandGreen.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = String.format(java.util.Locale.US, "%,.2f ر.س", booking.totalAmount),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = Color(0xFFF3F4F6))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "السيارة: ${associatedCar?.model ?: "مركبة غير معروفة"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandSecondary
                            )
                            Text(
                                text = "رقم اللوحة: ${associatedCar?.plateNo ?: ""}",
                                fontSize = 10.sp,
                                color = BrandAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Progress layout relative to timeline
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تاريخ الاستلام: ${sdf.format(java.util.Date(booking.pickupDate))}",
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "تاريخ التسليم: ${sdf.format(java.util.Date(booking.returnDate))}",
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = progressValue,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = BrandPrimary,
                            trackColor = Color(0xFFE5E7EB)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "نسبة انقضاء فترة العقد: ${String.format(java.util.Locale.US, "%.0f%%", progressValue * 100)}",
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    Icons.Filled.Event,
                                    contentDescription = null,
                                    tint = if (remainingDays <= 1) BrandRed else BrandPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (remainingDays == 0L) "العقد ينتهي اليوم!" else "متبقي $remainingDays يوم للاسترجاع",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (remainingDays <= 1) BrandRed else BrandPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // ====================================================================
        // SECTION C: Integrated Accounting & Outstanding Balances Dashboard
        // ====================================================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "محاسبة وماليات القيود المعلقة (الأستاذ العام)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BrandSecondary
                )
                Text(
                    text = "تفاصيل القيود",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary,
                    modifier = Modifier.clickable { onSafesClick() }
                )
            }
        }

        // Customer Receivables & Supplier Payables summaries
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "ذمم العملاء (مستحقات معلقة)",
                            fontSize = 10.sp,
                            color = Color(0xFF3F51B5),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(java.util.Locale.US, "%,.2f د.ل", totalCustomerReceivables),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = if (totalCustomerReceivables >= 0) ProfitGreen else DeficitRed
                        )
                        Text("التحصيل المالي المستهدف", fontSize = 8.sp, color = Color.Gray)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "مستحقات الموردين والشركات",
                            fontSize = 10.sp,
                            color = Color(0xFFEF6C00),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(java.util.Locale.US, "%,.2f د.ل", totalSupplierPayables),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = if (totalSupplierPayables >= 0) ProfitGreen else DeficitRed
                        )
                        Text("مستحقات للدفع كالمصروفات", fontSize = 8.sp, color = Color.Gray)
                    }
                }
            }
        }

        // Recent accounting double-entry vouchers log
        item {
            Text(
                text = "آخر القيود وسندات الصرف والقبض المحرحلة",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (accountingVouchers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لم يتم ترحيل أي سندات مالية أو قيود دبل إنبوري بالدفتر اليومي الأستاذ.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(accountingVouchers.take(4)) { voucher ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFEEEAEF), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Badge color representing receipt vs payment
                            val badgeBg = when (voucher.type) {
                                "RECEIPT" -> Color(0xFFE8F5E9)
                                "PAYMENT" -> Color(0xFFFFEBEE)
                                else -> Color(0xFFE8EAF6)
                            }
                            val badgeText = when (voucher.type) {
                                "RECEIPT" -> "سند قبض"
                                "PAYMENT" -> "سند صرف"
                                else -> "قيد محاسبي"
                            }
                            val badgeColor = when (voucher.type) {
                                "RECEIPT" -> ProfitGreen
                                "PAYMENT" -> DeficitRed
                                else -> Color(0xFF3F51B5)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(badgeBg)
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    badgeText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor
                                )
                            }

                            Column {
                                Text(
                                    text = voucher.description,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = BrandSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "رقم السند: ${voucher.voucherNo} | ${sdf.format(java.util.Date(voucher.date))}",
                                    fontSize = 9.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Text(
                            text = String.format(java.util.Locale.US, "%,.2f د.ل", voucher.totalAmount),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardActionButton(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(105.dp)
            .clickable(onClick = onClick)
            .testTag("action_$label"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F0F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BrandSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// 2. VEHICLES / CARS SCREEN
@Composable
fun CarsScreen(
    viewModel: CarRentalViewModel,
    onAddNewCarClick: () -> Unit,
    onCarClick: (Car) -> Unit
) {
    val carsList by viewModel.cars.collectAsState()
    var activeSubMode by remember { mutableStateOf("fleet") } // "fleet" or "maintenance"
    var selectedFilterType by remember { mutableStateOf("الكل") }
    val carTypes = listOf("الكل", "سيدان", "دفع رباعي", "فاخرة", "كهربائية")

    // State for local maintenance dialogs
    var showLogServiceDialogForCar by remember { mutableStateOf<Car?>(null) }
    var showEditThresholdDialogForCar by remember { mutableStateOf<Car?>(null) }
    var showQuickMileageDialogForCar by remember { mutableStateOf<Car?>(null) }

    val filteredCars = if (selectedFilterType == "الكل") {
        carsList
    } else {
        carsList.filter { it.type == selectedFilterType }
    }

    Scaffold(
        floatingActionButton = {
            if (activeSubMode == "fleet") {
                FloatingActionButton(
                    onClick = onAddNewCarClick,
                    containerColor = BrandPrimary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("add_car_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "إضافة سيارة")
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "أسطول المركبات والصيانة",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandSecondary
                )
                Text(
                    text = "تصفح أسطول السيارات ومتابعة قراءات عدادات المسافات وتنبيهات الصيانة الوقائية.",
                    fontSize = 13.sp,
                    color = BrandAccent
                )
            }

            // Segmented mode selector - Arabic styled
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE2E8F0))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeSubMode == "fleet") BrandPrimary else Color.Transparent)
                        .clickable { activeSubMode = "fleet" }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "المركبات",
                        color = if (activeSubMode == "fleet") Color.White else BrandSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeSubMode == "maintenance") BrandPrimary else Color.Transparent)
                        .clickable { activeSubMode = "maintenance" }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val alertCount = carsList.count { it.mileage >= it.lastServiceMileage + it.serviceThreshold || it.status == "صيانة" }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "الصيانة والعدادات",
                            color = if (activeSubMode == "maintenance") Color.White else BrandSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        if (alertCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Red)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = alertCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeSubMode == "map") BrandPrimary else Color.Transparent)
                        .clickable { activeSubMode = "map" }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Map,
                            contentDescription = null,
                            tint = if (activeSubMode == "map") Color.White else BrandPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "تتبع الخريطة",
                            color = if (activeSubMode == "map") Color.White else BrandSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (activeSubMode == "fleet") {
                // Category tabs list
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    items(carTypes) { type ->
                        val isSelected = selectedFilterType == type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) BrandPrimary else Color(0xFFF3F0F5))
                                .clickable { selectedFilterType = type }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = type,
                                color = if (isSelected) Color.White else BrandSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredCars.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateCard(
                            message = "لا توجد سيارات مطابقة للتصنيف حالياً.",
                            subMessage = "قم بإضافة أول سيارة باستخدام زر الإضافة بالأسفل."
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("cars_grid"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredCars) { car ->
                            CarGridCard(
                                car = car,
                                onUpdateMileage = { newMileage ->
                                    viewModel.updateCarMileage(car, newMileage)
                                },
                                onDelete = {
                                    viewModel.deleteCar(car)
                                },
                                onDetailsClick = {
                                    onCarClick(car)
                                }
                            )
                        }
                    }
                }
            } else if (activeSubMode == "maintenance") {
                // MAINTENANCE CENTRE HUB ACTIVE VIEW
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary KPIs Banner card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(BrandPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Build, contentDescription = "الصيانة الدورية", tint = BrandPrimary, modifier = Modifier.size(18.dp))
                                }
                                Text("ملخص حماية الأسطول والصيانة الوقائية", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // KPI 1: Alerts Required
                                val overdueList = carsList.filter { it.mileage >= it.lastServiceMileage + it.serviceThreshold || it.status == "صيانة" }
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("تنبيهات حرجة", fontSize = 10.sp, color = Color.LightGray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${overdueList.size} مركبات",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (overdueList.isNotEmpty()) Color.Red else Color.Green
                                    )
                                }

                                Spacer(modifier = Modifier.width(1.dp).height(30.dp).background(Color.DarkGray))

                                // KPI 2: Total kilometers
                                val totalDistance = carsList.sumOf { it.mileage }
                                Column(modifier = Modifier.weight(1.3f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("إجمالي مسافة الأسطول", fontSize = 10.sp, color = Color.LightGray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "%,d كم".format(totalDistance),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.width(1.dp).height(30.dp).background(Color.DarkGray))

                                // KPI 3: Serviced
                                val healthyCount = carsList.size - overdueList.size
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("حالة سليمة", fontSize = 10.sp, color = Color.LightGray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$healthyCount / ${carsList.size}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BrandGreen
                                    )
                                }
                            }
                        }
                    }

                    // Alerts warning lists if any
                    val overdueFleet = carsList.filter { it.mileage >= it.lastServiceMileage + it.serviceThreshold }
                    if (overdueFleet.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFEF2F2))
                                .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Filled.Notifications, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("🚨 تنبيهات العدادات الفورية المطلوبة", color = Color(0xFF991B1B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "السيارات التالية تجاوزت حدود المسافات المسموحة للصيانة الدورية ومستحقة فورياً لتبديل الزيت والقطع الاستهلاكية.",
                                        color = Color(0xFF7F1D1D),
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    overdueFleet.forEach { c ->
                                        val overdueAmount = c.mileage - (c.lastServiceMileage + c.serviceThreshold)
                                        Text(
                                            text = "• ${c.model} (لوحة: ${c.plateNo}): تجاوز بـ $overdueAmount كم",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF991B1B)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Vehicles Maintenance list header
                    Text("سجل قراءات الصيانة الدورية للأسطول", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)

                    if (carsList.isEmpty()) {
                        EmptyStateCard("لا يوجد مركبات مسجلة لمعالجة صيانتها حالياً.", "قم بتسجيل مركبة أولاً من علامة تبويب الأسطول.")
                    } else {
                        // Table-like row layout card for each car
                        carsList.forEach { car ->
                            val mileageSinceLast = car.mileage - car.lastServiceMileage
                            val progress = (mileageSinceLast.toFloat() / car.serviceThreshold.toFloat()).coerceIn(0f, 1f)
                            val remaining = (car.lastServiceMileage + car.serviceThreshold) - car.mileage
                            val isOverdue = remaining <= 0

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = BrandCard),
                                border = BorderStroke(1.dp, if (isOverdue) Color(0xFFFCA5A5) else Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Plate + car info row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(car.model, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = BrandSecondary)
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(BrandPrimary.copy(alpha = 0.1f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(car.type, color = BrandPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Text("لوحة: ${car.plateNo}", fontSize = 11.sp, color = BrandAccent)
                                            }
                                        }

                                        // Status tags
                                        if (isOverdue) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFFEF2F2))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text("⚠️ مستحق صيانة", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(BrandGreen.copy(alpha = 0.1f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text("✓ حالة جيدة", color = BrandGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // Distances dashboard details
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("العداد الحالي", fontSize = 10.sp, color = BrandAccent)
                                            Text("${car.mileage} كم", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
                                        }
                                        Column(modifier = Modifier.weight(1.2f)) {
                                            Text("قراءة آخر صيانة", fontSize = 10.sp, color = BrandAccent)
                                            Text(
                                                text = if (car.lastServiceMileage > 0) "${car.lastServiceMileage} كم" else "لم تخضع لصيانة بعد",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandSecondary
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1.2f)) {
                                            Text("عتبة التنبيه", fontSize = 10.sp, color = BrandAccent)
                                            Text("كل ${car.serviceThreshold} كم", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
                                        }
                                    }

                                    // Progress bar with threshold indicators
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "المسافة المقطوعة منذ آخر صيانة: $mileageSinceLast كم",
                                                fontSize = 10.sp,
                                                color = BrandAccent
                                            )
                                            Text(
                                                text = if (isOverdue) {
                                                    "تجاوز الحد بـ: ${-remaining} كم 🚨"
                                                } else {
                                                    "متبقي للصيانة: $remaining كم"
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isOverdue) Color.Red else BrandSecondary
                                            )
                                        }

                                        LinearProgressIndicator(
                                            progress = progress,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = if (isOverdue) Color.Red else if (progress > 0.8f) Color(0xFFFF9800) else BrandPrimary,
                                            trackColor = Color(0xFFE2E8F0)
                                        )
                                    }

                                    // Actions strip
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Record Maintenance
                                        Button(
                                            onClick = { showLogServiceDialogForCar = car },
                                            modifier = Modifier.weight(1.2f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isOverdue) Color.Red else BrandPrimary
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تسجيل صيانة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // Adjust Threshold Configurations
                                        OutlinedButton(
                                            onClick = { showEditThresholdDialogForCar = car },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(12.dp), tint = BrandSecondary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("ضبط الحد", fontSize = 11.sp, color = BrandSecondary, fontWeight = FontWeight.Bold)
                                        }

                                        // Quick speedometer mileage update
                                        OutlinedButton(
                                            onClick = { showQuickMileageDialogForCar = car },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(12.dp), tint = BrandSecondary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تعديل العداد", fontSize = 11.sp, color = BrandSecondary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                CarRentalInteractiveMapScreen(
                    viewModel = viewModel,
                    onCarClick = onCarClick
                )
            }
        }
    }

    // -------------------------------------------------------------
    // LOCAL DIALOGS - Maintenance Module Interaction Flow
    // -------------------------------------------------------------

    showLogServiceDialogForCar?.let { car ->
        var typedMileage by remember { mutableStateOf(car.mileage.toString()) }
        var typedNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showLogServiceDialogForCar = null },
            title = { Text("تسجيل تنفيذ صيانة دورية جديدة", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrandSecondary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("مركبة: ${car.model} (${car.plateNo})", fontSize = 12.sp, color = BrandAccent)
                    Text("سيتم تسجيل تصفير عداد الصيانة الحماية وبدء دورة المساقة الوقائية الجديدة من هذه القراءة.", fontSize = 11.sp)
                    
                    OutlinedTextField(
                        value = typedMileage,
                        onValueChange = { typedMileage = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("قراءة العداد عند إتمام الصيانة (كم)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = typedNotes,
                        onValueChange = { typedNotes = it },
                        label = { Text("ملاحظات / قطع الغيار التي تم تبديلها") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = typedMileage.toIntOrNull() ?: car.mileage
                        viewModel.recordCarMaintenance(car, parsed, typedNotes)
                        showLogServiceDialogForCar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text("تأكيد وحفظ الإجراء كامل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogServiceDialogForCar = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    showEditThresholdDialogForCar?.let { car ->
        var typedThreshold by remember { mutableStateOf(car.serviceThreshold.toString()) }

        AlertDialog(
            onDismissRequest = { showEditThresholdDialogForCar = null },
            title = { Text("ضبط العتبة الوقائية للصيانة", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("مركبة: ${car.model}", fontSize = 12.sp, color = BrandAccent)
                    Text("حدد المسافة الدورية المعتمدة لتوليد إشعارات وتنبيهات حتمية الصيانة الوقائية لهذه السيارة.", fontSize = 11.sp)
                    
                    OutlinedTextField(
                        value = typedThreshold,
                        onValueChange = { typedThreshold = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("عتبة المسافة الدورية (كم)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = typedThreshold.toIntOrNull() ?: car.serviceThreshold
                        viewModel.updateCarMaintenanceConfig(car, parsed)
                        showEditThresholdDialogForCar = null
                    }
                ) {
                    Text("تحديث وقيد العتبة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditThresholdDialogForCar = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    showQuickMileageDialogForCar?.let { car ->
        var typedMileage by remember { mutableStateOf(car.mileage.toString()) }

        AlertDialog(
            onDismissRequest = { showQuickMileageDialogForCar = null },
            title = { Text("تحديث قراءة عداد الكيلومترات للسيارة", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("سيارة: ${car.model}", fontSize = 12.sp, color = BrandAccent)
                    
                    OutlinedTextField(
                        value = typedMileage,
                        onValueChange = { typedMileage = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("المسافة الحالية المقطوعة بـ (كم)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = typedMileage.toIntOrNull() ?: car.mileage
                        viewModel.updateCarMileage(car, parsed)
                        showQuickMileageDialogForCar = null
                    }
                ) {
                    Text("تحديث عداد السيارة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickMileageDialogForCar = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun CarGridCard(
    car: Car,
    onUpdateMileage: (Int) -> Unit,
    onDelete: () -> Unit,
    onDetailsClick: () -> Unit
) {
    var showEditMileageDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDetailsClick)
            .testTag("car_card_${car.plateNo}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BrandCard),
        border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Multiple images render logic
            val imagesList = remember(car.images) {
                car.images.split(",").filter { it.isNotBlank() }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                if (imagesList.isNotEmpty()) {
                    AsyncImage(
                        model = imagesList.first(),
                        contentDescription = car.model,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BrandSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.DirectionsCar, contentDescription = "بدون صورة", tint = BrandAccent, modifier = Modifier.size(60.dp))
                    }
                }

                // Tag elements
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandSecondary.copy(alpha = 0.85f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = car.type,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val isAvailable = car.status == "متاحة"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isAvailable) BrandGreen else BrandOrange)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = car.status,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Body info
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = car.model,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = BrandSecondary
                    )
                    Text(
                        text = "${car.rentPricePerDay} ر.س / يوم",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BrandPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Plate & Chassis Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("رقم اللوحة", fontSize = 10.sp, color = BrandAccent)
                        Text(car.plateNo, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
                    }
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text("رقم الهيكل", fontSize = 10.sp, color = BrandAccent)
                        Text(car.chassisNo, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("عداد المشي", fontSize = 10.sp, color = BrandAccent)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("${car.mileage} كم", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "تعديل العداد",
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { showEditMileageDialog = true },
                                tint = BrandPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color.Gray) // fallback and custom colors representation
                        )
                        Text(
                            text = car.color,
                            fontSize = 11.sp,
                            color = BrandSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "حذف السيارة",
                            tint = BrandRed.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }

    if (showEditMileageDialog) {
        var distanceInput by remember { mutableStateOf(car.mileage.toString()) }
        AlertDialog(
            onDismissRequest = { showEditMileageDialog = false },
            title = { Text("تحديث عداد الكيلومترات", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = distanceInput,
                    onValueChange = { distanceInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("العداد الحالي (كم)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = distanceInput.toIntOrNull() ?: car.mileage
                        onUpdateMileage(parsed)
                        showEditMileageDialog = false
                    }
                ) {
                    Text("تحديث")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditMileageDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

// 3. BOOKINGS SCREEN
@Composable
fun BookingsScreen(
    viewModel: CarRentalViewModel,
    onAddNewBookingClick: () -> Unit,
    onRateBookingClick: (Booking) -> Unit
) {
    val bookingsList by viewModel.bookings.collectAsState()
    val carsList by viewModel.cars.collectAsState()
    var showSummaryBooking by remember { mutableStateOf<Booking?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNewBookingClick,
                containerColor = BrandPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_booking_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "إبرام حجز")
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "سجل عقود الحجوزات",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandSecondary
                )
                Text(
                    text = "تتبع فترات التأجير النشطة، وتاريخ تسليم السيارات وتلقي التقييمات.",
                    fontSize = 13.sp,
                    color = BrandAccent
                )
            }

            if (bookingsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateCard(
                        message = "لا توجد عقود تأجير مسجلة.",
                        subMessage = "استخدم زر الحجز السريع لتبدأ تسجيل العقود."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("bookings_scroll"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(bookingsList) { booking ->
                        val car = carsList.find { it.id == booking.carId }
                        BookingCardItem(
                            booking = booking,
                            car = car,
                            onUpdateStatus = { status ->
                                viewModel.updateBookingStatus(booking, status)
                            },
                            onRateClick = {
                                onRateBookingClick(booking)
                            },
                            onShowSummaryClick = {
                                showSummaryBooking = booking
                            }
                        )
                    }
                }
            }
        }
    }

    showSummaryBooking?.let { b ->
        val car = carsList.find { it.id == b.carId }
        BookingSummaryDialog(
            booking = b,
            car = car,
            onDismiss = { showSummaryBooking = null }
        )
    }
}

@Composable
fun BookingSummaryDialog(
    booking: Booking,
    car: Car?,
    onDismiss: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
    val pickupString = remember(booking.pickupDate) { sdf.format(Date(booking.pickupDate)) }
    val returnString = remember(booking.returnDate) { sdf.format(Date(booking.returnDate)) }
    
    val durationMs = booking.returnDate - booking.pickupDate
    val durationDays = (durationMs / (1000 * 60 * 60 * 24)).coerceAtLeast(1)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp)),
            border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Logo and Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "تذكرة استلام السيارة",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandSecondary
                        )
                        Text(
                            text = "يرجى مسح الرمز لمطابقة الفحص والاستلام",
                            fontSize = 10.sp,
                            color = BrandAccent
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BrandPrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.ConfirmationNumber,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Divider(
                    color = Color(0xFFE2E8F0),
                    thickness = 1.dp
                )

                // Ticket cut layout with info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Booking ID & Customer Name
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("الاسم المعتمد", fontSize = 9.sp, color = BrandAccent)
                            Text(booking.customerName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("رقم الحجز الموحد", fontSize = 9.sp, color = BrandAccent)
                            Text("#${booking.id}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                        }
                    }

                    // Car details helper row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("المركبة وتصنيفها", fontSize = 9.sp, color = BrandAccent)
                            Text(car?.model ?: "مركبة غير محددة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("رقم اللوحة", fontSize = 9.sp, color = BrandAccent)
                            Text(car?.plateNo ?: "جاري الفحص", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
                        }
                    }

                    // Rental Dates info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("موعد الاستلام", fontSize = 9.sp, color = BrandAccent)
                            Text(pickupString, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("مدة العقد", fontSize = 9.sp, color = BrandAccent)
                            Text("$durationDays يوم", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = BrandPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("موعد الإرجاع", fontSize = 9.sp, color = BrandAccent)
                            Text(returnString, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
                        }
                    }

                    Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                    // Total cost & payment status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("التكلفة الإجمالية (ر.س)", fontSize = 9.sp, color = BrandAccent)
                            Text("${booking.totalAmount} ر.س", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = BrandGreen)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BrandGreen.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("تم الدفع كاملاً", color = BrandGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // QR Code Render Canvas
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val sizeRaw = size.width
                        val cells = 15
                        val cellSize = sizeRaw / cells

                        fun drawBlock(x: Int, y: Int) {
                            drawRect(
                                color = Color(0xFF0F172A),
                                topLeft = Offset(x * cellSize, y * cellSize),
                                size = androidx.compose.ui.geometry.Size(cellSize + 0.5f, cellSize + 0.5f)
                            )
                        }

                        val random = kotlin.random.Random(booking.id.toLong() + 1045)

                        // Finder pattern Top-Left
                        for (i in 0..5) {
                            for (j in 0..5) {
                                if (i == 0 || i == 5 || j == 0 || j == 5 || (i in 2..3 && j in 2..3)) {
                                    drawBlock(i, j)
                                }
                            }
                        }

                        // Finder pattern Top-Right
                        for (i in (cells - 6) until cells) {
                            for (j in 0..5) {
                                val rx = i - (cells - 6)
                                if (rx == 0 || rx == 5 || j == 0 || j == 5 || (rx in 2..3 && j in 2..3)) {
                                    drawBlock(i, j)
                                }
                            }
                        }

                        // Finder pattern Bottom-Left
                        for (i in 0..5) {
                            for (j in (cells - 6) until cells) {
                                val ry = j - (cells - 6)
                                if (i == 0 || i == 5 || ry == 0 || ry == 5 || (i in 2..3 && ry in 2..3)) {
                                    drawBlock(i, j)
                                }
                            }
                        }

                        // Code details random blocks
                        for (i in 0 until cells) {
                            for (j in 0 until cells) {
                                val inTopLeft = i < 7 && j < 7
                                val inTopRight = i >= cells - 7 && j < 7
                                val inBottomLeft = i < 7 && j >= cells - 7
                                if (!inTopLeft && !inTopRight && !inBottomLeft) {
                                    if (random.nextBoolean()) {
                                        drawBlock(i, j)
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إغلاق", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
                    }

                    Button(
                        onClick = {
                            // Placeholder button action
                        },
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حفظ الصورة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BookingCardItem(
    booking: Booking,
    car: Car?,
    onUpdateStatus: (String) -> Unit,
    onRateClick: () -> Unit,
    onShowSummaryClick: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
    val pickupString = remember(booking.pickupDate) { sdf.format(Date(booking.pickupDate)) }
    val returnString = remember(booking.returnDate) { sdf.format(Date(booking.returnDate)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("booking_card_${booking.customerName}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BrandCard),
        border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "عقد حجز رقم #${booking.id}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = BrandAccent
                    )
                    Text(
                        text = booking.customerName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = BrandSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // QR Quick Button
                    OutlinedButton(
                        onClick = onShowSummaryClick,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.QrCode,
                            contentDescription = "بطاقة QR",
                            tint = BrandPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("الـ QR والكرت", fontSize = 10.sp, color = BrandPrimary, fontWeight = FontWeight.Bold)
                    }

                    // Booking Status
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (booking.status) {
                                    "مؤكد" -> BrandGreen.copy(alpha = 0.15f)
                                    "قيد التنفيذ" -> BrandOrange.copy(alpha = 0.15f)
                                    "مكتمل" -> BrandPrimary.copy(alpha = 0.15f)
                                    else -> Color.LightGray.copy(alpha = 0.35f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = booking.status,
                            color = when (booking.status) {
                                "مؤكد" -> BrandGreen
                                "قيد التنفيذ" -> BrandOrange
                                "مكتمل" -> BrandPrimary
                                else -> BrandSecondary
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFE1E2EC))
            Spacer(modifier = Modifier.height(12.dp))

            // Car associated details
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.DirectionsCar, contentDescription = "سيارة", tint = BrandPrimary)
                }
                Column {
                    Text(
                        text = car?.model ?: "سيارة غير متوفرة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BrandSecondary
                    )
                    Text(
                        text = "لوحة: ${car?.plateNo ?: "جاري الحذف"}",
                        fontSize = 12.sp,
                        color = BrandAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pickup & Return dates representation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF3F0F5))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("تاريخ الاستلام", fontSize = 10.sp, color = BrandAccent)
                    Text(pickupString, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
                }

                Icon(Icons.Filled.ArrowBack, contentDescription = "RTL", tint = BrandAccent, modifier = Modifier.size(16.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text("تاريخ التسليم", fontSize = 10.sp, color = BrandAccent)
                    Text(returnString, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Total amount and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("إجمالي التكلفة المدفوعة", fontSize = 10.sp, color = BrandAccent)
                    Text("${booking.totalAmount} ر.س", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = BrandPrimary)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (booking.status == "مؤكد") {
                        Button(
                            onClick = { onUpdateStatus("قيد التنفيذ") },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("بدء التسليم", fontSize = 11.sp, color = Color.White)
                        }
                    } else if (booking.status == "قيد التنفيذ") {
                        Button(
                            onClick = { onUpdateStatus("مكتمل") },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("استعادة السيارة", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    if (booking.status == "مكتمل") {
                        if (booking.rating == null) {
                            OutlinedButton(
                                onClick = onRateClick,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, BrandPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Star, contentDescription = "تقييم", tint = BrandOrange, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تقييم الخدمة", fontSize = 11.sp, color = BrandPrimary)
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BrandOrange.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.Star, contentDescription = "تم التقييم", tint = BrandOrange, modifier = Modifier.size(14.dp))
                                Text("${booking.rating}/5", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandOrange)
                            }
                        }
                    }
                }
            }

            // Rating message review drawer
            if (booking.ratingComment != null && booking.ratingComment.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandSurface.copy(alpha = 0.5f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "ملاحظة العميل: \"${booking.ratingComment}\"",
                        fontSize = 11.sp,
                        color = BrandSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// 4. FINANCIAL ACCOUNTS SCREEN (خزائن وبنوك)
@Composable
fun FinanceScreen(
    viewModel: CarRentalViewModel,
    onAddAccountClick: () -> Unit,
    onTransactionClick: () -> Unit
) {
    val accountsList by viewModel.accounts.collectAsState()
    val transactionsList by viewModel.transactions.collectAsState()

    val safes = accountsList.filter { it.type == "SAFE" }
    val banks = accountsList.filter { it.type == "BANK" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("finance_scroll"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "الصناديق والحسابات المالية",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandSecondary
                    )
                    Text(
                        text = "إشراف ومراقبة على الخزائن، الحسابات البنكية وحركات الدفع.",
                        fontSize = 13.sp,
                        color = BrandAccent
                    )
                }
            }
        }

        // Action Toolbar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onTransactionClick,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = "سحب/إيداع")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("عملية مالية سريعة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onAddAccountClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BrandPrimary)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "إضافة خزينة/بنك", tint = BrandPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة حساب", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandPrimary)
                }
            }
        }

        // SECTION 1: SECURE SAFES (صناديق وخزائن)
        item {
            Text(
                text = "صناديق وخزائن كاش",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = BrandSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (safes.isEmpty()) {
            item { EmptyStateCard("لا توجد خزائن كاش مسجلة.", "") }
        } else {
            items(safes) { safe ->
                AccountItemDisplay(account = safe, isSafe = true)
            }
        }

        // SECTION 2: BANK GENERAL ACCOUNTS (بنوك ومصارف)
        item {
            Text(
                text = "البنوك والمصارف المشتركة",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = BrandSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (banks.isEmpty()) {
            item { EmptyStateCard("لا توجد حسابات بنكية مسجلة.", "") }
        } else {
            items(banks) { bank ->
                AccountItemDisplay(account = bank, isSafe = false)
            }
        }

        // SECTION 3: TRANSACTION LOGS
        item {
            Text(
                text = "سجل القيود والعمليات الحالية",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = BrandSecondary,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        if (transactionsList.isEmpty()) {
            item { EmptyStateCard("لا توجد معاملات مالية مسجلة بعد.", "") }
        } else {
            items(transactionsList) { trans ->
                val associatedAcc = accountsList.find { it.id == trans.accountId }
                TransactionRowItem(transaction = trans, account = associatedAcc)
            }
        }
    }
}

@Composable
fun AccountItemDisplay(account: FinancialAccount, isSafe: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("acc_card_${account.name}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandCard),
        border = BorderStroke(1.dp, Color(0xFFE1E2EC))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSafe) BrandIndigo.copy(alpha = 0.15f) else BrandTeal.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSafe) Icons.Filled.Store else Icons.Filled.AccountBalance,
                        contentDescription = "أيقونة مالي",
                        tint = if (isSafe) BrandIndigo else BrandTeal
                    )
                }

                Column {
                    Text(
                        text = account.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BrandSecondary
                    )
                    Text(
                        text = if (isSafe) "صندوق كشف يدوي" else "حساب شبكة ومصرف",
                        fontSize = 10.sp,
                        color = BrandAccent
                    )
                }
            }

            Text(
                text = "${String.format("%,.2f", account.balance)} ر.س",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = BrandPrimary
            )
        }
    }
}

@Composable
fun TransactionRowItem(transaction: Transaction, account: FinancialAccount?) {
    val sdf = remember { SimpleDateFormat("HH:mm - yyyy/MM/dd", Locale.getDefault()) }
    val dateString = remember(transaction.timestamp) { sdf.format(Date(transaction.timestamp)) }
    val isDeposit = transaction.type == "إيداع"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BrandCard)
            .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDeposit) BrandGreen.copy(alpha = 0.15f) else BrandRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDeposit) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                        contentDescription = "نوع العملة",
                        tint = if (isDeposit) BrandGreen else BrandRed,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = transaction.description,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "الحساب: ${account?.name ?: "الرئيسي"} • $dateString",
                        fontSize = 9.sp,
                        color = BrandAccent
                    )
                }
            }

            Text(
                text = "${if (isDeposit) "+" else "-"}${transaction.amount} ر.س",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDeposit) BrandGreen else BrandRed
            )
        }
    }
}

// 5. REPORTS SCREEN
@Composable
fun ReportsScreen(viewModel: CarRentalViewModel) {
    val carsList by viewModel.cars.collectAsState()
    val bookingsList by viewModel.bookings.collectAsState()
    val accountsList by viewModel.accounts.collectAsState()

    val totalCashFluidity = accountsList.sumOf { it.balance }
    val totalRentedCount = carsList.count { it.status == "مؤجرة" }
    val totalBookingsMade = bookingsList.size

    val ratingsList = bookingsList.mapNotNull { it.rating }
    val avgRating = if (ratingsList.isNotEmpty()) ratingsList.average() else 4.8

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reports_scroll"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "تقارير وتحليلات الأداء",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandSecondary
                )
                Text(
                    text = "تقييم مؤشرات الكفاءة التشغيلية ونسب التوظيف والمالية.",
                    fontSize = 13.sp,
                    color = BrandAccent
                )
            }
        }

        // Summary Analytics Grid using 3 visual boxes
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Rate Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BrandPrimary.copy(alpha = 0.08f))
                        .border(1.dp, BrandPrimary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Text("تقييم الخدمة", fontSize = 10.sp, color = BrandAccent, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Star, contentDescription = "نجمة", tint = BrandOrange, modifier = Modifier.size(18.dp))
                        Text("${String.format("%.1f", avgRating)}/5", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = BrandSecondary)
                    }
                }

                // Booking count
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BrandTeal.copy(alpha = 0.08f))
                        .border(1.dp, BrandTeal.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Text("إجمالي الحجوزات", fontSize = 10.sp, color = BrandAccent, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$totalBookingsMade حجز مبرم", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = BrandSecondary)
                }

                // Fleet usage
                val usagePercent = if (carsList.isNotEmpty()) (totalRentedCount.toFloat() / carsList.size * 100).toInt() else 25
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BrandIndigo.copy(alpha = 0.08f))
                        .border(1.dp, BrandIndigo.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Text("نسبة تشغيل الأسطول", fontSize = 10.sp, color = BrandAccent, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$usagePercent%", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = BrandSecondary)
                }
            }
        }

        // Interactive Revenue Chart (Recharts-inspired design in Jetpack Compose)
        item {
            InteractiveRevenueChart(bookings = bookingsList)
        }

        // Custom visual chart using standard Jetpack Compose canvas styling!
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chart_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BrandCard),
                border = BorderStroke(1.dp, Color(0xFFE1E2EC))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "مؤشر حركة الحسابات البنكية والخزائن",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Minimal visual bar graph representation of accounts balances
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        accountsList.forEach { acc ->
                            val maxBalance = remember(accountsList) { accountsList.maxOfOrNull { it.balance } ?: 1.0 }
                            val normalizedRatio = remember(acc.balance, maxBalance) {
                                if (maxBalance > 0) (acc.balance / maxBalance).toFloat() else 0f
                            }
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(acc.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
                                    Text("${acc.balance} ر.س", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE1E2EC))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(normalizedRatio.coerceIn(0.01f, 1f))
                                            .background(
                                                if (acc.type == "SAFE") BrandIndigo else BrandTeal
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Customer Reviews Grid (الآراء والتقييمات للعملاء)
        item {
            Text(
                text = "ملاحظات العملاء وتقييماتهم",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = BrandSecondary,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        val reviewedBookings = bookingsList.filter { it.rating != null }
        if (reviewedBookings.isEmpty()) {
            item {
                EmptyStateCard(
                    message = "لا توجد ملاحظات أو تقييمات مكتملة من العملاء بعد.",
                    subMessage = "عند اكتمال أي حجز، يمكنك دعوة العميل لتقييم الخدمة."
                )
            }
        } else {
            items(reviewedBookings) { booking ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BrandCard)
                        .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(booking.customerName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandSecondary)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(Icons.Filled.Star, "نجمة", tint = BrandOrange, modifier = Modifier.size(14.dp))
                                Text("${booking.rating ?: 5.0}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandOrange)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = booking.ratingComment ?: "الخدمة ممتازة وسريعة جداً، شكرًا لكم.",
                            fontSize = 12.sp,
                            color = BrandAccent
                        )
                    }
                }
            }
        }
    }
}

// SHARED VIEW COMPONENTS
@Composable
fun EmptyStateCard(message: String, subMessage: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F0F5)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.SearchOff,
                contentDescription = "لا يوجد",
                tint = BrandAccent.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = BrandSecondary,
                textAlign = TextAlign.Center
            )
            if (subMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subMessage,
                    fontSize = 11.sp,
                    color = BrandAccent,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun BookingRowItem(booking: Booking, car: Car?) {
    val sdf = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
    val startText = remember(booking.pickupDate) { sdf.format(Date(booking.pickupDate)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandCard)
            .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.DirectionsCar, contentDescription = "سيارة", tint = BrandPrimary)
                }

                Column {
                    Text(
                        text = car?.model ?: "سيارة مستعارة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BrandSecondary
                    )
                    Text(
                        text = "العميل: ${booking.customerName}",
                        fontSize = 11.sp,
                        color = BrandAccent
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = booking.status,
                    color = BrandGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(
                    text = startText,
                    fontSize = 10.sp,
                    color = BrandAccent
                )
            }
        }
    }
}

// DIALOG IMPLEMENTATIONS CODES
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCarDialog(
    onDismiss: () -> Unit,
    onSave: (type: String, plate: String, chassis: String, color: String, model: String, mileage: String, price: String, images: List<String>) -> Unit
) {
    var brandModel by remember { mutableStateOf("") }
    var plateNo by remember { mutableStateOf("") }
    var chassisNo by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("سيدان") }
    var mileage by remember { mutableStateOf("") }
    var rentPrice by remember { mutableStateOf("") }
    var imageLink by remember { mutableStateOf("") }

    val typesList = listOf("سيدان", "دفع رباعي", "عائلية", "فاخرة", "كهربائية")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandCard)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "تسجيل سيارة جديدة بالأسطول",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = BrandSecondary
                )

                OutlinedTextField(
                    value = brandModel,
                    onValueChange = { brandModel = it },
                    label = { Text("نوع السيارة والموديل (e.g. نيسان سنترا 2024)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = plateNo,
                    onValueChange = { plateNo = it },
                    label = { Text("رقم اللوحة (e.g. أ ب ج 1234)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = chassisNo,
                    onValueChange = { chassisNo = it },
                    label = { Text("رقم الهيكل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("اللون الخارجي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Type Dropdown selection simulation in cards
                Text("نوع السيارة (التصنيف):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(typesList) { type ->
                        val isSelected = selectedType == type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) BrandPrimary else BrandSurface)
                                .clickable { selectedType = type }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(type, color = if (isSelected) Color.White else BrandSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = mileage,
                    onValueChange = { mileage = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("عداد المشي الحالي (كم)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rentPrice,
                    onValueChange = { rentPrice = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text("سعر التأجير اليومي (ر.س)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = imageLink,
                    onValueChange = { imageLink = it },
                    label = { Text("رابط صورة السيارة (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val imgList = if (imageLink.isBlank()) emptyList() else listOf(imageLink)
                            onSave(selectedType, plateNo, chassisNo, color, brandModel, mileage, rentPrice, imgList)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        modifier = Modifier.weight(1f),
                        enabled = brandModel.isNotBlank() && plateNo.isNotBlank()
                    ) {
                        Text("حفظ وتسجيل")
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}

@Composable
fun AddBookingDialog(
    cars: List<Car>,
    accounts: List<FinancialAccount>,
    preSelectedCarId: Int? = null,
    onDismiss: () -> Unit,
    onSave: (carId: Int, name: String, phone: String, pickupDate: Long, returnDate: Long, totalAmount: Double, accountId: Int) -> Unit
) {
    val context = LocalContext.current
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var selectedCar by remember { mutableStateOf<Car?>(null) }
    var selectedAccount by remember { mutableStateOf<FinancialAccount?>(null) }
    
    // Manage actual timestamps
    val now = System.currentTimeMillis()
    var pickupDate by remember { mutableStateOf(now) }
    var returnDate by remember { mutableStateOf(now + 24L * 3600L * 1000L) } // Default +1 day

    LaunchedEffect(cars, preSelectedCarId) {
        if (cars.isNotEmpty()) {
            selectedCar = cars.find { it.id == preSelectedCarId } ?: cars.first()
        }
    }

    LaunchedEffect(accounts) {
        if (accounts.isNotEmpty()) selectedAccount = accounts.first()
    }

    // Automatically calculate duration based on picked dates
    val durationDaysCalculated = remember(pickupDate, returnDate) {
        val diff = returnDate - pickupDate
        val days = (diff.toDouble() / (24.0 * 3600.0 * 1000.0)).coerceAtLeast(1.0)
        kotlin.math.ceil(days).toInt()
    }

    val totalCost = remember(selectedCar, durationDaysCalculated) {
        val price = selectedCar?.rentPricePerDay ?: 0.0
        durationDaysCalculated * price
    }

    // Simple date formatter helper
    val formattedPickup = remember(pickupDate) {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("ar"))
        sdf.format(Date(pickupDate))
    }
    
    val formattedReturn = remember(returnDate) {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("ar"))
        sdf.format(Date(returnDate))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandCard)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "عقد حجز وتأجير سيارة",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = BrandSecondary
                )

                // Selected vehicle label trigger
                Text("اختر السيارة المتاحة:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (cars.isEmpty()) {
                    Text("لا توجد سيارات متاحة لتأجيرها حالياً!", color = BrandRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(cars) { car ->
                            val isSelected = selectedCar?.id == car.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) BrandPrimary else BrandSurface)
                                    .clickable { selectedCar = car }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = car.model,
                                    color = if (isSelected) Color.White else BrandSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("اسم العميل بالكامل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    label = { Text("هاتف العميل الجوال") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Beautiful interactive Date pickers instead of typing!
                Text("تحديد تواريخ عقد الحجز:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pickup Date Button
                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance().apply { timeInMillis = pickupDate }
                            val dpd = android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val sel = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                    }
                                    pickupDate = sel.timeInMillis
                                    // Ensure returnDate is at least 1 day after pickupDate
                                    if (returnDate <= pickupDate) {
                                        returnDate = pickupDate + 24L * 3600L * 1000L
                                    }
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            )
                            dpd.show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("تاريخ الاستلام", fontSize = 9.sp, color = BrandAccent)
                            Text(formattedPickup, fontSize = 11.sp, color = BrandSecondary, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Return Date Button
                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance().apply { timeInMillis = returnDate }
                            val dpd = android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val sel = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        set(Calendar.HOUR_OF_DAY, 23)
                                        set(Calendar.MINUTE, 59)
                                        set(Calendar.SECOND, 59)
                                    }
                                    returnDate = sel.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            )
                            dpd.show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("تاريخ التسليم", fontSize = 9.sp, color = BrandAccent)
                            Text(formattedReturn, fontSize = 11.sp, color = BrandSecondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Calculated Duration badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandPrimary.copy(alpha = 0.05f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("مدة التأجير المحسوبة تلقائياً:", fontSize = 10.sp, color = BrandAccent)
                        Text("$durationDaysCalculated أيام", fontSize = 11.sp, color = BrandPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                Text("صندوق أو بنك السداد والتسوية:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(accounts) { acc ->
                        val isSelected = selectedAccount?.id == acc.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) BrandPrimary else BrandSurface)
                                .clickable { selectedAccount = acc }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = acc.name,
                                color = if (isSelected) Color.White else BrandSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Price total display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BrandPrimary.copy(alpha = 0.08f))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("المبلغ الإجمالي المستحق:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
                        Text("$totalCost ر.س", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = BrandPrimary)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val carId = selectedCar?.id ?: 0
                            val accId = selectedAccount?.id ?: 0
                            onSave(carId, customerName, customerPhone, pickupDate, returnDate, totalCost, accId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        modifier = Modifier.weight(1f),
                        enabled = customerName.isNotBlank() && customerPhone.isNotBlank() && selectedCar != null && selectedAccount != null && returnDate > pickupDate
                    ) {
                        Text("تأكيد وحفظ العقد")
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, startingBalance: String) -> Unit
) {
    var accountName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("SAFE") } // "SAFE" dynamic custom bank types
    var balance by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandCard)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "إضافة صندوق كاش أو بنك",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = BrandSecondary
                )

                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("اسم الحساب (مثال: عهدة الفرع، بنك الراجحي)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("تصنيف الحساب:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedType = "SAFE" },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedType == "SAFE") BrandPrimary.copy(alpha = 0.15f) else BrandSurface
                        ),
                        border = if (selectedType == "SAFE") BorderStroke(2.dp, BrandPrimary) else null
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Store, "خزينة كاش", tint = BrandPrimary)
                            Text("خزينة (كاش)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedType = "BANK" },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedType == "BANK") BrandPrimary.copy(alpha = 0.15f) else BrandSurface
                        ),
                        border = if (selectedType == "BANK") BorderStroke(2.dp, BrandPrimary) else null
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.AccountBalance, "بنك", tint = BrandPrimary)
                            Text("حساب بنكي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text("الرصيد الافتتاحي (ر.س)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onSave(accountName, selectedType, balance) },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        modifier = Modifier.weight(1f),
                        enabled = accountName.isNotBlank()
                    ) {
                        Text("إضافة")
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}

@Composable
fun ManualTransactionDialog(
    accounts: List<FinancialAccount>,
    onDismiss: () -> Unit,
    onSave: (accountId: Int, amount: String, type: String, description: String) -> Unit
) {
    var selectedAccount by remember { mutableStateOf<FinancialAccount?>(null) }
    var selectedType by remember { mutableStateOf("إيداع") } // "إيداع" or "سحب"
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    LaunchedEffect(accounts) {
        if (accounts.isNotEmpty()) selectedAccount = accounts.first()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandCard)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "تسجيل قيد مالي يدوي",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = BrandSecondary
                )

                Text("اختر الصندوق أو الحساب البنكي:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(accounts) { acc ->
                        val isSelected = selectedAccount?.id == acc.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) BrandPrimary else BrandSurface)
                                .clickable { selectedAccount = acc }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = acc.name,
                                color = if (isSelected) Color.White else BrandSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text("نوع المعاملة المباشرة:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { selectedType = "إيداع" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "إيداع") BrandGreen else BrandSurface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إيداع (+)", color = if (selectedType == "إيداع") Color.White else BrandSecondary, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { selectedType = "سحب" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "سحب") BrandRed else BrandSurface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("سحب (-)", color = if (selectedType == "سحب") Color.White else BrandSecondary, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("المبلغ (ر.س)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("سبب العملية / التفاصيل (e.g. نفقات وقود)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val accId = selectedAccount?.id ?: 0
                            onSave(accId, amount, selectedType, description)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        modifier = Modifier.weight(1f),
                        enabled = amount.isNotBlank() && description.isNotBlank() && selectedAccount != null
                    ) {
                        Text("تسجيل وحفظ")
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}

@Composable
fun RatingDialog(
    targetLabel: String,
    onDismiss: () -> Unit,
    onRate: (rating: Float, comment: String) -> Unit
) {
    var selectedRating by remember { mutableStateOf(5f) }
    var userComment by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BrandCard)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "تقييم العميل وملاحظات الخدمة",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = BrandSecondary
                )
                Text(
                    text = targetLabel,
                    fontSize = 11.sp,
                    color = BrandAccent,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Rate slider or starts indicator simulation
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { star ->
                        val isStarred = star <= selectedRating
                        Icon(
                            imageVector = if (isStarred) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = "Star",
                            tint = if (isStarred) BrandOrange else Color.LightGray,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { selectedRating = star.toFloat() }
                        )
                    }
                }

                Text(
                    text = "التقييم المستحق: $selectedRating من 5",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandSecondary
                )

                OutlinedTextField(
                    value = userComment,
                    onValueChange = { userComment = it },
                    label = { Text("تعليق أو ملاحظات العميل (اختياري)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onRate(selectedRating, userComment) },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إرسال التقييم")
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}

@Composable
fun SaudiPlateViewer(plateNo: String) {
    val parts = remember(plateNo) { plateNo.split(" ") }
    val letters = remember(parts) { parts.filter { it.any { c -> c.isLetter() } }.joinToString(" ") }
    val numbers = remember(parts) { parts.filter { it.any { c -> c.isDigit() } }.joinToString(" ") }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, Color(0xFF1E293B)),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .width(150.dp)
            .height(52.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFFF1F5F9))
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("السعودية", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                Text("KSA", fontSize = 7.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
            }
            
            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Gray))

            Column(
                modifier = Modifier
                    .weight(2.5f)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = letters.ifBlank { plateNo },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = numbers.ifBlank { "" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CarDetailsDialog(
    car: Car,
    onDismiss: () -> Unit,
    onUpdateMileage: (Int) -> Unit,
    onRentClick: () -> Unit
) {
    val imageList = remember(car.images) { car.images.split(",").filter { it.isNotBlank() } }
    var currentImgIndex by remember { mutableStateOf(0) }
    var showUpdateMileageDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تفاصيل المركبة الفنية",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = BrandSecondary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = BrandSecondary)
                    }
                }

                // Image Carousel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BrandSurface)
                ) {
                    if (imageList.isNotEmpty()) {
                        val currentImageUrl = imageList[currentImgIndex.coerceIn(0, imageList.lastIndex)]
                        AsyncImage(
                            model = currentImageUrl,
                            contentDescription = car.model,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Chevron Overlays if multiple images exist
                        if (imageList.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        currentImgIndex = if (currentImgIndex == 0) imageList.lastIndex else currentImgIndex - 1
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.White.copy(alpha = 0.75f), CircleShape)
                                ) {
                                    Icon(Icons.Default.ArrowForward, contentDescription = "التالي", tint = Color.Black)
                                }

                                IconButton(
                                    onClick = {
                                        currentImgIndex = if (currentImgIndex == imageList.lastIndex) 0 else currentImgIndex + 1
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.White.copy(alpha = 0.75f), CircleShape)
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "السابق", tint = Color.Black)
                                }
                            }

                            // Dots indicators
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                imageList.forEachIndexed { idx, _ ->
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (idx == currentImgIndex) BrandPrimary else Color.White.copy(alpha = 0.5f))
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.DirectionsCar, contentDescription = "بدون صورة", tint = BrandAccent, modifier = Modifier.size(50.dp))
                        }
                    }
                }

                // Model and Badges
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = car.model,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandSecondary
                        )
                        Text(
                            text = "${car.rentPricePerDay} ر.س / يوم",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = BrandPrimary
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandPrimary.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(car.type, color = BrandPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        val isAvailable = car.status == "متاحة"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isAvailable) BrandGreen.copy(alpha = 0.12f) else BrandOrange.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = car.status,
                                color = if (isAvailable) BrandGreen else BrandOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Divider(color = Color(0xFFE1E2EC))

                // Technical Specs Grid
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("المواصفات الفنية والرموز", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandSecondary)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Color
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("لون الهيكل", fontSize = 10.sp, color = BrandAccent)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    car.color.contains("أسود") -> Color.Black
                                                    car.color.contains("أبيض") -> Color.White
                                                    car.color.contains("كحلي") -> Color(0xFF1E3A8A)
                                                    car.color.contains("رمادي") -> Color.Gray
                                                    else -> BrandPrimary
                                                }
                                            )
                                            .border(0.5.dp, Color.LightGray, CircleShape)
                                    )
                                    Text(car.color, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
                                }
                            }
                        }

                        // Mileage
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("عداد الكيلومترات", fontSize = 10.sp, color = BrandAccent)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${car.mileage} كم", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "تعديل العداد",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { showUpdateMileageDialog = true },
                                        tint = BrandPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Chassis & Plate Saudi Render Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text("رقم الهيكل (VIN)", fontSize = 10.sp, color = BrandAccent)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = car.chassisNo,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        SaudiPlateViewer(plateNo = car.plateNo)
                    }
                }

                Divider(color = Color(0xFFE1E2EC))

                // Rental action / Warning message
                if (car.status == "متاحة") {
                    Button(
                        onClick = onRentClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("details_rent_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                    ) {
                        Icon(Icons.Filled.Handshake, contentDescription = "تأجير")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إبرام عقد تأجير لهذه المركبة", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandOrange.copy(alpha = 0.08f))
                            .border(1.dp, BrandOrange.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = "تنبيه", tint = BrandOrange)
                            Text(
                                text = "السيارة غير متاحة للتأجير حالياً بسبب حالتها: (${car.status})",
                                color = BrandOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showUpdateMileageDialog) {
        var textInput by remember { mutableStateOf(car.mileage.toString()) }
        AlertDialog(
            onDismissRequest = { showUpdateMileageDialog = false },
            title = { Text("تحديث قراءة العداد", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("المسافة الحالية المقطوعة (كم)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = textInput.toIntOrNull() ?: car.mileage
                        onUpdateMileage(parsed)
                        showUpdateMileageDialog = false
                    }
                ) {
                    Text("تحديث")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateMileageDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

// ----------------------------------------------------------------------------------
// RECHARTS-INSPIRED INTERACTIVE REVENUE VISUALIZATIONS SECTION
// ----------------------------------------------------------------------------------

data class ChartPoint(
    val label: String,      // e.g., "السبت" or "يونيو"
    val value: Double,     // Current revenue
    val count: Int,        // Number of bookings
    val percent: Float = 0f // Normalized ratio for bar height (0..1)
)

fun getDailyRevenueData(bookings: List<Booking>): List<ChartPoint> {
    val result = mutableListOf<ChartPoint>()
    val calendar = Calendar.getInstance()
    
    val sdfDay = SimpleDateFormat("EEEE", Locale("ar"))
    val sdfDate = SimpleDateFormat("d MMM", Locale("ar"))
    
    for (i in 6 downTo 0) {
        val testCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -i)
        }
        val dayName = sdfDay.format(testCal.time) // E.g., "الخميس"
        
        // Start and end of this calendar day
        val dayStart = testCal.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val dayEnd = testCal.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        
        // Exclude cancelled bookings ("ملغي")
        val filtered = bookings.filter { 
            it.status != "ملغي" && it.pickupDate >= dayStart && it.pickupDate <= dayEnd 
        }
        val sum = filtered.sumOf { it.totalAmount }
        val count = filtered.size
        
        result.add(ChartPoint(label = dayName, value = sum, count = count))
    }
    
    // Normalize percentage
    val maxVal = result.maxOfOrNull { it.value } ?: 1.0
    val divisor = if (maxVal > 0) maxVal else 1.0
    return result.map { it.copy(percent = (it.value / divisor).toFloat()) }
}

fun getMonthlyRevenueData(bookings: List<Booking>): List<ChartPoint> {
    val result = mutableListOf<ChartPoint>()
    val sdfMonth = SimpleDateFormat("MMMM", Locale("ar")) // E.g., "يونيو"
    
    for (i in 5 downTo 0) {
        val testCal = Calendar.getInstance().apply {
            add(Calendar.MONTH, -i)
        }
        val monthName = sdfMonth.format(testCal.time)
        
        val monthStart = testCal.apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val monthEnd = testCal.apply {
            set(Calendar.DAY_OF_MONTH, testCal.getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        
        // Exclude cancelled bookings ("ملغي")
        val filtered = bookings.filter { 
            it.status != "ملغي" && it.pickupDate >= monthStart && it.pickupDate <= monthEnd 
        }
        val sum = filtered.sumOf { it.totalAmount }
        val count = filtered.size
        
        result.add(ChartPoint(label = monthName, value = sum, count = count))
    }
    
    // Normalize percentage
    val maxVal = result.maxOfOrNull { it.value } ?: 1.0
    val divisor = if (maxVal > 0) maxVal else 1.0
    return result.map { it.copy(percent = (it.value / divisor).toFloat()) }
}

@Composable
fun InteractiveRevenueChart(bookings: List<Booking>) {
    var selectedTab by remember { mutableStateOf("daily") } // "daily" or "monthly"
    var selectedPoint by remember { mutableStateOf<ChartPoint?>(null) }

    val dailyPoints = remember(bookings) { getDailyRevenueData(bookings) }
    val monthlyPoints = remember(bookings) { getMonthlyRevenueData(bookings) }

    val currentPoints = if (selectedTab == "daily") dailyPoints else monthlyPoints
    val totalRevenueText = remember(currentPoints) {
        currentPoints.sumOf { it.value }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("revenue_chart_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BrandCard),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title and Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "تحليلات الإيرادات والعوائد",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = BrandSecondary
                    )
                    Text(
                        text = if (selectedTab == "daily") "توزيع الإيرادات لآخر 7 أيام" else "توزيع الإيرادات لآخر 6 أشهر",
                        fontSize = 11.sp,
                        color = BrandAccent
                    )
                }

                // Beautiful Modern Tab Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == "daily") BrandPrimary else Color.Transparent)
                            .clickable {
                                selectedTab = "daily"
                                selectedPoint = null
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "يومي",
                            color = if (selectedTab == "daily") Color.White else BrandSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == "monthly") BrandPrimary else Color.Transparent)
                            .clickable {
                                selectedTab = "monthly"
                                selectedPoint = null
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "شهري",
                            color = if (selectedTab == "monthly") Color.White else BrandSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Highlighting total revenue in timeframe
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("إجمالي دخل الفترة المحتسبة", fontSize = 10.sp, color = BrandAccent, fontWeight = FontWeight.Bold)
                    Text("${String.format("%,.2f", totalRevenueText)} ر.س", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = BrandPrimary)
                }

                // Legend indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(BrandPrimary, BrandTeal)
                                )
                            )
                    )
                    Text("إيرادات الإيجار", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
                }
            }

            // Chart bar visual columns
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(vertical = 8.dp)
            ) {
                // 1. Grid Background lines (Canvas)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    val numLines = 4
                    val step = canvasHeight / (numLines + 1)
                    for (i in 1..numLines) {
                        val y = step * i
                        drawLine(
                            color = Color(0xFFE2E8F0),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(canvasWidth, y),
                            strokeWidth = 1f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }
                }

                // 2. Interactive Column bars
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    currentPoints.forEach { point ->
                        val isSelected = selectedPoint?.label == point.label
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    selectedPoint = if (isSelected) null else point
                                },
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                // Background empty pillar path
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(18.dp)
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(Color(0xFFF1F5F9))
                                )

                                // Filled revenue progress pillar with elegant top curves gradient 
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(point.percent.coerceIn(0.04f, 1f))
                                        .width(18.dp)
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(
                                            if (isSelected) {
                                                Brush.verticalGradient(
                                                    colors = listOf(BrandIndigo, BrandPrimary)
                                                )
                                            } else {
                                                Brush.verticalGradient(
                                                    colors = listOf(BrandPrimary, BrandTeal)
                                                )
                                            }
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                        )
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))

                            // X-axis label text values
                            Text(
                                text = if (selectedTab == "daily") point.label.take(4) else point.label.take(5),
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isSelected) BrandPrimary else BrandAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 3. Floating Tooltip Overlay (styled to resemble Recharts' overlays)
                selectedPoint?.let { point ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 4.dp)
                            .shadow(6.dp, RoundedCornerShape(12.dp))
                            .background(BrandSecondary)
                            .border(1.dp, BrandPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = point.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${String.format("%,.2f", point.value)} ر.س",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF38BDF8)
                            )
                            Text(
                                text = "عدد العقود: ${point.count}",
                                fontSize = 9.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }

            // Quick instruction help footer to guide interactive user behaviors
            Text(
                text = "💡 انقر فوق أي عمود في الرسم البياني لعرض تفاصيل الإيرادات التفاعلية لتلك الفترة.",
                fontSize = 9.5.sp,
                color = BrandAccent,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// =========================================================================
// 8. USER PROFILE SCREEN (REGISTRATION, SECURE LOGIN & ACTIVE CLIENT PROFILE)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(viewModel: CarRentalViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current

    // For login / registration forms
    var isSignUpMode by remember { mutableStateOf(false) }
    
    var loginUsername by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }

    var regUsername by remember { mutableStateOf("") }
    var regFullName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regLicenseNo by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }

    // For updating profile
    var upFullName by remember { mutableStateOf("") }
    var upEmail by remember { mutableStateOf("") }
    var upPhone by remember { mutableStateOf("") }
    var upLicenseNo by remember { mutableStateOf("") }
    var upPassword by remember { mutableStateOf("") }

    LaunchedEffect(currentUser) {
        currentUser?.let {
            upFullName = it.fullName
            upEmail = it.email
            upPhone = it.phone
            upLicenseNo = it.licenseNo
            upPassword = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ThemeSettingsCard(viewModel = viewModel)

            if (currentUser == null) {
            // LOGIN & REGISTRATION SELECTION CARD
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BrandCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp)),
                border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Logo and Slogan
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(BrandPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = if (isSignUpMode) "إنشاء حساب مستأجر جديد" else "تسجيل الدخول لبوابة التأجير",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandSecondary
                    )

                    Text(
                        text = if (isSignUpMode) "قم بتسجيل بياناتك الشخصية لتتمكن من إتمام عملية الحجز وتتبع سياراتك المستأجرة" else "الرجاء تسجيل الدخول للوصول الآمن لبيانات الحجز وسياراتك النشطة",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = BrandAccent
                    )

                    // Tab Chooser
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE2E8F0))
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isSignUpMode) Color.White else Color.Transparent)
                                .clickable { isSignUpMode = false }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("دخول", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (!isSignUpMode) BrandSecondary else BrandAccent)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSignUpMode) Color.White else Color.Transparent)
                                .clickable { isSignUpMode = true }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("تسجيل جديد", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isSignUpMode) BrandSecondary else BrandAccent)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (!isSignUpMode) {
                        // LOGIN FORM
                        OutlinedTextField(
                            value = loginUsername,
                            onValueChange = { loginUsername = it },
                            label = { Text("اسم المستخدم") },
                            placeholder = { Text("أدخل اسم المستخدم") },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = BrandPrimary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = { loginPassword = it },
                            label = { Text("كلمة المرور") },
                            placeholder = { Text("أدخل كلمة المرور") },
                            leadingIcon = { Icon(Icons.Filled.VpnKey, contentDescription = null, tint = BrandPrimary) },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                viewModel.loginUser(loginUsername, loginPassword) { success, msg ->
                                    if (success) {
                                        loginUsername = ""
                                        loginPassword = ""
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("سجل الدخول بأمان", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        // REGISTER FORM
                        OutlinedTextField(
                            value = regUsername,
                            onValueChange = { regUsername = it },
                            label = { Text("اسم المستخدم الفريد (English)") },
                            leadingIcon = { Icon(Icons.Filled.AlternateEmail, contentDescription = null, tint = BrandPrimary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = regFullName,
                            onValueChange = { regFullName = it },
                            label = { Text("الاسم الكامل (مطابق للهوية الوطنية)") },
                            leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null, tint = BrandPrimary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = regEmail,
                            onValueChange = { regEmail = it },
                            label = { Text("البريد الإلكتروني") },
                            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = BrandPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = regPhone,
                            onValueChange = { regPhone = it },
                            label = { Text("رقم الهاتف / الجوال") },
                            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = BrandPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = regLicenseNo,
                            onValueChange = { regLicenseNo = it },
                            label = { Text("رقم رخصة القيادة") },
                            leadingIcon = { Icon(Icons.Filled.Description, contentDescription = null, tint = BrandPrimary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = regPassword,
                            onValueChange = { regPassword = it },
                            label = { Text("كلمة المرور السرية") },
                            leadingIcon = { Icon(Icons.Filled.VpnKey, contentDescription = null, tint = BrandPrimary) },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                viewModel.registerUser(
                                    username = regUsername,
                                    email = regEmail,
                                    fullName = regFullName,
                                    phone = regPhone,
                                    licenseNo = regLicenseNo,
                                    passwordRaw = regPassword
                                ) { success, msg ->
                                    if (success) {
                                        regUsername = ""
                                        regFullName = ""
                                        regEmail = ""
                                        regPhone = ""
                                        regLicenseNo = ""
                                        regPassword = ""
                                        isSignUpMode = false
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("إنشاء الحساب والموافقة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            // USER LOGGED IN PROFILE VIEWER
            val user = currentUser!!
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Profile Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.fullName.take(2).uppercase(),
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Text(
                            text = user.fullName,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "مستأجر موثق ومؤهل للخدمة ✅",
                                color = BrandGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Divider(color = Color.Gray.copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("اسم المستخدم", color = Color.LightGray, fontSize = 9.sp)
                                Text("@${user.username}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("رقم الرخصة المعتمد", color = Color.LightGray, fontSize = 9.sp)
                                Text(user.licenseNo, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Update details form
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(24.dp)),
                    border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "تحديث المستندات والبيانات الشخصية",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandSecondary
                        )

                        OutlinedTextField(
                            value = upFullName,
                            onValueChange = { upFullName = it },
                            label = { Text("الاسم الكامل") },
                            leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null, tint = BrandPrimary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = upEmail,
                            onValueChange = { upEmail = it },
                            label = { Text("البريد الإلكتروني") },
                            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = BrandPrimary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = upPhone,
                            onValueChange = { upPhone = it },
                            label = { Text("رقم الهاتف") },
                            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = BrandPrimary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = upLicenseNo,
                            onValueChange = { upLicenseNo = it },
                            label = { Text("رقم رخصة القيادة") },
                            leadingIcon = { Icon(Icons.Filled.Description, contentDescription = null, tint = BrandPrimary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = upPassword,
                            onValueChange = { upPassword = it },
                            label = { Text("تغيير كلمة المرور الشخصية (اختياري)") },
                            placeholder = { Text("اتركها فارغة لعدم التغيير") },
                            leadingIcon = { Icon(Icons.Filled.VpnKey, contentDescription = null, tint = BrandPrimary) },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                viewModel.updateProfile(
                                    fullName = upFullName,
                                    email = upEmail,
                                    phone = upPhone,
                                    licenseNo = upLicenseNo,
                                    newPasswordRaw = upPassword.takeIf { it.isNotBlank() }
                                ) { success, msg ->
                                    if (success) {
                                        upPassword = ""
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ البيانات والمستندات المحدثة", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.logoutUser() },
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تسجيل الخروج الآمن", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    }
}

// =========================================================================
// 9. CAR INTERACTIVE MAP SCREEN WITH VECTOR SIMULATION & TELEMETRY TRACKER
// =========================================================================
@Composable
fun CarRentalInteractiveMapScreen(
    viewModel: CarRentalViewModel,
    onCarClick: (Car) -> Unit
) {
    val carsList by viewModel.cars.collectAsState()
    
    // Simple state tracking
    var selectedCarForTracking by remember { mutableStateOf<Car?>(null) }
    var selectedCityFilter by remember { mutableStateOf("الكل") } // "الكل", "الرياض", "جدة"
    
    val cities = listOf("الكل", "الرياض", "جدة")
    
    // Assign simulation positions for cars in the view
    class SimCity(val city: String, val cityName: String, val lat: Float, val lon: Float)
    
    val simulatedCars = remember(carsList) {
        carsList.mapIndexed { index, car ->
            val sim = when {
                car.model.contains("مرسيدس") -> SimCity("الرياض", "فرع العليا، الرياض", 0.2f, -0.1f)
                car.model.contains("ليكزس") -> SimCity("جدة", "فرع الحمراء، جدة", -0.4f, 0.3f)
                car.model.contains("تسلا") -> SimCity("الرياض", "حي المالي، الرياض", 0.35f, 0.4f)
                else -> {
                    if (index % 2 == 0) {
                        SimCity("الرياض", "فرع الياسمين، الرياض", -0.15f, -0.3f)
                    } else {
                        SimCity("جدة", "فرع الروضة، جدة", -0.2f, 0.2f)
                    }
                }
            }
            CarLocation(car, sim.city, sim.cityName, sim.lat, sim.lon)
        }
    }

    val filteredLocations = remember(simulatedCars, selectedCityFilter) {
        if (selectedCityFilter == "الكل") simulatedCars else simulatedCars.filter { it.city == selectedCityFilter }
    }

    // Animation progress for simulated car tracking (0f to 1f)
    var animationProgress by remember { mutableStateOf(0f) }
    var isTrackingRentedCar by remember { mutableStateOf(false) }

    LaunchedEffect(selectedCarForTracking) {
        if (selectedCarForTracking != null && selectedCarForTracking?.status == "مؤجرة") {
            isTrackingRentedCar = true
            animationProgress = 0f
            // Smoothly animate telemetry progress loop
            while (isTrackingRentedCar) {
                kotlinx.coroutines.delay(30)
                animationProgress += 0.005f
                if (animationProgress >= 1f) {
                    animationProgress = 0f
                }
            }
        } else {
            isTrackingRentedCar = false
            animationProgress = 0f
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Tabs / Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            cities.forEach { city ->
                val isSelected = selectedCityFilter == city
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) BrandPrimary else Color(0xFFF3F0F5))
                        .clickable { selectedCityFilter = city }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (city == "الكل") "كافة المدن" else "فرع $city",
                        color = if (isSelected) Color.White else BrandSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Main Map Canvas Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp))
        ) {
            // Interactive Vector Map representation
            Canvas(modifier = Modifier.fillMaxSize()) {
                val mapWidth = size.width
                val mapHeight = size.height
                val centerX = mapWidth / 2f
                val centerY = mapHeight / 2f

                // 1. Draw regional mesh or background grid lines
                val gridColor = Color(0xFF1E293B)
                for (i in 1..8) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, (mapHeight / 9) * i),
                        end = Offset(mapWidth, (mapHeight / 9) * i),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset((mapWidth / 9) * i, 0f),
                        end = Offset((mapWidth / 9) * i, mapHeight),
                        strokeWidth = 1f
                    )
                }

                // 2. Draw styled radial waves representing Saudi Highway corindons
                drawCircle(
                    color = Color(0xFF3B82F6).copy(alpha = 0.05f),
                    radius = mapWidth * 0.35f,
                    center = Offset(centerX, centerY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
                drawCircle(
                    color = Color(0xFF3B82F6).copy(alpha = 0.03f),
                    radius = mapWidth * 0.6f,
                    center = Offset(centerX, centerY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                )

                // Draw connection corridor line (Riyadh to Jeddah)
                drawLine(
                    color = Color(0xFF334155),
                    start = Offset(centerX + (mapWidth * 0.2f), centerY - (mapHeight * 0.1f)), // Riyadh simulation
                    end = Offset(centerX - (mapWidth * 0.3f), centerY + (mapHeight * 0.2f)), // Jeddah simulation
                    strokeWidth = 3f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                )

                // 3. Draw animated Telemetry Tracker path if tracking
                selectedCarForTracking?.let { car ->
                    if (car.status == "مؤجرة") {
                        val loc = filteredLocations.find { it.car.id == car.id }
                        if (loc != null) {
                            val startX = centerX + (loc.latOffset * centerX)
                            val startY = centerY + (loc.lonOffset * centerY)
                            val endX = startX - 85f
                            val endY = startY + 70f

                            // Draw simulated travel route
                            drawLine(
                                color = Color(0xFFF59E0B),
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = 4f,
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )

                            // Draw telemetry arrow along the route
                            val activeX = startX + (endX - startX) * animationProgress
                            val activeY = startY + (endY - startY) * animationProgress
                            drawCircle(
                                color = Color(0xFFF59E0B),
                                radius = 8f,
                                center = Offset(activeX, activeY)
                            )
                            drawCircle(
                                color = Color(0xFFF59E0B).copy(alpha = 0.3f),
                                radius = 16f,
                                center = Offset(activeX, activeY)
                            )
                        }
                    }
                }
            }

            // City Centers Labels overlaid on map
            Text(
                text = "📍 فرع الوسطى الرئيسي (الرياض)",
                color = Color.LightGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center).offset(x = 60.dp, y = (-50).dp)
            )

            Text(
                text = "📍 فرع الغربية الإستراتيجي (جدة)",
                color = Color.LightGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center).offset(x = (-110).dp, y = 70.dp)
            )

            // 4. Interactive Clickable Car Pins on top of map
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val mapWidth = maxWidth
                val mapHeight = maxHeight
                
                filteredLocations.forEach { loc ->
                    val posX = (mapWidth / 2f) + (loc.latOffset.dp * (mapWidth / 2.5f).value)
                    val posY = (mapHeight / 2f) + (loc.lonOffset.dp * (mapHeight / 2.5f).value)
                    
                    val isSelected = selectedCarForTracking?.id == loc.car.id
                    val pinColor = when (loc.car.status) {
                        "متاحة" -> BrandGreen
                        "مؤجرة" -> BrandPrimary
                        else -> BrandOrange
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = posX - 22.dp, y = posY - 22.dp)
                            .size(44.dp)
                            .clickable { selectedCarForTracking = loc.car },
                        contentAlignment = Alignment.Center
                    ) {
                        // Radar Ripple for active selection
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(pinColor.copy(alpha = 0.25f), CircleShape)
                                    .border(1.dp, pinColor, CircleShape)
                            )
                        }
                        // Icon Pin
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else pinColor)
                                .border(1.5.dp, if (isSelected) pinColor else Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (loc.car.status == "مؤجرة") Icons.Filled.DriveEta else Icons.Filled.DirectionsCar,
                                contentDescription = loc.car.model,
                                tint = if (isSelected) pinColor else Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom descriptive status or tracker sheet Card
        AnimatedVisibility(
            visible = selectedCarForTracking != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            selectedCarForTracking?.let { car ->
                val loc = simulatedCars.find { it.car.id == car.id }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandCard),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header info and status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(car.model, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandSecondary)
                                Text(loc?.cityName ?: "فرع تأجير معتمد", fontSize = 10.sp, color = BrandAccent)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when (car.status) {
                                            "متاحة" -> BrandGreen.copy(alpha = 0.12f)
                                            "مؤجرة" -> BrandPrimary.copy(alpha = 0.12f)
                                            else -> BrandOrange.copy(alpha = 0.12f)
                                        }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = car.status,
                                    color = when (car.status) {
                                        "متاحة" -> BrandGreen
                                        "مؤجرة" -> BrandPrimary
                                        else -> BrandOrange
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Divider(color = Color(0xFFCBD5E1), thickness = 0.5.dp)

                        // Spec indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("سعر اليوم", fontSize = 9.sp, color = BrandAccent)
                                Text("${car.rentPricePerDay} ر.س", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("عداد السيارة", fontSize = 9.sp, color = BrandAccent)
                                Text("${car.mileage} كم", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("لوحة رقمية", fontSize = 9.sp, color = BrandAccent)
                                Text(car.plateNo, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandSecondary)
                            }
                        }

                        // Interactive Telemetry Tracking Details if rented
                        if (car.status == "مؤجرة") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFEF3C7))
                                    .border(1.dp, Color(0xFFFCD34D), RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD97706))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تتبع حي: السيارة متحركة حالياً متجهة نحو نقطة التسليم مجدولاً. (المسافة المتبقية: ~4.2 كم)",
                                    color = Color(0xFF92400E),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { selectedCarForTracking = null },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("إغلاق التتبع", fontSize = 11.sp)
                            }
                            Button(
                                onClick = { onCarClick(car) },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("عرض كامل التفاصيل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class CarLocation(
    val car: Car,
    val city: String,
    val cityName: String,
    val latOffset: Float,
    val lonOffset: Float
)

@Composable
fun ThemeSettingsCard(viewModel: CarRentalViewModel) {
    val isDark by viewModel.isDarkMode.collectAsState()
    
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BrandCard),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp)),
        border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "مظهر التطبيق والمظهر البصري",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandSecondary
                    )
                    Text(
                        text = "اختر المظهر الملائم لضمان القراءة الأسهل والأكثر راحة للعين",
                        fontSize = 11.sp,
                        color = BrandAccent
                    )
                }
            }

            Divider(color = BrandSurface, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandSurface.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Light mode choice
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!isDark) BrandCard else Color.Transparent)
                        .clickable { if (isDark) viewModel.toggleDarkMode() }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.LightMode,
                            contentDescription = "الوضع المضيء",
                            tint = if (!isDark) BrandPrimary else BrandAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "الوضع المضيء",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (!isDark) BrandPrimary else BrandAccent
                        )
                    }
                }

                // Dark mode choice
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) BrandCard else Color.Transparent)
                        .clickable { if (!isDark) viewModel.toggleDarkMode() }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        String.format("") // Dummy placeholder
                        Icon(
                            Icons.Filled.DarkMode,
                            contentDescription = "الوضع الداكن",
                            tint = if (isDark) BrandPrimary else BrandAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "الوضع الداكن",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isDark) BrandPrimary else BrandAccent
                        )
                    }
                }
            }
        }
    }
}


