package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cars")
data class Car(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // نوع السيارة (e.g., سيدان, دفع رباعي, عائلية, فاخرة)
    val plateNo: String, // رقم اللوحة
    val chassisNo: String, // رقم الهيكل
    val color: String, // اللون
    val model: String, // الموديل
    val mileage: Int, // عداد المشي (كيلومتر)
    val images: String, // روابط الصور (مفصولة بفاصلة)
    val rentPricePerDay: Double, // سعر التأجير اليومي
    val status: String = "متاحة", // حالة السيارة (متاحة, مؤجرة, صيانة)
    val serviceThreshold: Int = 5000, // مسافة الصيانة الدورية بالـ كم
    val lastServiceMileage: Int = 0, // قراءة العداد عند آخر صيانة
    val lastServiceDate: Long = 0L // تاريخ الصيانة الأخيرة (ميلي ثانية)
)

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val carId: Int,
    val customerName: String, // اسم العميل
    val customerPhone: String, // هاتف العميل
    val pickupDate: Long, // تاريخ الاستلام
    val returnDate: Long, // تاريخ التسليم
    val totalAmount: Double, // القيمة الإجمالية
    val status: String = "مؤكد", // حالة الطلب (مؤكد, قيد التنفيذ, مكتمل, ملغي)
    val rating: Float? = null, // تقييم الخدمة
    val ratingComment: String? = null // ملاحظات التقييم
)

@Entity(tableName = "financial_accounts")
data class FinancialAccount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, // اسم الصندوق أو البنك (e.g., "الخزينة الرئيسية", "بنك الراجحي")
    val type: String, // النوع ("SAFE" او "BANK")
    val balance: Double // الرصيد الحالي
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val accountId: Int, // معرف الصندوق أو البنك
    val bookingId: Int? = null, // معرف الحجز المرتبط (إن وجد)
    val amount: Double, // المبلغ
    val type: String, // نوع المعاملة ("إيداع" أو "سحب")
    val description: String, // الوصف والتفاصيل
    val timestamp: Long = System.currentTimeMillis() // التوقيت
)

@Entity(tableName = "users")
data class User(
    @PrimaryKey val username: String, // اسم المستخدم الفريد
    val email: String,                // البريد الإلكتروني
    val fullName: String,             // الاسم الكامل
    val phone: String,                // رقم الهاتف
    val licenseNo: String,            // رقم رخصة القيادة أو الهوية
    val passwordHash: String          // كلمة المرور المشفرة الكترونياً بـ SHA-256
)

// DAOs
@Dao
interface CarRentalDao {
    // Cars
    @Query("SELECT * FROM cars ORDER BY id DESC")
    fun getAllCars(): Flow<List<Car>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCar(car: Car)

    @Update
    suspend fun updateCar(car: Car)

    @Delete
    suspend fun deleteCar(car: Car)

    // Bookings
    @Query("SELECT * FROM bookings ORDER BY id DESC")
    fun getAllBookings(): Flow<List<Booking>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: Booking): Long

    @Update
    suspend fun updateBooking(booking: Booking)

    // Financial Accounts
    @Query("SELECT * FROM financial_accounts")
    fun getAllAccounts(): Flow<List<FinancialAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: FinancialAccount)

    @Update
    suspend fun updateAccount(account: FinancialAccount)

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    // Users
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
}

// Database Class
@Database(
    entities = [Car::class, Booking::class, FinancialAccount::class, Transaction::class, User::class],
    version = 3,
    exportSchema = false
)
abstract class CarRentalDatabase : RoomDatabase() {
    abstract fun carRentalDao(): CarRentalDao
}

// Repository
class CarRentalRepository(private val dao: CarRentalDao) {
    val cars: Flow<List<Car>> = dao.getAllCars()
    val bookings: Flow<List<Booking>> = dao.getAllBookings()
    val accounts: Flow<List<FinancialAccount>> = dao.getAllAccounts()
    val transactions: Flow<List<Transaction>> = dao.getAllTransactions()

    suspend fun addCar(car: Car) = dao.insertCar(car)
    suspend fun updateCar(car: Car) = dao.updateCar(car)
    suspend fun deleteCar(car: Car) = dao.deleteCar(car)

    suspend fun addBooking(booking: Booking, accountId: Int?, earnAmount: Double): Long {
        val bookingId = dao.insertBooking(booking)
        if (accountId != null && earnAmount > 0) {
            // Record deposit transaction
            val trans = Transaction(
                accountId = accountId,
                bookingId = bookingId.toInt(),
                amount = earnAmount,
                type = "إيداع",
                description = "قيمة إيجار للعميل ${booking.customerName}"
            )
            dao.insertTransaction(trans)
        }
        return bookingId
    }

    suspend fun rateBooking(bookingId: Int, rating: Float, comment: String) {
        // Find existing booking and update it if possible
        // (Typically we query it. For our VM structure, the VM will pass updated booking object)
    }

    suspend fun updateBooking(booking: Booking) = dao.updateBooking(booking)

    suspend fun addAccount(account: FinancialAccount) = dao.insertAccount(account)
    suspend fun updateAccount(account: FinancialAccount) = dao.updateAccount(account)

    suspend fun addTransaction(transaction: Transaction) = dao.insertTransaction(transaction)

    // User Profile Actions
    suspend fun getUserByUsername(username: String): User? = dao.getUserByUsername(username)
    suspend fun saveUser(user: User) = dao.insertUser(user)
}
