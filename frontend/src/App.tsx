
import { useState } from 'react';
import { Box, Paper, TextField, IconButton } from '@mui/material';
import SendIcon from '@mui/icons-material/Send';
import Sidebar from './components/Sidebar';
import './App.css';

function App() {
  const [files, setFiles] = useState<File[]>([]);
  const [messages, setMessages] = useState<{ text: string, sender: 'user' | 'bot' }[]>([]);
  const [input, setInput] = useState('');
  const [isUploading, setIsUploading] = useState(false);

  const handleFileUpload = async (file: File) => {
    setIsUploading(true);

    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await fetch('http://localhost:8080/api/upload', {
        method: 'POST',
        body: formData,
      });

      if (response.ok) {
        console.log('File uploaded successfully');
        setFiles((prev) => [...prev, file]);
      } else {
        console.error('File upload failed');
      }
    } catch (error) {
      console.error('Error uploading file:', error);
    } finally {
      setIsUploading(false);
    }
  };

  const handleSendMessage = () => {
    if (!input.trim()) return;
    setMessages([...messages, { text: input, sender: 'user' }]);
    setInput('');
    // TODO: Send to backend
  };

  return (
    <Box sx={{ display: 'flex', height: '100vh', bgcolor: 'background.default' }}>
      <Sidebar onFileUpload={handleFileUpload} uploadedFiles={files} isUploading={isUploading} />

      <Box sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column', height: '100vh' }}>
        <Box sx={{ flexGrow: 1, p: 2, overflowY: 'auto' }}>
          {/* Chat Messages Placeholder */}
          {messages.map((msg, idx) => (
            <Box key={idx} sx={{
              display: 'flex',
              justifyContent: msg.sender === 'user' ? 'flex-end' : 'flex-start',
              mb: 2
            }}>
              <Paper sx={{
                p: 2,
                bgcolor: msg.sender === 'user' ? '#f0f4c3' : 'white', // Light green for user
                color: 'black',
                maxWidth: '70%'
              }}>
                {msg.text}
              </Paper>
            </Box>
          ))}
        </Box>

        <Box sx={{ p: 2, bgcolor: 'background.paper', borderTop: '1px solid #e0e0e0' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', maxWidth: 800, mx: 'auto' }}>
            <TextField
              fullWidth
              placeholder="Type a message..."
              variant="outlined"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && handleSendMessage()}
              sx={{ mr: 1, bgcolor: 'white' }}
            />
            <IconButton color="primary" onClick={handleSendMessage}>
              <SendIcon />
            </IconButton>
          </Box>
        </Box>
      </Box>
    </Box>
  );
}

export default App;
