-- V163: Support ticket permission catalog entries + indexes
INSERT INTO permission_catalog_entry (permission_code, module_code, feature_code, function_code, function_name, description, sort_order) VALUES
('SUPPORT_TICKET_READ', 'SUPPORT', 'SUPPORT_TICKETS', 'VIEW', 'View Tickets', 'View support tickets', 400),
('SUPPORT_TICKET_CREATE', 'SUPPORT', 'SUPPORT_TICKETS', 'CREATE', 'Create Ticket', 'Create support tickets', 410),
('SUPPORT_TICKET_REPLY', 'SUPPORT', 'SUPPORT_TICKETS', 'REPLY', 'Reply', 'Reply on support tickets', 420),
('SUPPORT_TICKET_CLOSE', 'SUPPORT', 'SUPPORT_TICKETS', 'CLOSE', 'Close Ticket', 'Close support tickets', 430),
('SUPPORT_TICKET_MANAGE', 'SUPPORT', 'SUPPORT_TICKETS', 'MANAGE', 'Manage Tickets', 'Platform manage support tickets', 440),
('SUPPORT_TICKET_ASSIGN', 'SUPPORT', 'SUPPORT_TICKETS', 'ASSIGN', 'Assign Ticket', 'Assign support tickets', 450),
('SUPPORT_TICKET_STATUS_UPDATE', 'SUPPORT', 'SUPPORT_TICKETS', 'STATUS', 'Change Status', 'Change support ticket status', 460),
('SUPPORT_TICKET_INTERNAL_NOTE', 'SUPPORT', 'SUPPORT_TICKETS', 'INTERNAL_NOTE', 'Internal Note', 'Add internal support notes', 470)
ON DUPLICATE KEY UPDATE function_name = VALUES(function_name);

-- Helpful list filters
SET @idx := (SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'support_ticket' AND index_name = 'idx_st_priority');
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_st_priority ON support_ticket (customer_id, priority)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @idx2 := (SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'support_ticket' AND index_name = 'idx_st_category');
SET @sql2 := IF(@idx2 = 0, 'CREATE INDEX idx_st_category ON support_ticket (customer_id, category)', 'SELECT 1');
PREPARE s2 FROM @sql2; EXECUTE s2; DEALLOCATE PREPARE s2;
