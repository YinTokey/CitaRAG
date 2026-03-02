# CitaRAG Obsidian Plugin - Setup & Debug Guide

This guide explains how to install and debug the CitaRAG writing assistant plugin in your local Obsidian environment.

## 1. Prerequisites
- **Backend Service**: Ensure the CitaRAG Java backend is running either locally (usually at `http://localhost:8080`) or hosted remotely. The plugin connects to this backend.

## 2. Installation (End-Users)

1. Go to the [Releases page](https://github.com/superegg/CitaRAG/releases) of this repository.
2. Download the `citarag-obsidian.zip` file from the latest release.
3. Extract the contents into your Obsidian vault's plugins folder: `.obsidian/plugins/citarag-obsidian/`.
4. In Obsidian, go to `Settings` > `Community plugins`, click "Refresh," and toggle CitaRAG **ON**.

## 3. Usage
- Click the **Bot icon** in the left ribbon to open the CitaRAG Writing Assistant in the right sidebar.

## 4. Debugging

### Open Developer Tools
Since Obsidian is built on Electron, you can use the Chrome DevTools:
- Press `Cmd + Opt + I` (on macOS) or `Ctrl + Shift + I` (on Windows/Linux).
- Go to the **Console** tab to see logs, search results, and backend errors.

### Backend Connectivity
- If the chat fails or returns errors, check if the Java backend is active.
- Ensure the backend allows CORS from `app://obsidian.md` (the default origin for Obsidian).

## 5. Pro Setup: Hot Reloading (Recommended)

The "Hot Reload" plugin in Obsidian watches for changes in your plugin's folder. To make it work seamlessly with `npm run dev`:

### Method: Symbolic Link (Best Experience)
Instead of copying files every time, create a link so Obsidian sees your development folder directly.

1. **Delete** the `citarag-obsidian` folder you created in Step 2 if it's currently a normal folder.
2. Open your terminal and create a symbolic link:
   ```bash
   # Syntax: ln -s [Absolute Path to Frontend] [Path to Obsidian Plugins/citarag]

   ```
3. **Mark for Watch**: Create an empty file named `.hotreload` in the `frontend` directory. This tells the Hot Reload plugin to specifically watch this plugin.
   ```bash
   touch .hotreload
   ```
4. **Run Development Server**: In your `frontend` directory:
   ```bash
   npm run dev
   ```
5. **Result**: 
   - Every time you edit a React component and save, `main.js` is rebuilt.
   - The Obsidian **Hot Reload** plugin will see the `.hotreload` file, watch `main.js`, and perform an automatic reload.
   - You should see a small "Notice" in Obsidian saying the plugin has reloaded.

### Hot Reloading (Recommended)
To see changes without manual reloads:
1. Install the "Hot Reload" plugin from GitHub (search for "obsidian-hot-reload").
2. Run `npm run dev` in the `frontend` folder. This will watch for changes and rebuild `main.js` automatically.

### Backend Connectivity
- If the chat fails, check the Console for CORS errors. Ensure the Java backend allows requests from `app://obsidian.md`.
