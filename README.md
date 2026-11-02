# Jo Fashion Hair — Agendamento online

Sistema de agendamento do salão Jo Fashion Hair (Campo Alegre - SC):

- **Site público** (React/Vite): serviços e preços, equipe, horários e agendamento em 4 passos.
- **Painel `/admin`**: agenda, serviços e preços, profissionais e funções, expediente, horário e dados do salão, bloqueios, clientes e faltas, WhatsApp.
- **Robô do WhatsApp** (Evolution API): agenda, cancela e confirma presença por conversa.
- **Lembretes automáticos** 24h e 2h antes, pedindo confirmação.

Tudo o que o dono altera no painel aparece **na hora** no site e no robô (avisos em tempo real via SSE).

## Estrutura

```
jo-fashion-hair/
├── docker-compose.yml        PostgreSQL para desenvolvimento
├── backend/                  Spring Boot 3 (Java 21)
│   └── src/main/resources/db/migration   schema versionado (Flyway)
└── frontend/                 React 18 + Vite
    ├── src/site/             site público
    └── src/admin/            painel /admin
```

## Rodando em desenvolvimento

Pré-requisitos: Docker, Java 21, Maven e Node.js 20 ou mais novo.

```bash
# 1. Banco
docker compose up -d

# 2. API (em outro terminal) — carrega dados de exemplo
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Front-end (em outro terminal)
cd frontend
npm install
npm run dev
```

- Site: http://localhost:5173
- Painel: http://localhost:5173/admin (e-mail `admin@jofashionhair.local`, senha `admin12345`, só em dev)

Em dev, o WhatsApp não envia nada de verdade: as mensagens vão para o log da API.
Para conversar com o robô, use **Painel → WhatsApp → Simulador**.

## Variáveis de ambiente (produção)

| Variável | Para quê |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | PostgreSQL |
| `ADMIN_EMAIL`, `ADMIN_NOME`, `ADMIN_PASSWORD` | conta do dono, criada **só na primeira vez** que a API sobe |
| `ADMIN_FORCAR_SENHA=true` | emergência: redefine a senha do dono para `ADMIN_PASSWORD` ao iniciar (remova depois) |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | SMTP para os códigos por e-mail (Hostinger: `smtp.hostinger.com`, porta `465`) |
| `CORS_ORIGINS` | endereço do site, ex.: `https://jofashionhair.com.br` |
| `EVOLUTION_BASE_URL`, `EVOLUTION_API_KEY`, `EVOLUTION_INSTANCE` | Evolution API |
| `WEBHOOK_SECRET` | segredo na URL do webhook: `/api/webhooks/whatsapp/{segredo}` |
| `WHATSAPP_ENVIAR=true` | liga o envio real de mensagens |
| `SITE_URL` | endereço público do site, ex.: `https://jofashionhair.com.br` (link da política no WhatsApp) |
| `COOKIE_SEGURO` | `true` em produção (cookie do dispositivo só por HTTPS); `false` só em localhost |
| `MERCADOPAGO_ACCESS_TOKEN`, `MERCADOPAGO_EMAIL_PAGADOR` | opcional: sinal por Pix com confirmação automática (sem eles, o Pix usa a chave cadastrada no painel e a equipe confirma) |
| `MERCADOPAGO_WEBHOOK_SECRET` | opcional: confere a assinatura dos avisos do Mercado Pago |
| `UPLOADS_DIR` | pasta onde ficam as fotos enviadas pelo painel (use uma pasta que não é apagada nos deploys) |
| `VITE_API_URL` | (front-end, na hora do `npm run build`) endereço da API |

## Fotos e avaliações

- **Fotos** (logo, fotos do salão e da equipe): no painel, botão **Escolher do dispositivo**. O painel reduz
  fotos grandes antes de enviar. Os arquivos ficam na pasta `UPLOADS_DIR` do servidor
  (em desenvolvimento, `backend/uploads`). Faça backup dessa pasta junto com o banco.
- **Avaliações**: **Painel → Avaliações**. Cadastre as avaliações que quiser mostrar e o resumo
  (nota, total e link "Ver todas"). Sem avaliações cadastradas, a seção fica escondida no site.

## Segurança
- **Logins individuais**: cada pessoa entra com o próprio e-mail. O dono convida a equipe em **Painel → Acessos**.
- **Dispositivo novo**: o primeiro login em cada computador/celular só é liberado com um código enviado ao
  e-mail da conta. O dispositivo fica confirmado por 90 dias (cookie HttpOnly, invisível ao JavaScript).
  Em **Minha conta** dá para ver e remover dispositivos. Trocar a senha desconecta os outros.
- **Senha com verificação por e-mail (2 etapas)**: trocar ou recuperar a senha exige um código de 6 números
  enviado ao e-mail da conta (vale 15 min, máximo 5 tentativas, guardado só como hash).

- **Painel**: todas as rotas `/api/admin/**` exigem login; senha guardada só como hash (BCrypt).
- **Limite de tentativas**: 10 senhas erradas em 15 min bloqueiam o IP por 15 min; no máximo 10 agendamentos
  por hora por IP; até 300 consultas públicas por minuto por IP.
- **Sessão do painel**: só no `sessionStorage` (some ao fechar a aba) e expira após 30 min sem uso.
- **Segredos**: só em variáveis de ambiente do servidor. A única variável do front-end (`VITE_API_URL`) é pública.
- **Banco**: porta liberada só para o próprio servidor (`127.0.0.1`); em produção a API não sobe sem `DB_PASSWORD`.
- **Produção**: sem source maps no site e sem detalhes internos nas mensagens de erro da API.

## Privacidade (LGPD)

- **Política de privacidade** em `/privacidade`, com link no rodapé, no agendamento e na primeira mensagem
  do WhatsApp. Razão social, CNPJ, e-mail de contato, prazo de retenção e (opcional) texto próprio ficam em
  **Painel → Salão e horários**. Peça para um advogado revisar o texto antes de publicar.
- **Pedidos das clientes**: em **Painel → Clientes e faltas → Histórico**, os botões **Exportar dados** e
  **Excluir cliente e histórico** (exclusão definitiva; não dá para excluir quem tem horário futuro marcado).
- **Retenção automática**: todo dia 1º, às 4h, clientes sem atendimento há mais que o prazo definido
  (padrão 24 meses) são excluídas com o histórico, e conversas do robô com mais de 90 dias são apagadas.
- **Mapa do Google** só carrega quando a pessoa toca em "Ver mapa" (sem cookies do Google antes disso).

## Publicando

1. **API**: `mvn clean package` e rode o `.jar` com as variáveis acima, atrás de HTTPS.
2. **Front-end**: crie `frontend/.env.production` com `VITE_API_URL=https://api.seudominio.com.br`,
   rode `npm run build` e publique a pasta `dist/` (Vercel, Netlify, Nginx...).
   Configure o servidor para devolver `index.html` em qualquer rota (necessário para `/admin`).
3. **Mesmo domínio para site e API**: sirva a API em `https://seudominio.com.br/api` (Nginx repassando para a
   porta 8080) ou num subdomínio do mesmo domínio (`api.seudominio.com.br`). O cookie do dispositivo confirmado
   não funciona com domínios totalmente diferentes.
4. **Nginx na frente da API**: para os avisos em tempo real funcionarem, desligue o buffer na rota de eventos:

```nginx
location /api/public/eventos {
    proxy_pass http://localhost:8080;
    proxy_http_version 1.1;
    proxy_set_header Connection "";
    proxy_buffering off;
    proxy_read_timeout 1h;
}
```
