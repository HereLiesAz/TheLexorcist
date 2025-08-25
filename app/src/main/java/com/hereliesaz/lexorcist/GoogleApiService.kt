package com.hereliesaz.lexorcist

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.script.Script
import com.google.api.services.script.model.Content
import com.google.api.services.script.model.CreateProjectRequest
import com.google.api.services.script.model.File
import com.google.api.services.script.model.Project
import com.google.api.services.sheets.v4.Sheets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleApiService(credential: GoogleAccountCredential, applicationName: String) {

    private val transport = NetHttpTransport()
    private val jsonFactory = JacksonFactory.getDefaultInstance()

    val drive: Drive = Drive.Builder(transport, jsonFactory, credential)
        .setApplicationName(applicationName)
        .build()

    val sheets: Sheets = Sheets.Builder(transport, jsonFactory, credential)
        .setApplicationName(applicationName)
        .build()

    private val script: Script = Script.Builder(transport, jsonFactory, credential)
        .setApplicationName(applicationName)
        .build()

    suspend fun createMasterTemplate(): String? = withContext(Dispatchers.IO) {
        try {
            val templateContent = """
                Cover Sheet
                UNITED STATES DISTRICT COURT
                EASTERN DISTRICT OF LOUISIANA


                The following table:
                "{{PLAINTIFFS}}",CIVIL ACTION v. NO: {{CASE_NUMBER}}
                "{{DEFENDANTS}}",SECTION: {{SECTION}} ,JUDGE: {{JUDGE}}

                ____________________________________________________
                EXHIBIT {{EXHIBIT_NUMBER}}
                ____________________________________________________
                Description: {{EXHIBIT_NAME}}
                Date: {{EXHIBIT_DATE}}
                Metadata
                UNITED STATES DISTRICT COURT
                EASTERN DISTRICT OF LOUISIANA


                The following table:
                "{{PLAINTIFFS}}",CIVIL ACTION v. NO: {{CASE_NUMBER}}
                "{{DEFENDANTS}}",SECTION: {{SECTION}} ,JUDGE: {{JUDGE}}

                ____________________________________________________
                EXHIBIT {{EXHIBIT_NUMBER}}
                ____________________________________________________
                File Name: {{FILE_NAME}}
                File Type: {{FILE_TYPE}}
                File Size: {{FILE_SIZE}}
                Original Creation Date: {{CREATION_DATE}}
                Last Modified Date: {{MODIFIED_DATE}}
                File Owner: {{FILE_OWNER}}

                Google Drive ID: {{FILE_ID}}
                Chain of Custody
                UNITED STATES DISTRICT COURT
                EASTERN DISTRICT OF LOUISIANA


                The following table:
                "{{PLAINTIFFS}}",CIVIL ACTION v. NO: {{CASE_NUMBER}}
                "{{DEFENDANTS}}",SECTION: {{SECTION}} ,JUDGE: {{JUDGE}}

                ____________________________________________________
                This document tracks the possession of the evidence associated with Exhibit {{EXHIBIT_NUMBER}}.
                ____________________________________________________
                Initial Collection:
                - Item: {{EXHIBIT_NAME}}
                - Collected By: {{COLLECTED_BY}}
                - Date/Time of Collection: {{COLLECTION_DATE}}

                Custody Log:
                Affidavit
                UNITED STATES DISTRICT COURT
                EASTERN DISTRICT OF LOUISIANA


                The following table:
                "{{PLAINTIFFS}}",CIVIL ACTION v. NO: {{CASE_NUMBER}}
                "{{DEFENDANTS}}",SECTION: {{SECTION}} ,JUDGE: {{JUDGE}}

                ____________________________________________________
                EXHIBIT {{EXHIBIT_NUMBER}}
                ____________________________________________________

                DECLARATION OF RECORDS CUSTODIAN
                (Federal Rule of Evidence 902(11))

                Exhibit Number: {{EXHIBIT_NUMBER}}
                Exhibit Description: {{EXHIBIT_NAME}}

                I, {{AFFIANT_NAME}}, hereby declare under penalty of perjury that the following is true and correct:
                1. I am the {{AFFIANT_TITLE}} and am a custodian of records for the documents related to this matter.
                I have personal knowledge of the facts stated herein.
                2. The document attached hereto as Exhibit {{EXHIBIT_NUMBER}} is a true and correct copy of a record that was:
                (a) made at or near the time of the occurrence of the matters set forth by, or from information transmitted by, a person with knowledge of those matters;
                (b) kept in the course of a regularly conducted activity of the business; and
                (c) made by the regularly conducted activity as a regular practice.

                3. I am aware that this declaration is being provided in support of the authenticity of the aforementioned exhibit in the above-captioned legal proceeding.
                I declare under penalty of perjury under the laws of the United States of America that the foregoing is true and correct.

                Executed on: {{CURRENT_DATE}}



                ___________________________________

                {{AFFIANT_NAME}}
                {{AFFIANT_TITLE}}
                Table of Exhibits
                UNITED STATES DISTRICT COURT
                EASTERN DISTRICT OF LOUISIANA


                The following table:
                "{{PLAINTFFS}}",CIVIL ACTION v. NO: {{CASE_NUMBER}}
                "{{DEFENDANTS}}",SECTION: {{SECTION}} ,JUDGE: {{JUDGE}}

                ____________________________________________________
                EXHIBIT {{EXHIBIT_NUMBER}}
                ____________________________________________________

                PLAINTIFFS' TABLE OF EXHIBITS

                {{TABLE_OF_EXHIBITS}}
            """.trimIndent()

            val fileMetadata = com.google.api.services.drive.model.File()
            fileMetadata.name = "Lexorcist Master Template"
            fileMetadata.mimeType = "application/vnd.google-apps.document"
            val content = ByteArrayContent.fromString("text/plain", templateContent)
            drive.files().create(fileMetadata, content).setFields("id").execute()?.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun createSpreadsheet(title: String, caseInfo: Map<String, String>): String? = withContext(Dispatchers.IO) {
        try {
            val fileMetadata = com.google.api.services.drive.model.File()
            fileMetadata.name = title
            fileMetadata.mimeType = "application/vnd.google-apps.spreadsheet"
            drive.files().create(fileMetadata).setFields("id").execute()?.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun attachScript(spreadsheetId: String, masterTemplateId: String) = withContext(Dispatchers.IO) {
        try {
            val project = Project().setTitle("Case Tools Script").setParentId(spreadsheetId)
            val createdProject = script.projects().create(project as CreateProjectRequest?).execute()
            val scriptId = createdProject.scriptId ?: return@withContext

            val scriptContent = """
                function onOpen() {
                  SpreadsheetApp.getUi()
                      .createMenu('Case Tools')
                      .addItem('Generate Cover Sheet', 'generateCoverSheet')
                      .addItem('Generate Metadata', 'generateMetadata')
                      .addItem('Generate Chain of Custody', 'generateChainOfCustody')
                      .addItem('Generate Affidavit', 'generateAffidavit')
                      .addItem('Generate Table of Exhibits', 'generateTableOfExhibits')
                      .addToUi();
                }

                function getTemplate(templateName) {
                  var masterTemplateId = "$masterTemplateId";
                  var templateFile = DriveApp.getFileById(masterTemplateId);
                  var templateContent = templateFile.getBlob().getDataAsString();
                  var templateRegex = new RegExp("(?<=" + templateName + ")[\\s\\S]*?(?=\\n[A-Z][a-z_ ']+|\$)");
                  var match = templateContent.match(templateRegex);
                  return match ? match[0].trim() : null;
                }

                function generateDocument(templateName, documentTitle) {
                  var sheet = SpreadsheetApp.getActiveSheet();
                  var range = sheet.getActiveRange();
                  var row = range.getRow();
                  var headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0];
                  var data = sheet.getRange(row, 1, 1, sheet.getLastColumn()).getValues()[0];
                  var rowData = {};
                  for (var i = 0; i < headers.length; i++) {
                    rowData[headers[i]] = data[i];
                  }

                  var template = getTemplate(templateName);
                  if (!template) {
                    SpreadsheetApp.getUi().alert('Could not find the "' + templateName + '" template in the master document.');
                    return;
                  }

                  for (var key in rowData) {
                    var placeholder = new RegExp('{{' + key + '}}', 'g');
                    template = template.replace(placeholder, rowData[key]);
                  }

                  var newDoc = DocumentApp.create(documentTitle + ' - ' + (rowData['Exhibit Name'] || 'Exhibit ' + rowData['Exhibit No.']));
                  newDoc.getBody().setText(template);
                  var url = newDoc.getUrl();
                  SpreadsheetApp.getUi().alert('Document created: ' + newDoc.getName(), url, SpreadsheetApp.getUi().ButtonSet.OK);
                }

                function generateCoverSheet() { generateDocument('Cover Sheet', 'Cover Sheet'); }
                function generateMetadata() { generateDocument('Metadata', 'Metadata'); }
                function generateChainOfCustody() { generateDocument('Chain of Custody', 'Chain of Custody'); }
                function generateAffidavit() { generateDocument('Affidavit', 'Affidavit'); }
                function generateTableOfExhibits() { generateDocument('Table of Exhibits', 'Table of Exhibits'); }
            """.trimIndent()

            val codeFile = File().setName("Code").setType("SERVER_JS").setSource(scriptContent)
            val content = Content().setFiles(listOf(codeFile))
            script.projects().updateContent(scriptId, content).execute()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}