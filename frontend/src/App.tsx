import { useState, useRef, useEffect } from 'react';
import { Box, Paper, TextField, IconButton, Typography, CircularProgress, Switch, Button, Collapse, Fade, List, ListItem, ListItemIcon, ListItemText } from '@mui/material';
import SendIcon from '@mui/icons-material/Send';
import CopyAllIcon from '@mui/icons-material/CopyAll';
import ThumbUpOutlinedIcon from '@mui/icons-material/ThumbUpOutlined';
import ThumbDownOutlinedIcon from '@mui/icons-material/ThumbDownOutlined';
import ArticleOutlinedIcon from '@mui/icons-material/ArticleOutlined';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import DriveFileRenameOutlineIcon from '@mui/icons-material/DriveFileRenameOutline';
import KeyboardDoubleArrowRightIcon from '@mui/icons-material/KeyboardDoubleArrowRight';
import KeyboardDoubleArrowLeftIcon from '@mui/icons-material/KeyboardDoubleArrowLeft';
import LibraryBooksIcon from '@mui/icons-material/LibraryBooks';
import ChatBubbleOutlineIcon from '@mui/icons-material/ChatBubbleOutline';
import PsychologyIcon from '@mui/icons-material/Psychology';
import FormatQuoteIcon from '@mui/icons-material/FormatQuote';

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
  const [isWebSearch, setIsWebSearch] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);

  const messagesEndRef = useRef<null | HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

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
        body: JSON.stringify({ query: userMsg.text, web_search: isWebSearch }),
      });

      if (response.ok) {
        const data = await response.json();
        const botMsg: Message = {
          text: data.answer,
          sender: 'bot',
          citations: data.citations
        };
        setMessages((prev) => [...prev, botMsg]);
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

  const handleNewChat = () => {
    setMessages([]);
    setInput('');
    setActiveCitations([]);
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', bgcolor: 'transparent', position: 'relative', overflow: 'hidden' }}>

      {/* HEADER: Top Right Buttons */}
      <Box sx={{
        position: 'absolute',
        top: 10,
        right: 10,
        zIndex: 5,
        display: 'flex',
        gap: 1
      }}>
        <IconButton
          size="small"
          onClick={handleNewChat}
          sx={{
            bgcolor: '#6366f1',
            color: 'white',
            borderRadius: 2,
            '&:hover': { bgcolor: '#4f46e5' }
          }}
        >
          <DriveFileRenameOutlineIcon fontSize="small" />
        </IconButton>
        <IconButton
          size="small"
          onClick={() => setIsSettingsOpen(true)}
          sx={{
            bgcolor: 'var(--background-modifier-form-field)',
            color: 'var(--text-muted)',
            borderRadius: 2,
            '&:hover': { bgcolor: 'var(--background-modifier-hover)' }
          }}
        >
          <KeyboardDoubleArrowRightIcon fontSize="small" />
        </IconButton>
      </Box>

      {/* MAIN VIEW: Chat & Input */}
      <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', pointerEvents: isSettingsOpen ? 'none' : 'auto', opacity: isSettingsOpen ? 0 : 1, transition: 'opacity 0.2s' }}>
        {/* 1. Scrollable Chat Area */}
        <Box sx={{ flexGrow: 1, p: 2, overflowY: 'auto', pb: 20, pt: 6 }}>
          {messages.length === 0 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%', flexDirection: 'column', opacity: 0.6 }}>
              <Box sx={{ mb: 2, p: 2, borderRadius: '50%', bgcolor: 'var(--background-modifier-form-field)' }}>
                <ArticleOutlinedIcon sx={{ fontSize: 40, color: 'var(--text-accent)' }} />
              </Box>
              <Typography variant="h6" fontWeight="bold">CitaRAG</Typography>
              <Typography variant="caption" align="center" color="text.secondary">Your research assistant</Typography>
            </Box>
          )}

          {messages.map((msg, idx) => (
            <Box key={idx} sx={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: msg.sender === 'user' ? 'flex-end' : 'flex-start',
              mb: 3
            }}>
              {/* Message Bubble */}
              <Typography variant="body1" sx={{
                whiteSpace: 'pre-wrap',
                color: 'var(--text-normal)',
                fontSize: '0.95rem',
                lineHeight: 1.6,
                fontWeight: msg.sender === 'user' ? 400 : 400,
                maxWidth: '100%',
                mb: 1
              }}>
                {msg.text}
              </Typography>

              {/* Citations & Actions Row (Bot Only) */}
              {msg.sender === 'bot' && (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, width: '100%' }}>
                  {msg.citations && msg.citations.length > 0 && (
                    <Button
                      startIcon={<ArticleOutlinedIcon sx={{ fontSize: 16 }} />}
                      size="small"
                      onClick={() => setActiveCitations(activeCitations === msg.citations ? [] : msg.citations!)}
                      sx={{
                        textTransform: 'none',
                        color: 'var(--text-accent)',
                        bgcolor: 'var(--background-primary-alt)',
                        fontSize: '0.75rem',
                        py: 0.2,
                        px: 1,
                        borderRadius: 4,
                        minWidth: 0,
                        '&:hover': { bgcolor: 'var(--background-modifier-hover)' }
                      }}
                    >
                      {msg.citations.length} source{msg.citations.length !== 1 ? 's' : ''}
                    </Button>
                  )}

                  <Box sx={{ flexGrow: 1 }} />

                  {/* Action Buttons */}
                  <Box sx={{ display: 'flex', gap: 1 }}>
                    <IconButton size="small" sx={{ p: 0.5, color: 'var(--text-muted)' }} onClick={() => navigator.clipboard.writeText(msg.text)}>
                      <CopyAllIcon sx={{ fontSize: 16 }} />
                    </IconButton>
                    <IconButton size="small" sx={{ p: 0.5, color: 'var(--text-muted)' }}>
                      <ThumbUpOutlinedIcon sx={{ fontSize: 16 }} />
                    </IconButton>
                    <IconButton size="small" sx={{ p: 0.5, color: 'var(--text-muted)' }}>
                      <ThumbDownOutlinedIcon sx={{ fontSize: 16 }} />
                    </IconButton>
                  </Box>
                </Box>
              )}
            </Box>
          ))}

          {isChatting && (
            <Box sx={{ display: 'flex', mb: 3 }}>
              <CircularProgress size={16} sx={{ color: 'var(--text-accent)' }} />
            </Box>
          )}
          <div ref={messagesEndRef} />
        </Box>

        {/* 2. Floating Input Card Area */}
        <Box sx={{
          position: 'absolute',
          bottom: 0,
          left: 0,
          right: 0,
          p: 2,
          background: 'linear-gradient(to top, var(--background-primary) 80%, transparent)'
        }}>

          {/* Citations Overlay (Stacks above input) */}
          <Collapse in={activeCitations.length > 0}>
            <Box sx={{
              mb: 2,
              maxHeight: '30vh',
              overflowY: 'auto',
              bgcolor: 'var(--background-secondary)',
              borderRadius: 3,
              p: 1.5,
              border: '1px solid var(--background-modifier-border)'
            }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography variant="caption" fontWeight="bold">Sources</Typography>
                <Typography variant="caption" sx={{ cursor: 'pointer', color: 'var(--text-accent)' }} onClick={() => setActiveCitations([])}>Close</Typography>
              </Box>
              <CitationList citations={activeCitations} />
            </Box>
          </Collapse>

          {/* Card Container */}
          <Paper elevation={3} sx={{
            p: 0,
            borderRadius: 4,
            border: '1px solid var(--background-modifier-border)',
            bgcolor: 'var(--background-primary)',
            overflow: 'hidden'
          }}>
            <Box sx={{ p: 1.5 }}>
              <TextField
                fullWidth
                multiline
                maxRows={4}
                placeholder="Ask agent to help you search information..."
                variant="standard"
                InputProps={{ disableUnderline: true }}
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyPress={(e) => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    handleSendMessage();
                  }
                }}
                sx={{
                  '& .MuiInputBase-input': {
                    fontSize: '0.95rem',
                    lineHeight: 1.5,
                    color: 'var(--text-normal)'
                  }
                }}
              />
            </Box>

            {/* Footer inside card */}
            <Box sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              px: 1.5,
              pb: 1.5,
              pt: 0.5
            }}>
              {/* Left: Toggles */}
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                  <Switch
                    size="small"
                    checked={isWebSearch}
                    onChange={(e) => setIsWebSearch(e.target.checked)}
                    sx={{
                      '& .MuiSwitch-switchBase.Mui-checked': { color: 'var(--text-accent)' },
                      '& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track': { backgroundColor: 'var(--text-accent)' }
                    }}
                  />
                  <Typography variant="caption" color="text.secondary">Web</Typography>
                </Box>

                <Box
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    bgcolor: 'var(--background-modifier-form-field)',
                    borderRadius: 1,
                    px: 1,
                    py: 0.5,
                    cursor: 'pointer'
                  }}
                >
                  <Typography variant="caption" sx={{ fontSize: '0.75rem', fontWeight: 600 }}>Gemini 1.5 Pro</Typography>
                </Box>
              </Box>

              {/* Right: Send Button */}
              <IconButton
                size="small"
                onClick={handleSendMessage}
                disabled={isChatting || !input.trim()}
                sx={{
                  bgcolor: input.trim() ? 'var(--interactive-accent)' : 'var(--background-modifier-form-field)',
                  color: input.trim() ? 'white' : 'var(--text-muted)',
                  width: 32,
                  height: 32,
                  '&:hover': { bgcolor: 'var(--interactive-accent-hover)' }
                }}
              >
                <ArrowUpwardIcon fontSize="small" />
              </IconButton>
            </Box>
          </Paper>

          {/* Helper text/links under card */}
          <Box sx={{ display: 'flex', justifyContent: 'center', mt: 1 }}>
            <UploadSection onFileUpload={handleFileUpload} uploadedFiles={files} isUploading={isUploading} />
          </Box>
        </Box>
      </Box>

      {/* SETTINGS / MENU VIEW (Overlay) */}
      <Fade in={isSettingsOpen}>
        <Box sx={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          bgcolor: 'var(--background-primary)',
          zIndex: 10,
          p: 2,
          display: 'flex',
          flexDirection: 'column'
        }}>
          {/* Menu Header */}
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
            <IconButton
              size="small"
              onClick={() => setIsSettingsOpen(false)}
              sx={{
                bgcolor: 'var(--background-modifier-form-field)',
                color: 'var(--text-muted)',
                borderRadius: 2,
                mr: 2,
                '&:hover': { bgcolor: 'var(--background-modifier-hover)' }
              }}
            >
              <KeyboardDoubleArrowLeftIcon fontSize="small" />
            </IconButton>
          </Box>

          {/* Menu Items */}
          <List sx={{ width: '100%', bgcolor: 'transparent' }}>

            <ListItem disablePadding sx={{ mb: 1, borderRadius: 2, '&:hover': { bgcolor: 'var(--background-modifier-hover)' } }}>
              <ListItemIcon sx={{ minWidth: 40 }}>
                <LibraryBooksIcon sx={{ color: 'var(--text-normal)' }} />
              </ListItemIcon>
              <ListItemText primary="Library" primaryTypographyProps={{ color: 'text.primary', fontWeight: 500 }} />
            </ListItem>

            <ListItem disablePadding sx={{ mb: 1, borderRadius: 2, '&:hover': { bgcolor: 'var(--background-modifier-hover)' } }}>
              <ListItemIcon sx={{ minWidth: 40 }}>
                <ChatBubbleOutlineIcon sx={{ color: 'var(--text-normal)' }} />
              </ListItemIcon>
              <ListItemText primary="AI Chat" primaryTypographyProps={{ color: 'text.primary', fontWeight: 500 }} />
            </ListItem>

            <ListItem disablePadding sx={{ mb: 1, borderRadius: 2, '&:hover': { bgcolor: 'var(--background-modifier-hover)' } }}>
              <ListItemIcon sx={{ minWidth: 40 }}>
                <PsychologyIcon sx={{ color: 'var(--text-normal)' }} />
              </ListItemIcon>
              <ListItemText primary="models" primaryTypographyProps={{ color: 'text.primary', fontWeight: 500 }} />
            </ListItem>

            <ListItem disablePadding sx={{ mb: 1, borderRadius: 2, '&:hover': { bgcolor: 'var(--background-modifier-hover)' } }}>
              <ListItemIcon sx={{ minWidth: 40 }}>
                <FormatQuoteIcon sx={{ color: 'var(--text-normal)' }} />
              </ListItemIcon>
              <ListItemText primary="Citation Style" primaryTypographyProps={{ color: 'text.primary', fontWeight: 500 }} />
            </ListItem>

          </List>

        </Box>
      </Fade>

    </Box>
  );
}

export default App;
