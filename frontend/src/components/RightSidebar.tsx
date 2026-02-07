import React from 'react';
import { Box, Typography, Paper, Divider } from '@mui/material';

interface Citation {
    text: string;
    score: number;
    metadata: {
        section_header?: string;
        page_number?: string;
        [key: string]: any;
    };
}

interface RightSidebarProps {
    citations: Citation[];
}

const RightSidebar: React.FC<RightSidebarProps> = ({ citations }) => {
    return (
        <Box
            sx={{
                width: 350,
                height: '100vh',
                borderLeft: '1px solid #e0e0e0',
                bgcolor: '#f9fafb', // Creating a soft background
                p: 2,
                overflowY: 'auto',
                display: 'flex',
                flexDirection: 'column',
            }}
        >
            <Typography variant="h6" sx={{ mb: 2, fontWeight: 600, color: '#374151' }}>
                Sources & Context
            </Typography>

            {citations.length === 0 ? (
                <Typography variant="body2" color="text.secondary" sx={{ fontStyle: 'italic' }}>
                    Select a response to view its sources.
                </Typography>
            ) : (
                citations.map((citation, idx) => (
                    <Paper
                        key={idx}
                        elevation={0}
                        sx={{
                            p: 2,
                            mb: 2,
                            borderRadius: 2,
                            border: '1px solid #e5e7eb',
                            transition: 'all 0.2s',
                            '&:hover': {
                                borderColor: '#6366f1', // Indigo hover
                                boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
                            },
                        }}
                    >
                        {/* Header: Page & Section */}
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                            <Typography variant="caption" sx={{ color: '#4f46e5', fontWeight: 600, bgcolor: '#eef2ff', px: 1, py: 0.5, borderRadius: 1 }}>
                                Page {citation.metadata.page_number || '?'}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                                {(citation.score * 100).toFixed(0)}% Match
                            </Typography>
                        </Box>

                        {citation.metadata.section_header && (
                            <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 600 }}>
                                {citation.metadata.section_header}
                            </Typography>
                        )}

                        <Divider sx={{ my: 1 }} />

                        <Typography variant="body2" sx={{ color: '#4b5563', fontSize: '0.85rem', lineHeight: 1.5 }}>
                            {citation.text.length > 200 ? citation.text.substring(0, 200) + '...' : citation.text}
                        </Typography>
                    </Paper>
                ))
            )}
        </Box>
    );
};

export default RightSidebar;
