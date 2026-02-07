
import { useState } from 'react';
import { Box, Paper, TextField, IconButton, Typography, CircularProgress } from '@mui/material';
import SendIcon from '@mui/icons-material/Send';
import Sidebar from './components/Sidebar';
import RightSidebar from './components/RightSidebar';
import './App.css';

interface Message {
  text: string;
  sender: 'user' | 'bot';
  citations?: any[];
}

function App() {
  const [files, setFiles] = useState<File[]>([]);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [isUploading, setIsUploading] = useState(false);
  const [isChatting, setIsChatting] = useState(false);
  const [activeCitations, setActiveCitations] = useState<any[]>([]);

  const handleFileUpload = async (file: File) => {
    setIsUploading(true);
    const formData = new FormData();
    formData.append('files', file);

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

  const handleSendMessage = async () => {
    if (!input.trim()) return;

    const userMsg: Message = { text: input, sender: 'user' };
    setMessages((prev) => [...prev, userMsg]);
    setInput('');
    setIsChatting(true);

    try {
      const response = await fetch('http://localhost:8080/api/chat', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ query: userMsg.text }),
      });

      if (response.ok) {
        const data = await response.json();
        const botMsg: Message = {
          text: data.answer,
          sender: 'bot',
          citations: data.citations
        };
        setMessages((prev) => [...prev, botMsg]);
        setActiveCitations(data.citations || []); // Auto-show citations for latest message
      } else {
        setMessages((prev) => [...prev, { text: "Error: Unable to get response.", sender: 'bot' }]);
      }
    } catch (error) {
      console.error('Chat error:', error);
      setMessages((prev) => [...prev, { text: "Network error. Please try again.", sender: 'bot' }]);
    } finally {
      setIsChatting(false);
    }
  };

  const handleMessageClick = (msg: Message) => {
    if (msg.sender === 'bot' && msg.citations) {
      setActiveCitations(msg.citations);
    }
  };

  return (
    <Box sx={{ display: 'flex', height: '100vh', bgcolor: '#f3f4f6' }}>
      {/* Left Sidebar (Uploads) */}
      <Sidebar onFileUpload={handleFileUpload} uploadedFiles={files} isUploading={isUploading} />

      {/* Center Chat Area */}
      <Box sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column', height: '100vh', position: 'relative' }}>
        <Box sx={{ flexGrow: 1, p: 3, overflowY: 'auto' }}>
          {messages.length === 0 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%', flexDirection: 'column', opacity: 0.5 }}>
              <Typography variant="h4" fontWeight="bold" color="text.secondary">CitaRAG</Typography>
              <Typography variant="subtitle1" color="text.secondary">Upload a document and start chatting.</Typography>
            </Box>
          )}

          {messages.map((msg, idx) => (
            <Box key={idx} sx={{
              display: 'flex',
              justifyContent: msg.sender === 'user' ? 'flex-end' : 'flex-start',
              mb: 3
            }}>
              <Paper
                onClick={() => handleMessageClick(msg)}
                elevation={msg.sender === 'user' ? 4 : 1}
                sx={{
                  p: 2.5,
                  borderRadius: msg.sender === 'user' ? '20px 20px 0 20px' : '20px 20px 20px 0',
                  bgcolor: msg.sender === 'user' ? '#4f46e5' : 'white', // Indigo for user
                  color: msg.sender === 'user' ? 'white' : '#1f2937',
                  maxWidth: '75%',
                  cursor: msg.sender === 'bot' ? 'pointer' : 'default',
                  transition: 'transform 0.1s',
                  '&:active': msg.sender === 'bot' ? { transform: 'scale(0.98)' } : {},
                  boxShadow: msg.sender === 'user'
                    ? '0 10px 15px -3px rgba(79, 70, 229, 0.3)'
                    : '0 4px 6px -1px rgba(0, 0, 0, 0.1)'
                }}>
                <Typography sx={{ lineHeight: 1.6, fontSize: '1rem' }}>{msg.text}</Typography>
              </Paper>
            </Box>
          ))}
          {isChatting && (
            <Box sx={{ display: 'flex', justifyContent: 'flex-start', mb: 3 }}>
              <Paper sx={{ p: 2, borderRadius: '20px 20px 20px 0', bgcolor: 'white' }}>
                <CircularProgress size={20} sx={{ color: '#4f46e5' }} />
              </Paper>
            </Box>
          )}
        </Box>

        {/* Input Area */}
        <Box sx={{ p: 3, bgcolor: 'white', borderTop: '1px solid #e5e7eb' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', maxWidth: 900, mx: 'auto', bgcolor: '#f9fafb', borderRadius: 3, px: 2, py: 1, border: '1px solid #e5e7eb' }}>
            <TextField
              fullWidth
              placeholder="Ask a question about your documents..."
              variant="standard"
              InputProps={{ disableUnderline: true }}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && handleSendMessage()}
              sx={{ mr: 1 }}
            />
            <IconButton
              color="primary"
              onClick={handleSendMessage}
              disabled={isChatting || !input.trim()}
              sx={{
                bgcolor: input.trim() ? '#4f46e5' : 'transparent',
                color: input.trim() ? 'white' : 'action.disabled',
                '&:hover': { bgcolor: '#4338ca' }
              }}
            >
              <SendIcon />
            </IconButton>
          </Box>
        </Box>
      </Box>

      {/* Right Sidebar (Citations) */}
      <RightSidebar citations={activeCitations} />
    </Box>
  );
}

export default App;
