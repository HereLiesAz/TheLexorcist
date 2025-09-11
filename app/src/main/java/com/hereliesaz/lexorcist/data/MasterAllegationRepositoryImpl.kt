package com.hereliesaz.lexorcist.data

import android.app.Application
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hereliesaz.lexorcist.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasterAllegationRepositoryImpl
    @Inject
    constructor(
        private val application: Application,
        private val gson: Gson,
    ) : MasterAllegationRepository {
        override fun getMasterAllegations(): Flow<List<MasterAllegation>> =
            flow {
                val inputStream = application.resources.openRawResource(R.raw.master_allegations)
                val reader = InputStreamReader(inputStream)
                val allegationListType = object : TypeToken<List<MasterAllegation>>() {}.type
                val allegations: List<MasterAllegation> = gson.fromJson(reader, allegationListType)
                emit(allegations)
            }
    }

