
import React, { useRef } from 'react';
import { Box, Button, List, ListItem, ListItemText, Typography, Divider } from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import InsertDriveFileIcon from '@mui/icons-material/InsertDriveFile';

interface SidebarProps {
    onFileUpload: (file: File) => void;
    uploadedFiles: File[];
}

const Sidebar: React.FC<SidebarProps> = ({ onFileUpload, uploadedFiles }) => {
    const fileInputRef = useRef<HTMLInputElement>(null);

    const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];
        if (file) {
            onFileUpload(file);
        }
    };

    const handleButtonClick = () => {
        fileInputRef.current?.click();
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

            <Button
                variant="contained"
                startIcon={<CloudUploadIcon />}
                onClick={handleButtonClick}
                sx={{
                    mb: 2,
                    bgcolor: 'black',
                    color: 'white',
                    '&:hover': {
                        bgcolor: '#333',
                    },
                    textTransform: 'none',
                }}
            >
                Upload File
            </Button>
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
