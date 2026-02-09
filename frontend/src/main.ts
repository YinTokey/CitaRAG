import { App, Plugin, PluginSettingTab, Setting, WorkspaceLeaf, ItemView } from 'obsidian';
import { CitaRAGView, VIEW_TYPE_CITARAG } from './view';

export default class CitaRAGPlugin extends Plugin {
    async onload() {
        this.registerView(
            VIEW_TYPE_CITARAG,
            (leaf) => new CitaRAGView(leaf)
        );

        this.addRibbonIcon('bot', 'CitaRAG Assistant', () => {
            this.activateView();
        });

        this.addCommand({
            id: 'open-citarag-view',
            name: 'Open CitaRAG Assistant',
            callback: () => {
                this.activateView();
            }
        });
    }

    async activateView() {
        const { workspace } = this.app;

        let leaf: WorkspaceLeaf | null = null;
        const leaves = workspace.getLeavesOfType(VIEW_TYPE_CITARAG);

        if (leaves.length > 0) {
            // A leaf with our view already exists, use that
            leaf = leaves[0];
        } else {
            // Our view could not be found in the workspace, create a new leaf
            // in the right sidebar for it
            leaf = workspace.getRightLeaf(false);
            if (leaf) {
                await leaf.setViewState({ type: VIEW_TYPE_CITARAG, active: true });
            }
        }

        // "Reveal" the leaf in case it is in a collapsed sidebar
        if (leaf) {
            workspace.revealLeaf(leaf);
        }
    }
}
