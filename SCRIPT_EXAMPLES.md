# The Lexorcist: Scripting Examples

This document provides a set of curated, functional scripts designed to showcase the powerful scripting capabilities of The Lexorcist. You can use these examples in the app's Script Builder to see the features in action or as a starting point for your own custom scripts.

The scripting engine uses Javascript (Mozilla Rhino) and interacts with the application through a global `Lexorcist` object.

- `Lexorcist.ui` is used to control user interface elements, like the navigation menu.
- `Lexorcist.ai` is used to access the generative AI features.

---

## Script 1: A Simple Dynamic Screen

This is a "hello world" example for the Scriptable Screens feature. It demonstrates the absolute basics:
1.  It defines a simple UI layout in a JSON string.
2.  It uses `Lexorcist.ui.upsertMenuItem()` to add a new item to the left-hand navigation rail.
3.  It links the menu item to the JSON screen definition. When you click the "Simple Screen" item, the app will dynamically build and display the screen.

````javascript
/****************************************************************
 * Simple Screen Tutorial
 *
 * This script adds a new item to the navigation menu called
 * "Simple Screen". Clicking it opens a basic screen that is
 * defined entirely by the JSON string below.
 ****************************************************************/

// 1. Define the UI for our screen in a JSON string.
// This structure is parsed by the app to build a native UI.
const simpleScreenJson = `{
  "title": "My First Scripted Screen",
  "components": [
    {
      "type": "text",
      "content": "Hello from Javascript!",
      "style": "headline"
    },
    {
      "type": "text",
      "content": "This entire screen was defined in a JSON string and rendered dynamically by the app. You can create custom user interfaces for your workflows."
    }
  ]
}`;

// 2. Create the menu item that will open our screen.
Lexorcist.ui.upsertMenuItem(
  "simple_tutorial_menu", // A unique ID for this menu item
  "Simple Screen",        // The text label shown in the navigation rail
  true,                   // true = visible, false = hidden
  null,                   // The name of a JS function to call on click (we aren't using this here)
  simpleScreenJson        // The JSON screen definition to load on click
);
````

---

## Script 2: Comprehensive In-App Tutorial with AI

This script is a more advanced example that creates a user-friendly, in-app tutorial. It demonstrates:
1.  A more complex, scrollable screen layout with various text styles and buttons.
2.  How to link a button's `onClickFunction` to a Javascript function within the script.
3.  How to make an asynchronous call to the Generative AI using `Lexorcist.ai.generate()`.
4.  How to handle the AI's response in a callback function and use it to modify the UI.

````javascript
/****************************************************************
 * In-App Tutorial with AI Demonstration
 *
 * This script adds an "App Tutorial" item to the menu.
 * The tutorial screen explains the app's features and includes
 * a button to demonstrate the AI summarization capability.
 ****************************************************************/

// This is the text content of the tutorial. We define it here so we can
// easily pass it to the AI for summarization.
const tutorialText = `
  The Lexorcist is designed to help you securely collect, manage, and prepare
  digital evidence for legal proceedings.
  1. Cases: Use the 'Cases' menu to create or select a case. All your evidence
     and notes will be organized under the currently selected case.
  2. Evidence: Add evidence like photos, videos, or audio recordings from the
     'Evidence' screen. The app can automatically extract text from images and audio.
  3. Scripting: The 'Script' menu allows you to write Javascript code to automate
     tasks. You can automatically tag evidence based on keywords, or even add new
     UI elements to the app, like this tutorial screen itself!
`;

// This function is the callback for our AI request.
// It will be executed when the AI model returns its summary.
function showAiSummary(summary) {
  // The summary from the AI is passed as an argument.
  // We will display this summary by creating a NEW, temporary menu item.
  // This demonstrates how async operations can feed back into the UI.
  Lexorcist.ui.upsertMenuItem(
    "tutorial_summary_result",
    "AI: " + summary.substring(0, 25) + "...", // Show a snippet of the summary
    true,
    null,
    null // This menu item doesn't open a screen
  );
}

// This function is called when the button on our tutorial screen is clicked.
function summarizeTutorial() {
  // We ask the AI to summarize the tutorial's text in one short sentence.
  // We provide the text and the name of the function to call when done.
  Lexorcist.ai.generate(
    "Please summarize the following text in one short sentence: " + tutorialText,
    showAiSummary // Pass the callback function itself
  );
}

// Here we define the JSON for the tutorial screen.
const tutorialScreenJson = `{
  "title": "App Tutorial",
  "components": [
    { "type": "text", "content": "Welcome to The Lexorcist!", "style": "headline" },
    { "type": "text", "content": "This application is designed to help you securely collect, manage, and prepare digital evidence for legal proceedings." },
    { "type": "spacer" },
    { "type": "text", "content": "1. Cases", "style": "title" },
    { "type": "text", "content": "Use the 'Cases' menu to create or select a case. All your evidence and notes will be organized under the currently selected case." },
    { "type":"spacer" },
    { "type": "text", "content": "2. Evidence", "style": "title" },
    { "type": "text", "content": "Add evidence like photos, videos, or audio recordings from the 'Evidence' screen. The app can automatically extract text from images and audio." },
    { "type": "spacer" },
    { "type": "text", "content": "3. Scripting", "style": "title" },
    { "type": "text", "content": "The 'Script' menu allows you to write Javascript code to automate tasks. You can automatically tag evidence based on keywords, or even add new UI elements to the app, like this tutorial screen itself!" },
    { "type": "spacer" },
    { "type": "text", "content": "Demonstrate AI", "style": "title" },
    { "type": "text", "content": "Click the button below to use the integrated AI to summarize the text on this page. The summary will appear as a new item in the navigation menu." },
    {
      "type": "button",
      "label": "Summarize with AI",
      "onClickFunction": "summarizeTutorial"
    }
  ]
}`;

// Finally, create the main menu item that opens the tutorial screen.
Lexorcist.ui.upsertMenuItem(
  "app_tutorial_menu",    // Unique ID
  "App Tutorial",         // Label
  true,                   // isVisible
  null,                   // onClickFunction
  tutorialScreenJson      // screenJson
);
````
