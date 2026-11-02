# MyHair — versão de demonstração

Cópia do sistema para publicar no Render e deixar qualquer pessoa testar.

## Como funciona

- **Visitante**: abre o site e ganha uma **cópia só dele** dos dados. O painel (`/admin`) abre **sem senha**.
  Tudo o que ele mudar (preços, serviços, agendamentos, fotos) aparece só para ele, no painel e no site.
  Depois de algumas horas sem uso (variável `DEMO_HORAS`), a cópia é descartada.
- **Você (dono da demonstração)**: abre `https://SEU-ENDERECO.onrender.com/?mestre=SUA_CHAVE`, entra no
  painel com `ADMIN_EMAIL` e `ADMIN_PASSWORD` e edita os **dados originais**, que todo visitante novo recebe.
  O código de "dispositivo novo" aparece nos **Logs** do Render (linha `[E-MAIL DESLIGADO]`).
- **Desligado na demonstração**: envio real de WhatsApp e e-mail, lembretes automáticos e Pix do Mercado Pago
  (o sinal funciona no modo manual, com o botão "Sinal recebido").
- **Imagens** ficam guardadas no banco (o disco do plano gratuito do Render é apagado a cada reinício).

## Variáveis (no painel do Render)

| Variável | Para quê |
|---|---|
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | seu acesso de dono (criado na primeira vez que o sistema sobe) |
| `DEMO_CHAVE_MESTRA` | palavra secreta do modo dono (`?mestre=...`) |
| `DEMO_MAX_VISITANTES` | visitantes ao mesmo tempo (padrão 25) |
| `DEMO_HORAS` | horas que a cópia de um visitante dura sem uso (padrão 3) |

As variáveis do banco são preenchidas sozinhas pelo `render.yaml`.
