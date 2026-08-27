import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Paper,
  Stack,
  Tab,
  Tabs,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { httpClient } from '../../api/httpClient';
import { getErrorMessage } from '../../api/httpClient';
import { ConfigEditableRow } from '../../components/config/ConfigEditableRow';
import { useAuth } from '../../auth/useAuth';

type Template = {
  templateId: string;
  templateCode: string;
  templateName: string;
  status: string;
  versionNo: number;
  systemDefault?: boolean;
};

type Pack = {
  packId: string;
  packCode: string;
  packName: string;
  status: string;
  versionNo: number;
  purpose: string;
};

type ModulePack = {
  packCode: string;
  packName: string;
  moduleCodes: string[];
  description: string;
};

type Defaults = {
  defaultReportingCurrency?: string | null;
  defaultScrapPercent?: number | null;
  defaultOverheadPercent?: number | null;
  fxMissingPolicy?: string;
  releaseIncompletePolicy?: string;
  policyNotes?: string[];
};

type WfDef = {
  workflowDefinitionId?: string;
  name?: string;
  entityType?: string;
  active?: boolean;
};

/**
 * P3-07 Configuration Center (PLATFORM) — scorecards, questionnaire packs,
 * costing defaults, module pack catalog, workflow read model.
 */
export function PlatformConfigurationPage() {
  const { hasRole, activeCustomerId } = useAuth();
  const canEditConfig = hasRole('SYSTEM_ADMINISTRATOR', 'ADMIN', 'CUSTOMER_ADMIN', 'COSTING_MANAGER');
  const [tab, setTab] = useState(0);
  const [templates, setTemplates] = useState<Template[]>([]);
  const [packs, setPacks] = useState<Pack[]>([]);
  const [modulePacks, setModulePacks] = useState<ModulePack[]>([]);
  const [defaults, setDefaults] = useState<Defaults | null>(null);
  const [workflows, setWorkflows] = useState<WfDef[]>([]);
  const [customerId, setCustomerId] = useState('');
  const [currency, setCurrency] = useState('');
  const [scrap, setScrap] = useState('');
  const [overhead, setOverhead] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  const loadScorecards = useCallback(async () => {
    const { data } = await httpClient.get<Template[]>('/api/v1/vendor-selection/scorecards/templates');
    setTemplates(data);
  }, []);

  const loadPacks = useCallback(async () => {
    const { data } = await httpClient.get<Pack[]>('/api/v1/configuration/questionnaire-packs');
    setPacks(data);
  }, []);

  const loadModules = useCallback(async () => {
    const { data } = await httpClient.get<ModulePack[]>('/api/v1/configuration/module-packs');
    setModulePacks(data);
  }, []);

  const loadWorkflows = useCallback(async () => {
    const { data } = await httpClient.get<WfDef[]>('/api/v1/configuration/workflow-definitions');
    setWorkflows(data);
  }, []);

  const loadDefaults = useCallback(async () => {
    if (!customerId.trim()) {
      setDefaults(null);
      return;
    }
    const { data } = await httpClient.get<Defaults>('/api/v1/configuration/costing-defaults', {
      params: { customerId: customerId.trim() },
    });
    setDefaults(data);
    setCurrency(data.defaultReportingCurrency ?? '');
    setScrap(data.defaultScrapPercent != null ? String(data.defaultScrapPercent) : '');
    setOverhead(data.defaultOverheadPercent != null ? String(data.defaultOverheadPercent) : '');
  }, [customerId]);

  useEffect(() => {
    if (activeCustomerId && !customerId) {
      setCustomerId(activeCustomerId);
    }
  }, [activeCustomerId]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    void (async () => {
      try {
        await Promise.all([loadScorecards(), loadPacks(), loadModules(), loadWorkflows()]);
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Load failed');
      }
    })();
  }, [loadScorecards, loadPacks, loadModules, loadWorkflows]);

  async function publishTemplate(id: string) {
    try {
      await httpClient.post(`/api/v1/vendor-selection/scorecards/templates/${encodeURIComponent(id)}/publish`);
      setInfo('Scorecard published');
      await loadScorecards();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Publish failed');
    }
  }

  async function retireTemplate(id: string) {
    try {
      await httpClient.post(`/api/v1/vendor-selection/scorecards/templates/${encodeURIComponent(id)}/deactivate`);
      setInfo('Scorecard retired');
      await loadScorecards();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Retire failed (last ACTIVE blocked)');
    }
  }

  async function publishPack(id: string) {
    try {
      await httpClient.post(`/api/v1/configuration/questionnaire-packs/${encodeURIComponent(id)}/publish`);
      setInfo('Questionnaire pack published');
      await loadPacks();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Pack publish failed (mandatory gates?)');
    }
  }

  async function saveDefaults() {
    if (!customerId.trim()) {
      setError('customerId required');
      return;
    }
    try {
      await httpClient.put('/api/v1/configuration/costing-defaults', {
        defaultReportingCurrency: currency || undefined,
        defaultScrapPercent: scrap !== '' ? Number(scrap) : undefined,
        defaultOverheadPercent: overhead !== '' ? Number(overhead) : undefined,
      }, { params: { customerId: customerId.trim() } });
      setInfo('Costing defaults saved (FX/release policies remain system-fixed)');
      await loadDefaults();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Save defaults failed');
    }
  }

  return (
    <Box p={2}>
      <Typography variant="h5" gutterBottom>
        Configuration Center (P3-07)
      </Typography>
      <Typography variant="body2" color="text.secondary" mb={2}>
        Customer-configurable scorecards, questionnaire packs, costing defaults, module pack catalog, workflow
        definitions. Human admin only — no AI publish/approve. FX missing = NOT_AVAILABLE. Incomplete PR RELEASE =
        BLOCK.
      </Typography>
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      {info && (
        <Alert severity="info" sx={{ mb: 2 }} onClose={() => setInfo(null)}>
          {info}
        </Alert>
      )}
      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2 }}>
        <Tab label="Scorecards (7a)" />
        <Tab label="Questionnaires (7b)" />
        <Tab label="Costing defaults (7c)" />
        <Tab label="Module packs (7d)" />
        <Tab label="Workflow (7e)" />
      </Tabs>

      {tab === 0 && (
        <Paper sx={{ p: 2 }}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Code</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Ver</TableCell>
                <TableCell>Default</TableCell>
                <TableCell />
              </TableRow>
            </TableHead>
            <TableBody>
              {templates.map((t) => (
                <TableRow key={t.templateId}>
                  <TableCell>{t.templateCode}</TableCell>
                  <TableCell>{t.templateName}</TableCell>
                  <TableCell>{t.status}</TableCell>
                  <TableCell>{t.versionNo}</TableCell>
                  <TableCell>{t.systemDefault ? 'YES' : '—'}</TableCell>
                  <TableCell>
                    {t.status === 'DRAFT' && (
                      <Button size="small" onClick={() => void publishTemplate(t.templateId)}>
                        Publish
                      </Button>
                    )}
                    {t.status === 'ACTIVE' && (
                      <Button size="small" color="warning" onClick={() => void retireTemplate(t.templateId)}>
                        Retire
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      )}

      {tab === 1 && (
        <Paper sx={{ p: 2 }}>
          <Typography variant="body2" color="text.secondary" mb={1}>
            Publish requires all active mandatory-gate questions included (§11).
          </Typography>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Code</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Purpose</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Ver</TableCell>
                <TableCell />
              </TableRow>
            </TableHead>
            <TableBody>
              {packs.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6}>No packs yet — create via API POST /api/v1/configuration/questionnaire-packs</TableCell>
                </TableRow>
              )}
              {packs.map((p) => (
                <TableRow key={p.packId}>
                  <TableCell>{p.packCode}</TableCell>
                  <TableCell>{p.packName}</TableCell>
                  <TableCell>{p.purpose}</TableCell>
                  <TableCell>{p.status}</TableCell>
                  <TableCell>{p.versionNo}</TableCell>
                  <TableCell>
                    {p.status === 'DRAFT' && (
                      <Button size="small" onClick={() => void publishPack(p.packId)}>
                        Publish
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      )}

      {tab === 2 && (
        <Paper sx={{ p: 2 }}>
          <Stack spacing={2} maxWidth={640}>
            <Typography variant="subtitle1" fontWeight={600}>
              Costing Defaults
            </Typography>
            <Stack direction="row" spacing={1} alignItems="center">
              <TextField
                label="Customer ID"
                required
                size="small"
                value={customerId}
                onChange={(e) => setCustomerId(e.target.value)}
                sx={{ minWidth: 280 }}
              />
              <Button variant="outlined" onClick={() => void loadDefaults()}>
                Load
              </Button>
            </Stack>
            <Typography variant="body2" color="text.secondary">
              Click the pencil to edit one value. FX and release policies are system-fixed (not editable).
            </Typography>
            <Box>
              <ConfigEditableRow
                label="Default Currency"
                displayValue={currency || '—'}
                value={currency}
                fieldType="text"
                canEdit={canEditConfig && Boolean(customerId.trim())}
                lockedHint={
                  canEditConfig
                    ? 'Load a customer ID first'
                    : 'You do not have authority to edit configuration'
                }
                validate={(v) =>
                  !v.trim()
                    ? 'Currency is required'
                    : v.trim().length > 3
                      ? 'Use ISO currency code (e.g. INR, USD)'
                      : null
                }
                onSave={async (next) => {
                  if (!customerId.trim()) throw new Error('Customer ID required');
                  try {
                    await httpClient.put(
                      '/api/v1/configuration/costing-defaults',
                      {
                        defaultReportingCurrency: next.trim().toUpperCase(),
                        defaultScrapPercent: scrap !== '' ? Number(scrap) : undefined,
                        defaultOverheadPercent: overhead !== '' ? Number(overhead) : undefined,
                      },
                      { params: { customerId: customerId.trim() } },
                    );
                    setCurrency(next.trim().toUpperCase());
                    setInfo('Default currency saved');
                    setError(null);
                    await loadDefaults();
                  } catch (e) {
                    throw new Error(getErrorMessage(e));
                  }
                }}
              />
              <ConfigEditableRow
                label="Scrap Percentage"
                displayValue={scrap !== '' ? `${scrap}%` : '—'}
                value={scrap}
                fieldType="number"
                canEdit={canEditConfig && Boolean(customerId.trim())}
                lockedHint={
                  canEditConfig
                    ? 'Load a customer ID first'
                    : 'You do not have authority to edit configuration'
                }
                validate={(v) => {
                  if (v === '') return null;
                  const n = Number(v);
                  if (Number.isNaN(n) || n < 0) return 'Must be a number ≥ 0';
                  return null;
                }}
                onSave={async (next) => {
                  if (!customerId.trim()) throw new Error('Customer ID required');
                  try {
                    await httpClient.put(
                      '/api/v1/configuration/costing-defaults',
                      {
                        defaultReportingCurrency: currency || undefined,
                        defaultScrapPercent: next !== '' ? Number(next) : undefined,
                        defaultOverheadPercent: overhead !== '' ? Number(overhead) : undefined,
                      },
                      { params: { customerId: customerId.trim() } },
                    );
                    setScrap(next);
                    setInfo('Scrap percentage saved');
                    setError(null);
                    await loadDefaults();
                  } catch (e) {
                    throw new Error(getErrorMessage(e));
                  }
                }}
              />
              <ConfigEditableRow
                label="Overhead Percentage"
                displayValue={overhead !== '' ? `${overhead}%` : '—'}
                value={overhead}
                fieldType="number"
                canEdit={canEditConfig && Boolean(customerId.trim())}
                lockedHint={
                  canEditConfig
                    ? 'Load a customer ID first'
                    : 'You do not have authority to edit configuration'
                }
                validate={(v) => {
                  if (v === '') return null;
                  const n = Number(v);
                  if (Number.isNaN(n) || n < 0) return 'Must be a number ≥ 0';
                  return null;
                }}
                onSave={async (next) => {
                  if (!customerId.trim()) throw new Error('Customer ID required');
                  try {
                    await httpClient.put(
                      '/api/v1/configuration/costing-defaults',
                      {
                        defaultReportingCurrency: currency || undefined,
                        defaultScrapPercent: scrap !== '' ? Number(scrap) : undefined,
                        defaultOverheadPercent: next !== '' ? Number(next) : undefined,
                      },
                      { params: { customerId: customerId.trim() } },
                    );
                    setOverhead(next);
                    setInfo('Overhead percentage saved');
                    setError(null);
                    await loadDefaults();
                  } catch (e) {
                    throw new Error(getErrorMessage(e));
                  }
                }}
              />
              <ConfigEditableRow
                label="FX Missing Policy"
                displayValue={defaults?.fxMissingPolicy ?? 'NOT_AVAILABLE'}
                value={defaults?.fxMissingPolicy ?? 'NOT_AVAILABLE'}
                canEdit={false}
                lockedHint="System-fixed — not customer-editable"
              />
              <ConfigEditableRow
                label="Release Incomplete Policy"
                displayValue={defaults?.releaseIncompletePolicy ?? 'BLOCK'}
                value={defaults?.releaseIncompletePolicy ?? 'BLOCK'}
                canEdit={false}
                lockedHint="System-fixed — not customer-editable"
              />
            </Box>
          </Stack>
        </Paper>
      )}

      {tab === 3 && (
        <Paper sx={{ p: 2 }}>
          <Typography variant="body2" color="text.secondary" mb={1}>
            Apply packs via Platform Customers → module entitlements (not auto-applied here).
          </Typography>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Pack</TableCell>
                <TableCell>Modules</TableCell>
                <TableCell>Description</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {modulePacks.map((m) => (
                <TableRow key={m.packCode}>
                  <TableCell>
                    {m.packCode}
                    <br />
                    <Typography variant="caption">{m.packName}</Typography>
                  </TableCell>
                  <TableCell>{m.moduleCodes.join(', ')}</TableCell>
                  <TableCell>{m.description}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      )}

      {tab === 4 && (
        <Paper sx={{ p: 2 }}>
          <Typography variant="body2" color="text.secondary" mb={1}>
            Workflow definitions (read model). Manage create/activate via existing workflow APIs — no AI approver steps.
          </Typography>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Entity type</TableCell>
                <TableCell>Active</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {workflows.length === 0 && (
                <TableRow>
                  <TableCell colSpan={4}>No workflow definitions found</TableCell>
                </TableRow>
              )}
              {workflows.map((w) => (
                <TableRow key={w.workflowDefinitionId ?? w.name}>
                  <TableCell>{w.workflowDefinitionId}</TableCell>
                  <TableCell>{w.name}</TableCell>
                  <TableCell>{w.entityType}</TableCell>
                  <TableCell>{String(w.active)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      )}
    </Box>
  );
}
