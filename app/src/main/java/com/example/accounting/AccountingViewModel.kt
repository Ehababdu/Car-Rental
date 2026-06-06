package com.example.accounting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountingViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AccountingDatabase = Room.databaseBuilder(
        application,
        AccountingDatabase::class.java,
        "accounting_ledger_database"
    ).build()

    private val repository = AccountingRepository(database)

    // Flow State Collectors from database Row Tables
    val accounts = repository.accounts
    val partners = repository.partners
    val vouchers = repository.vouchers
    val voucherLines = repository.voucherLines
    val measurements = repository.measurements

    // UI Feedback Controllers
    private val _uiNotification = MutableStateFlow<String?>(null)
    val uiNotification: StateFlow<String?> = _uiNotification.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.seedDefaultDataIfNeeded()
            } catch (e: Exception) {
                _uiNotification.value = "فشل في تهيئة دليل الحسابات: ${e.localizedMessage}"
            }
        }
    }

    fun clearNotification() {
        _uiNotification.value = null
    }

    // Account Creation
    fun addAccount(code: String, name: String, type: AccountType, parentId: Int?, isGroup: Boolean) {
        viewModelScope.launch {
            try {
                repository.createAccount(code, name, type, parentId, isGroup)
                _uiNotification.value = "تمت إضافة الحساب '$name' بنجاح بنظام شجرة الحسابات."
            } catch (e: Exception) {
                _uiNotification.value = "خطأ أثناء إضافة الحساب: ${e.localizedMessage}"
            }
        }
    }

    // Customer / Supplier registration
    fun addPartner(name: String, phone: String, type: String, notes: String) {
        viewModelScope.launch {
            try {
                repository.createPartner(name, phone, type, notes)
                val typeAr = if (type == "CUSTOMER") "العميل" else "المورد"
                _uiNotification.value = "تم تسجيل $typeAr '$name' الجديد بالقيود المالية."
            } catch (e: Exception) {
                _uiNotification.value = "خطأ أثناء تسجيل الحساب المساعد: ${e.localizedMessage}"
            }
        }
    }

    // Record Partner Collections / Payments
    fun recordPartnerTransaction(
        partnerId: Int,
        amount: Double,
        isPayment: Boolean,
        safeOrBankAccountId: Int,
        notes: String
    ) {
        viewModelScope.launch {
            try {
                val success = repository.recordPartnerTransaction(partnerId, amount, isPayment, safeOrBankAccountId, notes)
                if (success) {
                    _uiNotification.value = "تم تسجيل السند المالي للجهة وتحديث دفاتر الحسابات والذمم بنجاح."
                } else {
                    _uiNotification.value = "فشل تسجيل المعاملة المالية للجهة المستهدفة!"
                }
            } catch (e: Exception) {
                _uiNotification.value = "فشل في تسجيل المعاملة: ${e.localizedMessage}"
            }
        }
    }

    // Record Simplified Inflow / Outflow
    fun recordQuickTransaction(
        safeOrBankAccountId: Int,
        targetAccountId: Int,
        amount: Double,
        isPayment: Boolean,
        memo: String
    ) {
        viewModelScope.launch {
            try {
                val success = repository.recordQuickTransaction(safeOrBankAccountId, targetAccountId, amount, isPayment, memo)
                if (success) {
                    val label = if (isPayment) "سند الصرف" else "سند القبض"
                    _uiNotification.value = "تم ترحيل $label المبسط لدفتر اليومية بنجاح."
                } else {
                    _uiNotification.value = "فشل ترحيل السند المالي، تأكد من صحة القيم المدخلة."
                }
            } catch (e: Exception) {
                _uiNotification.value = "خطأ أثناء الترحيل المبسط: ${e.localizedMessage}"
            }
        }
    }

    // Record Safe/Bank Transfer
    fun recordTransferTransaction(
        sourceAccountId: Int,
        destinationAccountId: Int,
        amount: Double,
        memo: String
    ) {
        viewModelScope.launch {
            try {
                val success = repository.recordTransferTransaction(sourceAccountId, destinationAccountId, amount, memo)
                if (success) {
                    _uiNotification.value = "تم تحويل المبلغ وتأكيد القيد الدفتري بنجاح للفروع المصرفية."
                } else {
                    _uiNotification.value = "تنبيه! فشل تحويل الحركة النقدية. يرجى التحقق من المدخلات."
                }
            } catch (e: Exception) {
                _uiNotification.value = "فشل التحويل المصرفي: ${e.localizedMessage}"
            }
        }
    }

    // Voucher Post logic
    fun postVoucher(
        voucherNo: String,
        type: String,
        description: String,
        date: Long,
        lines: List<Pair<Int, Triple<Double, Double, String>>>
    ) {
        viewModelScope.launch {
            try {
                val success = repository.saveDoubleEntryVoucher(voucherNo, type, description, date, lines)
                if (success) {
                    _uiNotification.value = "تم ترحيل السند القيد رقم $voucherNo للدفاتر ودفتر الأستاذ بنجاح."
                } else {
                    _uiNotification.value = "فشل ترحيل السند القيد! الأطراف المدينة والدائنة غير متزنة."
                }
            } catch (e: Exception) {
                _uiNotification.value = "خطأ أثناء ترحيل السند القيد: ${e.localizedMessage}"
            }
        }
    }

    // Delete Voucher
    fun deleteVoucher(voucherId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteVoucher(voucherId)
                _uiNotification.value = "تم حذف السند وتراجع وتحديث الحسابات المرتبطة به بنجاح."
            } catch (e: Exception) {
                _uiNotification.value = "فشل حذف السند: ${e.localizedMessage}"
            }
        }
    }

    // Sizing Profiles
    fun addMeasurement(
        name: String,
        phone: String,
        neck: Double,
        shoulder: Double,
        chest: Double,
        waist: Double,
        sleeve: Double,
        length: Double,
        notes: String
    ) {
        viewModelScope.launch {
            try {
                val timing = System.currentTimeMillis()
                val profile = TailorMeasurement(
                    customerName = name,
                    phone = phone,
                    neck = neck,
                    shoulder = shoulder,
                    chest = chest,
                    waist = waist,
                    sleeve = sleeve,
                    length = length,
                    notes = notes,
                    date = timing
                )
                repository.addMeasurement(profile)
                _uiNotification.value = "تم حفظ بطاقة قياسات الخياطة والتطريز للعميل '$name'."
            } catch (e: Exception) {
                _uiNotification.value = "تعذر تسجيل بطاقة القياسات: ${e.localizedMessage}"
            }
        }
    }

    fun removeMeasurement(profile: TailorMeasurement) {
        viewModelScope.launch {
            try {
                repository.deleteMeasurement(profile)
                _uiNotification.value = "تم حذف بطاقة القياسات المحددة."
            } catch (e: Exception) {
                _uiNotification.value = "خطأ بالمسح: ${e.localizedMessage}"
            }
        }
    }
}
