import {
  Box, Button, Chip, CircularProgress, Divider, Drawer, IconButton, List, ListItem,
  Stack, TextField, Typography, Alert,
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import SendIcon from '@mui/icons-material/Send';
import SupportAgentIcon from '@mui/icons-material/SupportAgent';
import { useState } from 'react';
import {
  chatWithHelpingAgent,
  type HelpingAgentChatResponse,
  type HelpingAgentContext,
} from '../../api/helpingAgentApi';
import { getErrorMessage } from '../../api/httpClient';

type Message = { role: 'user' | 'assistant'; text: string; meta?: HelpingAgentChatResponse };

type Props = {
  open: boolean;
  onClose: () => void;
  context?: HelpingAgentContext;
  suggestedPrompts?: string[];
};

const DEFAULT_PROMPTS = [
  'Explain this cost breakdown',
  'What is driving the cost?',
  'What is missing for a complete cost?',
  'How is the should-cost built?',
  'Why are we above or below target?',
  'What changed from the previous revision?',
  'How can I investigate cost reduction?',
];

export function HelpingAgentPanel({ open, onClose, context, suggestedPrompts }: Props) {
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [messages, setMessages] = useState<Message[]>([{
    role: 'assistant',
    text: 'You opened Costing Help. I stay silent until you ask. I can explain Product Costing data you are authorized to see (BOM, material, process, tooling, should-cost, targets, quotes, versions). I cannot change costs or approve workflows. Type a question or use a suggestion below.',
  }]);
  const prompts = suggestedPrompts?.length ? suggestedPrompts : DEFAULT_PROMPTS;

  async function send(question: string) {
    const q = question.trim();
    if (!q || loading) return;
    setError(null);
    setMessages((m) => [...m, { role: 'user', text: q }]);
    setInput('');
    setLoading(true);
    try {
      const res = await chatWithHelpingAgent(q, context);
      setMessages((m) => [...m, { role: 'assistant', text: res.answer, meta: res }]);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <Drawer anchor="right" open={open} onClose={onClose} PaperProps={{ sx: { width: { xs: '100%', sm: 420 } } }}>
      <Box sx={{ p: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
        <SupportAgentIcon color="primary" />
        <Typography variant="h6" sx={{ flex: 1 }}>Costing Help</Typography>
        <IconButton onClick={onClose} aria-label="Close helping agent"><CloseIcon /></IconButton>
      </Box>
      <Divider />
      <Box sx={{ px: 2, py: 1 }}>
        <Typography variant="caption" color="text.secondary">
          Read-only · Opens only when you ask · Product Costing data only · Sources when available
        </Typography>
      </Box>
      <Stack direction="row" flexWrap="wrap" gap={0.5} sx={{ px: 2, pb: 1 }}>
        {prompts.map((p) => (
          <Chip key={p} size="small" label={p} onClick={() => send(p)} disabled={loading} />
        ))}
      </Stack>
      <Divider />
      <List sx={{ flex: 1, overflow: 'auto', px: 1 }}>
        {messages.map((msg, idx) => (
          <ListItem key={idx} alignItems="flex-start" sx={{ flexDirection: 'column', gap: 0.5 }}>
            <Typography variant="caption" color="text.secondary">{msg.role === 'user' ? 'You' : 'Assistant'}</Typography>
            <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>{msg.text}</Typography>
            {msg.meta && (
              <Stack direction="row" gap={0.5} flexWrap="wrap">
                <Chip size="small" label={msg.meta.classification} />
                <Chip size="small" label={`Confidence ${msg.meta.confidence}`} />
                {msg.meta.sources?.map((s) => (
                  <Chip key={`${s.type}-${s.id}`} size="small" variant="outlined" label={s.label || s.id} />
                ))}
              </Stack>
            )}
            {msg.meta?.missingData?.length ? (
              <Alert severity="warning" sx={{ width: '100%', mt: 0.5 }}>{msg.meta.missingData.join(' · ')}</Alert>
            ) : null}
          </ListItem>
        ))}
        {loading && <ListItem><CircularProgress size={22} /></ListItem>}
      </List>
      {error && <Alert severity="error" sx={{ mx: 2, mb: 1 }}>{error}</Alert>}
      <Box sx={{ p: 2, display: 'flex', gap: 1 }}>
        <TextField fullWidth size="small" placeholder="Ask about cost, BOM, should-cost…"
          value={input} onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); void send(input); } }}
          disabled={loading} />
        <Button variant="contained" endIcon={<SendIcon />} onClick={() => void send(input)} disabled={loading || !input.trim()}>
          Send
        </Button>
      </Box>
    </Drawer>
  );
}
