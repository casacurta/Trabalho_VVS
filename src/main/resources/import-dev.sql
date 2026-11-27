-- ============================================================================
-- Dados de demonstração carregados automaticamente no perfil de desenvolvimento
-- (%dev). Executado após o drop-and-create do Hibernate.
-- Não é carregado no perfil de teste nem em produção.
-- ============================================================================

-- Médicos
INSERT INTO Medico (idMedico, crm, especialidade) VALUES (1, '24016/RS', 'Oncologista');
INSERT INTO Medico (idMedico, crm, especialidade) VALUES (2, '18309/SC', 'Ortopedista');
INSERT INTO Medico (idMedico, crm, especialidade) VALUES (3, '04985/SP', 'Cardiologista');

-- Prontuário
INSERT INTO Prontuario (idProntuario, data, descricao)
VALUES (1, DATE '2024-03-10', 'Paciente apresenta febre e dores no corpo');

-- Pessoa (dados comuns) e Paciente (dados específicos) — CPF válido: 111.444.777-35
INSERT INTO Pessoa (idPessoa, nome, endereco)
VALUES (1, 'Antonio Carlos', 'Av. 24 de Outubro 267, ap 201');

INSERT INTO Paciente (idPaciente, convenio, cpf, data_Nascimento, situacao, idProntuario)
VALUES (1, 'IPE', '11144477735', DATE '2002-01-08', 'EM_OBSERVACAO', 1);

-- Telefones
INSERT INTO Telefone (idTelefone, ddd, numero, idPessoa) VALUES (1, 51, 987462758, 1);
INSERT INTO Telefone (idTelefone, ddd, numero, idPessoa) VALUES (2, 54, 999573956, 1);

-- E-mails
INSERT INTO Lista_emails (Paciente_idPaciente, emails) VALUES (1, 'antonio1@mail.com');
INSERT INTO Lista_emails (Paciente_idPaciente, emails) VALUES (1, 'antonio2@mail.com');

-- Consulta
INSERT INTO Consulta (idConsulta, data, horario, idMedico, idPaciente)
VALUES (1, DATE '2030-05-20', TIME '09:30:00', 1, 1);
