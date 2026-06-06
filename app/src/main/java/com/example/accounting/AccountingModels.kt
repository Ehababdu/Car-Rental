package com.example.accounting

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ----------------------------------------------------------------------------------
// ACCOUNTING DATA MODEL ENTITIES (Double-Entry Bookkeeping & Tailor Sizing)
// ----------------------------------------------------------------------------------

enum class AccountType {
    ASSET,      // الأصول
    LIABILITY,  // الخصوم
    EQUITY,     // حقوق الملكية
    REVENUE,    // الإيرادات
    EXPENSE     // المصروفات
}

@Entity(tableName = "accounting_accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val code: String, // e.g. "1101"
    val name: String, // e.g. "الصندوق الرئيسي"
    val type: AccountType,
    val balance: Double = 0.0,
    val parentId: Int? = null,
    val isGroup: Boolean = false // If true, can have sub-accounts but no direct postings
)

@Entity(tableName = "accounting_partners")
data class AccountPartner(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val type: String, // "CUSTOMER" or "SUPPLIER"
    val balance: Double = 0.0,
    val notes: String = ""
)

@Entity(tableName = "accounting_vouchers")
data class Voucher(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val voucherNo: String,
    val date: Long,
    val type: String, // "JOURNAL", "RECEIPT", "PAYMENT"
    val description: String,
    val totalAmount: Double
)

@Entity(
    tableName = "accounting_voucher_lines",
    foreignKeys = [
        ForeignKey(
            entity = Voucher::class,
            parentColumns = ["id"],
            childColumns = ["voucherId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("voucherId"), Index("accountId")]
)
data class VoucherLine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val voucherId: Int,
    val accountId: Int,
    val debit: Double, // المدين
    val credit: Double, // الدائن
    val memo: String = ""
)

@Entity(tableName = "tailor_measurements")
data class TailorMeasurement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerName: String,
    val phone: String,
    val neck: Double,     // قياس الرقبة
    val shoulder: Double, // الأكتاف
    val chest: Double,    // الصدر
    val waist: Double,    // الوسط
    val sleeve: Double,   // طول الكم
    val length: Double,   // الطول الكلي
    val notes: String = "",
    val date: Long = System.currentTimeMillis()
)

// ----------------------------------------------------------------------------------
// DAOs DEFINITION
// ----------------------------------------------------------------------------------

@Dao
interface AccountingDao {
    // Accounts
    @Query("SELECT * FROM accounting_accounts ORDER BY code ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Query("UPDATE accounting_accounts SET balance = balance + :amount WHERE id = :accountId")
    suspend fun adjustAccountBalance(accountId: Int, amount: Double)

    // Partners
    @Query("SELECT * FROM accounting_partners ORDER BY id DESC")
    fun getAllPartners(): Flow<List<AccountPartner>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartner(partner: AccountPartner): Long

    @Query("UPDATE accounting_partners SET balance = balance + :amount WHERE id = :partnerId")
    suspend fun adjustPartnerBalance(partnerId: Int, amount: Double)

    // Vouchers
    @Query("SELECT * FROM accounting_vouchers ORDER BY date DESC")
    fun getAllVouchers(): Flow<List<Voucher>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucher(voucher: Voucher): Long

    @Query("DELETE FROM accounting_vouchers WHERE id = :voucherId")
    suspend fun deleteVoucherById(voucherId: Int)

    // Voucher Lines
    @Query("SELECT * FROM accounting_voucher_lines WHERE voucherId = :voucherId")
    suspend fun getLinesForVoucher(voucherId: Int): List<VoucherLine>

    @Query("SELECT * FROM accounting_voucher_lines ORDER BY id DESC")
    fun getAllVoucherLines(): Flow<List<VoucherLine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucherLine(line: VoucherLine): Long

    // Sizing & Measurements
    @Query("SELECT * FROM tailor_measurements ORDER BY date DESC")
    fun getAllMeasurements(): Flow<List<TailorMeasurement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: TailorMeasurement): Long

    @Delete
    suspend fun deleteMeasurement(measurement: TailorMeasurement)
}

// ----------------------------------------------------------------------------------
// ROOM DATABASE DEFINITION
// ----------------------------------------------------------------------------------

@Database(
    entities = [
        Account::class,
        AccountPartner::class,
        Voucher::class,
        VoucherLine::class,
        TailorMeasurement::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AccountingDatabase : RoomDatabase() {
    abstract fun accountingDao(): AccountingDao
}
