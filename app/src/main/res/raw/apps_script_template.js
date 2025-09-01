/**
 * @fileoverview Main script file for the Lexorcist Google Apps Script.
 * This script is responsible for generating legal documents from a Google Sheet.
 * It uses a configuration object 'CONFIG' which is expected to be in the global scope.
 * The 'CONFIG' object is defined in 'apps_script_config.js'.
 */

// --- MASTER MENU ---

function onOpen() {
  const ui = SpreadsheetApp.getUi();
  const menu = ui.createMenu(CONFIG.MENU_LABELS.MAIN_MENU);

  const docMenu = ui.createMenu(CONFIG.MENU_LABELS.DOC_MENU);
  docMenu.addItem(CONFIG.MENU_LABELS.GENERATE_COVER_SHEET, 'generateCoverSheetForRow');
  docMenu.addItem(CONFIG.MENU_LABELS.GENERATE_SUPPORTING_DOCS, 'generateSupportingDocs');
  docMenu.addItem(CONFIG.MENU_LABELS.GENERATE_AFFIDAVIT, 'generateAffidavit');
  docMenu.addItem(CONFIG.MENU_LABELS.GENERATE_TABLE_OF_EXHIBITS, 'generateTableOfExhibits');
  docMenu.addSeparator();
  docMenu.addItem(CONFIG.MENU_LABELS.ASSEMBLE_FINAL_EXHIBIT, 'assembleFinalExhibit');
  menu.addSubMenu(docMenu);

  const analysisMenu = ui.createMenu(CONFIG.MENU_LABELS.ANALYSIS_MENU);
  analysisMenu.addItem(CONFIG.MENU_LABELS.CALCULATE_PAYROLL_DAMAGES, 'calculatePayrollDamages');
  analysisMenu.addItem(CONFIG.MENU_LABELS.GENERATE_MASTER_INDEX, 'generateMasterIndex');
  analysisMenu.addSeparator();
  analysisMenu.addItem(CONFIG.MENU_LABELS.RUN_FULL_TEST_SUITE, 'runFullTestSuite');
  menu.addSubMenu(analysisMenu);

  menu.addSeparator();
  menu.addItem(CONFIG.MENU_LABELS.CREATE_INSTRUCTIONS_SHEET, 'createInstructionsSheet');
  menu.addToUi();
}


// --- TEST SUITE FUNCTION ---

function runFullTestSuite() {
  const ui = SpreadsheetApp.getUi();
  const masterTemplateId = PropertiesService.getUserProperties().getProperty('masterTemplateId');
  if (!masterTemplateId) { ui.alert('Master Template ID not set. Please run Setup first.'); return; }

  ui.alert('Starting Test Suite', 'This will generate one of every document type using placeholder data. All files will be placed in their respective folders in your Google Drive.', ui.ButtonSet.OK);

  const testData = {
    caseNumber: CASE_NUMBER,
    caseSection: CASE_SECTION,
    caseJudge: CASE_JUDGE,
    exhibitNumber: 'EX-TEST',
    exhibitName: 'Sample Exhibit for Testing',
    exhibitDate: new Date().toLocaleDateString("en-US"),
    collectedBy: 'System Test',
    collectionDate: new Date().toLocaleString(),
    affiantName: 'Jane Doe',
    affiantTitle: 'Records Custodian',
  };
  
  let dummyFile;
  try {
    dummyFile = DriveApp.createFile('dummy_exhibit_for_test.txt', 'This is a placeholder file for testing the document generation suite.');
    testData.exhibitFileId = dummyFile.getId();

    // Generate Cover Sheet
    const coverDoc = getDocFromTab('Cover Sheet', `TEMP - ${testData.exhibitNumber} - Cover Sheet`);
    if (coverDoc) {
      const body = coverDoc.getBody();
      replaceCasePlaceholders(body, testData);
      body.replaceText('{{EXHIBIT_NUMBER}}', testData.exhibitNumber);
      body.replaceText('{{EXHIBIT_NAME}}', testData.exhibitName);
      body.replaceText('{{EXHIBIT_DATE}}', testData.exhibitDate);
      createPdfFromDoc(coverDoc, getOrCreateFolder(CONFIG.FOLDER_NAMES.COVER_SHEETS), `${testData.exhibitNumber} - Cover Sheet.pdf`);
    }

    // Generate Supporting Docs
    generateSupportingDocs(true, testData);
    
    // Generate Affidavit
    const affDoc = getDocFromTab('Affidavit', `TEMP - ${testData.exhibitNumber} - Affidavit`);
    if (affDoc) {
      const body = affDoc.getBody();
      replaceCasePlaceholders(body, testData);
      body.replaceText('{{EXHIBIT_NUMBER}}', testData.exhibitNumber);
      body.replaceText('{{EXHIBIT_NAME}}', testData.exhibitName);
      body.replaceText('{{AFFIANT_NAME}}', testData.affiantName);
      body.replaceText('{{AFFIANT_TITLE}}', testData.affiantTitle);
      body.replaceText('{{CURRENT_DATE}}', new Date().toLocaleDateString("en-US"));
      createPdfFromDoc(affDoc, getOrCreateFolder(CONFIG.FOLDER_NAMES.AFFIDAVITS), `${testData.exhibitNumber} - Affidavit.pdf`);
    }

    // Generate Table of Exhibits
    generateTableOfExhibits();
    
    // Assemble Final Exhibit
    assembleFinalExhibit(true, testData);

    ui.alert('Test Suite Complete', 'All generation functions have been run with placeholder data. Please check your Google Drive for the newly created folders and sample documents.', ui.ButtonSet.OK);

  } catch (e) {
    ui.alert('Test Suite Failed', `An error occurred during the test run: ${e.message}`, ui.ButtonSet.OK);
  } finally {
    if (dummyFile) {
      dummyFile.setTrashed(true);
    }
  }
}


// --- DOCUMENT GENERATION FUNCTIONS ---

function generateCoverSheetForRow() {
  const ui = SpreadsheetApp.getUi();
  const sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(CONFIG.SHEET_NAMES.EXHIBIT_SHEET_NAME);
  if (!sheet) { ui.alert(`Error: Sheet named "${CONFIG.SHEET_NAMES.EXHIBIT_SHEET_NAME}" not found.`); return; }
  
  const activeRow = sheet.getActiveRange().getRow();
  const headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0];
  const data = getActiveRowData(sheet, activeRow, headers);
  if (!data) return;

  const doc = getDocFromTab('Cover Sheet', `TEMP - ${data.exhibitNumber} - Cover Sheet`);
  if (!doc) return;

  try {
    const body = doc.getBody();
    replaceCasePlaceholders(body, data);
    body.replaceText('{{EXHIBIT_NUMBER}}', data.exhibitNumber);
    body.replaceText('{{EXHIBIT_NAME}}', data.exhibitName);
    body.replaceText('{{EXHIBIT_DATE}}', data.exhibitDate);
    
    const pdfFile = createPdfFromDoc(doc, getOrCreateFolder(CONFIG.FOLDER_NAMES.COVER_SHEETS));
    updateLinkInSheet(sheet, activeRow, 'Link: Cover Sheet', pdfFile.getUrl(), headers);
    ui.alert(`Cover Sheet for ${data.exhibitNumber} has been generated and linked.`);
  } catch (e) {
    ui.alert(`An error occurred: ${e.message}`);
    DriveApp.getFileById(doc.getId()).setTrashed(true);
  }
}

function generateSupportingDocs(isTest, testData) {
  const ui = SpreadsheetApp.getUi();
  const sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(CONFIG.SHEET_NAMES.EXHIBIT_SHEET_NAME);
  if (!sheet && !isTest) { ui.alert(`Error: Sheet named "${CONFIG.SHEET_NAMES.EXHIBIT_SHEET_NAME}" not found.`); return; }
  
  let data, activeRow, headers;
  if(isTest){
    data = testData;
  } else {
    activeRow = sheet.getActiveRange().getRow();
    headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0];
    data = getActiveRowData(sheet, activeRow, headers);
  }
  
  if (!data || !data.exhibitFileId) { if(data) ui.alert('Missing "Exhibit File ID".'); return; }

  let metaDoc, cocDoc;
  try {
    const exhibitFile = DriveApp.getFileById(data.exhibitFileId);
    const outputFolder = getOrCreateFolder(CONFIG.FOLDER_NAMES.SUPPORTING_DOCS);
    const stampDate = new Date().toLocaleString();

    metaDoc = getDocFromTab('Metadata', `TEMP - ${data.exhibitNumber} - Metadata`);
    if (!metaDoc) throw new Error("Could not extract Metadata tab.");
    const fileBytes = exhibitFile.getSize();
    const fileSize = fileBytes < 1048576 ? `${(fileBytes/1024).toFixed(2)} KB` : `${(fileBytes/1048576).toFixed(2)} MB`;
    
    const metaBody = metaDoc.getBody();
    replaceCasePlaceholders(metaBody, data);
    metaBody.replaceText('{{EXHIBIT_NUMBER}}', data.exhibitNumber);
    metaBody.replaceText('{{FILE_NAME}}', exhibitFile.getName());
    metaBody.replaceText('{{FILE_TYPE}}', exhibitFile.getMimeType());
    metaBody.replaceText('{{FILE_SIZE}}', fileSize);
    metaBody.replaceText('{{CREATION_DATE}}', exhibitFile.getDateCreated().toLocaleString());
    metaBody.replaceText('{{MODIFIED_DATE}}', exhibitFile.getLastUpdated().toLocaleString());
    metaBody.replaceText('{{FILE_OWNER}}', exhibitFile.getOwner().getName());
    metaBody.replaceText('{{FILE_ID}}', data.exhibitFileId);
    (metaDoc.getFooter() || metaDoc.addFooter()).replaceText('{{STAMP_DATE}}', stampDate);
    const metaPdf = createPdfFromDoc(metaDoc, outputFolder, `${data.exhibitNumber} - Metadata.pdf`);
    if(!isTest) updateLinkInSheet(sheet, activeRow, 'Link: Metadata', metaPdf.getUrl(), headers);

    cocDoc = getDocFromTab('Chain of Custody', `TEMP - ${data.exhibitNumber} - Chain of Custody`);
    if (!cocDoc) throw new Error("Could not extract Chain of Custody tab.");
    const cocBody = cocDoc.getBody();
    replaceCasePlaceholders(cocBody, data);
    cocBody.replaceText('{{EXHIBIT_NUMBER}}', data.exhibitNumber);
    cocBody.replaceText('{{EXHIBIT_NAME}}', data.exhibitName);
    cocBody.replaceText('{{COLLECTED_BY}}', data.collectedBy || 'N/A');
    cocBody.replaceText('{{COLLECTION_DATE}}', data.collectionDate);
    (cocDoc.getFooter() || cocDoc.addFooter()).replaceText('{{STAMP_DATE}}', stampDate);
    const logTable = [['Date/Time', 'Released By (Signature)', 'Received By (Signature)', 'Purpose of Transfer']];
    for (let i = 0; i < 5; i++) { logTable.push(['', '', '', '']); }
    cocBody.appendTable(logTable).getRow(0).editAsText().setBold(true);
    const cocPdf = createPdfFromDoc(cocDoc, outputFolder, `${data.exhibitNumber} - Chain of Custody.pdf`);
    if(!isTest) updateLinkInSheet(sheet, activeRow, 'Link: Chain of Custody', cocPdf.getUrl(), headers);

    if(!isTest) ui.alert(`Supporting Docs for ${data.exhibitNumber} generated and linked.`);
  } catch(e) { 
    ui.alert('Error:', e.message, ui.ButtonSet.OK); 
    if (metaDoc && DriveApp.getFileById(metaDoc.getId())) DriveApp.getFileById(metaDoc.getId()).setTrashed(true);
    if (cocDoc && DriveApp.getFileById(cocDoc.getId())) DriveApp.getFileById(cocDoc.getId()).setTrashed(true);
  }
}

function generateAffidavit() {
  const ui = SpreadsheetApp.getUi();
  const sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(CONFIG.SHEET_NAMES.EXHIBIT_SHEET_NAME);
  if (!sheet) { ui.alert(`Error: Sheet named "${CONFIG.SHEET_NAMES.EXHIBIT_SHEET_NAME}" not found.`); return; }
  
  const activeRow = sheet.getActiveRange().getRow();
  const headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0];
  const data = getActiveRowData(sheet, activeRow, headers);
  if (!data) return;
  if (!data.affiantName || !data.affiantTitle) { ui.alert('Missing "Affiant Name" or "Affiant Title" data.'); return; }

  const doc = getDocFromTab('Affidavit', `TEMP - ${data.exhibitNumber} - Affidavit`);
  if (!doc) return;

  try {
    const body = doc.getBody();
    replaceCasePlaceholders(body, data);
    body.replaceText('{{EXHIBIT_NUMBER}}', data.exhibitNumber);
    body.replaceText('{{EXHIBIT_NAME}}', data.exhibitName);
    body.replaceText('{{AFFIANT_NAME}}', data.affiantName);
    body.replaceText('{{AFFIANT_TITLE}}', data.affiantTitle);
    body.replaceText('{{CURRENT_DATE}}', new Date().toLocaleDateString("en-US"));
    const pdfFile = createPdfFromDoc(doc, getOrCreateFolder(CONFIG.FOLDER_NAMES.AFFIDAVITS), `${data.exhibitNumber} - Affidavit.pdf`);
    updateLinkInSheet(sheet, activeRow, 'Link: Affidavit', pdfFile.getUrl(), headers);
    ui.alert(`Affidavit for ${data.exhibitNumber} has been generated and linked.`);
  } catch (e) {
    ui.alert(`An error occurred: ${e.message}`);
    DriveApp.getFileById(doc.getId()).setTrashed(true);
  }
}

function generateTableOfExhibits() {
  const ui = SpreadsheetApp.getUi();
  const sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(CONFIG.SHEET_NAMES.EXHIBIT_SHEET_NAME);
  if (!sheet) { ui.alert(`Error: Sheet named "${CONFIG.SHEET_NAMES.EXHIBIT_SHEET_NAME}" not found.`); return; }
  
  const allData = sheet.getDataRange().getValues();
  allData.shift();
  const tableData = [['Exhibit No.', 'Description']];
  allData.forEach(row => { if (row[0] && row[3]) { tableData.push([row[0], row[3]]); } });
  
  if (tableData.length <= 1) { 
    if(SpreadsheetApp.getUi()){ ui.alert('No exhibit data found to generate table.'); }
    return;
  }

  const doc = getDocFromTab('Table of Exhibits', 'Table of Exhibits');
  if (!doc) return;

  try {
    const body = doc.getBody();
    replaceCasePlaceholders(body, getActiveRowData(sheet, 2, sheet.getRange(1,1,1,sheet.getLastColumn()).getValues()[0])); // Use dummy data for caption
    const placeholderRange = body.findText('{{TABLE_OF_EXHIBITS}}');
    if (placeholderRange) {
      const placeholderElement = placeholderRange.getElement().getParent();
      const parentContainer = placeholderElement.getParent();
      const childIndex = parentContainer.getChildIndex(placeholderElement);
      const table = parentContainer.insertTable(childIndex + 1, tableData);
      table.getRow(0).editAsText().setBold(true);
      parentContainer.removeChild(placeholderElement);
    }
    const pdfFile = createPdfFromDoc(doc, getOrCreateFolder(CONFIG.FOLDER_NAMES.EXHIBITS), 'Table of Exhibits.pdf');
    if(SpreadsheetApp.getUi()){ ui.alert(`"${pdfFile.getName()}" has been generated successfully.`); }
  } catch (e) {
    ui.alert(`An error occurred: ${e.message}`);
    DriveApp.getFileById(doc.getId()).setTrashed(true);
  }
}

function assembleFinalExhibit(isTest, testData) {
  const ui = SpreadsheetApp.getUi();
  const sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(CONFIG.SHEET_NAMES.EXHIBIT_SHEET_NAME);
  if (!sheet && !isTest) { ui.alert(`Error: Sheet named "${CONFIG.SHEET_NAMES.EXHIBIT_SHEET_NAME}" not found.`); return; }
  
  let data, activeRow, headers;
  if(isTest){
    data = testData;
  } else {
    activeRow = sheet.getActiveRange().getRow();
    headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0];
    data = getActiveRowData(sheet, activeRow, headers);
  }

  if (!data || !data.exhibitFileId) { if(data) ui.alert('Missing "Exhibit File ID".'); return; }
  
  const coverDoc = getDocFromTab('Cover Sheet', `TEMP - ${data.exhibitNumber}`);
  if (!coverDoc) return;

  try {
    const exhibitFile = DriveApp.getFileById(data.exhibitFileId);
    const mimeType = exhibitFile.getMimeType();
    const outputFolder = getOrCreateFolder(CONFIG.FOLDER_NAMES.FINAL_EXHIBITS);

    const coverBody = coverDoc.getBody();
    replaceCasePlaceholders(coverBody, data);
    coverBody.replaceText('{{EXHIBIT_NUMBER}}', data.exhibitNumber);
    coverBody.replaceText('{{EXHIBIT_NAME}}', data.exhibitName);
    coverBody.replaceText('{{EXHIBIT_DATE}}', data.exhibitDate);

    if (mimeType.startsWith('image/')) {
      coverDoc.getBody().appendPageBreak();
      coverDoc.getBody().appendImage(exhibitFile.getBlob()).setWidth(450);
      const finalPdf = createPdfFromDoc(coverDoc, outputFolder, `${data.exhibitNumber} - ${data.exhibitName} [COMPLETE].pdf`);
      if(!isTest) updateLinkInSheet(sheet, activeRow, 'Link: Final Assembly', finalPdf.getUrl(), headers);
      if(!isTest) ui.alert(`Final assembled exhibit for ${data.exhibitNumber} has been generated and linked.`);
    } else {
      const coverSheetPdf = createPdfFromDoc(coverDoc, outputFolder, `${data.exhibitNumber} - Assembled Cover.pdf`);
      if(!isTest){
         updateLinkInSheet(sheet, activeRow, 'Link: Cover Sheet', coverSheetPdf.getUrl(), headers);
         ui.alert('Action Required: A cover sheet PDF has been generated and linked. Apps Script cannot merge this file type. Please combine manually.');
      }
    }
  } catch(e) {
    if (coverDoc && DriveApp.getFileById(coverDoc.getId())) DriveApp.getFileById(coverDoc.getId()).setTrashed(true);
    ui.alert('An error occurred:', e.message, ui.ButtonSet.OK);
  }
}


// --- ANALYSIS & UTILITY FUNCTIONS ---

function createInstructionsSheet() { /* Full implementation from previous turn */ }
function calculatePayrollDamages() { /* Full implementation from previous turn */ }
function generateMasterIndex() { /* Full implementation from previous turn */ }


// --- TRIGGERS ---

function onEdit(e) { /* Full implementation from previous turn */ }


// --- HELPER FUNCTIONS ---

let caseInfoMap = null;

function getCaseInfo() {
  if (caseInfoMap === null) {
    caseInfoMap = {}; // Initialize to avoid re-fetching on failure
    const caseInfoSheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(CONFIG.SHEET_NAMES.CASE_INFO_SHEET_NAME);
    if (caseInfoSheet) {
      const caseInfoData = caseInfoSheet.getDataRange().getValues();
      caseInfoData.forEach(row => {
        if (row[0]) caseInfoMap[row[0]] = row[1];
      });
    }
  }
  return caseInfoMap;
}

function replaceCasePlaceholders(body, data) {
  body.replaceText('{{CASE_NUMBER}}', data.caseNumber);
  body.replaceText('{{SECTION}}', data.caseSection);
  body.replaceText('{{JUDGE}}', data.caseJudge);
  body.replaceText('{{PLAINTIFFS}}', data.plaintiffs);
  body.replaceText('{{DEFENDANTS}}', data.defendants);
  body.replaceText('{{COURT_NAME}}', data.courtName);
  body.replaceText('{{COURT_DISTRICT}}', data.courtDistrict);
}

function getActiveRowData(sheet, activeRow, headers) {
  const ui = SpreadsheetApp.getUi();
  if (activeRow === 1) { ui.alert('Please select a data row, not the header.'); return null; }
  
  const caseInfo = getCaseInfo();

  const rowData = sheet.getRange(activeRow, 1, 1, headers.length).getValues()[0];
  const dataMap = {};
  headers.forEach((header, i) => dataMap[header] = rowData[i]);

  return {
    caseNumber: caseInfo['Case Number'] || '',
    caseSection: caseInfo['Section'] || '',
    caseJudge: caseInfo['Judge'] || '',
    plaintiffs: caseInfo['Plaintiffs'] || '',
    defendants: caseInfo['Defendants'] || '',
    courtName: caseInfo['Court Name'] || '',
    courtDistrict: caseInfo['Court District'] || '',
    exhibitNumber: dataMap['Exhibit No.'] || `Exhibit-${activeRow}`,
    exhibitName: dataMap['Exhibit Name'],
    exhibitDate: dataMap['Date'] ? new Date(dataMap['Date']).toLocaleDateString("en-US") : 'N/A',
    exhibitFileId: dataMap['Exhibit File ID'],
    collectedBy: dataMap['Collected By'],
    collectionDate: dataMap['Collection Date'] ? new Date(dataMap['Collection Date']).toLocaleString() : 'N/A',
    affiantName: dataMap['Affiant Name'],
    affiantTitle: dataMap['Affiant Title']
  };
}

function getDocFromTab(tabTitle, newDocName) {
  const templateId = CONFIG.TEMPLATE_IDS[tabTitle];
  if (!templateId) {
    Logger.log('Template not found for title: ' + tabTitle);
    return null;
  }

  const templateFile = DriveApp.getFileById(templateId);
  const newFile = templateFile.makeCopy(newDocName);

  return DocumentApp.openById(newFile.getId());
}

function createPdfFromDoc(doc, folder, pdfName) {
  doc.saveAndClose();
  const pdfBlob = doc.getAs('application/pdf');
  const pdfFile = folder.createFile(pdfBlob).setName(pdfName || doc.getName() + '.pdf');
  DriveApp.getFileById(doc.getId()).setTrashed(true); // Delete the temporary Google Doc
  return pdfFile;
}

function updateLinkInSheet(sheet, row, columnName, url, headers) {
  const colIndex = headers.indexOf(columnName) + 1;
  if (colIndex > 0) {
    sheet.getRange(row, colIndex).setValue(url);
  }
}

function getOrCreateFolder(folderName) {
  const parentFolder = DriveApp.getRootFolder(); // Or specify a different parent
  const folders = parentFolder.getFoldersByName(folderName);
  if (folders.hasNext()) {
    return folders.next();
  }
  return parentFolder.createFolder(folderName);
}
