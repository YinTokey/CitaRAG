import { useState, useRef, useEffect } from 'react';
import { Box, Paper, TextField, IconButton, Typography, CircularProgress, Button, Collapse, Fade, Menu, MenuItem, ListItemIcon, ListItemText, List, ListItem, Divider, LinearProgress } from '@mui/material';
import CopyAllIcon from '@mui/icons-material/CopyAll';
import ThumbUpOutlinedIcon from '@mui/icons-material/ThumbUpOutlined';
import ArticleOutlinedIcon from '@mui/icons-material/ArticleOutlined';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import DriveFileRenameOutlineIcon from '@mui/icons-material/DriveFileRenameOutline';
import HistoryIcon from '@mui/icons-material/History';
import MoreHorizIcon from '@mui/icons-material/MoreHoriz';
import AddIcon from '@mui/icons-material/Add';
import LanguageIcon from '@mui/icons-material/Language';
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined';
import LibraryBooksIcon from '@mui/icons-material/LibraryBooks';
import KeyboardDoubleArrowLeftIcon from '@mui/icons-material/KeyboardDoubleArrowLeft';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import remarkMath from 'remark-math';
import rehypeKatex from 'rehype-katex';

import CitationList from './components/CitationList';
import LibraryView from './components/LibraryView';
import './App.css';

interface Message {
  text: string;
  sender: 'user' | 'bot';
  citations?: any[];
}

type ViewState = 'chat' | 'menu' | 'library';

const AVAILABLE_MODELS = [
  {
    id: 'llama3.2:1b',
    name: 'LLaMA 3.2 1B',       // ~1.3 GB - fastest
  },
  {
    id: 'gemma2:2b',
    name: 'Gemma 2 2B',          // ~1.6 GB - good quality/size ratio
  },
  {
    id: 'qwen2.5:1.5b',
    name: 'Qwen 2.5 1.5B',       // ~1 GB - great for the size
  },
];

function App() {
  const [files, setFiles] = useState<File[]>([]);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [isUploading, setIsUploading] = useState(false);
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [isChatting, setIsChatting] = useState(false);
  const [activeCitations, setActiveCitations] = useState<any[]>([]);
  const [isWebSearch, setIsWebSearch] = useState(false);
  const [plusMenuAnchor, setPlusMenuAnchor] = useState<null | HTMLElement>(null);
  const [headerMenuAnchor, setHeaderMenuAnchor] = useState<null | HTMLElement>(null);
  const [modelMenuAnchor, setModelMenuAnchor] = useState<null | HTMLElement>(null);
  const [selectedModel, setSelectedModel] = useState<any>(null);

  // Model Download State
  const [isDownloadingModel, setIsDownloadingModel] = useState(false);
  const [downloadProgress, setDownloadProgress] = useState(0);
  const [downloadingModelName, setDownloadingModelName] = useState<string>('');
  const [downloadAbortController, setDownloadAbortController] = useState<AbortController | null>(null);

  // Navigation State
  const [currentView, setCurrentView] = useState<ViewState>('chat');

  const messagesEndRef = useRef<null | HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  useEffect(() => {
    const checkModels = async () => {
      try {
        const response = await fetch('http://localhost:8080/api/models');
        if (response.ok) {
          const existingModels = await response.json();
          if (existingModels && existingModels.length > 0) {
            for (const avail of AVAILABLE_MODELS) {
              const modelName = avail.id.split(':')[0];
              if (existingModels.some((m: string) => m.includes(modelName))) {
                setSelectedModel(avail);
                return;
              }
            }
          }
        }
      } catch (e) {
        console.error("Failed to fetch initial models", e);
      }
    };
    checkModels();
  }, []);

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
        body: JSON.stringify({
          query: userMsg.text,
          web_search: isWebSearch,
          model: selectedModel ? selectedModel.id : 'phi3:mini'
        }),
      });

      if (!response.ok) {
        throw new Error('Network response was not ok');
      }

      const reader = response.body?.getReader();
      if (!reader) throw new Error('Response body is null');

      const decoder = new TextDecoder();
      let botMsg: Message = { text: '', sender: 'bot', citations: [] };
      setMessages((prev) => [...prev, botMsg]);

      let buffer = '';
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });

        const parts = buffer.split('\n\n');
        buffer = parts.pop() || '';

        for (const part of parts) {
          const lines = part.split('\n');
          let eventData = '';
          let eventType = 'message';

          for (const line of lines) {
            if (line.startsWith('event:')) {
              eventType = line.slice(6).trim();
            } else if (line.startsWith('data:')) {
              eventData += (eventData ? '\n' : '') + line.slice(5);
            }
          }

          if (!eventData && eventType === 'message') continue;

          try {
            if (eventData.startsWith('{') || eventData.startsWith('[')) {
              try {
                const event = JSON.parse(eventData);
                if (event.type === 'citations') {
                  botMsg.citations = event.data;
                  setMessages((prev) => {
                    const newMessages = [...prev];
                    newMessages[newMessages.length - 1] = { ...botMsg };
                    return newMessages;
                  });
                  continue;
                }
              } catch (e) {
                // Not valid JSON, process as text
              }
            }

            botMsg.text += eventData;
            setMessages((prev) => {
              const newMessages = [...prev];
              newMessages[newMessages.length - 1] = { ...botMsg };
              return newMessages;
            });
          } catch (e) {
            console.error("Error processing event", e, eventData);
            botMsg.text += eventData;
          }
        }
      }

    } catch (error) {
      console.error('Chat error:', error);
      setMessages((prev) => [...prev, { text: "Network error. Please try again.", sender: 'bot' }]);
    } finally {
      setIsChatting(false);
    }
  };

  const handleHeaderMoreClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setHeaderMenuAnchor(event.currentTarget);
  };

  const handleHeaderMenuClose = () => {
    setHeaderMenuAnchor(null);
  };

  const handlePlusClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setPlusMenuAnchor(event.currentTarget);
  };

  const handlePlusMenuClose = () => {
    setPlusMenuAnchor(null);
  };

  const handleUploadClick = () => {
    handlePlusMenuClose();
    setCurrentView('library');
    setTimeout(() => setIsUploadOpen(true), 100);
  };

  const handleLibraryClick = () => {
    handlePlusMenuClose();
    setCurrentView('library');
  };

  const handleModelClick = (event: React.MouseEvent<HTMLElement>) => {
    setModelMenuAnchor(event.currentTarget);
  };

  const handleModelClose = () => {
    setModelMenuAnchor(null);
  };

  const handleModelSelect = async (modelId: string) => {
    const model = AVAILABLE_MODELS.find(m => m.id === modelId);
    if (!model) return;

    handleModelClose();

    // check if model exists
    try {
      const response = await fetch('http://localhost:8080/api/models');
      const models = await response.json();
      const modelName = model.id.split(':')[0]; // Simple check

      const exists = models.some((m: string) => m.includes(modelName));

      if (exists) {
        setSelectedModel(model);
      } else {
        // Pull model
        const abortController = new AbortController();
        setDownloadAbortController(abortController);
        setIsDownloadingModel(true);
        setDownloadProgress(0);
        setDownloadingModelName(model.name);
        try {
          const pullResponse = await fetch('http://localhost:8080/api/models/pull', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: model.id }),
            signal: abortController.signal
          });

          if (!pullResponse.ok) throw new Error("Failed to pull model");

          const reader = pullResponse.body?.getReader();
          const decoder = new TextDecoder();

          if (reader) {
            while (true) {
              const { done, value } = await reader.read();
              if (done) break;

              const chunk = decoder.decode(value, { stream: true });
              const lines = chunk.split('\n').filter(line => line.trim() !== '');

              for (const line of lines) {
                try {
                  const jsonStr = line.startsWith('data:') ? line.slice(5) : line;
                  const data = JSON.parse(jsonStr);

                  // Log for debugging
                  console.log('Ollama pull progress:', data);

                  // Handle all possible status types from Ollama
                  if (data.status) {
                    // Check for completion
                    if (data.status === 'success' || data.status.includes('success')) {
                      setDownloadProgress(100);
                    }
                    // Check for progress with total/completed
                    else if (data.total && data.completed) {
                      const progress = (data.completed / data.total) * 100;
                      setDownloadProgress(progress);
                    }
                    // If downloading/pulling but no progress info, show indeterminate (50%)
                    else if (data.status.includes('downloading') ||
                      data.status.includes('pulling') ||
                      data.status.includes('verifying')) {
                      // Keep progress bar visible but indeterminate
                      if (downloadProgress === 0) {
                        setDownloadProgress(10); // Show some activity
                      }
                    }
                  }
                } catch (e) {
                  console.warn('Failed to parse progress:', line);
                }
              }
            }
          }

          setSelectedModel(model);
        } catch (e) {
          console.error("Failed to download model", e);
          alert("Failed to download model " + model.name);
        } finally {
          setIsDownloadingModel(false);
          setDownloadProgress(0);
          setDownloadingModelName('');
          setDownloadAbortController(null);
        }
      }
    } catch (e) {
      console.error("Error checking/pulling model", e);
      setSelectedModel(model); // Optimistic fallback
    }
  };

  const handleCancelDownload = () => {
    if (downloadAbortController) {
      downloadAbortController.abort();
      setIsDownloadingModel(false);
      setDownloadProgress(0);
      setDownloadingModelName('');
      setDownloadAbortController(null);
    }
  };

  const handleNewChat = () => {
    setMessages([]);
    setInput('');
    setActiveCitations([]);
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', bgcolor: 'transparent', position: 'relative', overflow: 'hidden' }}>

      {/* HEADER */}
      {currentView === 'chat' && (
        <Box sx={{
          position: 'absolute',
          top: 14,
          right: 14,
          left: 14,
          zIndex: 5,
          display: 'flex',
          gap: 1.5,
          alignItems: 'center',
          justifyContent: 'flex-end'
        }}>
          {isDownloadingModel && (
            <Fade in={isDownloadingModel}>
              <Box sx={{
                mr: 'auto', // push to left
                display: 'flex',
                alignItems: 'center',
                gap: 1,
                bgcolor: 'var(--background-primary)',
                p: 0.5,
                px: 1.5,
                borderRadius: 2,
                border: '1px solid var(--background-modifier-border)',
                boxShadow: '0 1px 2px rgba(0,0,0,0.05)'
              }}>
                <CircularProgress size={14} thickness={5} />
                <Typography variant="caption" sx={{ fontWeight: 600, color: 'var(--text-normal)' }}>
                  Downloading {downloadingModelName || "Model"}...
                </Typography>
                <Box sx={{ width: 100, ml: 1 }}>
                  <LinearProgress variant="determinate" value={downloadProgress} sx={{ height: 4, borderRadius: 2 }} />
                </Box>
                <Typography variant="caption" color="text.secondary" sx={{ minWidth: 35 }}>
                  {Math.round(downloadProgress)}%
                </Typography>
                <IconButton
                  size="small"
                  onClick={handleCancelDownload}
                  sx={{
                    ml: 0.5,
                    width: 20,
                    height: 20,
                    '&:hover': { bgcolor: 'var(--background-modifier-hover)' }
                  }}
                >
                  <CloseIcon sx={{ fontSize: 14 }} />
                </IconButton>
              </Box>
            </Fade>
          )}
          <IconButton
            size="small"
            onClick={handleNewChat}
            sx={{
              bgcolor: 'var(--background-primary)',
              color: 'var(--text-normal)',
              border: '1px solid var(--background-modifier-border)',
              borderRadius: '8px',
              width: 32,
              height: 32,
              minWidth: 32,
              p: 0,
              '&:hover': { bgcolor: 'var(--background-modifier-hover)' },
              boxShadow: '0 1px 2px rgba(0,0,0,0.05)'
            }}
          >
            <DriveFileRenameOutlineIcon sx={{ fontSize: 16 }} />
          </IconButton>

          <IconButton
            size="small"
            sx={{
              bgcolor: 'var(--background-primary)',
              color: 'var(--text-normal)',
              border: '1px solid var(--background-modifier-border)',
              borderRadius: '8px',
              width: 32,
              height: 32,
              p: 0,
              '&:hover': { bgcolor: 'var(--background-modifier-hover)' },
              boxShadow: '0 1px 2px rgba(0,0,0,0.05)'
            }}
          >
            <HistoryIcon sx={{ fontSize: 16 }} />
          </IconButton>

          <IconButton
            size="small"
            onClick={handleHeaderMoreClick}
            sx={{
              bgcolor: 'var(--background-primary)',
              color: 'var(--text-normal)',
              border: '1px solid var(--background-modifier-border)',
              borderRadius: '8px',
              width: 32,
              height: 32,
              p: 0,
              '&:hover': { bgcolor: 'var(--background-modifier-hover)' },
              boxShadow: '0 1px 2px rgba(0,0,0,0.05)'
            }}
          >
            <MoreHorizIcon sx={{ fontSize: 16 }} />
          </IconButton>
        </Box>
      )}

      {/* HEADER MENU */}
      <Menu
        anchorEl={headerMenuAnchor}
        open={Boolean(headerMenuAnchor)}
        onClose={handleHeaderMenuClose}
        PaperProps={{
          sx: {
            bgcolor: 'var(--background-primary)',
            border: '1px solid var(--background-modifier-border)',
            borderRadius: 2,
            minWidth: 150
          }
        }}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <MenuItem onClick={handleHeaderMenuClose} sx={{ fontSize: '0.9rem' }}>Settings</MenuItem>
      </Menu>

      {/* MAIN VIEW: Chat & Input */}
      <Box sx={{
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        opacity: currentView === 'chat' ? 1 : 0,
        pointerEvents: currentView === 'chat' ? 'auto' : 'none',
        transition: 'opacity 0.2s',
        visibility: currentView === 'chat' ? 'visible' : 'hidden'
      }}>
        {/* 1. Scrollable Chat Area */}
        <Box sx={{ flexGrow: 1, p: 2, overflowY: 'auto', pb: 24, pt: 8 }}>
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
              {/* Message Bubble/Text */}
              <Box sx={{
                p: msg.sender === 'user' ? 1.5 : 0,
                borderRadius: msg.sender === 'user' ? '18px 18px 4px 18px' : 0,
                bgcolor: msg.sender === 'user' ? '#6366f1' : 'transparent',
                color: msg.sender === 'user' ? 'white' : 'var(--text-normal)',
                maxWidth: '85%',
                boxShadow: msg.sender === 'user' ? '0 2px 4px rgba(0,0,0,0.05)' : 'none',
                mb: 1,
                userSelect: 'text', // Enable text selection
                width: msg.sender === 'bot' ? '100%' : 'auto'
              }}>
                <Box sx={{
                  fontFamily: 'inherit',
                  '& p': { m: 0, mb: 2, lineHeight: 1.6 },
                  '& p:last-child': { mb: 0 },
                  '& ul, & ol': {
                    m: 0,
                    pl: 3,
                    mb: 2,
                    '& li': {
                      display: 'list-item',
                      listStyleType: 'disc',
                      mb: 1,
                      lineHeight: 1.6
                    }
                  },
                  '& li > p': { mb: 0.5 },
                  '& code': { bgcolor: 'rgba(0,0,0,0.06)', px: 0.8, py: 0.2, borderRadius: 1, fontFamily: 'monospace', fontSize: '0.85em' },
                  '& pre': { bgcolor: '#f5f5f5', p: 1.5, borderRadius: 2, overflowX: 'auto', my: 2, border: '1px solid rgba(0,0,0,0.05)', '& code': { bgcolor: 'transparent', p: 0, fontSize: '0.9em' } },
                  '& h1, & h2, & h3, & h4': { mt: 3, mb: 1.5, fontWeight: 600, color: 'var(--header-text)' },
                  '& h1': { fontSize: '1.4em' },
                  '& h2': { fontSize: '1.25em' },
                  '& h3': { fontSize: '1.1em' },
                  '& blockquote': { borderLeft: '4px solid #ddd', m: 0, pl: 2, color: 'text.secondary', my: 2 },
                  '& table': { borderCollapse: 'collapse', width: '100%', mb: 2 },
                  '& th, & td': { border: '1px solid #ddd', p: 1, textAlign: 'left' },
                  '& th': { bgcolor: '#f8f9fa' }
                }}>
                  <ReactMarkdown
                    remarkPlugins={[remarkGfm, remarkMath]}
                    rehypePlugins={[rehypeKatex]}
                  >
                    {msg.text}
                  </ReactMarkdown>
                </Box>
              </Box>

              {/* Citations & Actions Row (Bot Only) */}
              {msg.sender === 'bot' && (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, width: '100%', ml: 1 }}>
                  {msg.citations && msg.citations.length > 0 && (
                    <Button
                      startIcon={<ArticleOutlinedIcon sx={{ fontSize: 14 }} />}
                      size="small"
                      onClick={() => setActiveCitations(activeCitations === msg.citations ? [] : msg.citations!)}
                      sx={{
                        textTransform: 'none',
                        color: 'var(--text-accent)',
                        bgcolor: 'transparent',
                        fontSize: '0.7rem',
                        py: 0,
                        px: 1,
                        minWidth: 0,
                        '&:hover': { bgcolor: 'var(--background-modifier-hover)' }
                      }}
                    >
                      {msg.citations.length} sources
                    </Button>
                  )}

                  <Box sx={{ flexGrow: 1 }} />

                  {/* Action Buttons */}
                  <Box sx={{ display: 'flex', gap: 0.5 }}>
                    <IconButton size="small" sx={{ p: 0.4, color: 'var(--text-muted)' }} onClick={() => navigator.clipboard.writeText(msg.text)}>
                      <CopyAllIcon sx={{ fontSize: 14 }} />
                    </IconButton>
                    <IconButton size="small" sx={{ p: 0.4, color: 'var(--text-muted)' }}>
                      <ThumbUpOutlinedIcon sx={{ fontSize: 14 }} />
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

          <Paper elevation={0} sx={{
            p: 0,
            borderRadius: 5,
            border: '1px solid var(--background-modifier-border)',
            bgcolor: 'var(--background-primary)',
            overflow: 'hidden',
            boxShadow: '0 4px 20px rgba(0,0,0,0.08)'
          }}>
            <Box sx={{ px: 2, pt: 1.5 }}>
              <TextField
                fullWidth
                multiline
                maxRows={6}
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
                    fontSize: '0.9rem',
                    lineHeight: 1.6,
                    color: 'var(--text-normal)'
                  }
                }}
              />
            </Box>

            <Box sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              px: 1,
              pb: 1.5,
              pt: 0.5
            }}>
              {/* Left Action Icons */}
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                <IconButton
                  size="small"
                  onClick={handlePlusClick}
                  sx={{ color: 'var(--text-muted)', '&:hover': { bgcolor: 'var(--background-modifier-hover)' } }}
                >
                  <AddIcon fontSize="small" />
                </IconButton>

                <IconButton
                  size="small"
                  onClick={() => setIsWebSearch(!isWebSearch)}
                  sx={{
                    color: isWebSearch ? 'var(--text-accent)' : 'var(--text-muted)',
                    bgcolor: isWebSearch ? 'rgba(99, 102, 241, 0.1)' : 'transparent',
                    '&:hover': { bgcolor: isWebSearch ? 'rgba(99, 102, 241, 0.2)' : 'var(--background-modifier-hover)' }
                  }}
                >
                  <LanguageIcon fontSize="small" />
                </IconButton>

                <Box
                  onClick={handleModelClick}
                  sx={{
                    ml: 1,
                    px: 1,
                    py: 0.5,
                    borderRadius: 1,
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 0.5,
                    '&:hover': { bgcolor: 'var(--background-modifier-hover)' }
                  }}
                >
                  <Typography variant="caption" sx={{ fontWeight: 600, color: 'var(--text-normal)', opacity: 0.8 }}>
                    {selectedModel ? selectedModel.name : "Select Model"}
                  </Typography>
                  <ExpandMoreIcon sx={{ fontSize: 14, color: 'var(--text-muted)', opacity: 0.7 }} />
                </Box>
              </Box>

              {/* Right: Send Button */}
              <IconButton
                size="small"
                onClick={handleSendMessage}
                disabled={isChatting || !input.trim()}
                sx={{
                  color: input.trim() ? 'var(--interactive-accent)' : 'var(--text-muted)',
                  border: '1px solid var(--background-modifier-border)',
                  bgcolor: 'transparent',
                  p: 0.8,
                  '&:hover': {
                    bgcolor: 'var(--background-modifier-hover)',
                    color: 'var(--interactive-accent-hover)'
                  },
                  '&.Mui-disabled': { color: 'var(--text-muted)', opacity: 0.5 }
                }}
              >
                <ArrowUpwardIcon fontSize="small" />
              </IconButton>
            </Box>
          </Paper>

          {/* Plus Menu */}
          <Menu
            anchorEl={plusMenuAnchor}
            open={Boolean(plusMenuAnchor)}
            onClose={handlePlusMenuClose}
            elevation={3}
            anchorOrigin={{ vertical: 'top', horizontal: 'left' }}
            transformOrigin={{ vertical: 'bottom', horizontal: 'left' }}
            PaperProps={{
              sx: {
                borderRadius: 2,
                mt: -1,
                minWidth: 160,
                bgcolor: 'var(--background-primary)',
                border: '1px solid var(--background-modifier-border)'
              }
            }}
          >
            <MenuItem onClick={handleUploadClick} sx={{ py: 1 }}>
              <ListItemIcon sx={{ minWidth: '32px !important' }}>
                <CloudUploadOutlinedIcon fontSize="small" />
              </ListItemIcon>
              <ListItemText primary="Upload file" primaryTypographyProps={{ fontSize: '0.85rem' }} />
            </MenuItem>
            <MenuItem onClick={handleLibraryClick} sx={{ py: 1 }}>
              <ListItemIcon sx={{ minWidth: '32px !important' }}>
                <LibraryBooksIcon fontSize="small" />
              </ListItemIcon>
              <ListItemText primary="Library" primaryTypographyProps={{ fontSize: '0.85rem' }} />
            </MenuItem>
          </Menu>
        </Box>
      </Box>

      {/* LIBRARY VIEW (Full Overlay) */}
      <Fade in={currentView === 'library'}>
        <Box sx={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          zIndex: 20
        }}>
          <LibraryView
            files={files}
            onUpload={handleFileUpload}
            onBack={() => setCurrentView('chat')} // Back to chat
            isUploading={isUploading}
            isUploadOpen={isUploadOpen}
            setIsUploadOpen={setIsUploadOpen}
          />
        </Box>
      </Fade>

      {/* Model Selection Menu */}
      <Menu
        anchorEl={modelMenuAnchor}
        open={Boolean(modelMenuAnchor)}
        onClose={handleModelClose}
        anchorOrigin={{ vertical: 'top', horizontal: 'left' }}
        transformOrigin={{ vertical: 'bottom', horizontal: 'left' }}
        PaperProps={{
          sx: {
            borderRadius: 2,
            mt: -1,
            minWidth: 200,
            bgcolor: 'var(--background-primary)',
            border: '1px solid var(--background-modifier-border)',
            boxShadow: '0 4px 12px rgba(0,0,0,0.1)'
          }
        }}
      >
        {AVAILABLE_MODELS.map((model) => (
          <MenuItem
            key={model.id}
            onClick={() => handleModelSelect(model.id)}
            sx={{ py: 1, fontSize: '0.85rem', display: 'flex', justifyContent: 'space-between' }}
          >
            {model.name}
            {selectedModel && selectedModel.id === model.id && <CheckIcon sx={{ fontSize: 16, color: 'var(--text-accent)' }} />}
          </MenuItem>
        ))}
      </Menu>



    </Box>
  );
}

export default App;
