package com.hereliesaz.lexorcist.data

import android.content.Context
import com.hereliesaz.lexorcist.data.objectbox.MyObjectBox
import dagger.hilt.android.qualifiers.ApplicationContext
import io.objectbox.BoxStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObjectBoxManager @Inject constructor(@ApplicationContext context: Context) {

    private val boxStore: BoxStore = MyObjectBox.builder().androidContext(context.applicationContext).build()

    fun getBoxStore(): BoxStore {
        return boxStore
    }
}
