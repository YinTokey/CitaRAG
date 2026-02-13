import React, { useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { App as ObsidianApp } from 'obsidian';
import { Box, Typography, Paper, IconButton, CircularProgress } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import { Document } from '../types';

interface DocumentPreviewProps {
    document: Document | null;
    highlightText?: string;
    // initialPage is not supported by iframe viewer easily without specific params, ignoring for now
    initialPage?: number;
    onClose: () => void;
    isLoading?: boolean;
    app?: ObsidianApp;
}

const DocumentPreview: React.FC<DocumentPreviewProps> = ({ document, highlightText, onClose, isLoading, app }) => {
    const contentRef = useRef<HTMLDivElement>(null);

    // Scroll to highlight for text mode
    useEffect(() => {
        if (document && !isPdf(document) && highlightText && contentRef.current) {
            setTimeout(() => {
                const mark = contentRef.current?.querySelector('mark');
                if (mark) {
                    mark.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }
            }, 300);
        }
    }, [document, highlightText]);


    if (!document && !isLoading) return null;

    const isPdf = (doc: Document) => {
        return doc.filename.toLowerCase().endsWith('.pdf');
    };

    // Helper to highlight text (Legacy/Text mode)
    const renderContent = () => {
        if (isPdf(document!)) {
            // PDF RENDERER (Native)
            let fileUrl = `http://localhost:8080/api/documents/${document!.id}/content`;

            // If running in Obsidian and we have the app instance, try to load from local file system
            if (app) {
                // User requested: .obsidian/plugins/citarag/files/
                // We use app.vault.configDir to get '.obsidian' (or whatever it is renamed to)
                const configDir = app.vault.configDir;
                const localPath = `${configDir}/plugins/citarag/files/${document!.filename}`;

                // Obsidian's WebView needs a special resource path
                const resourcePath = app.vault.adapter.getResourcePath(localPath);
                if (resourcePath) {
                    fileUrl = resourcePath;
                }
            }

            return (
                <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', width: '100%' }}>
                    <iframe
                        src={fileUrl}
                        width="100%"
                        height="100%"
                        style={{ border: 'none', flexGrow: 1 }}
                        title={document!.title}
                    />
                </Box>
            );
        }

        // TEXT RENDERER
        if (!document?.content) return <Typography color="text.secondary">No content available for preview.</Typography>;
        if (!highlightText) return <Typography sx={{ whiteSpace: 'pre-wrap' }}>{document.content}</Typography>;

        const text = document.content;
        const search = highlightText.trim();
        if (!search) return <Typography sx={{ whiteSpace: 'pre-wrap' }}>{text}</Typography>;

        const parts = text.split(new RegExp(`(${escapeRegExp(search)})`, 'gi'));

        return (
            <Typography sx={{ whiteSpace: 'pre-wrap', lineHeight: 1.6 }}>
                {parts.map((part, i) =>
                    part.toLowerCase() === search.toLowerCase() ? (
                        <mark key={i} style={{ backgroundColor: '#fff3cd', color: 'inherit', padding: '0 2px', borderRadius: '2px' }}>
                            {part}
                        </mark>
                    ) : (
                        part
                    )
                )}
            </Typography>
        );
    };

    function escapeRegExp(string: string) {
        return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    }

    const modalContent = (
        <Paper
            elevation={3}
            sx={{
                position: 'fixed',
                top: '5%',
                left: '5%',
                right: '5%',
                bottom: '5%',
                zIndex: 9999,
                display: 'flex',
                flexDirection: 'column',
                bgcolor: 'var(--background-primary)',
                borderRadius: 2,
                overflow: 'hidden',
                border: '1px solid var(--divider-color)',
                boxShadow: '0 8px 32px rgba(0,0,0,0.5)'
            }}
        >
            {/* Header */}
            <Box sx={{
                p: 1.5,
                borderBottom: '1px solid var(--divider-color)',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                bgcolor: 'var(--background-secondary)'
            }}>
                <Box sx={{ overflow: 'hidden', mr: 2 }}>
                    <Typography variant="subtitle1" fontWeight="bold" noWrap>
                        {document?.title || "Document Preview"}
                    </Typography>
                    {document && (
                        <Typography variant="caption" color="text.secondary" noWrap display="block">
                            {document.author} • {document.publicationDate}
                        </Typography>
                    )}
                </Box>

                <IconButton onClick={onClose} size="small" sx={{ ml: 1 }}>
                    <CloseIcon />
                </IconButton>
            </Box>

            {/* Content */}
            <Box ref={contentRef} sx={{ flexGrow: 1, overflowY: 'auto', p: 0, bgcolor: '#f5f5f5', position: 'relative', display: 'flex', flexDirection: 'column' }}>
                {isLoading ? (
                    <Box sx={{ display: 'flex', justifyContent: 'center', mt: 10 }}>
                        <CircularProgress />
                    </Box>
                ) : (
                    <Box sx={{ p: isPdf(document!) ? 0 : 4, flexGrow: 1, display: 'flex', flexDirection: 'column' }}>
                        {renderContent()}
                    </Box>
                )}
            </Box>
        </Paper>
    );

    return createPortal(modalContent, window.document.body);
};

export default DocumentPreview;
