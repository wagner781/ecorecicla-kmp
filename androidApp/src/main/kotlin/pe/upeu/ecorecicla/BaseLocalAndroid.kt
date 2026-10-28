package pe.upeu.ecorecicla

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import pe.upeu.ecorecicla.data.local.BaseLocal
import pe.upeu.ecorecicla.db.EcoDatabase

fun baseLocalAndroid(context: Context)=BaseLocal(EcoDatabase(AndroidSqliteDriver(EcoDatabase.Schema,context,"ecorutas.db")))
