INSERT INTO goals (id, name, description, deletion_protected)
VALUES (1, '�ڕW���ݒ�', '�V�X�e���p�̊���ڕW', TRUE)
ON CONFLICT DO NOTHING;

SELECT setval(
  pg_get_serial_sequence('goals','id'),
  COALESCE((SELECT MAX(id) FROM goals), 0),
  true
);
