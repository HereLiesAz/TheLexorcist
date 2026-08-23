# Privacy Policy

This policy applies to The Lexorcist ("the Application"), created by Hereliesaz
("the Service Provider") and offered free of charge, "AS IS".

The Application handles legal evidence. That may include material subject to
attorney–client privilege. This policy describes exactly what the Application
collects, where it is stored, and what leaves your device, so you can decide
what to put into it.

**Effective 2026-08-23.** This replaces an earlier version that stated the
Application "does not collect any personally identifiable information" and named
Google Drive as the only third-party service. Both statements were inaccurate;
the corrections are described below.

## What the Application collects

Everything below is collected only when you choose to capture or import it.

**Evidence you capture or import**

- Photographs and screenshots, and any text extracted from them.
- Audio and video recordings, and any transcript produced from them.
- Documents and free-text notes you enter.

**Data you import from your device**, each behind a separate permission prompt
you can decline:

- **SMS messages** — sender, recipient, body and timestamp, for contacts and
  date ranges you select.
- **Call logs** — number, direction, timestamp and duration.
- **Precise location** — a location fix attached to an evidence item, and
  location history you import from a Google Takeout file.
- **Photos, video and audio** from your device's storage.
- **Camera** capture.

**Mail you import**, when you connect an account: message headers, bodies and
attachments from Gmail, Outlook, or any IMAP mailbox you configure.

**Case information you enter**: parties, court, case number, jurisdiction,
allegations, exhibits, tags and commentary.

SMS messages, call logs, precise location and mail are personally identifiable
information, and they concern other people as well as you. Importing them is
your decision and your responsibility.

## Where it is stored

All case data is stored **on your device**, in a database file named
`lexorcist_data.xlsx` in the Application's private storage, together with the
original evidence files.

The database is **encrypted at rest** using a key held in your device's hardware
keystore. Original evidence files (photographs, audio, video) are stored in the
Application's private directory but are **not yet individually encrypted**;
they are protected by Android's app sandbox, which prevents other applications
from reading them, and by device encryption.

Case data is **excluded from Android automatic backup** and from device-to-device
transfer. It is not copied to your Google account backup.

## What leaves your device

Nothing leaves your device unless you enable it.

**Cloud sync (optional).** If you enable it, the database and your case folders
are uploaded to a provider you choose and sign in to — Google Drive or Dropbox.
The Service Provider has no access to that storage.

**Google account access.** Signing in with Google grants the Application:

- Full access to your Google Drive (read, write and delete for **all** files in
  that account, not only files the Application created).
- Full access to your Google Sheets.
- Read-only access to your Gmail.

If you sign in with an account that holds other clients' material, be aware that
the Application holds a credential broad enough to reach it. Narrowing this to
app-created files only is a known outstanding item.

**Cloud AI (optional, script-driven).** The Application's scripting engine
exposes a function, `lex.ai.generate`, that sends text to Google's Gemini API.
A script you write or install can pass evidence text to it. If you run such a
script, that evidence content is transmitted to Google. No script does this
unless it is written to.

**Google Apps Script (optional, script-driven).** A script can invoke
`lex.google.runAppsScript`, which calls the Google Apps Script API using your
signed-in credential.

**Shared scripts and templates.** The Application can download scripts and
templates that other users have published to a shared, public spreadsheet, and
can publish yours to it. Published items carry the name and email address you
supply. **Shared scripts are not reviewed, signed or sandboxed against network
access** — a script obtained this way runs with the two capabilities above.
Treat installing one as you would running any untrusted code against your case
files.

On-device processing — OCR, speech-to-text and text similarity — runs locally
and sends nothing anywhere.

## Third-party services

Depending on the features you use:

- [Google (Drive, Sheets, Gmail, Apps Script, Firebase, Gemini)](https://www.google.com/policies/privacy/)
- [Dropbox](https://www.dropbox.com/privacy)
- [Microsoft (OneDrive, Outlook)](https://privacy.microsoft.com/privacystatement)

The Service Provider operates no server and receives no data from the
Application.

## Security

The database is encrypted at rest with a hardware-backed key. OAuth credentials
are encrypted separately. Automatic backup is disabled.

No method of storage or transmission is completely secure, and the Service
Provider cannot guarantee absolute security. In particular:

- Original evidence files are not individually encrypted.
- A device that is rooted, compromised, or unlocked in someone else's hands
  offers no protection.
- Anything you sync to a cloud provider is subject to that provider's security
  and to the credentials on that account.

## Your control

- Every import is behind a permission you can refuse.
- Cloud sync is off unless you turn it on.
- Deleting a case removes its data from your device. Copies already synced to a
  cloud provider must be deleted there.
- Uninstalling removes all local case data. There is no backup, by design.

## Changes to this policy

The Service Provider may update this policy. Material changes will be posted
here, with the effective date above updated.

## Contact

Questions or corrections: raise an issue on the project repository.
