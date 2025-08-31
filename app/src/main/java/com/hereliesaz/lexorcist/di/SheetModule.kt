package com.hereliesaz.lexorcist.di

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.SheetsScopes
import com.hereliesaz.lexorcist.GoogleApiService
import com.hereliesaz.lexorcist.data.AllegationDao
import com.hereliesaz.lexorcist.data.CaseDao
import com.hereliesaz.lexorcist.data.EvidenceDao
import com.hereliesaz.lexorcist.data.FilterDao
import com.hereliesaz.lexorcist.data.remote.AllegationDaoImpl
import com.hereliesaz.lexorcist.data.remote.CaseDaoImpl
import com.hereliesaz.lexorcist.data.remote.EvidenceDaoImpl
import com.hereliesaz.lexorcist.data.remote.FilterDaoImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SheetModule {

    @Binds
    abstract fun bindCaseDao(impl: CaseDaoImpl): CaseDao

    @Binds
    abstract fun bindEvidenceDao(impl: EvidenceDaoImpl): EvidenceDao

    @Binds
    abstract fun bindAllegationDao(impl: AllegationDaoImpl): AllegationDao

    @Binds
    abstract fun bindFilterDao(impl: FilterDaoImpl): FilterDao

    companion object {
        @Provides
        @Singleton
        fun provideGoogleApiService(@ApplicationContext context: Context): GoogleApiService? {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null) {
                val credential = GoogleAccountCredential.usingOAuth2(
                    context,
                    listOf(DriveScopes.DRIVE, SheetsScopes.SPREADSHEETS)
                )
                credential.selectedAccount = account.account
                return GoogleApiService(credential, "The Lexorcist")
            }
            return null
        }
    }
}
