import { useEffect } from 'react';
import { telefoneVisivel } from '../util/formato.js';
import { Cabecalho, Rodape } from './Layout.jsx';
import { useDadosSite } from './useDadosSite.js';
import '../styles/site.css';

/**
 * Política de Privacidade (LGPD).
 * Se o salão escreveu um texto próprio no painel, ele é exibido.
 * Senão, mostra o modelo abaixo, preenchido com os dados do painel.
 */
export default function PaginaPrivacidade() {
  const { info, horarios, nome, avaliacoes, produtos } = useDadosSite();

  useEffect(() => {
    window.scrollTo(0, 0);
    document.title = `Política de Privacidade | ${nome}`;
  }, [nome]);

  return (
    <div className="site">
      <Cabecalho info={info} nome={nome} naHome={false} temAvaliacoes={!!avaliacoes} temProdutos={!!produtos} />
      <main className="privacidade claro">
        <article className="conteudo privacidade-texto">
          <p className="sobretitulo">LGPD</p>
          <h1>Política de Privacidade</h1>
          {info?.politicaPrivacidade ? <TextoProprio texto={info.politicaPrivacidade} /> : <Modelo info={info} nome={nome} />}
        </article>
      </main>
      <Rodape info={info} nome={nome} horarios={horarios} naHome={false} temAvaliacoes={!!avaliacoes} temProdutos={!!produtos} />
    </div>
  );
}

/** Texto escrito no painel. Linhas que começam com "## " viram títulos. */
function TextoProprio({ texto }) {
  return texto
    .split(/\n\s*\n/)
    .filter(Boolean)
    .map((bloco, i) =>
      bloco.startsWith('## ') ? <h2 key={i}>{bloco.slice(3)}</h2> : <p key={i}>{bloco}</p>,
    );
}

function Modelo({ info, nome }) {
  const responsavel = info?.razaoSocial || nome;
  const endereco = [info?.endereco, info?.cidade].filter(Boolean).join(', ');
  const meses = info?.retencaoMeses || 24;
  const prazo = meses % 12 === 0 ? `${meses / 12} ${meses === 12 ? 'ano' : 'anos'}` : `${meses} meses`;
  const contatos = [
    info?.emailPrivacidade && `pelo e-mail ${info.emailPrivacidade}`,
    info?.whatsapp && `pelo WhatsApp ${telefoneVisivel(info.whatsapp)}`,
  ].filter(Boolean);
  const contato = contatos.length ? contatos.join(' ou ') : 'pelos canais de atendimento do salão';

  return (
    <>
      <p className="privacidade-intro">
        Esta política explica, de forma simples, quais dados o {nome} coleta quando você agenda um horário,
        para que eles são usados e quais são os seus direitos, conforme a Lei Geral de Proteção de Dados
        (Lei nº 13.709/2018).
      </p>

      <h2>Quem cuida dos seus dados</h2>
      <p>
        O responsável pelos dados é <strong>{responsavel}</strong>
        {info?.cnpj && <>, CNPJ {info.cnpj}</>}
        {endereco && <>, com endereço em {endereco}</>}. Para qualquer assunto sobre seus dados, fale com a
        gente {contato}.
      </p>

      <h2>Quais dados coletamos</h2>
      <ul>
        <li><strong>Nome e número de WhatsApp</strong>, informados por você ao agendar pelo site ou pelo WhatsApp.</li>
        <li><strong>Seus agendamentos:</strong> serviço, profissional, data, horário, valor e se o atendimento aconteceu.</li>
        <li><strong>Mensagens trocadas com nosso atendimento automático</strong> no WhatsApp, enquanto a conversa estiver em andamento.</li>
      </ul>
      <p>Não pedimos CPF, endereço, senha nem dados de pagamento pelo site.</p>

      <h2>Para que usamos</h2>
      <ul>
        <li>Reservar o seu horário e confirmar o agendamento.</li>
        <li>Enviar lembretes antes do atendimento e permitir que você confirme ou cancele pelo WhatsApp.</li>
        <li>Enviar promoções do salão pelo WhatsApp, só se você autorizar. Para sair a qualquer momento, basta responder SAIR.</li>
        <li>Organizar a agenda do salão e o histórico de atendimentos, inclusive faltas.</li>
      </ul>
      <p>
        Fazemos isso para prestar o serviço que você pediu e com base no legítimo interesse do salão em
        organizar a agenda e evitar horários vagos. Não usamos seus dados para propaganda nem os vendemos.
      </p>

      <h2>Com quem compartilhamos</h2>
      <p>Só com empresas necessárias para o funcionamento do agendamento:</p>
      <ul>
        <li>o serviço de mensagens do WhatsApp, para enviar confirmações e lembretes;</li>
        <li>a empresa de pagamentos (Mercado Pago), quando o serviço cobra sinal por Pix, só para processar esse pagamento;</li>
        <li>a empresa que hospeda o site e o sistema de agendamento;</li>
        <li>o Google, apenas se você tocar em “Ver mapa” ou “Como chegar”.</li>
      </ul>

      <h2>Por quanto tempo guardamos</h2>
      <p>
        Seus dados ficam guardados enquanto você for cliente. Depois de <strong>{prazo}</strong> sem nenhum
        atendimento, seu cadastro e seu histórico de agendamentos são excluídos automaticamente. As conversas do
        atendimento automático são apagadas depois de 90 dias.
      </p>

      <h2>Seus direitos</h2>
      <p>A qualquer momento, você pode pedir para:</p>
      <ul>
        <li>saber quais dados temos sobre você e receber uma cópia;</li>
        <li>corrigir dados incompletos ou errados;</li>
        <li>excluir seu cadastro e todo o seu histórico (se houver um agendamento futuro, ele precisa ser cancelado antes).</li>
      </ul>
      <p>Basta pedir {contato}. Respondemos o mais rápido possível, no prazo da lei.</p>

      <h2>Segurança</h2>
      <p>
        Os dados ficam em servidor protegido, com conexão criptografada (HTTPS), e só a equipe do salão acessa
        o painel de agendamentos, com senha.
      </p>

      <h2>Cookies</h2>
      <p>
        Este site não usa cookies de propaganda nem de rastreamento. O mapa do Google só é carregado se você
        pedir para vê-lo.
      </p>

      <h2>Alterações</h2>
      <p>Esta política pode ser atualizada. A versão em vigor é sempre a publicada nesta página.</p>
    </>
  );
}
