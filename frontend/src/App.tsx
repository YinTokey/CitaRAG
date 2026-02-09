import { useState } from 'react';
import { Box, Paper, TextField, IconButton, Typography, CircularProgress } from '@mui/material';
import SendIcon from '@mui/icons-material/Send';
import UploadSection from './components/UploadSection';
import CitationList from './components/CitationList';
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
        setActiveCitations(data.citations || []);
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

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', bgcolor: 'transparent' }}>
      {/* Scrollable Chat Area */}
      <Box sx={{ flexGrow: 1, p: 2, overflowY: 'auto' }}>
        {messages.length === 0 && (
          <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%', flexDirection: 'column', opacity: 0.5 }}>
            <Typography variant="h6" fontWeight="bold">CitaRAG</Typography>
            <Typography variant="caption" align="center">Upload documents and ask questions.</Typography>
          </Box>
        )}

        {messages.map((msg, idx) => (
          <Box key={idx} sx={{
            display: 'flex',
            justifyContent: msg.sender === 'user' ? 'flex-end' : 'flex-start',
            mb: 2
          }}>
            <Paper
              onClick={() => msg.citations && setActiveCitations(msg.citations)}
              elevation={1}
              sx={{
                p: 1.5,
                borderRadius: msg.sender === 'user' ? '12px 12px 0 12px' : '12px 12px 12px 0',
                bgcolor: msg.sender === 'user' ? '#4f46e5' : 'var(--background-secondary)',
                color: msg.sender === 'user' ? 'white' : 'var(--text-normal)',
                maxWidth: '90%',
                cursor: msg.sender === 'bot' ? 'pointer' : 'default',
              }}>
              <Typography sx={{ fontSize: '0.9rem' }}>{msg.text}</Typography>
            </Paper>
          </Box>
        ))}
        {isChatting && (
          <Box sx={{ display: 'flex', justifyContent: 'flex-start', mb: 2 }}>
            <CircularProgress size={16} sx={{ color: '#4f46e5' }} />
          </Box>
        )}
      </Box>

      {/* Citations Panel */}
      {activeCitations.length > 0 && (
        <Box sx={{
          maxHeight: '40%',
          overflowY: 'auto',
          borderTop: '2px solid var(--divider-color)',
          bgcolor: 'var(--background-secondary-alt)',
          p: 1.5
        }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1, alignItems: 'center' }}>
            <Typography variant="caption" sx={{ fontWeight: 'bold', textTransform: 'uppercase', letterSpacing: 0.5 }}>Sources</Typography>
            <Typography
              variant="caption"
              sx={{ cursor: 'pointer', color: 'var(--text-accent)', '&:hover': { textDecoration: 'underline' } }}
              onClick={() => setActiveCitations([])}
            >
              Done
            </Typography>
          </Box>
          <CitationList citations={activeCitations} />
        </Box>
      )}

      {/* Input Area */}
      <Box sx={{ p: 1.5, borderTop: '1px solid var(--divider-color)', bgcolor: 'var(--background-primary)' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 1, gap: 1 }}>
          <Typography variant="caption" sx={{ color: 'var(--text-muted)', flexGrow: 1 }}>
            Model: <b>Gemini 2.2 Pro</b>
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, px: 1, py: 0.2, borderRadius: 5, border: '1px solid var(--divider-color)', bgcolor: 'var(--background-secondary)' }}>
            <Typography variant="caption" sx={{ fontSize: '0.65rem' }}>Web</Typography>
            <Box sx={{ width: 14, height: 14, bgcolor: 'var(--text-muted)', borderRadius: '50%', opacity: 0.3 }} />
          </Box>
        </Box>

        <Box sx={{
          display: 'flex',
          alignItems: 'center',
          bgcolor: 'var(--background-modifier-form-field)',
          borderRadius: 2,
          px: 1.5,
          border: '1px solid var(--divider-color)',
          '&:focus-within': { borderColor: '#4f46e5' }
        }}>
          <TextField
            fullWidth
            placeholder="Ask agent to help you search..."
            variant="standard"
            InputProps={{ disableUnderline: true }}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleSendMessage()}
            sx={{
              input: { color: 'var(--text-normal)', fontSize: '0.9rem', py: 1.2 }
            }}
          />
          <IconButton
            size="small"
            onClick={handleSendMessage}
            disabled={isChatting || !input.trim()}
            sx={{
              bgcolor: input.trim() ? '#4f46e5' : 'transparent',
              color: input.trim() ? 'white' : 'var(--text-muted)',
              '&:hover': { bgcolor: '#4338ca' }
            }}
          >
            <SendIcon fontSize="small" />
          </IconButton>
        </Box>

        <Box sx={{ mt: 1.5 }}>
          <UploadSection onFileUpload={handleFileUpload} uploadedFiles={files} isUploading={isUploading} />
        </Box>
      </Box>
    </Box>
  );
}

export default App;
