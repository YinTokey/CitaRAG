import { useState, useRef, useEffect } from 'react';
import { App as ObsidianApp } from 'obsidian';
import { Box, Paper, TextField, IconButton, Typography, CircularProgress, Button, Collapse, Fade, Menu, MenuItem, ListItemIcon, ListItemText, List, ListItem, Divider, LinearProgress, Tooltip } from '@mui/material';
import CopyAllIcon from '@mui/icons-material/CopyAll';
import ThumbUpOutlinedIcon from '@mui/icons-material/ThumbUpOutlined';
import ArticleOutlinedIcon from '@mui/icons-material/ArticleOutlined';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import DriveFileRenameOutlineIcon from '@mui/icons-material/DriveFileRenameOutline';
import HistoryIcon from '@mui/icons-material/History';
import MoreHorizIcon from '@mui/icons-material/MoreHoriz';
import AddIcon from '@mui/icons-material/Add';
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
import DocumentPreview from './components/DocumentPreview';
import { Document } from './types';
import './App.css';

interface Message {
  text: string;
  sender: 'user' | 'bot';
  citations?: any[];
}

interface UploadItem {
  id: string;
  file: File;
  progress: number;
  status: 'pending' | 'uploading' | 'processing' | 'completed' | 'error';
  error?: string;
}

type ViewState = 'chat' | 'menu' | 'library';

const AVAILABLE_MODELS = [
  {
    id: 'gpt-5-mini',
    name: 'GPT-5 Mini'
  },
  {
    id: 'gpt-5-nano',
    name: 'GPT-5 Nano',
  }
];


interface AppProps {
  app?: ObsidianApp;
}

function App({ app }: AppProps) {
  const [files, setFiles] = useState<File[]>([]);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');

  // Upload State
  const [uploadQueue, setUploadQueue] = useState<UploadItem[]>([]);
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [isChatting, setIsChatting] = useState(false);
  const [activeCitations, setActiveCitations] = useState<any[]>([]);
  const [plusMenuAnchor, setPlusMenuAnchor] = useState<null | HTMLElement>(null);
  const [headerMenuAnchor, setHeaderMenuAnchor] = useState<null | HTMLElement>(null);
  const [modelMenuAnchor, setModelMenuAnchor] = useState<null | HTMLElement>(null);
  const [selectedModel, setSelectedModel] = useState<any>(null);


  // Navigation State
  const [currentView, setCurrentView] = useState<ViewState>('chat');

  // Preview State
  const [previewDoc, setPreviewDoc] = useState<Document | null>(null);
  const [previewHighlight, setPreviewHighlight] = useState<string>('');
  const [previewPage, setPreviewPage] = useState<number>(1);
  const [isPreviewLoading, setIsPreviewLoading] = useState(false);

  const messagesEndRef = useRef<null | HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  useEffect(() => {
    // Automatically select the first model
    if (AVAILABLE_MODELS.length > 0) {
      setSelectedModel(AVAILABLE_MODELS[0]);
    }
  }, []);

  // Polling Effect
  useEffect(() => {
    const interval = setInterval(async () => {
      const processingItems = uploadQueue.filter(item => item.status === 'processing');
      if (processingItems.length === 0) return;

      const updatedQueue = [...uploadQueue];
      let hasChanges = false;

      for (const item of processingItems) {
        try {
          const res = await fetch(`http://localhost:8080/api/documents/${item.id}`);
          if (res.ok) {
            const doc = await res.json();
            // Backend status: PENDING, PROCESSING, COMPLETED, FAILED
            const status = doc.processingStatus?.toLowerCase() || 'pending';

            if (status === 'completed') {
              const index = updatedQueue.findIndex(i => i.id === item.id);
              if (index !== -1) {
                updatedQueue[index] = { ...updatedQueue[index], status: 'completed', progress: 100 };
                hasChanges = true;
              }
            } else if (status === 'failed') {
              const index = updatedQueue.findIndex(i => i.id === item.id);
              if (index !== -1) {
                updatedQueue[index] = { ...updatedQueue[index], status: 'error', error: doc.errorMessage || 'Processing failed' };
                hasChanges = true;
              }
            } else {
              // Update Progress
              const backendProgress = doc.processingProgress || 0;
              const index = updatedQueue.findIndex(i => i.id === item.id);
              if (index !== -1 && updatedQueue[index].progress !== backendProgress) {
                updatedQueue[index] = { ...updatedQueue[index], progress: backendProgress };
                hasChanges = true;
              }
            }
          }
        } catch (e) {
          console.error("Polling error", e);
        }
      }

      if (hasChanges) {
        setUploadQueue(updatedQueue);
      }

    }, 1000); // Poll every 1 second

    return () => clearInterval(interval);
  }, [uploadQueue]);

  // Queue processing effect (Initiator)
  useEffect(() => {
    const pending = uploadQueue.filter(item => item.status === 'pending');
    const uploading = uploadQueue.filter(item => item.status === 'uploading');

    // Allow up to 3 concurrent network uploads
    if (pending.length > 0 && uploading.length < 3) {
      const toUpload = pending.slice(0, 3 - uploading.length);
      toUpload.forEach(item => performUpload(item));
    }
  }, [uploadQueue]);

  const performUpload = async (item: UploadItem) => {
    const file = item.file;

    // Update status to uploading
    setUploadQueue(prev => prev.map(i => i.id === item.id ? { ...i, status: 'uploading', progress: 10 } : i));

    try {
      // If running in Obsidian, save a copy to the local plugin folder for preview
      if (app) {
        try {
          const configDir = app.vault.configDir;
          const targetDir = `${configDir}/plugins/citarag/files`;

          // Ensure directory exists
          if (!(await app.vault.adapter.exists(targetDir))) {
            await app.vault.adapter.mkdir(targetDir);
          }

          // Update progress
          setUploadQueue(prev => prev.map(i => i.id === item.id ? { ...i, progress: 30 } : i));

          const arrayBuffer = await file.arrayBuffer();
          const targetPath = `${targetDir}/${file.name}`;
          await app.vault.adapter.writeBinary(targetPath, arrayBuffer);
          console.log(`Saved file copy to ${targetPath}`);

          // Delete source file
          try {
            const fs = (window as any).require('fs');

            // Dynamic path injected by esbuild
            let rootDir = '';
            // @ts-ignore
          } catch (delErr) {
            console.warn("Could not delete source file:", delErr);
          };

        } catch (err) {
          console.error("Failed to save local file copy for preview", err);
        }
      }

      // Backend Upload
      const formData = new FormData();
      formData.append('file', file); // Backend expects 'file'

      // Progress simulation for network upload
      setUploadQueue(prev => prev.map(i => i.id === item.id ? { ...i, progress: 60 } : i));

      const response = await fetch('http://localhost:8080/api/documents/upload', {
        method: 'POST',
        body: formData,
      });

      if (response.ok) {
        const doc = await response.json();
        // Success - network upload done. Now waiting for processing.
        // We use the returned ID for polling.
        // Update item ID to match backend ID for polling?
        // Current item.id is random string. We should store backend ID.
        // But we used item.id as key.
        // Let's update the item to store the meaningful ID or just replace it?
        // Replacing ID might break keys if used in map. 
        // Let's just update the ID in the queue object to match backend ID 
        setUploadQueue(prev => prev.map(i => i.id === item.id ? { ...i, id: doc.id.toString(), status: 'processing', progress: 5 } : i));

      } else {
        throw new Error('Upload failed');
      }
    } catch (error) {
      console.error('Error uploading file:', error);
      setUploadQueue(prev => prev.map(i => i.id === item.id ? { ...i, status: 'error', error: 'Upload failed' } : i));
    }
  };

  const handleFileUpload = (newFiles: File[]) => {
    const newItems: UploadItem[] = newFiles.map(f => ({
      id: Math.random().toString(36).substr(2, 9),
      file: f,
      progress: 0,
      status: 'pending'
    }));
    setUploadQueue(prev => [...prev, ...newItems]);
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
    setSelectedModel(model);
  };

  const handleCancelDownload = () => {
    // Left empty since downloading models is obsolete
  };

  const handleNewChat = () => {
    setMessages([]);
    setInput('');
    setActiveCitations([]);
  };

  const handleCitationClick = async (citation: any) => {
    // 1. Identify Document
    const filename = citation.metadata?.filename || citation.metadata?.source;
    if (!filename) return;

    setIsPreviewLoading(true);
    setPreviewDoc(null);
    setPreviewHighlight(citation.text);

    // Extract page number if available
    let page = 1;
    if (citation.metadata?.page_number) {
      page = parseInt(citation.metadata.page_number, 10) || 1;
    }
    setPreviewPage(page);

    try {
      // 2. Fetch all docs (summary) to find ID
      const res = await fetch('http://localhost:8080/api/documents');
      const allDocs: Document[] = await res.json();
      const found = allDocs.find(d => d.filename === filename);

      if (found) {
        // 3. Fetch full document content using the ID
        const fullRes = await fetch(`http://localhost:8080/api/documents/${found.id}`);
        if (fullRes.ok) {
          const fullDoc = await fullRes.json();
          setPreviewDoc(fullDoc);
        } else {
          console.error("Failed to fetch full document");
        }
      } else {
        console.warn("Document not found in library:", filename);
      }
    } catch (e) {
      console.error("Failed to load document for preview", e);
    } finally {
      setIsPreviewLoading(false);
    }
  };

  const handleLibraryPreview = async (doc: Document) => {
    setIsPreviewLoading(true);
    setPreviewDoc(null);
    setPreviewHighlight('');
    setPreviewPage(1);

    try {
      // Fetch full doc just in case
      const fullRes = await fetch(`http://localhost:8080/api/documents/${doc.id}`);
      if (fullRes.ok) {
        const fullDoc = await fullRes.json();
        setPreviewDoc(fullDoc);
      }
    } catch (e) {
      console.error(e);
    } finally {
      setIsPreviewLoading(false);
    }
  };

  const renderMessageContent = (msg: Message) => {
    // Helper to process text and wrap citations
    const processText = (text: string) => {
      if (!msg.citations || msg.citations.length === 0) return text;

      const regex = /\(([^)]+?),\s*(\d{4}[a-z]?)\)/g;
      const parts = [];
      let lastIndex = 0;
      let match;

      while ((match = regex.exec(text)) !== null) {
        if (match.index > lastIndex) {
          parts.push(text.substring(lastIndex, match.index));
        }

        const fullMatch = match[0];
        const author = match[1];
        const year = match[2];

        // Find matching citation
        const relevantCitation = msg.citations.find(cit => {
          const meta = cit.metadata || {};
          const cAuthor = meta.author || meta.Author || meta.creator || "";
          const cDate = meta.date || meta.year || meta.publicationDate || meta.created || "";

          if (!cAuthor) return false;

          const authorMatch = cAuthor.toLowerCase().includes(author.split(' ')[0].toLowerCase());
          const yearMatch = cDate.includes(year);

          return authorMatch; // Prioritize author match
        });

        if (relevantCitation) {
          parts.push(
            <Tooltip key={match.index} title={
              <Box sx={{ p: 1 }}>
                <Typography variant="subtitle2" fontWeight="bold" sx={{ color: 'white' }}>{relevantCitation.metadata?.title}</Typography>
                <Typography variant="caption" sx={{ color: '#ddd' }}>Click to preview</Typography>
              </Box>
            } arrow>
              <span
                style={{
                  color: '#4f46e5',
                  cursor: 'pointer',
                  fontWeight: 600,
                  backgroundColor: 'rgba(79, 70, 229, 0.1)',
                  padding: '2px 4px',
                  borderRadius: '4px'
                }}
                onClick={() => handleCitationClick(relevantCitation)}
              >
                {fullMatch}
              </span>
            </Tooltip>
          );
        } else {
          parts.push(fullMatch);
        }

        lastIndex = regex.lastIndex;
      }

      if (lastIndex < text.length) {
        parts.push(text.substring(lastIndex));
      }

      return parts;
    };

    return (
      <ReactMarkdown
        remarkPlugins={[remarkGfm, remarkMath]}
        rehypePlugins={[rehypeKatex]}
        components={{
          p: ({ children }) => {
            return <p>{Array.isArray(children) ? children.map((child, i) => {
              if (typeof child === 'string') return <span key={i}>{processText(child)}</span>;
              return <span key={i}>{child}</span>;
            }) : (typeof children === 'string' ? processText(children) : children)}</p>;
          },
          li: ({ children }) => {
            return <li>{Array.isArray(children) ? children.map((child, i) => {
              if (typeof child === 'string') return <span key={i}>{processText(child)}</span>;
              return <span key={i}>{child}</span>;
            }) : (typeof children === 'string' ? processText(children) : children)}</li>;
          }
        }}
      >
        {msg.text}
      </ReactMarkdown>
    );
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
        <Box sx={{ flexGrow: 1, p: 2, overflowY: 'auto', pt: 8, display: 'flex', flexDirection: 'column' }}>
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
                  {renderMessageContent(msg)}
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

          {/* Upload Queue Progress */}


          <div ref={messagesEndRef} />
        </Box>

        {/* 2. Fixed Bottom Input Area */}
        <Box sx={{ p: 2, pt: 0, bgcolor: 'transparent', width: '100%', maxWidth: '900px', mx: 'auto' }}>
          <Collapse in={activeCitations.length > 0}>
            <Box sx={{
              mb: 1,
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
      <Fade in={currentView === 'library'} unmountOnExit>
        <Box sx={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          zIndex: 20
        }}>
          <LibraryView
            onUpload={handleFileUpload}
            onBack={() => setCurrentView('chat')}
            isUploading={uploadQueue.some(item => item.status === 'uploading')}
            uploadQueue={uploadQueue}
            onClearQueue={() => setUploadQueue([])}
            isUploadOpen={isUploadOpen}
            setIsUploadOpen={setIsUploadOpen}
            onPreview={handleLibraryPreview}
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



      <DocumentPreview
        document={previewDoc}
        highlightText={previewHighlight}
        initialPage={previewPage}
        isLoading={isPreviewLoading}
        onClose={() => setPreviewDoc(null)}
        app={app}
      />
    </Box>
  );
}

export default App;
