import { ItemView, WorkspaceLeaf } from 'obsidian';
import { Root, createRoot } from 'react-dom/client';
import React from 'react';
import App from './App';

export const VIEW_TYPE_CITARAG = 'citarag-view';

export class CitaRAGView extends ItemView {
    root: Root | null = null;

    constructor(leaf: WorkspaceLeaf) {
        super(leaf);
    }

    getViewType() {
        return VIEW_TYPE_CITARAG;
    }

    getDisplayText() {
        return 'CitaRAG Assistant';
    }

    async onOpen() {
        const container = this.containerEl.children[1];
        container.empty();
        container.setAttr("id", "citarag-root"); // Help with styling isolation if needed

        this.root = createRoot(container);
        this.root.render(
            <React.StrictMode>
                <React.StrictMode>
                    <App app={this.app} />
                </React.StrictMode>
            </React.StrictMode>
        );
    }

    async onClose() {
        if (this.root) {
            this.root.unmount();
        }
    }
}
