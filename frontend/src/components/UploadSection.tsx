import React, { useRef } from 'react';
import { Box, Button, List, ListItem, ListItemText, Typography, CircularProgress } from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import InsertDriveFileIcon from '@mui/icons-material/InsertDriveFile';

interface UploadSectionProps {
    onFileUpload: (files: File[]) => void;
    uploadedFiles: File[];
    isUploading: boolean;
}

const UploadSection: React.FC<UploadSectionProps> = ({ onFileUpload, uploadedFiles, isUploading }) => {
    const fileInputRef = useRef<HTMLInputElement>(null);

    const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const fileList = event.target.files;
        if (fileList && fileList.length > 0) {
            const filesArray = Array.from(fileList);
            onFileUpload(filesArray);
            event.target.value = '';
        }
    };

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Button
                    variant="outlined"
                    size="small"
                    startIcon={<CloudUploadIcon />}
                    onClick={() => fileInputRef.current?.click()}
                    disabled={isUploading}
                    sx={{
                        textTransform: 'none',
                        flexGrow: 1,
                        borderColor: 'var(--divider-color)',
                        color: 'var(--text-normal)',
                        '&:hover': { borderColor: '#4f46e5' }
                    }}
                >
                    {isUploading ? 'Uploading...' : 'Upload'}
                </Button>
                {isUploading && (
                    <CircularProgress size={16} sx={{ color: 'var(--text-accent)' }} />
                )}
            </Box>
            <input
                type="file"
                multiple
                ref={fileInputRef}
                style={{ display: 'none' }}
                onChange={handleFileChange}
            />

            {uploadedFiles.length > 0 && (
                <List dense sx={{
                    maxHeight: '100px',
                    overflowY: 'auto',
                    border: '1px solid var(--divider-color)',
                    borderRadius: 1,
                    bgcolor: 'var(--background-secondary-alt)'
                }}>
                    {uploadedFiles.map((file, index) => (
                        <ListItem key={index} sx={{ py: 0 }}>
                            <InsertDriveFileIcon sx={{ mr: 1, color: 'var(--text-muted)', fontSize: 16 }} />
                            <ListItemText
                                primary={file.name}
                                primaryTypographyProps={{
                                    fontSize: '0.75rem',
                                    noWrap: true,
                                    color: 'var(--text-normal)'
                                }}
                            />
                        </ListItem>
                    ))}
                </List>
            )}
        </Box>
    );
};

export default UploadSection;
