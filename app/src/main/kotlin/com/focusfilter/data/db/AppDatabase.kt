// Copyright (C) 2026 FocusFilter
// SPDX-License-Identifier: GPL-3.0-or-later
package com.focusfilter.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.focusfilter.data.db.converters.KeywordsConverter
import com.focusfilter.data.db.dao.FilteredNotificationDao
import com.focusfilter.data.db.dao.FocusModeDao
import com.focusfilter.data.db.dao.RuleDao
import com.focusfilter.data.db.dao.SenderReputationDao
import com.focusfilter.data.db.entities.FilteredNotification
import com.focusfilter.data.db.entities.FocusMode
import com.focusfilter.data.db.entities.Rule
import com.focusfilter.data.db.entities.SenderReputation
import com.focusfilter.data.keyword.KeywordSafelist
import com.focusfilter.data.keyword.KeywordSafelistDao
import com.focusfilter.model.NotificationAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [FilteredNotification::class, Rule::class, FocusMode::class, KeywordSafelist::class, SenderReputation::class],
    version = 10,
    exportSchema = false
)
@TypeConverters(KeywordsConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun filteredNotificationDao(): FilteredNotificationDao
    abstract fun ruleDao(): RuleDao
    abstract fun focusModeDao(): FocusModeDao
    abstract fun keywordSafelistDao(): KeywordSafelistDao
    abstract fun senderReputationDao(): SenderReputationDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE filtered_notifications ADD COLUMN blockReason TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE filtered_notifications ADD COLUMN isOverride INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE focus_modes ADD COLUMN displayName TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE focus_modes ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE focus_modes ADD COLUMN iconName TEXT NOT NULL DEFAULT 'ic_shield'")
                database.execSQL("ALTER TABLE focus_modes ADD COLUMN scheduleEnabled INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE focus_modes ADD COLUMN scheduleStart TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE focus_modes ADD COLUMN scheduleEnd TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE focus_modes ADD COLUMN isCustom INTEGER NOT NULL DEFAULT 0")
                database.execSQL("UPDATE focus_modes SET displayName='Gaming Mode', description='Block distractions, allow important', iconName='ic_gamepad' WHERE type='GAMING'")
                database.execSQL("UPDATE focus_modes SET displayName='Work Mode', description='Prioritize work apps and important alerts', iconName='ic_briefcase' WHERE type='WORK'")
                database.execSQL("UPDATE focus_modes SET displayName='Sleep Mode', description='Only emergencies will break through', iconName='ic_moon' WHERE type='SLEEP'")
                database.execSQL("UPDATE focus_modes SET displayName='Custom Mode', description='Your own rules and filters', iconName='ic_settings', isCustom=1 WHERE type='CUSTOM'")
                database.execSQL("""INSERT OR IGNORE INTO focus_modes (type,displayName,description,iconName,isActive,allowedApps,allowedContacts,allowedKeywords,blockedKeywords,defaultAction,isCustom,scheduleEnabled,scheduleStart,scheduleEnd) VALUES ('STUDY','Study Mode','Deep focus for learning and studying','ic_timer',0,'','','alarm,urgent,emergency,call','','BLOCK',0,0,'','')""")
                database.execSQL("""INSERT OR IGNORE INTO focus_modes (type,displayName,description,iconName,isActive,allowedApps,allowedContacts,allowedKeywords,blockedKeywords,defaultAction,isCustom,scheduleEnabled,scheduleStart,scheduleEnd) VALUES ('EXAM','Exam Mode','Maximum focus — only emergencies pass','ic_block',0,'','','emergency,urgent,alarm','','BLOCK',0,0,'','')""")
                database.execSQL("""INSERT OR IGNORE INTO focus_modes (type,displayName,description,iconName,isActive,allowedApps,allowedContacts,allowedKeywords,blockedKeywords,defaultAction,isCustom,scheduleEnabled,scheduleStart,scheduleEnd) VALUES ('DEEP_FOCUS','Deep Focus','Zero interruptions — you are in the zone','ic_shield',0,'','','emergency,alarm','','BLOCK',0,0,'','')""")
                database.execSQL("""INSERT OR IGNORE INTO focus_modes (type,displayName,description,iconName,isActive,allowedApps,allowedContacts,allowedKeywords,blockedKeywords,defaultAction,isCustom,scheduleEnabled,scheduleStart,scheduleEnd) VALUES ('READING','Reading Mode','Quiet time for books and articles','ic_check_circle',0,'','','urgent,emergency,call','','BLOCK',0,0,'','')""")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE focus_modes ADD COLUMN silenceCalls INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE focus_modes ADD COLUMN isBuiltIn INTEGER NOT NULL DEFAULT 0")
                database.execSQL("UPDATE focus_modes SET isBuiltIn=1 WHERE type IN ('GAMING','WORK','SLEEP','CUSTOM','STUDY','EXAM','DEEP_FOCUS','READING')")
                database.execSQL("UPDATE focus_modes SET silenceCalls=0 WHERE type='SLEEP'")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val cursor = database.query("SELECT type, allowedKeywords, blockedKeywords FROM focus_modes")
                while (cursor.moveToNext()) {
                    val type        = cursor.getString(0) ?: continue
                    val allowedCsv  = cursor.getString(1) ?: ""
                    val blockedCsv  = cursor.getString(2) ?: ""
                    val allowedJson = csvToJsonArray(allowedCsv)
                    val blockedJson = csvToJsonArray(blockedCsv)
                    database.execSQL(
                        "UPDATE focus_modes SET allowedKeywords=?, blockedKeywords=? WHERE type=?",
                        arrayOf(allowedJson, blockedJson, type)
                    )
                }
                cursor.close()
            }

            private fun csvToJsonArray(csv: String): String {
                if (csv.isBlank()) return "[]"
                if (csv.trimStart().startsWith('[')) return csv
                val items = csv.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .joinToString(",") { "\"${it.replace("\\", "\\\\").replace("\"", "\\\"")}\"" }
                return "[$items]"
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE filtered_notifications ADD COLUMN isPermanent INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE filtered_notifications ADD COLUMN sbnKey TEXT"
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS sender_reputation (
                        packageName TEXT PRIMARY KEY NOT NULL,
                        allowCount INTEGER NOT NULL DEFAULT 0,
                        blockCount INTEGER NOT NULL DEFAULT 0,
                        lastSeen INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE keyword_safelist ADD COLUMN category TEXT NOT NULL DEFAULT 'CUSTOM'")
                database.execSQL("ALTER TABLE keyword_safelist ADD COLUMN isDefault INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE keyword_safelist ADD COLUMN isEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS keyword_safelist (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        keyword TEXT NOT NULL,
                        addedAt INTEGER NOT NULL
                    )"""
                )
                // Strip emojis from built-in mode display names
                database.execSQL("UPDATE focus_modes SET displayName='Gaming Mode'   WHERE type='GAMING'     AND (displayName LIKE '🎮%' OR displayName LIKE '%Gaming Mode')")
                database.execSQL("UPDATE focus_modes SET displayName='Work Mode'     WHERE type='WORK'       AND (displayName LIKE '💼%' OR displayName LIKE '%Work Mode')")
                database.execSQL("UPDATE focus_modes SET displayName='Sleep Mode'    WHERE type='SLEEP'      AND (displayName LIKE '😴%' OR displayName LIKE '%Sleep Mode')")
                database.execSQL("UPDATE focus_modes SET displayName='Custom Mode'   WHERE type='CUSTOM'     AND (displayName LIKE '%Custom Mode')")
                database.execSQL("UPDATE focus_modes SET displayName='Study Mode'    WHERE type='STUDY'      AND (displayName LIKE '%Study Mode')")
                database.execSQL("UPDATE focus_modes SET displayName='Exam Mode'     WHERE type='EXAM'       AND (displayName LIKE '%Exam Mode')")
                database.execSQL("UPDATE focus_modes SET displayName='Deep Focus'    WHERE type='DEEP_FOCUS' AND (displayName LIKE '%Deep Focus')")
                database.execSQL("UPDATE focus_modes SET displayName='Reading Mode'  WHERE type='READING'    AND (displayName LIKE '%Reading Mode')")
                // Ensure silenceCalls=false for all built-in modes except none
                database.execSQL("UPDATE focus_modes SET silenceCalls=0 WHERE isBuiltIn=1")
            }
        }


        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Recreate focus_modes without the removed dndEnabled column
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS focus_modes_new (
                        type TEXT PRIMARY KEY NOT NULL,
                        displayName TEXT NOT NULL DEFAULT '',
                        description TEXT NOT NULL DEFAULT '',
                        iconName TEXT NOT NULL DEFAULT 'ic_shield',
                        isActive INTEGER NOT NULL DEFAULT 0,
                        silenceCalls INTEGER NOT NULL DEFAULT 0,
                        allowedApps TEXT NOT NULL DEFAULT '',
                        allowedContacts TEXT NOT NULL DEFAULT '',
                        allowedKeywords TEXT NOT NULL DEFAULT '[]',
                        blockedKeywords TEXT NOT NULL DEFAULT '[]',
                        defaultAction TEXT NOT NULL DEFAULT 'BLOCK',
                        isCustom INTEGER NOT NULL DEFAULT 0,
                        isBuiltIn INTEGER NOT NULL DEFAULT 0,
                        scheduleEnabled INTEGER NOT NULL DEFAULT 0,
                        scheduleStart TEXT NOT NULL DEFAULT '',
                        scheduleEnd TEXT NOT NULL DEFAULT ''
                    )
                """)
                database.execSQL("""
                    INSERT INTO focus_modes_new
                        (type,displayName,description,iconName,isActive,silenceCalls,
                         allowedApps,allowedContacts,allowedKeywords,blockedKeywords,
                         defaultAction,isCustom,isBuiltIn,scheduleEnabled,scheduleStart,scheduleEnd)
                    SELECT type,displayName,description,iconName,isActive,silenceCalls,
                         allowedApps,allowedContacts,allowedKeywords,blockedKeywords,
                         defaultAction,isCustom,isBuiltIn,scheduleEnabled,scheduleStart,scheduleEnd
                    FROM focus_modes
                """)
                database.execSQL("DROP TABLE focus_modes")
                database.execSQL("ALTER TABLE focus_modes_new RENAME TO focus_modes")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "focusfilter.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .build()

                INSTANCE = instance

                CoroutineScope(Dispatchers.IO).launch {
                    ensureBuiltInModes(instance.focusModeDao())
                    seedDefaultRules(instance.ruleDao())
                    seedDefaultKeywords(instance.keywordSafelistDao(), context.applicationContext)
                }

                instance
            }
        }

        suspend fun ensureBuiltInModes(dao: FocusModeDao) {
            val existing      = dao.getAllModesList()
            val existingTypes = existing.map { it.type }.toSet()

            val builtInModes = listOf(
                FocusMode(type = "GAMING",     displayName = "Gaming Mode",   description = "Block distractions, allow important", iconName = "ic_gamepad",  silenceCalls = false, allowedKeywords = listOf("urgent","emergency","payment","OTP","verify"), defaultAction = NotificationAction.BLOCK.name, isBuiltIn = true),
                FocusMode(type = "WORK",       displayName = "Work Mode",     description = "Prioritize work apps and important alerts", iconName = "ic_briefcase",  silenceCalls = false, allowedKeywords = listOf("meeting","call","urgent","payment","OTP","bank"), defaultAction = NotificationAction.BLOCK.name, isBuiltIn = true),
                FocusMode(type = "SLEEP",      displayName = "Sleep Mode",    description = "Only emergencies will break through", iconName = "ic_moon",  silenceCalls = false, allowedKeywords = listOf("emergency","urgent","alarm"), defaultAction = NotificationAction.BLOCK.name, isBuiltIn = true),
                FocusMode(type = "CUSTOM",     displayName = "Custom Mode",   description = "Your own rules and filters", iconName = "ic_settings", silenceCalls = false, defaultAction = NotificationAction.ALLOW.name, isBuiltIn = true),
                FocusMode(type = "STUDY",      displayName = "Study Mode",    description = "Deep focus for learning and studying", iconName = "ic_timer",  silenceCalls = false, allowedKeywords = listOf("alarm","urgent","emergency","call"), defaultAction = NotificationAction.BLOCK.name, isBuiltIn = true),
                FocusMode(type = "EXAM",       displayName = "Exam Mode",     description = "Maximum focus — only emergencies pass", iconName = "ic_block",  silenceCalls = false, allowedKeywords = listOf("emergency","urgent","alarm"), defaultAction = NotificationAction.BLOCK.name, isBuiltIn = true),
                FocusMode(type = "DEEP_FOCUS", displayName = "Deep Focus",    description = "Zero interruptions — you're in the zone", iconName = "ic_shield",  silenceCalls = false, allowedKeywords = listOf("emergency","alarm"), defaultAction = NotificationAction.BLOCK.name, isBuiltIn = true),
                FocusMode(type = "READING",    displayName = "Reading Mode",  description = "Quiet time for books and articles", iconName = "ic_check_circle", silenceCalls = false, allowedKeywords = listOf("urgent","emergency","call"), defaultAction = NotificationAction.BLOCK.name, isBuiltIn = true)
            )

            builtInModes.forEach { mode ->
                if (mode.type !in existingTypes) {
                    dao.insertOrReplace(mode)
                } else {
                    val current = existing.find { it.type == mode.type }
                    if (current != null && !current.isBuiltIn) {
                        dao.insertOrReplace(
                            current.copy(
                                isBuiltIn    = true,
                                displayName  = mode.displayName,
                                silenceCalls = false,
                            )
                        )
                    }
                }
            }
        }

        private suspend fun seedDefaultRules(dao: RuleDao) {
            val existing = dao.getEnabledRules()
            if (existing.isNotEmpty()) return
            val defaultKeywords = listOf("OTP", "bank", "urgent", "verify", "emergency", "payment", "password", "security")
            defaultKeywords.forEach { keyword ->
                dao.insert(Rule(
                    type      = "KEYWORD",
                    value     = keyword.lowercase(),
                    action    = NotificationAction.ALLOW.name,
                    label     = keyword,
                    isEnabled = true,
                    focusModes = "ALL"
                ))
            }
        }

        private suspend fun seedDefaultKeywords(dao: KeywordSafelistDao, ctx: Context) {
            val prefs = ctx.getSharedPreferences("focusfilter_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("keywords_seeded_v6", false)) return

            data class Seed(val keyword: String, val category: String)

            val seeds = listOf(
                // EMERGENCY
                Seed("emergency alert", "EMERGENCY"), Seed("medical emergency", "EMERGENCY"),
                Seed("emergency contact", "EMERGENCY"), Seed("sos alert", "EMERGENCY"),
                Seed("critical alert", "EMERGENCY"), Seed("safety alert", "EMERGENCY"),
                Seed("amber alert", "EMERGENCY"), Seed("evacuation", "EMERGENCY"),
                Seed("emergency broadcast", "EMERGENCY"), Seed("fire alert", "EMERGENCY"),
                Seed("flood warning", "EMERGENCY"), Seed("earthquake alert", "EMERGENCY"),
                Seed("help me", "EMERGENCY"), Seed("need help", "EMERGENCY"),
                Seed("call me now", "EMERGENCY"), Seed("call asap", "EMERGENCY"),
                Seed("danger", "EMERGENCY"), Seed("in danger", "EMERGENCY"),
                Seed("accident", "EMERGENCY"), Seed("police", "EMERGENCY"),
                Seed("ambulance", "EMERGENCY"), Seed("paramedic", "EMERGENCY"),
                Seed("rescue", "EMERGENCY"), Seed("stranded", "EMERGENCY"),
                Seed("trapped", "EMERGENCY"), Seed("missing person", "EMERGENCY"),
                Seed("lockdown", "EMERGENCY"),
                // MEDICAL
                Seed("hospital", "MEDICAL"), Seed("icu", "MEDICAL"),
                Seed("intensive care", "MEDICAL"), Seed("prescription", "MEDICAL"),
                Seed("medication", "MEDICAL"), Seed("take your pill", "MEDICAL"),
                Seed("dosage", "MEDICAL"), Seed("pharmacy", "MEDICAL"),
                Seed("blood pressure", "MEDICAL"), Seed("blood sugar", "MEDICAL"),
                Seed("glucose level", "MEDICAL"), Seed("heart rate", "MEDICAL"),
                Seed("oxygen level", "MEDICAL"), Seed("seizure", "MEDICAL"),
                Seed("surgery", "MEDICAL"), Seed("operation", "MEDICAL"),
                Seed("test results", "MEDICAL"), Seed("lab results", "MEDICAL"),
                Seed("scan results", "MEDICAL"), Seed("health alert", "MEDICAL"),
                Seed("critical condition", "MEDICAL"), Seed("overdose", "MEDICAL"),
                Seed("allergic reaction", "MEDICAL"), Seed("anaphylaxis", "MEDICAL"),
                Seed("chest pain", "MEDICAL"), Seed("stroke", "MEDICAL"),
                Seed("cardiac arrest", "MEDICAL"),
                // OTP
                Seed("otp", "OTP"), Seed("one-time password", "OTP"),
                Seed("one time password", "OTP"), Seed("one-time code", "OTP"),
                Seed("one time code", "OTP"), Seed("verification code", "OTP"),
                Seed("your code is", "OTP"), Seed("your otp is", "OTP"),
                Seed("authentication code", "OTP"), Seed("login code", "OTP"),
                Seed("security code", "OTP"), Seed("two-factor", "OTP"),
                Seed("two factor", "OTP"), Seed("multi-factor", "OTP"),
                Seed("do not share this", "OTP"), Seed("use this code", "OTP"),
                Seed("enter this code", "OTP"),
                // BANK_FRAUD
                Seed("fraud alert", "BANK_FRAUD"), Seed("unauthorized transaction", "BANK_FRAUD"),
                Seed("suspicious activity", "BANK_FRAUD"), Seed("suspicious login", "BANK_FRAUD"),
                Seed("unrecognized login", "BANK_FRAUD"), Seed("account locked", "BANK_FRAUD"),
                Seed("account blocked", "BANK_FRAUD"), Seed("account suspended", "BANK_FRAUD"),
                Seed("card blocked", "BANK_FRAUD"), Seed("card compromised", "BANK_FRAUD"),
                Seed("security breach", "BANK_FRAUD"), Seed("identity theft", "BANK_FRAUD"),
                Seed("unusual login", "BANK_FRAUD"), Seed("failed login attempt", "BANK_FRAUD"),
                Seed("unauthorized access", "BANK_FRAUD"),
                Seed("your account has been locked", "BANK_FRAUD"),
                Seed("your account has been suspended", "BANK_FRAUD"),
                // CALLS
                Seed("missed call", "CALLS"), Seed("missed video call", "CALLS"),
                Seed("missed voice call", "CALLS"), Seed("new voicemail", "CALLS"),
                Seed("voicemail from", "CALLS"), Seed("tried to reach you", "CALLS"),
                Seed("callback requested", "CALLS"), Seed("please call back", "CALLS"),
                Seed("call me back", "CALLS"),
                // MALAYSIAN
                Seed("resit", "OTP"),
                Seed("invois", "OTP"),
                Seed("bayaran", "OTP"),
                Seed("pembayaran diterima", "OTP"),
                Seed("transaksi berjaya", "OTP"),
                Seed("pengesahan", "OTP"),
                Seed("wang telah", "OTP"),
                Seed("akaun anda", "BANK_FRAUD"),
                Seed("had kredit", "BANK_FRAUD")
            )

            seeds.forEach { s ->
                dao.insert(KeywordSafelist(
                    keyword    = s.keyword,
                    category   = s.category,
                    isDefault  = true,
                    isEnabled  = true
                ))
            }

            prefs.edit().putBoolean("keywords_seeded_v6", true).apply()
        }
    }
}
