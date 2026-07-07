# ProGuard rules for ExpenseTracker

# Keep Room entities
-keep class com.expensetracker.app.data.local.entity.** { *; }

# Keep domain models
-keep class com.expensetracker.app.domain.model.** { *; }

# Keep Retrofit interfaces
-keep,allowobfuscation interface com.expensetracker.app.data.remote.api.** { *; }

# Keep DTO classes
-keep class com.expensetracker.app.data.remote.api.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# OpenCSV
-keep class com.opencsv.** { *; }

# Vico
-keep class com.patrykandpatrick.vico.** { *; }
