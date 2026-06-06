package com.example.data

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CarRentalViewModel(application: Application) : AndroidViewModel(application) {

    private val database: CarRentalDatabase by lazy {
        Room.databaseBuilder(
            application,
            CarRentalDatabase::class.java,
            "car_rental_database"
        ).fallbackToDestructiveMigration()
            .build()
    }

    private val repository: CarRentalRepository by lazy {
        CarRentalRepository(database.carRentalDao())
    }

    // Theme Preference with persistent storage
    private val sharedPrefs = application.getSharedPreferences("car_rental_prefs", Context.MODE_PRIVATE)
    private val _isDarkMode = MutableStateFlow(sharedPrefs.getBoolean("dark_mode_enabled", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        val current = _isDarkMode.value
        val newValue = !current
        _isDarkMode.value = newValue
        sharedPrefs.edit().putBoolean("dark_mode_enabled", newValue).apply()
        com.example.ui.theme.isAppInDarkMode.value = newValue
    }

    // UI States observed by Composables
    val cars: StateFlow<List<Car>> = repository.cars
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookings: StateFlow<List<Booking>> = repository.bookings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<FinancialAccount>> = repository.accounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Feedback Toast or Notification flow
    private val _notifications = MutableStateFlow<List<String>>(emptyList())
    val notifications: StateFlow<List<String>> = _notifications.asStateFlow()

    init {
        com.example.ui.theme.isAppInDarkMode.value = _isDarkMode.value
        // Pre-populate with premium starting data if the database is empty in a launch coroutine
        viewModelScope.launch {
            cars.take(1).collect { currentCars ->
                if (currentCars.isEmpty()) {
                    seedInitialData()
                }
            }
        }
    }

    private suspend fun seedInitialData() {
        // Initial Cars
        val initialCars = listOf(
            Car(
                type = "فاخرة",
                plateNo = "أ ب ج 1234",
                chassisNo = "CHS987654321012",
                color = "أسود ملكي",
                model = "مرسيدس S-Class 2024",
                mileage = 1500,
                images = "https://images.unsplash.com/photo-1549399542-7e3f8b79c341?q=80&w=600,https://images.unsplash.com/photo-1618843479313-40f8afb4b4d8?q=80&w=600",
                rentPricePerDay = 1200.0,
                status = "متاحة"
            ),
            Car(
                type = "دفع رباعي",
                plateNo = "ر س ط 9999",
                chassisNo = "CHS456123098111",
                color = "أبيض مطفي",
                model = "ليكزس LX600 2023",
                mileage = 8400,
                images = "https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?q=80&w=600,https://images.unsplash.com/photo-1511919884226-fd3cad34687c?q=80&w=600",
                rentPricePerDay = 950.0,
                status = "متاحة"
            ),
            Car(
                type = "سيدان",
                plateNo = "ح ك م 4567",
                chassisNo = "CHS556677889900",
                color = "كحلي معدني",
                model = "تويوتا كامري 2024",
                mileage = 4200,
                images = "https://images.unsplash.com/photo-1621007947382-bb3c3994e3fb?q=80&w=600,https://images.unsplash.com/photo-1580273916550-e323be2ae537?q=80&w=600",
                rentPricePerDay = 250.0,
                status = "متاحة"
            ),
            Car(
                type = "كهربائية",
                plateNo = "ن م ك 2026",
                chassisNo = "CHS112233445566",
                color = "رمادي فضائي",
                model = "تسلا موديل Y 2024",
                mileage = 11000,
                images = "https://images.unsplash.com/photo-1614162692292-7ac56d7f7f1e?q=80&w=600,https://images.unsplash.com/photo-1563720223185-11003d516935?q=80&w=600",
                rentPricePerDay = 450.0,
                status = "مؤجرة"
            )
        )

        initialCars.forEach { repository.addCar(it) }

        // Initial Financial Safes and Banks (صناديق وبنوك)
        val initialAccounts = listOf(
            FinancialAccount(name = "الخزينة الرئيسية (الكاش)", type = "SAFE", balance = 5000.0),
            FinancialAccount(name = "صندوق الطوارئ والعهد", type = "SAFE", balance = 1200.0),
            FinancialAccount(name = "مصرف الراجحي", type = "BANK", balance = 24500.0),
            FinancialAccount(name = "البنك الأهلي السعودي (SNB)", type = "BANK", balance = 18000.0)
        )

        initialAccounts.forEach { repository.addAccount(it) }

        // Initial Bookings
        val testBooking = Booking(
            carId = 4,
            customerName = "عبدالرحمن الماجد",
            customerPhone = "+966501234567",
            pickupDate = System.currentTimeMillis() - 2 * 24 * 3600 * 1000, // 2 days ago
            returnDate = System.currentTimeMillis() + 3 * 24 * 3600 * 1000, // 3 days later
            totalAmount = 2250.0,
            status = "قيد التنفيذ"
        )
        val bId = repository.addBooking(testBooking, null, 0.0)

        // Seed an initial transaction
        repository.addTransaction(
            Transaction(
                accountId = 3, // مصرف الراجحي
                bookingId = bId.toInt(),
                amount = 2250.0,
                type = "إيداع",
                description = "قيمة إيجار تسلا موديل Y - عبدالرحمن الماجد",
                timestamp = System.currentTimeMillis() - 2 * 24 * 3600 * 1000
            )
        )

        // Notification toast
        addNotification("تم تهيئة نظام تأجير السيارات وصناديق الحسابات بنجاح!")
    }

    // CAR OPERATIONS
    fun addNewCar(
        type: String,
        plateNo: String,
        chassisNo: String,
        color: String,
        model: String,
        mileage: Int,
        rentPrice: Double,
        imagesList: List<String>,
        serviceThreshold: Int = 5000,
        lastServiceMileage: Int = 0
    ) {
        viewModelScope.launch {
            val commaSeparatedImages = if (imagesList.isEmpty()) {
                "https://images.unsplash.com/photo-1549399542-7e3f8b79c341?q=80&w=600"
            } else {
                imagesList.joinToString(",")
            }
            val newCar = Car(
                type = type,
                plateNo = plateNo,
                chassisNo = chassisNo,
                color = color,
                model = model,
                mileage = mileage,
                rentPricePerDay = rentPrice,
                images = commaSeparatedImages,
                status = "متاحة",
                serviceThreshold = serviceThreshold,
                lastServiceMileage = lastServiceMileage,
                lastServiceDate = if (lastServiceMileage > 0) System.currentTimeMillis() else 0L
            )
            repository.addCar(newCar)
            addNotification("تمت إضافة مركبة جديدة بنجاح: $model ($plateNo)")
        }
    }

    fun deleteCar(car: Car) {
        viewModelScope.launch {
            repository.deleteCar(car)
            addNotification("تم حذف المركبة: ${car.model}")
        }
    }

    fun updateCarMileage(car: Car, newMileage: Int) {
        viewModelScope.launch {
            val updated = car.copy(mileage = newMileage)
            repository.addCar(updated)
            addNotification("تم تحديث عداد المركبة ${car.model} إلى $newMileage كم")
            
            // Check if mileage hits threshold
            val limit = updated.lastServiceMileage + updated.serviceThreshold
            if (newMileage >= limit) {
                addNotification("🚨 تنبيه صيانة: تخطت السيارة ${car.model} حد الصيانة الدوري بمقدار ${newMileage - limit} كم (الحد الأقصى $limit كم)")
            }
        }
    }

    fun recordCarMaintenance(car: Car, serviceMileage: Int, notes: String = "") {
        viewModelScope.launch {
            val updated = car.copy(
                lastServiceMileage = serviceMileage,
                lastServiceDate = System.currentTimeMillis(),
                // If car was in maintenance status, reset to available
                status = if (car.status == "صيانة") "متاحة" else car.status
            )
            repository.addCar(updated)
            val memo = if (notes.isNotBlank()) " ($notes)" else ""
            addNotification("🔧 تم تسجيل إجراء الصيانة الدورية بنجاح للسيارة ${car.model} عند العداد $serviceMileage كم$memo")
        }
    }
    
    fun updateCarMaintenanceConfig(car: Car, serviceThreshold: Int) {
        viewModelScope.launch {
            val updated = car.copy(serviceThreshold = serviceThreshold)
            repository.addCar(updated)
            addNotification("⚙️ تم تحديث حد الصيانة للسيارة ${car.model} ليصبح كل $serviceThreshold كم")
        }
    }

    // BOOKING OPERATIONS
    fun createBooking(
        carId: Int,
        customerName: String,
        customerPhone: String,
        pickupDate: Long,
        returnDate: Long,
        totalAmount: Double,
        depositAccountId: Int
    ) {
        viewModelScope.launch {
            val booking = Booking(
                carId = carId,
                customerName = customerName,
                customerPhone = customerPhone,
                pickupDate = pickupDate,
                returnDate = returnDate,
                totalAmount = totalAmount,
                status = "مؤكد"
            )

            // Auto-update car status to Rented ("مؤجرة")
            val targetCarList = cars.value
            val associatedCar = targetCarList.find { it.id == carId }
            if (associatedCar != null) {
                repository.addCar(associatedCar.copy(status = "مؤجرة"))
            }

            // Create Booking
            val bookingId = repository.addBooking(booking, depositAccountId, totalAmount)

            // Adjust account balance
            val currentAccs = accounts.value
            val selectedAcc = currentAccs.find { it.id == depositAccountId }
            if (selectedAcc != null) {
                val updatedBalance = selectedAcc.balance + totalAmount
                repository.addAccount(selectedAcc.copy(balance = updatedBalance))
            }

            // Add notification
            addNotification("تم إبرام عقد حجز بنجاح للعميل $customerName بمبلغ $totalAmount ر.س")
        }
    }

    fun updateBookingStatus(booking: Booking, newStatus: String) {
        viewModelScope.launch {
            val updated = booking.copy(status = newStatus)
            repository.updateBooking(updated)

            // If completed or cancelled, set car back to free
            if (newStatus == "مكتمل" || newStatus == "ملغي") {
                val carList = cars.value
                val assocCar = carList.find { it.id == booking.carId }
                if (assocCar != null) {
                    repository.addCar(assocCar.copy(status = "متاحة"))
                }
            }

            addNotification("تحديث حالة العقد للعميل ${booking.customerName} إلى $newStatus")
        }
    }

    fun rateService(bookingId: Int, rating: Float, comment: String) {
        viewModelScope.launch {
            val bookingsList = bookings.value
            val target = bookingsList.find { it.id == bookingId }
            if (target != null) {
                val rated = target.copy(rating = rating, ratingComment = comment)
                repository.updateBooking(rated)
                addNotification("تم تسجيل تقييم الخدمة بنجاح: $rating/5")
            }
        }
    }

    // ACCOUNTS OPERATIONS (سحب، إيداع، تحويل، وصيانات)
    fun addFinancialAccount(name: String, type: String, balance: Double) {
        viewModelScope.launch {
            val newAcc = FinancialAccount(name = name, type = type, balance = balance)
            repository.addAccount(newAcc)
            addNotification("تم تسجيل صندوق/حساب مالي جديد: $name")
        }
    }

    fun recordManualTransaction(accountId: Int, amount: Double, type: String, description: String) {
        viewModelScope.launch {
            val accountList = accounts.value
            val selectedAcc = accountList.find { it.id == accountId }
            if (selectedAcc != null) {
                val updatedBalance = if (type == "إيداع") {
                    selectedAcc.balance + amount
                } else {
                    selectedAcc.balance - amount
                }

                if (updatedBalance < 0 && type == "سحب") {
                    addNotification("فشل! رصيد الحساب المالي ${selectedAcc.name} غير كافٍ لإتمام عملية السحب")
                    return@launch
                }

                // Apply account update
                repository.addAccount(selectedAcc.copy(balance = updatedBalance))

                // Insert Transaction log
                repository.addTransaction(
                    Transaction(
                        accountId = accountId,
                        amount = amount,
                        type = type,
                        description = description
                    )
                )

                addNotification("تمت عملية $type بنجاح بمبلغ $amount ر.س للحساب ${selectedAcc.name}")
            }
        }
    }

    // INTERNAL HELPER TO ADD APPLICABILITY TOAST NOTIFICATIONS
    private fun addNotification(message: String) {
        val current = _notifications.value.toMutableList()
        current.add(0, message) // Insert at top
        _notifications.value = current

        // Dynamic detection of contract rental or delivery to send custom titled notification
        val title = when {
            message.contains("إبرام") || message.contains("حجز") -> "📝 عقد تأجير سيارة جديد"
            message.contains("تحديث حالة العقد") || message.contains("مكتمل") || message.contains("ملغي") -> "🚗 استلام/استرداد سيارة"
            message.contains("صندوق") || message.contains("حساب") || message.contains("سند") -> "💰 محاسبة وسندات مالية"
            else -> "🔔 تنبيه نظام تأجير السيارات"
        }
        sendSystemNotification(title, message)
    }

    private fun sendSystemNotification(title: String, message: String) {
        try {
            val context = getApplication<Application>().applicationContext
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "car_rental_notifications"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                var channel = notificationManager.getNotificationChannel(channelId)
                if (channel == null) {
                    channel = NotificationChannel(
                        channelId,
                        "إشعارات وعقود نظام التأجير",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "حالة العقود، تأجير واسترداد المركبات والمعاملات المالية"
                        enableLights(true)
                        enableVibration(true)
                    }
                    notificationManager.createNotificationChannel(channel)
                }
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    // --------------------------------------------------------------------
    // USER AUTHENTICATION & MANAGEMENT
    // --------------------------------------------------------------------
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun registerUser(
        username: String,
        email: String,
        fullName: String,
        phone: String,
        licenseNo: String,
        passwordRaw: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            if (username.isBlank() || email.isBlank() || fullName.isBlank() || phone.isBlank() || licenseNo.isBlank() || passwordRaw.isBlank()) {
                onResult(false, "فضلاً، يرجى ملء كافة الحقول بالمعلومات الصحيحة")
                return@launch
            }
            val existing = repository.getUserByUsername(username.trim())
            if (existing != null) {
                onResult(false, "خطأ: اسم المستخدم هذا مسجل مسبقاً بالتطبيق")
                return@launch
            }
            val hash = hashPassword(passwordRaw)
            val newUser = User(
                username = username.trim(),
                email = email.trim(),
                fullName = fullName.trim(),
                phone = phone.trim(),
                licenseNo = licenseNo.trim(),
                passwordHash = hash
            )
            repository.saveUser(newUser)
            _currentUser.value = newUser
            addNotification("🎉 ترحيب حار! تم إنشاء حساب جديد للمستأجر ${fullName.trim()} بنجاح")
            onResult(true, "تم التسجيل بنجاح")
        }
    }

    fun loginUser(username: String, passwordRaw: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (username.isBlank() || passwordRaw.isBlank()) {
                onResult(false, "فضلاً، تحقق من إدخال اسم المستخدم وكلمة المرور")
                return@launch
            }
            val user = repository.getUserByUsername(username.trim())
            if (user == null) {
                onResult(false, "اسم المستخدم غير مسجل، يرجى إنشاء حساب جديد")
                return@launch
            }
            val inputHash = hashPassword(passwordRaw)
            if (user.passwordHash == inputHash) {
                _currentUser.value = user
                addNotification("👋 مرحباً بك مجدداً يا ${user.fullName}! تم تسجيل الدخول بنجاح")
                onResult(true, "تم تسجيل الدخول بنجاح")
            } else {
                onResult(false, "خطأ: كلمة المرور المدخلة غير صحيحة")
            }
        }
    }

    fun logoutUser() {
        val prevName = _currentUser.value?.fullName ?: "المستخدم"
        _currentUser.value = null
        addNotification("🔒 تم تسجيل الخروج بنجاح لحساب $prevName")
    }

    fun updateProfile(
        fullName: String,
        email: String,
        phone: String,
        licenseNo: String,
        newPasswordRaw: String?,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val current = _currentUser.value
            if (current == null) {
                onResult(false, "لا يوجد مستخدم نشط حالياً لتعديل ملفه الشخصي")
                return@launch
            }
            if (fullName.isBlank() || email.isBlank() || phone.isBlank() || licenseNo.isBlank()) {
                onResult(false, "فضلاً، لا يمكن ترك البيانات الأساسية فارغة")
                return@launch
            }
            val hash = if (!newPasswordRaw.isNullOrBlank()) {
                hashPassword(newPasswordRaw)
            } else {
                current.passwordHash
            }
            val updated = current.copy(
                fullName = fullName.trim(),
                email = email.trim(),
                phone = phone.trim(),
                licenseNo = licenseNo.trim(),
                passwordHash = hash
            )
            repository.saveUser(updated)
            _currentUser.value = updated
            addNotification("⚙️ تم تحديث معلومات ملفك الشخصي للمستأجر بنجاح")
            onResult(true, "تم التحديث بنجاح")
        }
    }

    private fun hashPassword(password: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(password.toByteArray())
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            password
        }
    }
}
