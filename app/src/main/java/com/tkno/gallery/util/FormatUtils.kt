package com.tkno.gallery.util

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.log10
import kotlin.math.pow

object FormatUtils {

    private val headerDateFormatter = DateTimeFormatter.ofPattern("MMMM d, y", Locale.getDefault())
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
    private val headerDateFormat = SimpleDateFormat("MMMM d, y", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("MMM d, y · HH:mm", Locale.getDefault())
    
    // Limits for Year 1 AD (-62,135,596,800s) to Year 1 Billion AD (31,556,952,000,000,000s)
    private const val MIN_YEAR_1_SEC = -62_135_596_800L
    private const val MAX_YEAR_1_BILLION_SEC = 31_556_952_000_000_000L

    // High-performance LRU Cache for formatted date headers to eliminate SimpleDateFormat allocation overhead
    private val dateHeaderCache = ConcurrentHashMap<Long, String>(128)
    private val dateShortCache = ConcurrentHashMap<Long, String>(128)
    private val dateMonthYearCache = ConcurrentHashMap<Long, String>(128)

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
    }

    fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatDateHeader(timestampSec: Long): String {
        if (timestampSec == 0L) return "Unknown Date"
        val clampedSec = timestampSec.coerceIn(MIN_YEAR_1_SEC, MAX_YEAR_1_BILLION_SEC)
        return try {
            val localDate = Instant.ofEpochSecond(clampedSec).atZone(ZoneId.systemDefault()).toLocalDate()
            val dayKey = localDate.toEpochDay()
            dateHeaderCache.getOrPut(dayKey) {
                localDate.format(headerDateFormatter)
            }
        } catch (e: Exception) {
            // Fallback for extreme timestamps (e.g. year 1 AD to 1 Billion AD, overflow, etc.)
            try {
                val safeMs = when {
                    clampedSec > Long.MAX_VALUE / 1000L -> Long.MAX_VALUE
                    clampedSec < Long.MIN_VALUE / 1000L -> Long.MIN_VALUE
                    else -> clampedSec * 1000L
                }
                val date = Date(safeMs)
                synchronized(headerDateFormat) {
                    headerDateFormat.format(date)
                }
            } catch (e2: Exception) {
                "Unknown Date"
            }
        }
    }

    fun formatDateShort(timestampSec: Long): String {
        if (timestampSec == 0L) return "Unknown Date"
        val clampedSec = timestampSec.coerceIn(MIN_YEAR_1_SEC, MAX_YEAR_1_BILLION_SEC)
        val minuteKey = clampedSec / 60
        return dateShortCache.getOrPut(minuteKey) {
            try {
                val safeMs = when {
                    clampedSec > Long.MAX_VALUE / 1000L -> Long.MAX_VALUE
                    clampedSec < Long.MIN_VALUE / 1000L -> Long.MIN_VALUE
                    else -> clampedSec * 1000L
                }
                synchronized(shortDateFormat) {
                    shortDateFormat.format(Date(safeMs))
                }
            } catch (e: Exception) {
                "Unknown Date"
            }
        }
    }

    fun formatMonthYear(timestampSec: Long): String {
        if (timestampSec == 0L) return ""
        val clampedSec = timestampSec.coerceIn(MIN_YEAR_1_SEC, MAX_YEAR_1_BILLION_SEC)
        return try {
            val localDate = Instant.ofEpochSecond(clampedSec).atZone(ZoneId.systemDefault()).toLocalDate()
            val monthKey = localDate.year * 100L + localDate.monthValue
            dateMonthYearCache.getOrPut(monthKey) {
                localDate.format(monthYearFormatter)
            }
        } catch (e: Exception) {
            ""
        }
    }
}
