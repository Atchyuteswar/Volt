package com.kazexyt.volt.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class VoltWidgetReceiver : GlanceAppWidgetReceiver() {

    // This tells the system which GlanceAppWidget to display
    override val glanceAppWidget: GlanceAppWidget = VoltWidget()
}