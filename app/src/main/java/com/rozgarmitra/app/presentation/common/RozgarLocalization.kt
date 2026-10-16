package com.rozgarmitra.app.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rozgarmitra.app.R
import com.rozgarmitra.app.data.ApplicationStatus
import com.rozgarmitra.app.data.JobStatus

object RozgarLocalization {

    @Composable
    fun getTradeDisplayName(trade: String): String {
        val clean = trade.trim()
        return when {
            clean.startsWith("Mason", ignoreCase = true) -> stringResource(R.string.trade_mason)
            clean.startsWith("Painter", ignoreCase = true) -> stringResource(R.string.trade_painter)
            clean.startsWith("Electrician", ignoreCase = true) -> stringResource(R.string.trade_electrician)
            clean.startsWith("Plumber", ignoreCase = true) -> stringResource(R.string.trade_plumber)
            clean.startsWith("Welder", ignoreCase = true) -> stringResource(R.string.trade_welder)
            clean.startsWith("Driver", ignoreCase = true) -> stringResource(R.string.trade_driver)
            clean.startsWith("Cleaner", ignoreCase = true) -> stringResource(R.string.trade_cleaner)
            clean.startsWith("Gardener", ignoreCase = true) -> stringResource(R.string.trade_gardener)
            clean.startsWith("Farm", ignoreCase = true) -> stringResource(R.string.trade_farm_worker)
            clean.startsWith("Construction", ignoreCase = true) -> stringResource(R.string.trade_construction_labour)
            clean.startsWith("Helper", ignoreCase = true) -> stringResource(R.string.trade_helper)
            clean.startsWith("Cook", ignoreCase = true) -> stringResource(R.string.trade_cook)
            clean.startsWith("Security", ignoreCase = true) -> stringResource(R.string.trade_security_guard)
            clean.startsWith("Mechanic", ignoreCase = true) -> stringResource(R.string.trade_mechanic)
            clean.startsWith("Carpenter", ignoreCase = true) -> stringResource(R.string.trade_carpenter)
            clean.equals("All Jobs", ignoreCase = true) -> stringResource(R.string.filter_all_jobs)
            else -> trade
        }
    }

    @Composable
    fun getApplicationStatusDisplayName(status: ApplicationStatus): String {
        return when (status) {
            ApplicationStatus.APPLIED -> stringResource(R.string.status_applied)
            ApplicationStatus.ACCEPTED -> stringResource(R.string.status_accepted)
            ApplicationStatus.REJECTED -> stringResource(R.string.status_rejected)
            ApplicationStatus.COMPLETED -> stringResource(R.string.status_completed)
            ApplicationStatus.CANCELLED -> stringResource(R.string.status_cancelled)
        }
    }

    @Composable
    fun getJobStatusDisplayName(status: JobStatus): String {
        return when (status) {
            JobStatus.ACTIVE -> stringResource(R.string.metric_active_jobs)
            JobStatus.FILLED -> stringResource(R.string.tag_filled)
            JobStatus.COMPLETED -> stringResource(R.string.status_completed)
            JobStatus.CLOSED -> stringResource(R.string.btn_job_filled)
            JobStatus.DRAFT -> "Draft"
        }
    }

    @Composable
    fun getWageTypeDisplayName(wageType: String): String {
        return when (wageType.lowercase()) {
            "per day", "daily", "day" -> stringResource(R.string.per_day)
            "per hour", "hourly", "hour" -> stringResource(R.string.per_hour)
            "fixed" -> stringResource(R.string.wage_fixed)
            else -> wageType
        }
    }

    @Composable
    fun getPerkDisplayName(perk: String): String {
        return when (perk.lowercase()) {
            "food" -> stringResource(R.string.perk_food)
            "shelter" -> stringResource(R.string.perk_shelter)
            "transport" -> stringResource(R.string.perk_transport)
            else -> perk
        }
    }

    @Composable
    fun getDifficultyDisplayName(diff: String): String {
        return when (diff.lowercase()) {
            "easy" -> stringResource(R.string.difficulty_easy)
            "medium" -> stringResource(R.string.difficulty_medium)
            "hard" -> stringResource(R.string.difficulty_hard)
            else -> diff
        }
    }

    @Composable
    fun getLocalizedExperience(tier: String): String {
        return when {
            tier.contains("< 1", ignoreCase = true) -> stringResource(R.string.exp_under_1_yr)
            tier.contains("1 - 2", ignoreCase = true) -> stringResource(R.string.exp_1_2_yr)
            tier.contains("3 - 5", ignoreCase = true) -> stringResource(R.string.exp_3_5_yr)
            tier.contains("5+", ignoreCase = true) -> stringResource(R.string.exp_5_plus_yr)
            else -> tier
        }
    }
}

@Composable
fun getTradeDisplayName(trade: String): String = RozgarLocalization.getTradeDisplayName(trade)

@Composable
fun getLocalizedTradeName(trade: String): String = RozgarLocalization.getTradeDisplayName(trade)

@Composable
fun getApplicationStatusDisplayName(status: ApplicationStatus): String = RozgarLocalization.getApplicationStatusDisplayName(status)

@Composable
fun getApplicationStatusDisplayName(status: String): String {
    val enumStatus = try {
        ApplicationStatus.valueOf(status.uppercase())
    } catch (_: Exception) {
        ApplicationStatus.APPLIED
    }
    return RozgarLocalization.getApplicationStatusDisplayName(enumStatus)
}

@Composable
fun getLocalizedApplicationStatus(status: ApplicationStatus): String = RozgarLocalization.getApplicationStatusDisplayName(status)

@Composable
fun getJobStatusDisplayName(status: JobStatus): String = RozgarLocalization.getJobStatusDisplayName(status)

@Composable
fun getJobStatusDisplayName(status: String): String {
    val enumStatus = try {
        JobStatus.valueOf(status.uppercase())
    } catch (_: Exception) {
        JobStatus.ACTIVE
    }
    return RozgarLocalization.getJobStatusDisplayName(enumStatus)
}

@Composable
fun getWageTypeDisplayName(wageType: String): String = RozgarLocalization.getWageTypeDisplayName(wageType)

@Composable
fun getLocalizedWageType(wageType: String): String = RozgarLocalization.getWageTypeDisplayName(wageType)

@Composable
fun getPerkDisplayName(perk: String): String = RozgarLocalization.getPerkDisplayName(perk)

@Composable
fun getDifficultyDisplayName(diff: String): String = RozgarLocalization.getDifficultyDisplayName(diff)

@Composable
fun getLocalizedExperience(tier: String): String = RozgarLocalization.getLocalizedExperience(tier)


