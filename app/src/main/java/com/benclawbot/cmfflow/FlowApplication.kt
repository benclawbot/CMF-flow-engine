package com.benclawbot.cmfflow

import android.app.Application
import androidx.room.Room
import com.benclawbot.cmfflow.data.FlowDatabase

class FlowApplication : Application() {
    val database: FlowDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            FlowDatabase::class.java,
            "cmf-flow.db",
        ).build()
    }
}
