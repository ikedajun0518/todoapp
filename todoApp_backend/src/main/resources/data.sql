INSERT INTO goals (id, name, description, deletion_protected)
VALUES (1, '目標未設定', 'システム用の既定目標', TRUE)
ON CONFLICT DO NOTHING;

SELECT setval(
  pg_get_serial_sequence('goals','id'),
  COALESCE((SELECT MAX(id) FROM goals), 0),
  true
);
