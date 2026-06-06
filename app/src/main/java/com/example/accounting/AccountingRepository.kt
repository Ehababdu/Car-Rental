package com.example.accounting

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AccountingRepository(private val database: AccountingDatabase) {
    private val dao = database.accountingDao()

    val accounts: Flow<List<Account>> = dao.getAllAccounts()
    val partners: Flow<List<AccountPartner>> = dao.getAllPartners()
    val vouchers: Flow<List<Voucher>> = dao.getAllVouchers()
    val voucherLines: Flow<List<VoucherLine>> = dao.getAllVoucherLines()
    val measurements: Flow<List<TailorMeasurement>> = dao.getAllMeasurements()

    // 1. Initial Database Seeding
    suspend fun seedDefaultDataIfNeeded() {
        val count = accounts.first().size
        if (count == 0) {
            database.withTransaction {
                // Main Groups
                val assetsId = dao.insertAccount(Account(code = "1", name = "الأصول", type = AccountType.ASSET, isGroup = true)).toInt()
                val liabilitiesId = dao.insertAccount(Account(code = "2", name = "الخصوم", type = AccountType.LIABILITY, isGroup = true)).toInt()
                val equityId = dao.insertAccount(Account(code = "3", name = "حقوق الملكية", type = AccountType.EQUITY, isGroup = true)).toInt()
                val revenuesId = dao.insertAccount(Account(code = "4", name = "الإيرادات", type = AccountType.REVENUE, isGroup = true)).toInt()
                val expensesId = dao.insertAccount(Account(code = "5", name = "المصروفات", type = AccountType.EXPENSE, isGroup = true)).toInt()

                // Sub-Groups
                val curAssetsId = dao.insertAccount(Account(code = "11", name = "الأصول المتداولة", type = AccountType.ASSET, parentId = assetsId, isGroup = true)).toInt()
                val fixAssetsId = dao.insertAccount(Account(code = "12", name = "الأصول الثابتة", type = AccountType.ASSET, parentId = assetsId, isGroup = true)).toInt()
                
                val curLiabilitiesId = dao.insertAccount(Account(code = "21", name = "الالتزامات المتداولة", type = AccountType.LIABILITY, parentId = liabilitiesId, isGroup = true)).toInt()
                val ownerEquityId = dao.insertAccount(Account(code = "31", name = "رأس المال المدفوع", type = AccountType.EQUITY, parentId = equityId, isGroup = true)).toInt()
                
                val rentRevenuesId = dao.insertAccount(Account(code = "41", name = "إيرادات التشغيل", type = AccountType.REVENUE, parentId = revenuesId, isGroup = true)).toInt()
                val operatingExpensesId = dao.insertAccount(Account(code = "51", name = "مصاريف عامة وتشغيلية", type = AccountType.EXPENSE, parentId = expensesId, isGroup = true)).toInt()

                // Direct Posting Accounts
                dao.insertAccount(Account(code = "1101", name = "الصندوق الرئيسي", type = AccountType.ASSET, parentId = curAssetsId, isGroup = false, balance = 50000.0))
                dao.insertAccount(Account(code = "1102", name = "الحساب البنكي الجاري", type = AccountType.ASSET, parentId = curAssetsId, isGroup = false, balance = 250000.0))
                dao.insertAccount(Account(code = "1103", name = "ذمم العملاء والمدينين", type = AccountType.ASSET, parentId = curAssetsId, isGroup = false))
                dao.insertAccount(Account(code = "1201", name = "أسطول السيارات والمعدات", type = AccountType.ASSET, parentId = fixAssetsId, isGroup = false, balance = 750000.0))

                dao.insertAccount(Account(code = "2101", name = "ذمم الموردين والدائنين", type = AccountType.LIABILITY, parentId = curLiabilitiesId, isGroup = false))
                dao.insertAccount(Account(code = "3101", name = "رأس المال الأصلي", type = AccountType.EQUITY, parentId = ownerEquityId, isGroup = false, balance = 1000000.0))

                dao.insertAccount(Account(code = "4101", name = "إيرادات إيجار المركبات", type = AccountType.REVENUE, parentId = rentRevenuesId, isGroup = false))
                dao.insertAccount(Account(code = "4102", name = "إيرادات الخياطة والتطريز", type = AccountType.REVENUE, parentId = rentRevenuesId, isGroup = false))

                dao.insertAccount(Account(code = "5101", name = "مصاريف صيانة السيارات", type = AccountType.EXPENSE, parentId = operatingExpensesId, isGroup = false))
                dao.insertAccount(Account(code = "5102", name = "مصاريف الوقود وبطاقات شحن", type = AccountType.EXPENSE, parentId = operatingExpensesId, isGroup = false))
                dao.insertAccount(Account(code = "5103", name = "مصاريف إيجار المعرض والمشغل", type = AccountType.EXPENSE, parentId = operatingExpensesId, isGroup = false))
            }
        }
    }

    // 2. Add New Account
    suspend fun createAccount(code: String, name: String, type: AccountType, parentId: Int?, isGroup: Boolean) {
        val account = Account(code = code, name = name, type = type, parentId = parentId, isGroup = isGroup)
        dao.insertAccount(account)
    }

    // 3. Add Partner
    suspend fun createPartner(name: String, phone: String, type: String, notes: String) {
        val partner = AccountPartner(name = name, phone = phone, type = type, notes = notes)
        dao.insertPartner(partner)
    }

    // 3.5 Record Transaction against a Customer or Supplier Partner (with double entry vouchers)
    suspend fun recordPartnerTransaction(
        partnerId: Int,
        amount: Double,
        isPayment: Boolean, // true for paying out (صرف), false for receiving (قبض)
        safeOrBankAccountId: Int,
        notes: String
    ): Boolean {
        if (amount <= 0) return false
        return database.withTransaction {
            val partnersList = dao.getAllPartners().first()
            val partner = partnersList.find { it.id == partnerId } ?: return@withTransaction false
            val isCustomer = partner.type == "CUSTOMER"

            // Adjust partner balance
            // Customer: Debit represents they owe us. Receipt (قبض) reduces debt (toward zero, so +amount), Payment increases debt (-amount).
            // Supplier: Credit represents we owe them. Payment (صرف) reduces liability (toward zero, so -amount), Receipt increases liability (+amount).
            val balanceAdjustment = if (isCustomer) {
                if (isPayment) -amount else amount
            } else {
                if (isPayment) amount else -amount
            }
            dao.adjustPartnerBalance(partnerId, balanceAdjustment)

            // Create double-entry general journal voucher
            val voucherNo = "PTN-${1000 + (Math.random() * 8999).toInt()}"
            val voucherId = dao.insertVoucher(
                Voucher(
                    voucherNo = voucherNo,
                    date = System.currentTimeMillis(),
                    type = if (isPayment) "PAYMENT" else "RECEIPT",
                    description = "$notes - جهة: ${partner.name}",
                    totalAmount = amount
                )
            ).toInt()

            // Find general ledger accounts for Partner Group
            val accs = dao.getAllAccounts().first()
            val partnerLedgerAccountId = if (isCustomer) {
                accs.find { it.code == "1103" }?.id ?: dao.insertAccount(
                    Account(code = "1103", name = "ذمم العملاء والمدينين", type = AccountType.ASSET, parentId = accs.find { it.code == "11" }?.id)
                ).toInt()
            } else {
                accs.find { it.code == "2101" }?.id ?: dao.insertAccount(
                    Account(code = "2101", name = "ذمم الموردين والدائنين", type = AccountType.LIABILITY, parentId = accs.find { it.code == "21" }?.id)
                ).toInt()
            }

            if (isPayment) {
                // Payment (صرف): Partner Ledger gets DEBITED (+ for customer asset / - for supplier liability), Cash Account gets CREDITED (-)
                dao.insertVoucherLine(VoucherLine(voucherId = voucherId, accountId = partnerLedgerAccountId, debit = amount, credit = 0.0, memo = notes))
                dao.insertVoucherLine(VoucherLine(voucherId = voucherId, accountId = safeOrBankAccountId, debit = 0.0, credit = amount, memo = notes))

                // Update ledger balances
                dao.adjustAccountBalance(partnerLedgerAccountId, amount) // Asset increases or liability decreases
                dao.adjustAccountBalance(safeOrBankAccountId, -amount) // Asset decreases
            } else {
                // Receipt (قبض): Cash Account gets DEBITED (+), Partner Ledger gets CREDITED (- for customer asset / + for supplier liability)
                dao.insertVoucherLine(VoucherLine(voucherId = voucherId, accountId = safeOrBankAccountId, debit = amount, credit = 0.0, memo = notes))
                dao.insertVoucherLine(VoucherLine(voucherId = voucherId, accountId = partnerLedgerAccountId, debit = 0.0, credit = amount, memo = notes))

                // Update ledger balances
                dao.adjustAccountBalance(safeOrBankAccountId, amount) // Asset increases
                dao.adjustAccountBalance(partnerLedgerAccountId, -amount) // Asset decreases or liability increases
            }
            true
        }
    }

    // 3.6 Record Quick general cash transaction (Simplified receipts/payments)
    suspend fun recordQuickTransaction(
        safeOrBankAccountId: Int,
        targetAccountId: Int,
        amount: Double,
        isPayment: Boolean, // true for output (صرف), false for input (قبض)
        memo: String
    ): Boolean {
        if (amount <= 0) return false
        return database.withTransaction {
            val voucherNo = "SND-${1000 + (Math.random() * 8999).toInt()}"
            val voucherId = dao.insertVoucher(
                Voucher(
                    voucherNo = voucherNo,
                    date = System.currentTimeMillis(),
                    type = if (isPayment) "PAYMENT" else "RECEIPT",
                    description = memo,
                    totalAmount = amount
                )
            ).toInt()

            if (isPayment) {
                // Payment/صرف: Debit target account (+ expense), Credit safe (- cash)
                dao.insertVoucherLine(VoucherLine(voucherId = voucherId, accountId = targetAccountId, debit = amount, credit = 0.0, memo = memo))
                dao.insertVoucherLine(VoucherLine(voucherId = voucherId, accountId = safeOrBankAccountId, debit = 0.0, credit = amount, memo = memo))

                // Update ledger balances
                val targetAccount = accounts.first().find { it.id == targetAccountId }
                val safeAccount = accounts.first().find { it.id == safeOrBankAccountId }

                if (targetAccount != null) {
                    val multiplier = when (targetAccount.type) {
                        AccountType.ASSET, AccountType.EXPENSE -> 1.0
                        AccountType.LIABILITY, AccountType.EQUITY, AccountType.REVENUE -> -1.0
                    }
                    dao.adjustAccountBalance(targetAccountId, amount * multiplier)
                }
                if (safeAccount != null) {
                    val multiplier = when (safeAccount.type) {
                        AccountType.ASSET, AccountType.EXPENSE -> 1.0
                        AccountType.LIABILITY, AccountType.EQUITY, AccountType.REVENUE -> -1.0
                    }
                    dao.adjustAccountBalance(safeOrBankAccountId, -amount * multiplier)
                }
            } else {
                // Receipt/قبض: Debit safe (+ cash), Credit target account (+ revenue)
                dao.insertVoucherLine(VoucherLine(voucherId = voucherId, accountId = safeOrBankAccountId, debit = amount, credit = 0.0, memo = memo))
                dao.insertVoucherLine(VoucherLine(voucherId = voucherId, accountId = targetAccountId, debit = 0.0, credit = amount, memo = memo))

                // Update ledger balances
                val safeAccount = accounts.first().find { it.id == safeOrBankAccountId }
                val targetAccount = accounts.first().find { it.id == targetAccountId }

                if (safeAccount != null) {
                    val multiplier = when (safeAccount.type) {
                        AccountType.ASSET, AccountType.EXPENSE -> 1.0
                        AccountType.LIABILITY, AccountType.EQUITY, AccountType.REVENUE -> -1.0
                    }
                    dao.adjustAccountBalance(safeOrBankAccountId, amount * multiplier)
                }
                if (targetAccount != null) {
                    val multiplier = when (targetAccount.type) {
                        AccountType.ASSET, AccountType.EXPENSE -> 1.0
                        AccountType.LIABILITY, AccountType.EQUITY, AccountType.REVENUE -> -1.0
                    }
                    dao.adjustAccountBalance(targetAccountId, -amount * multiplier)
                }
            }
            true
        }
    }

    // 3.7 Record Transfer (تحويل نقدية بين الصناديق والبنوك)
    suspend fun recordTransferTransaction(
        sourceAccountId: Int,
        destinationAccountId: Int,
        amount: Double,
        memo: String
    ): Boolean {
        if (amount <= 0 || sourceAccountId == destinationAccountId) return false
        return database.withTransaction {
            val voucherNo = "TRF-${1000 + (Math.random() * 8999).toInt()}"
            val voucherId = dao.insertVoucher(
                Voucher(
                    voucherNo = voucherNo,
                    date = System.currentTimeMillis(),
                    type = "JOURNAL",
                    description = memo,
                    totalAmount = amount
                )
            ).toInt()

            // Debit receiving
            dao.insertVoucherLine(VoucherLine(voucherId = voucherId, accountId = destinationAccountId, debit = amount, credit = 0.0, memo = memo))
            // Credit sending
            dao.insertVoucherLine(VoucherLine(voucherId = voucherId, accountId = sourceAccountId, debit = 0.0, credit = amount, memo = memo))

            // Update balances
            val sourceAcc = accounts.first().find { it.id == sourceAccountId }
            val destAcc = accounts.first().find { it.id == destinationAccountId }

            if (sourceAcc != null) {
                val multiplier = when (sourceAcc.type) {
                    AccountType.ASSET, AccountType.EXPENSE -> 1.0
                    AccountType.LIABILITY, AccountType.EQUITY, AccountType.REVENUE -> -1.0
                }
                dao.adjustAccountBalance(sourceAccountId, -amount * multiplier)
            }
            if (destAcc != null) {
                val multiplier = when (destAcc.type) {
                    AccountType.ASSET, AccountType.EXPENSE -> 1.0
                    AccountType.LIABILITY, AccountType.EQUITY, AccountType.REVENUE -> -1.0
                }
                dao.adjustAccountBalance(destinationAccountId, amount * multiplier)
            }
            true
        }
    }

    // 4. Save Double-Entry Voucher
    suspend fun saveDoubleEntryVoucher(
        voucherNo: String,
        type: String,
        description: String,
        date: Long,
        lines: List<Pair<Int, Triple<Double, Double, String>>> // List of: accountId to Triple(Debit, Credit, Memo)
    ): Boolean {
        // Validation: Verify total debits equal total credits
        val totalDebits = lines.sumOf { it.second.first }
        val totalCredits = lines.sumOf { it.second.second }
        if (Math.abs(totalDebits - totalCredits) > 0.01) {
            return false // Mismatched double-entry is not allowed to post
        }

        database.withTransaction {
            // Post general Voucher header
            val voucherId = dao.insertVoucher(
                Voucher(
                    voucherNo = voucherNo,
                    date = date,
                    type = type,
                    description = description,
                    totalAmount = totalDebits
                )
            ).toInt()

            // Post voucher ledger lines and update balances
            for (lineData in lines) {
                val accId = lineData.first
                val debit = lineData.second.first
                val credit = lineData.second.second
                val memo = lineData.second.third

                // Save individual VoucherLine
                dao.insertVoucherLine(
                    VoucherLine(
                        voucherId = voucherId,
                        accountId = accId,
                        debit = debit,
                        credit = credit,
                        memo = memo
                    )
                )

                // Adjust the ledger balance for this account
                // Asset / Expense: Increases with Debit (+), Decreases with Credit (-)
                // Liability / Equity / Revenue: Decreases with Debit (-), Increases with Credit (+)
                val accountRaw = accounts.first().find { it.id == accId }
                if (accountRaw != null) {
                    val multiplier = when (accountRaw.type) {
                        AccountType.ASSET, AccountType.EXPENSE -> 1.0
                        AccountType.LIABILITY, AccountType.EQUITY, AccountType.REVENUE -> -1.0
                    }
                    val balChange = (debit - credit) * multiplier
                    dao.adjustAccountBalance(accId, balChange)
                }
            }
        }
        return true
    }

    // 5. Delete Voucher & Revert Balances
    suspend fun deleteVoucher(voucherId: Int) {
        database.withTransaction {
            val lines = dao.getLinesForVoucher(voucherId)
            for (line in lines) {
                val accId = line.accountId
                val accountRaw = accounts.first().find { it.id == accId }
                if (accountRaw != null) {
                    val multiplier = when (accountRaw.type) {
                        AccountType.ASSET, AccountType.EXPENSE -> 1.0
                        AccountType.LIABILITY, AccountType.EQUITY, AccountType.REVENUE -> -1.0
                    }
                    // Reversing: Subtract debit adjustments, Add back credit adjustments
                    val balChange = (line.credit - line.debit) * multiplier
                    dao.adjustAccountBalance(accId, balChange)
                }
            }
            dao.deleteVoucherById(voucherId)
        }
    }

    // 6. Tailor Measurements
    suspend fun addMeasurement(measurement: TailorMeasurement) {
        dao.insertMeasurement(measurement)
    }

    suspend fun deleteMeasurement(measurement: TailorMeasurement) {
        dao.deleteMeasurement(measurement)
    }
}
