# CitaRAG
CitaRAG is a Retrieval-Augmented Generation (RAG) Writing Assistant designed primarily as an Obsidian plugin. It allows users to query their document library intelligently.

## Quick Start

To run the entire CitaRAG stack locally (including the Java Backend, PostgreSQL, and Milvus Vector Store), simply ensure Docker is installed and run:

```bash
# Build and start the backend services
docker compose up --build
```
*Note: You do **not** need to set the OpenAI API key as an environment variable. You can input it directly in the **Settings** (top right corner) of the Obsidian plugin once it is running.*


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
