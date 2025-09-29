package com.hereliesaz.lexorcist.service

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.model.Script
import com.hereliesaz.lexorcist.model.Template
import com.hereliesaz.lexorcist.utils.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultExtrasSeeder @Inject constructor(
    private val googleApiService: GoogleApiService,
    private val settingsManager: SettingsManager,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) {

    private data class DefaultExtras(
        val scripts: List<Script>,
        val templates: List<Template>
    )

    suspend fun seedDefaultExtrasIfNeeded() = withContext(Dispatchers.IO) {
        if (settingsManager.areDefaultExtrasSeeded()) {
            Log.i("DefaultExtrasSeeder", "Default extras already seeded. Skipping.")
            return@withContext
        }

        Log.i("DefaultExtrasSeeder", "Starting to seed default extras...")

        try {
            // 1. Fetch existing remote items to avoid duplicates
            val remoteScriptsResult = googleApiService.getSharedScripts()
            val remoteTemplatesResult = googleApiService.getSharedTemplates()

            val remoteScriptNames = if (remoteScriptsResult is Result.Success) {
                remoteScriptsResult.data.map { it.name }.toSet()
            } else {
                Log.e("DefaultExtrasSeeder", "Failed to fetch remote scripts. Seeding will proceed based on empty list.")
                emptySet()
            }

            val remoteTemplateNames = if (remoteTemplatesResult is Result.Success) {
                remoteTemplatesResult.data.map { it.name }.toSet()
            } else {
                Log.e("DefaultExtrasSeeder", "Failed to fetch remote templates. Seeding will proceed based on empty list.")
                emptySet()
            }

            // 2. Parse local JSON
            val jsonString = context.assets.open("default_extras.json").bufferedReader().use { it.readText() }
            val typeToken = object : TypeToken<DefaultExtras>() {}.type
            val localExtras: DefaultExtras = gson.fromJson(jsonString, typeToken)

            // 3. Compare and upload missing scripts
            localExtras.scripts.forEach { script ->
                if (script.name !in remoteScriptNames) {
                    Log.i("DefaultExtrasSeeder", "Seeding script: ${script.name}")
                    googleApiService.shareAddon(
                        name = script.name,
                        description = script.description,
                        content = script.content,
                        type = "Script",
                        authorName = "Az",
                        authorEmail = "hereliesaz@gmail.com",
                        court = script.court
                    )
                }
            }

            // 4. Compare and upload missing templates
            localExtras.templates.forEach { template ->
                if (template.name !in remoteTemplateNames) {
                    Log.i("DefaultExtrasSeeder", "Seeding template: ${template.name}")
                    googleApiService.shareAddon(
                        name = template.name,
                        description = template.description,
                        content = template.content,
                        type = "Template",
                        authorName = "Az",
                        authorEmail = "hereliesaz@gmail.com",
                        court = template.court
                    )
                }
            }

            // 5. Mark as seeded
            settingsManager.setDefaultExtrasSeeded(true)
            Log.i("DefaultExtrasSeeder", "Default extras seeding process completed.")

        } catch (e: Exception) {
            Log.e("DefaultExtrasSeeder", "Failed to seed default extras", e)
        }
    }
}