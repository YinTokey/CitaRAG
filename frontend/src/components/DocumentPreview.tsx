import React, { useEffect, useRef } from 'react';
import { Box, Typography, Paper, IconButton, CircularProgress } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import { Document } from '../types';

interface DocumentPreviewProps {
    document: Document | null;
    highlightText?: string;
    onClose: () => void;
    isLoading?: boolean;
}

const DocumentPreview: React.FC<DocumentPreviewProps> = ({ document, highlightText, onClose, isLoading }) => {
    const contentRef = useRef<HTMLDivElement>(null);

    // Scroll to highlight
    useEffect(() => {
        if (document && highlightText && contentRef.current) {
            // Simple timeout to allow rendering
            setTimeout(() => {
                const mark = contentRef.current?.querySelector('mark');
                if (mark) {
                    mark.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }
            }, 300);
        }
    }, [document, highlightText]);

    if (!document && !isLoading) return null;

    // Helper to highlight text
    const renderContent = () => {
        if (!document?.content) return <Typography color="text.secondary">No content available for preview.</Typography>;
        if (!highlightText) return <Typography sx={{ whiteSpace: 'pre-wrap' }}>{document.content}</Typography>;

        // Simple substring check - implies exact match. 
        // For distinct text chunks, exact match might fail due to whitespace. 
        // We'll normalize whitespace for matching.

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

    return (
        <Paper
            elevation={3}
            sx={{
                position: 'fixed',
                top: '5%',
                left: '25%', // Centered roughly or to the right of chat
                right: '5%',
                bottom: '5%',
                zIndex: 1300, // Higher than other overlays
                display: 'flex',
                flexDirection: 'column',
                bgcolor: 'var(--background-primary)',
                borderRadius: 2,
                overflow: 'hidden'
            }}
        >
            {/* Header */}
            <Box sx={{
                p: 2,
                borderBottom: '1px solid var(--divider-color)',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                bgcolor: 'var(--background-modifier-hover)'
            }}>
                <Box>
                    <Typography variant="subtitle1" fontWeight="bold">
                        {document?.title || "Document Preview"}
                    </Typography>
                    {document && (
                        <Typography variant="caption" color="text.secondary">
                            {document.author} • {document.publicationDate}
                        </Typography>
                    )}
                </Box>
                <IconButton onClick={onClose} size="small">
                    <CloseIcon />
                </IconButton>
            </Box>

            {/* Content */}
            <Box ref={contentRef} sx={{ flexGrow: 1, overflowY: 'auto', p: 4, bgcolor: 'var(--background-primary)' }}>
                {isLoading ? (
                    <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
                        <CircularProgress />
                    </Box>
                ) : (
                    renderContent()
                )}
            </Box>
        </Paper>
    );
};

export default DocumentPreview;
