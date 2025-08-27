package com.hereliesaz.lexorcist.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
//import com.google.api.services.script.v1.model.Content
//import com.google.api.services.script.v1.model.File
//import com.google.api.services.script.v1.model.Project

class ScriptEditorActivity : AppCompatActivity() {

    private lateinit var scriptEditText: EditText
    private lateinit var saveScriptButton: Button
    private var spreadsheetId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_script_editor)

        scriptEditText = findViewById(R.id.script_edit_text)
        saveScriptButton = findViewById(R.id.save_script_button)

        spreadsheetId = intent.getStringExtra("spreadsheetId")

        if (spreadsheetId == null) {
            Toast.makeText(this, "Spreadsheet ID not found.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // TODO: Initialize the Script API service
        // and fetch the script content.

        saveScriptButton.setOnClickListener {
            val scriptContent = scriptEditText.text.toString()
            if (scriptContent.isNotEmpty()) {
                // TODO: Save the script using the Script API.
            }
        }
    }
}
