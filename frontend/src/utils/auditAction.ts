/** Chip color helper for audit log actions (CREATE/UPDATE/TRANSITION/VERIFY/DELETE). */
export type AuditActionColor = 'success' | 'info' | 'error' | 'warning' | 'default';

export function actionColor(action: string): AuditActionColor {
  if (action === 'CREATE') return 'success';
  if (action === 'UPDATE') return 'info';
  if (action === 'TRANSITION') return 'warning';
  if (action === 'VERIFY') return 'info';
  if (action === 'DELETE') return 'error';
  return 'default';
}
