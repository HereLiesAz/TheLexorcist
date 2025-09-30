package com.hereliesaz.lexorcist.service

import android.content.Context
import android.util.Log
import com.hereliesaz.lexorcist.data.SettingsManager
import com.hereliesaz.lexorcist.model.Script
import com.hereliesaz.lexorcist.model.Template
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultExtrasSeeder @Inject constructor(
    private val googleApiService: GoogleApiService,
    private val settingsManager: SettingsManager,
    @ApplicationContext private val context: Context
) {

    suspend fun seedDefaultExtrasIfNeeded() = withContext(Dispatchers.IO) {
        if (settingsManager.areDefaultExtrasSeeded()) {
            Log.i("DefaultExtrasSeeder", "Default extras already seeded. Skipping.")
            return@withContext
        }

        Log.i("DefaultExtrasSeeder", "Starting to seed default extras...")

        try {
            // 1. Fetch existing remote items to avoid duplicates
            val remoteScripts = googleApiService.getSharedScripts()
            val remoteScriptNames = remoteScripts.map { it.name }.toSet()

            val remoteTemplates = googleApiService.getSharedTemplates()
            val remoteTemplateNames = remoteTemplates.map { it.name }.toSet()

            // 2. Load local extras from CSV files
            val localScripts = loadDefaultScriptsFromCsv()
            val localTemplates = loadDefaultTemplatesFromCsv()

            // 3. Compare and upload missing scripts
            localScripts.forEach { script ->
                if (script.name !in remoteScriptNames) {
                    Log.i("DefaultExtrasSeeder", "Seeding script: ${script.name}")
                    googleApiService.shareAddon(
                        name = script.name,
                        description = script.description,
                        content = script.content,
                        type = "Script",
                        authorName = script.authorName,
                        authorEmail = script.authorEmail,
                        court = script.court ?: ""
                    )
                }
            }

            // 4. Compare and upload missing templates
            localTemplates.forEach { template ->
                if (template.name !in remoteTemplateNames) {
                    Log.i("DefaultExtrasSeeder", "Seeding template: ${template.name}")
                    googleApiService.shareAddon(
                        name = template.name,
                        description = template.description,
                        content = template.content,
                        type = "Template",
                        authorName = template.authorName,
                        authorEmail = template.authorEmail,
                        court = template.court ?: ""
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

    private fun loadDefaultScriptsFromCsv(): List<Script> {
        val scripts = mutableListOf<Script>()
        try {
            context.assets.open("default_scripts.csv").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLine() // Skip header
                    val regex = "\"(.*?)\"".toRegex()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val tokens = regex.findAll(line!!).map { it.groupValues[1] }.toList()
                        if (tokens.size >= 5) {
                            val authorCombined = tokens[2]
                            val authorName = authorCombined.substringBefore(" <").trim()
                            val authorEmail = authorCombined.substringAfter("<").substringBefore(">").trim()

                            scripts.add(Script(
                                id = tokens[0],
                                name = tokens[1],
                                description = tokens[3],
                                content = tokens[4],
                                authorName = authorName,
                                authorEmail = authorEmail
                            ))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DefaultExtrasSeeder", "Failed to load default scripts from CSV", e)
        }
        return scripts
    }

    private fun loadDefaultTemplatesFromCsv(): List<Template> {
        val templates = mutableListOf<Template>()
        try {
            context.assets.open("default_templates.csv").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLine() // Skip header
                    val regex = "\"(.*?)\"".toRegex()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val tokens = regex.findAll(line!!).map { it.groupValues[1] }.toList()
                        if (tokens.size >= 7) {
                            templates.add(Template(
                                id = tokens[0],
                                name = tokens[1],
                                description = tokens[2],
                                authorName = tokens[3],
                                authorEmail = tokens[4],
                                court = tokens[5],
                                content = tokens[6]
                            ))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DefaultExtrasSeeder", "Failed to load default templates from CSV", e)
        }
        return templates
    }
}