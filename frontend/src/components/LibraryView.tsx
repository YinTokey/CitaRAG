import React, { useRef, useState, useEffect } from 'react';
import { Box, Typography, Button, IconButton, Tab, Tabs, TextField, InputAdornment, Dialog, DialogTitle, DialogContent, Fade, Chip, CircularProgress, Backdrop, DialogActions, List, ListItem, ListItemButton, ListItemText, LinearProgress } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined';
import SearchIcon from '@mui/icons-material/Search';
import CloseIcon from '@mui/icons-material/Close';
import CheckIcon from '@mui/icons-material/Check';
import NoteAddOutlinedIcon from '@mui/icons-material/NoteAddOutlined';
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined';
import KeyboardDoubleArrowLeftIcon from '@mui/icons-material/KeyboardDoubleArrowLeft';
import LibraryBooksIcon from '@mui/icons-material/LibraryBooks';
import CreateNewFolderIcon from '@mui/icons-material/CreateNewFolder';
import FolderOpenIcon from '@mui/icons-material/FolderOpen';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';

import { Document } from '../types';

interface UploadItem {
    id: string;
    file: File;
    progress: number;
    status: 'pending' | 'uploading' | 'processing' | 'completed' | 'error';
    error?: string;
}

interface LibraryViewProps {
    onUpload: (files: File[]) => void;
    onBack: () => void;
    isUploading: boolean;
    uploadQueue: UploadItem[];
    onClearQueue: () => void;
    isUploadOpen: boolean;
    setIsUploadOpen: (open: boolean) => void;
    onPreview: (doc: Document) => void;
}

const LibraryView: React.FC<LibraryViewProps> = ({ onUpload, onBack, isUploading, uploadQueue, onClearQueue, isUploadOpen, setIsUploadOpen, onPreview }) => {
    const fileInputRef = useRef<HTMLInputElement>(null);
    const [searchQuery, setSearchQuery] = useState('');

    // Backend Data State
    const [documents, setDocuments] = useState<Document[]>([]);
    const [isLoadingData, setIsLoadingData] = useState(false);

    // Fetch Data on Mount
    useEffect(() => {
        fetchData();
    }, []);

    // Refresh library when items complete
    const prevCompletedCount = useRef(0);
    useEffect(() => {
        const completedCount = uploadQueue.filter(i => i.status === 'completed').length;
        if (completedCount > prevCompletedCount.current) {
            fetchData();
        }
        prevCompletedCount.current = completedCount;
    }, [uploadQueue]);

    const fetchData = async () => {
        setIsLoadingData(true);
        try {
            const docsRes = await fetch('http://localhost:8080/api/documents');

            if (docsRes.ok) setDocuments(await docsRes.json());
        } catch (error) {
            console.error("Failed to fetch library data:", error);
        } finally {
            setIsLoadingData(false);
        }
    };

    const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        if (event.target.files && event.target.files.length > 0) {
            handleUploadWrapper(Array.from(event.target.files));
        }
    };

    const handleDrop = (event: React.DragEvent<HTMLDivElement>) => {
        event.preventDefault();
        if (event.dataTransfer.files && event.dataTransfer.files.length > 0) {
            handleUploadWrapper(Array.from(event.dataTransfer.files));
        }
    };

    const handleUploadWrapper = (files: File[]) => {
        onUpload(files);
        // setIsUploadOpen(false); // Keep open to show progress
    };

    const handleDragOver = (event: React.DragEvent<HTMLDivElement>) => {
        event.preventDefault();
    };



    // Filter documents
    const filteredDocs = documents.filter(d =>
        (d.title || d.filename).toLowerCase().includes(searchQuery.toLowerCase()) ||
        (d.author || '').toLowerCase().includes(searchQuery.toLowerCase())
    );



    return (
        <Box className="library-view" sx={{ position: 'relative', height: '100%', display: 'flex', flexDirection: 'column' }}>

            {/* Loading Overlay */}
            <Backdrop
                sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 1, position: 'absolute' }}
                open={isLoadingData}
            >
                <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                    <CircularProgress color="inherit" />
                    <Typography variant="body2" sx={{ mt: 2, color: 'white' }}>
                        Loading Library...
                    </Typography>
                </Box>
            </Backdrop>

            {/* Header */}
            <Box sx={{ mb: 2, pt: 1, flexShrink: 0 }}>
                <Button
                    onClick={onBack}
                    size="small"
                    sx={{
                        color: 'var(--text-muted)',
                        minWidth: 32,
                        height: 32,
                        p: 0,
                        border: '1px solid var(--background-modifier-border)',
                        borderRadius: '6px',
                        bgcolor: 'var(--background-primary)',
                        '&:hover': { bgcolor: 'var(--background-modifier-hover)' }
                    }}
                >
                    <KeyboardDoubleArrowLeftIcon sx={{ fontSize: 18 }} />
                </Button>

                <Box sx={{ display: 'flex', alignItems: 'center', mt: 2, gap: 1.5, justifyContent: 'space-between' }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                        <LibraryBooksIcon sx={{ color: 'var(--text-normal)', fontSize: 24 }} />
                        <Typography variant="h6" fontWeight="700" sx={{ color: 'var(--text-normal)' }}>
                            Library
                        </Typography>
                    </Box>

                    {documents.length > 0 && (
                        <Button
                            onClick={() => setIsUploadOpen(true)}
                            startIcon={<CloudUploadOutlinedIcon />}
                            size="small"
                            variant="outlined"
                            sx={{
                                color: 'var(--text-normal)',
                                borderColor: 'var(--divider-color)',
                                textTransform: 'none'
                            }}
                        >
                            Upload
                        </Button>
                    )}
                </Box>
            </Box>

            {/* Content Area */}
            {documents.length === 0 && !isLoadingData ? (
                <Fade in={true} timeout={500}>
                    <Box className="library-empty-state" sx={{ flexGrow: 1 }}>
                        <div className="empty-state-icon-circle">
                            <CloudUploadOutlinedIcon sx={{ fontSize: 32, color: 'var(--text-muted)' }} />
                        </div>
                        <Typography variant="body1" fontWeight="500" sx={{ color: 'var(--text-normal)', mb: 1 }}>
                            Your library is empty
                        </Typography>
                        <Button
                            variant="outlined"
                            onClick={() => setIsUploadOpen(true)}
                            sx={{ mt: 2 }}
                        >
                            Upload file
                        </Button>
                    </Box>
                </Fade>
            ) : (
                <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}>

                    {/* Upload Queue */}


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
                        sx={{ mb: 2 }}
                    />

                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                        <Typography variant="subtitle2" sx={{ color: 'var(--text-muted)' }}>
                            Documents ({documents.length})
                        </Typography>
                    </Box>

                    <Box sx={{ flexGrow: 1, overflowY: 'auto', pb: 4 }}>
                        {
                            filteredDocs.map((doc) => (
                                <Fade in={true} key={doc.id} timeout={300}>
                                    <Box className="file-list-item" sx={{ mb: 1.5 }}>
                                        <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1.5 }}>
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
                                                <Typography variant="body2" fontWeight="600" sx={{ color: 'var(--text-normal)', mb: 0.5 }}>
                                                    {doc.title || doc.filename}
                                                </Typography>
                                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                                                    <Typography variant="caption" sx={{ color: 'var(--text-muted)' }}>
                                                        {doc.author || 'Unknown Author'}
                                                    </Typography>
                                                    <Typography variant="caption" sx={{ color: 'var(--text-muted)' }}>•</Typography>
                                                    <Typography variant="caption" sx={{ color: 'var(--text-muted)' }}>
                                                        {doc.publicationDate || doc.uploadDate?.substring(0, 4) || 'Unknown Date'}
                                                    </Typography>
                                                </Box>

                                                <Button
                                                    size="small"
                                                    onClick={() => onPreview(doc)}
                                                    sx={{
                                                        textTransform: 'none',
                                                        color: 'var(--text-accent)',
                                                        ml: 1,
                                                        fontSize: '0.7rem'
                                                    }}
                                                >
                                                    Preview
                                                </Button>
                                            </Box>
                                        </Box>
                                    </Box>
                                </Fade>
                            ))
                        }
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
                    sx: { bgcolor: 'var(--background-primary)', color: 'var(--text-normal)', borderRadius: 3 }
                }}
            >
                <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    Upload to Library
                    <IconButton size="small" onClick={() => setIsUploadOpen(false)}><CloseIcon fontSize="small" /></IconButton>
                </DialogTitle>
                <DialogContent>
                    {uploadQueue.length === 0 ? (
                        <div
                            className="upload-drop-zone"
                            onDrop={handleDrop}
                            onDragOver={handleDragOver}
                            onClick={() => fileInputRef.current?.click()}
                        >
                            <Box sx={{ p: 2, borderRadius: '50%', bgcolor: 'var(--bg-subtle)', mb: 2, color: 'var(--color-accent)' }}>
                                <CloudUploadOutlinedIcon sx={{ fontSize: 32 }} />
                            </Box>
                            <Typography variant="body1" fontWeight="600">Upload documents</Typography>
                            <Typography variant="caption" sx={{ color: 'var(--text-muted)', mb: 3 }}>Drag & drop or Browse</Typography>
                        </div>
                    ) : (
                        /* Upload Queue in Dialog */
                        <Box sx={{ mt: 0, maxHeight: '300px', overflowY: 'auto' }}>
                            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2, alignItems: 'center' }}>
                                <Typography variant="subtitle2" fontWeight="bold">Upload Status</Typography>
                                <Button size="small" onClick={onClearQueue} disabled={isUploading}>
                                    {isUploading ? 'Uploading...' : 'Done / Upload More'}
                                </Button>
                            </Box>
                            {uploadQueue.map(item => (
                                <Box key={item.id} sx={{ mb: 2 }}>
                                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                                        <Typography variant="body2" noWrap sx={{ maxWidth: '70%' }}>{item.file.name}</Typography>
                                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                            {item.status === 'completed' && <CheckIcon color="success" fontSize="small" />}
                                            {item.status === 'error' && <CloseIcon color="error" fontSize="small" />}
                                            <Typography variant="caption" color={item.status === 'error' ? 'error' : 'textSecondary'}>
                                                {(item.status === 'uploading' || item.status === 'processing') ? `${item.status} (${item.progress}%)` : item.status}
                                            </Typography>
                                        </Box>
                                    </Box>
                                    <LinearProgress variant="determinate" value={item.progress} color={item.status === 'error' ? 'error' : item.status === 'completed' ? 'success' : 'primary'} />
                                    {item.error && <Typography variant="caption" color="error">{item.error}</Typography>}
                                </Box>
                            ))}
                        </Box>
                    )}

                    <input type="file" multiple ref={fileInputRef} style={{ display: 'none' }} onChange={handleFileChange} />
                </DialogContent>
            </Dialog>



        </Box >
    );
};

export default LibraryView;
