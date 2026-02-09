import React, { useRef, useState } from 'react';
import { Box, Typography, Button, IconButton, Tab, Tabs, TextField, InputAdornment, Dialog, DialogTitle, DialogContent, Fade, Chip } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined';
import SearchIcon from '@mui/icons-material/Search';
import CloseIcon from '@mui/icons-material/Close';
import NoteAddOutlinedIcon from '@mui/icons-material/NoteAddOutlined';
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined';
import KeyboardDoubleArrowLeftIcon from '@mui/icons-material/KeyboardDoubleArrowLeft';

interface LibraryViewProps {
    files: File[];
    onUpload: (file: File) => void;
    onBack: () => void;
}

const LibraryView: React.FC<LibraryViewProps> = ({ files, onUpload, onBack }) => {
    const [tabIndex, setTabIndex] = useState(0);
    const [isUploadOpen, setIsUploadOpen] = useState(false);
    const fileInputRef = useRef<HTMLInputElement>(null);
    const [searchQuery, setSearchQuery] = useState('');

    const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];
        if (file) {
            onUpload(file);
            setIsUploadOpen(false);
        }
    };

    const handleDrop = (event: React.DragEvent<HTMLDivElement>) => {
        event.preventDefault();
        const file = event.dataTransfer.files?.[0];
        if (file) {
            onUpload(file);
            setIsUploadOpen(false);
        }
    };

    const handleDragOver = (event: React.DragEvent<HTMLDivElement>) => {
        event.preventDefault();
    };

    // Filter files based on search query (mock search for now since files are just File objects)
    const filteredFiles = files.filter(f => f.name.toLowerCase().includes(searchQuery.toLowerCase()));

    return (
        <Box className="library-view">

            {/* Header */}
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 4, pt: 1 }}>
                <IconButton onClick={onBack} size="small" sx={{ mr: 1, color: 'var(--text-muted)' }}>
                    <KeyboardDoubleArrowLeftIcon />
                </IconButton>
                <Typography variant="h6" fontWeight="600" sx={{ flexGrow: 1, color: 'var(--text-normal)' }}>
                    Library
                </Typography>

                {/* Only show small upload button if populated */}
                {files.length > 0 && (
                    <IconButton
                        onClick={() => setIsUploadOpen(true)}
                        size="small"
                        sx={{
                            bgcolor: 'var(--color-accent)',
                            color: 'white',
                            '&:hover': { bgcolor: 'var(--color-accent-hover)' },
                            borderRadius: '8px',
                            width: 32,
                            height: 32
                        }}
                    >
                        <CloudUploadOutlinedIcon sx={{ fontSize: 18 }} />
                    </IconButton>
                )}
            </Box>

            {/* EMPTY STATE */}
            {files.length === 0 ? (
                <Fade in={true} timeout={500}>
                    <Box className="library-empty-state">
                        <div className="empty-state-icon-circle">
                            <CloudUploadOutlinedIcon sx={{ fontSize: 32, color: 'var(--text-muted)' }} />
                        </div>

                        <Typography variant="body1" fontWeight="500" sx={{ color: 'var(--text-normal)', mb: 1 }}>
                            Your library is empty
                        </Typography>
                        <Typography variant="caption" sx={{ color: 'var(--text-muted)', mb: 4, display: 'block', maxWidth: '200px', lineHeight: 1.5 }}>
                            Upload documents to let AI provide precise citations.
                        </Typography>

                        <Button
                            variant="outlined"
                            onClick={() => setIsUploadOpen(true)}
                            sx={{
                                textTransform: 'none',
                                color: 'var(--text-normal)',
                                borderColor: 'var(--divider-color)',
                                borderRadius: '8px',
                                px: 3,
                                py: 1,
                                '&:hover': {
                                    bgcolor: 'var(--background-secondary)',
                                    borderColor: 'var(--text-muted)'
                                }
                            }}
                        >
                            Upload file
                        </Button>
                    </Box>
                </Fade>
            ) : (
                /* POPULATED STATE */
                <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
                    <TextField
                        fullWidth
                        placeholder="Search sources..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        size="small"
                        InputProps={{
                            startAdornment: (
                                <InputAdornment position="start">
                                    <SearchIcon fontSize="small" sx={{ color: 'var(--text-muted)' }} />
                                </InputAdornment>
                            )
                        }}
                        className="library-search-input"
                        sx={{ mb: 3 }}
                    />

                    <Tabs
                        value={tabIndex}
                        onChange={(_, v) => setTabIndex(v)}
                        className="clean-tabs"
                        sx={{ mb: 2, borderBottom: '1px solid var(--divider-color)' }}
                    >
                        <Tab label={`Sources (${files.length})`} />
                        <Tab label="Collections" />
                    </Tabs>

                    <Box sx={{ flexGrow: 1, overflowY: 'auto' }}>
                        {filteredFiles.map((file, idx) => (
                            <Fade in={true} key={idx} timeout={300 + (idx * 50)}>
                                <Box className="file-list-item" sx={{ mb: 1.5 }}>
                                    {/* Document Item */}
                                    <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1.5 }}>
                                        {/* Icon Placeholder */}
                                        <Box sx={{
                                            width: 32,
                                            height: 40,
                                            bgcolor: 'var(--background-secondary)',
                                            borderRadius: 1,
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            border: '1px solid var(--background-modifier-border)'
                                        }}>
                                            <InsertDriveFileOutlinedIcon sx={{ fontSize: 20, color: 'var(--text-muted)' }} />
                                        </Box>

                                        <Box sx={{ flexGrow: 1 }}>
                                            <Typography variant="body2" fontWeight="600" sx={{ color: 'var(--text-normal)', mb: 0.5, lineHeight: 1.2 }}>
                                                {file.name}
                                            </Typography>

                                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                                                <Typography variant="caption" sx={{ color: 'var(--text-muted)', fontStyle: 'italic' }}>
                                                    Unknown Author
                                                </Typography>
                                                <Typography variant="caption" sx={{ color: 'var(--text-muted)' }}>•</Typography>
                                                <Typography variant="caption" sx={{ color: 'var(--text-muted)' }}>
                                                    {new Date().getFullYear()}
                                                </Typography>
                                            </Box>

                                            <Button
                                                size="small"
                                                startIcon={<NoteAddOutlinedIcon fontSize="small" />}
                                                sx={{
                                                    textTransform: 'none',
                                                    color: 'var(--text-normal)',
                                                    borderColor: 'var(--divider-color)',
                                                    border: '1px solid',
                                                    borderRadius: '6px',
                                                    fontSize: '0.7rem',
                                                    py: 0.2,
                                                    px: 1,
                                                    '&:hover': { bgcolor: 'var(--background-secondary)' }
                                                }}
                                            >
                                                Add to collection
                                            </Button>
                                        </Box>
                                    </Box>
                                </Box>
                            </Fade>
                        ))}
                    </Box>
                </Box>
            )}

            {/* UPLOAD DIALOG */}
            <Dialog
                open={isUploadOpen}
                onClose={() => setIsUploadOpen(false)}
                fullWidth
                maxWidth="sm"
                PaperProps={{
                    sx: {
                        bgcolor: 'var(--background-primary)',
                        color: 'var(--text-normal)',
                        backgroundImage: 'none',
                        borderRadius: 3,
                        boxShadow: '0 10px 30px rgba(0,0,0,0.5)' // Force a shadow since obsidian might strip it
                    }
                }}
            >
                <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '1rem', p: 3, pb: 1 }}>
                    Upload to Library
                    <IconButton size="small" onClick={() => setIsUploadOpen(false)} sx={{ color: 'var(--text-muted)' }}>
                        <CloseIcon fontSize="small" />
                    </IconButton>
                </DialogTitle>
                <DialogContent sx={{ p: 3 }}>
                    <div
                        className="upload-drop-zone"
                        onDrop={handleDrop}
                        onDragOver={handleDragOver}
                        onClick={() => fileInputRef.current?.click()}
                    >
                        <Box sx={{
                            p: 2,
                            borderRadius: '50%',
                            bgcolor: 'var(--bg-subtle)',
                            mb: 2,
                            color: 'var(--color-accent)'
                        }}>
                            <CloudUploadOutlinedIcon sx={{ fontSize: 32 }} />
                        </Box>

                        <Typography variant="body1" fontWeight="600" sx={{ mb: 1 }}>
                            Upload documents
                        </Typography>

                        <Typography variant="caption" sx={{ color: 'var(--text-muted)', mb: 3 }}>
                            Drag & drop or <span style={{ color: 'var(--color-accent)', fontWeight: 600 }}>Browse</span>
                        </Typography>

                        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', justifyContent: 'center' }}>
                            {['PDF', 'DOCX', 'TXT', 'MD'].map(ext => (
                                <Chip key={ext} label={ext} size="small" sx={{
                                    height: 20,
                                    fontSize: '0.65rem',
                                    bgcolor: 'var(--background-secondary)',
                                    color: 'var(--text-muted)'
                                }} />
                            ))}
                        </Box>
                    </div>
                    <input
                        type="file"
                        ref={fileInputRef}
                        style={{ display: 'none' }}
                        onChange={handleFileChange}
                    />
                </DialogContent>
            </Dialog>

        </Box>
    );
};

export default LibraryView;
