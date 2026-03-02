# CitaRAG
CitaRAG is a Retrieval-Augmented Generation (RAG) Writing Assistant designed primarily as an Obsidian plugin. It allows users to query their document library intelligently.

## Quick Start

To run the entire CitaRAG stack locally (including the Java Backend, PostgreSQL, and Milvus Vector Store), follow these steps:

1. **Set your OpenAI API Key**: Create a file named `.env` in the backend directory and add your key:
   ```env
   OPENAI_API_KEY=sk-your-openai-api-key-here
   ```

2. **Start the stack**:
   ```bash
   docker compose up --build
   ```
*Note: The backend will use this key for both document embedding and chat generation.*


## Installing the Obsidian Plugin

Once the backend is spinning, you can install the Obsidian frontend GUI.

### Standard Users
We recommend installing the plugin into Obsidian directly:
1. Navigate to the [Releases](https://github.com/superegg/CitaRAG/releases) page of this repository.
2. Download the `citarag-obsidian.zip` and extract it into your `.obsidian/plugins/citarag-obsidian/` directory.
3. Refresh your Community Plugins in Obsidian and enable **CitaRAG**.

### Local Development
If you wish to modify the React frontend:
```bash
cd frontend
npm install
npm run dev
```
More detailed instructions on Hot Reloading can be found in [doc/obsidian.md](doc/obsidian.md).
