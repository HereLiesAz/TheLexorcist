package com.hereliesaz.lexorcist.data.repository

import android.content.Context
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.data.MasterAllegation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegalRepository @Inject constructor(@ApplicationContext private val context: Context) {

    /**
     * Loads the master list of allegations from the res/raw/master_allegations.json file.
     */
    fun getMasterAllegations(): Flow<List<MasterAllegation>> = flow {
        try {
            val inputStream = context.resources.openRawResource(R.raw.master_allegations)
            val reader = InputStreamReader(inputStream)
            val allegationListType = object : TypeToken<List<MasterAllegation>>() {}.type
            val allegations: List<MasterAllegation> = Gson().fromJson(reader, allegationListType)
            emit(allegations)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }
}