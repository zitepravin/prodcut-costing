import { Box, Chip, Collapse, IconButton, Stack, Typography } from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import { useState } from 'react';
import type { BomTreeNode } from '../../types/api';
import { formatMoney } from '../../utils/money';

interface BomTreeViewProps {
  nodes: BomTreeNode[];
  depth?: number;
  selectedId?: string | null;
  onSelect?: (node: BomTreeNode) => void;
}

function statusColor(status: string): 'default' | 'success' | 'warning' | 'info' {
  switch (status) {
    case 'RELEASED':
      return 'success';
    case 'DRAFT':
      return 'warning';
    case 'WHAT_IF':
      return 'info';
    default:
      return 'default';
  }
}

function BomTreeNodeRow({
  node,
  depth,
  selectedId,
  onSelect,
}: {
  node: BomTreeNode;
  depth: number;
  selectedId?: string | null;
  onSelect?: (node: BomTreeNode) => void;
}) {
  const hasChildren = (node.children?.length ?? 0) > 0;
  const [open, setOpen] = useState(depth < 2);
  const selected = selectedId === node.bomNodeId;

  return (
    <Box>
      <Stack
        direction="row"
        spacing={1}
        onClick={() => onSelect?.(node)}
        sx={{
          alignItems: 'center',
          pl: depth * 2,
          py: 0.75,
          pr: 1,
          cursor: 'pointer',
          borderRadius: 1,
          bgcolor: selected ? 'action.selected' : 'transparent',
          '&:hover': { bgcolor: 'action.hover' },
        }}
      >
        <IconButton
          size="small"
          disabled={!hasChildren}
          onClick={(e) => {
            e.stopPropagation();
            setOpen((v) => !v);
          }}
          sx={{ visibility: hasChildren ? 'visible' : 'hidden' }}
        >
          {open ? <ExpandMoreIcon fontSize="small" /> : <ChevronRightIcon fontSize="small" />}
        </IconButton>
        <Typography variant="body2" sx={{ fontWeight: 600, minWidth: 120 }}>
          {node.partNumber}
        </Typography>
        {node.revision && (
          <Typography variant="caption" color="text.secondary">
            rev {node.revision}
          </Typography>
        )}
        <Typography variant="caption" color="text.secondary" sx={{ fontFamily: 'ui-monospace, monospace' }}>
          qty {node.quantityPer} {node.uom}
        </Typography>
        {node.makeBuyFlag && <Chip size="small" label={node.makeBuyFlag} variant="outlined" />}
        <Chip size="small" label={node.status} color={statusColor(node.status)} variant="outlined" />
        {node.overrideFlag && <Chip size="small" label="Override" color="warning" />}
        {node.materialCode && (
          <Typography variant="caption" color="text.secondary">
            {node.materialCode}
          </Typography>
        )}
        <Box sx={{ flex: 1 }} />
        <Typography variant="body2" sx={{ fontFamily: 'ui-monospace, monospace', fontWeight: 600 }}>
          {formatMoney(node.rolledUpCost)}
        </Typography>
      </Stack>
      {hasChildren && (
        <Collapse in={open} timeout="auto" unmountOnExit>
          {node.children.map((child) => (
            <BomTreeNodeRow
              key={child.bomNodeId}
              node={child}
              depth={depth + 1}
              selectedId={selectedId}
              onSelect={onSelect}
            />
          ))}
        </Collapse>
      )}
    </Box>
  );
}

export function BomTreeView({ nodes, depth = 0, selectedId, onSelect }: BomTreeViewProps) {
  if (!nodes.length) {
    return (
      <Typography color="text.secondary" sx={{ p: 2 }}>
        No BOM nodes for this program.
      </Typography>
    );
  }
  return (
    <Box>
      {nodes.map((n) => (
        <BomTreeNodeRow
          key={n.bomNodeId}
          node={n}
          depth={depth}
          selectedId={selectedId}
          onSelect={onSelect}
        />
      ))}
    </Box>
  );
}
