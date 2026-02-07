
import React, { useRef } from 'react';
import { Box, Button, List, ListItem, ListItemText, Typography, Divider, CircularProgress } from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import InsertDriveFileIcon from '@mui/icons-material/InsertDriveFile';

interface SidebarProps {
    onFileUpload: (file: File) => void;
    uploadedFiles: File[];
    isUploading: boolean;
}

const Sidebar: React.FC<SidebarProps> = ({ onFileUpload, uploadedFiles, isUploading }) => {
    const fileInputRef = useRef<HTMLInputElement>(null);

    const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];
        if (file) {
            onFileUpload(file);
            // Reset input value to allow uploading the same file again if needed
            event.target.value = '';
        }
    };

    const handleButtonClick = () => {
        if (!isUploading) {
            fileInputRef.current?.click();
        }
    };

    return (
        <Box
            sx={{
                width: 250,
                height: '100vh',
                bgcolor: '#f5f5f5', // Light grey background for sidebar to distinguish from white chat
                borderRight: '1px solid #e0e0e0',
                display: 'flex',
                flexDirection: 'column',
                p: 2,
            }}
        >
            <Typography variant="h6" sx={{ mb: 2, fontWeight: 'bold' }}>
                Files
            </Typography>

            <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                <Button
                    variant="contained"
                    startIcon={<CloudUploadIcon />}
                    onClick={handleButtonClick}
                    disabled={isUploading}
                    sx={{
                        bgcolor: 'black',
                        color: 'white',
                        '&:hover': {
                            bgcolor: '#333',
                        },
                        textTransform: 'none',
                        flexGrow: 1,
                    }}
                >
                    {isUploading ? 'Uploading...' : 'Upload File'}
                </Button>
                {isUploading && (
                    <CircularProgress size={24} sx={{ ml: 2, color: 'black' }} />
                )}
            </Box>
            <input
                type="file"
                ref={fileInputRef}
                style={{ display: 'none' }}
                onChange={handleFileChange}
            />

            <Divider sx={{ mb: 2 }} />

            <List sx={{ flexGrow: 1, overflowY: 'auto' }}>
                {uploadedFiles.map((file, index) => (
                    <ListItem key={index} sx={{ px: 0 }}>
                        <InsertDriveFileIcon sx={{ mr: 1, color: 'text.secondary', fontSize: 20 }} />
                        <ListItemText
                            primary={file.name}
                            primaryTypographyProps={{
                                fontSize: '0.875rem',
                                noWrap: true,
                            }}
                        />
                    </ListItem>
                ))}
                {uploadedFiles.length === 0 && (
                    <Typography variant="body2" color="text.secondary" align="center" sx={{ mt: 2 }}>
                        No files uploaded
                    </Typography>
                )}
            </List>
        </Box>
    );
};

export default Sidebar;
