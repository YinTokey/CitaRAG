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

interface CitationListProps {
    citations: Citation[];
    onCitationClick: (citation: Citation) => void;
}

const CitationList: React.FC<CitationListProps> = ({ citations, onCitationClick }) => {
    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            {citations.map((citation, idx) => (
                <Paper
                    key={idx}
                    elevation={0}
                    onClick={() => onCitationClick(citation)}
                    sx={{
                        p: 1.5,
                        borderRadius: 1,
                        border: '1px solid var(--divider-color)',
                        bgcolor: 'var(--background-primary) !important',
                        cursor: 'pointer',
                        transition: 'all 0.2s ease',
                        '&:hover': {
                            borderColor: '#4f46e5',
                            bgcolor: 'rgba(79, 70, 229, 0.04) !important',
                        }
                    }}
                >
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                        <Typography variant="caption" sx={{ color: '#4f46e5', fontWeight: 'bold' }}>
                            Page {citation.metadata.page_number || '?'}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                            {(citation.score * 100).toFixed(0)}% Match
                        </Typography>
                    </Box>

                    {citation.metadata.section_header && (
                        <Typography variant="caption" display="block" sx={{ mb: 0.5, fontWeight: 'bold' }}>
                            {citation.metadata.section_header}
                        </Typography>
                    )}

                    <Typography variant="caption" sx={{ color: 'var(--text-normal)', fontSize: '0.75rem', lineHeight: 1.4 }}>
                        {citation.text.length > 150 ? citation.text.substring(0, 150) + '...' : citation.text}
                    </Typography>
                </Paper>
            ))}
        </Box>
    );
};

export default CitationList;
