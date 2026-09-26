package com.example.waterreminder.complication

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.example.waterreminder.R
import com.example.waterreminder.data.WaterRepository
import com.example.waterreminder.presentation.MainActivity

class WaterComplicationService : ComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return createComplicationData(1250, 2000, type)
    }

    override fun onComplicationRequest(request: ComplicationRequest, listener: ComplicationRequestListener) {
        val repository = WaterRepository(applicationContext)
        val intake = repository.getCurrentIntake()
        val goal = repository.getDailyGoal()
        val data = createComplicationData(intake, goal, request.complicationType)
        listener.onComplicationData(data)
    }

    private fun createComplicationData(intake: Int, goal: Int, type: ComplicationType): ComplicationData? {
        val icon = Icon.createWithResource(applicationContext, R.drawable.ic_water_drop_complication)
        val iconMonochromatic = MonochromaticImage.Builder(icon).build()
        val percent = if (goal > 0) ((intake.toFloat() / goal) * 100).toInt().coerceIn(0, 100) else 0
        val percentageText = PlainComplicationText.Builder("$percent%").build()

        val openAppIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return when (type) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = intake.toFloat().coerceIn(0f, goal.toFloat()),
                    min = 0f,
                    max = goal.toFloat(),
                    contentDescription = PlainComplicationText.Builder("Water Intake $percent%").build()
                )
                    .setText(percentageText)
                    .setMonochromaticImage(iconMonochromatic)
                    .setTapAction(tapPendingIntent)
                    .build()
            }
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = percentageText,
                    contentDescription = PlainComplicationText.Builder("Water Intake $percent%").build()
                )
                    .setMonochromaticImage(iconMonochromatic)
                    .setTapAction(tapPendingIntent)
                    .build()
            }
            ComplicationType.SMALL_IMAGE -> {
                SmallImageComplicationData.Builder(
                    smallImage = SmallImage.Builder(icon, SmallImageType.ICON).build(),
                    contentDescription = PlainComplicationText.Builder("Water Drop Icon").build()
                )
                    .setTapAction(tapPendingIntent)
                    .build()
            }
            ComplicationType.MONOCHROMATIC_IMAGE -> {
                MonochromaticImageComplicationData.Builder(
                    monochromaticImage = iconMonochromatic,
                    contentDescription = PlainComplicationText.Builder("Water Drop Icon").build()
                )
                    .setTapAction(tapPendingIntent)
                    .build()
            }
            else -> null
        }
    }

    companion object {
        fun requestComplicationUpdate(context: Context) {
            val componentName = ComponentName(context, WaterComplicationService::class.java)
            val requester = ComplicationDataSourceUpdateRequester.create(context, componentName)
            requester.requestUpdateAll()
        }
    }
}
