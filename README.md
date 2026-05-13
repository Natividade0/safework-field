# SafeField

PWA mobile-first para Segurança do Trabalho em campo, criado para uso por TST em obras industriais e construção civil.

## Objetivo

Registrar operações críticas com poucos toques, mesmo sem internet:

- APR - Análise Preliminar de Risco
- PT - Permissão de Trabalho
- DDS - Diálogo Diário de Segurança
- Gestão de EPI
- Inspeção de campo e não conformidades
- Ocorrências e quase-acidentes
- Colaboradores, ASO e treinamentos
- Dashboard e histórico

## Stack

- React + Vite
- CSS mobile-first customizado
- IndexedDB para armazenamento local
- Service Worker para cache offline
- PWA instalável via navegador
- Exportação de relatório via impressão/PDF do navegador

## Como rodar

```bash
npm install
npm run dev
```

## Como gerar build

```bash
npm run build
npm run preview
```

## Status atual

Esta é a primeira versão funcional do front-end. Já inclui:

- Interface escura padrão
- Menu mobile-first com botões grandes
- Cadastro local de registros
- Banco de perigos e controles por tipo de tarefa
- Assinatura digital em canvas
- IndexedDB offline
- Service Worker
- Manifest PWA
- Dashboard com indicadores
- Histórico pesquisável

## Próximas etapas recomendadas

1. Adicionar Supabase para login, banco em nuvem e sincronização real.
2. Implementar templates específicos de APR, PT, DDS e EPI.
3. Adicionar geração de PDF com jsPDF e html2canvas.
4. Adicionar push notifications para vencimento de PT, ASO, CA e treinamentos.
5. Adicionar QR code de obra e colaborador.
6. Criar testes em Android 10+ e iOS 14+.

## Critérios de campo usados no design

- Tela pequena de celular
- Uso com uma mão
- Contraste alto para sol forte
- Ações críticas com poucos toques
- Operação offline como prioridade
