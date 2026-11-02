import { useRef, useState } from 'react';
import { adminApi } from '../../api/admin.js';
import { useCarregar } from '../../hooks/useCarregar.js';
import { capitalizar, linkWhatsapp, telefoneVisivel } from '../../util/formato.js';
import { Campo, Carga, useAvisos } from '../componentes.jsx';

const ESTADOS = {
  INICIAL: 'Início',
  AGUARDANDO_SERVICO: 'Escolhendo o serviço',
  AGUARDANDO_PROFISSIONAL: 'Escolhendo a profissional',
  AGUARDANDO_DATA_HORA: 'Escolhendo dia e horário',
  AGUARDANDO_ESCOLHA_HORARIO: 'Escolhendo entre horários sugeridos',
  AGUARDANDO_ESCOLHA_CANCELAMENTO: 'Cancelando',
  AGUARDANDO_CONFIRMACAO_LEMBRETE: 'Respondendo ao lembrete',
  ATENDIMENTO_HUMANO: 'Pediu atendente',
  FINALIZADO: 'Concluída',
};

export default function WhatsApp() {
  return (
    <div className="pagina">
      <header className="pagina-cabeca">
        <div>
          <h1>WhatsApp</h1>
          <p className="pagina-sub">Conversas do atendimento automático e um simulador para testar o robô.</p>
        </div>
      </header>
      <Conversas />
      <Simulador />
    </div>
  );
}

function Conversas() {
  const avisos = useAvisos();
  const [estado, setEstado] = useState('ATENDIMENTO_HUMANO');
  const conversas = useCarregar(() => adminApi.conversas(estado || null), [estado]);

  async function devolver(c) {
    try {
      await adminApi.reativarBot(c.telefone);
      avisos.sucesso(`O atendimento automático voltou a responder ${telefoneVisivel(c.telefone)}.`);
      conversas.recarregar(true);
    } catch (e) {
      avisos.erro(e.message);
    }
  }

  return (
    <section className="painel">
      <div className="painel-cabeca">
        <h2>Conversas</h2>
        <select value={estado} onChange={(e) => setEstado(e.target.value)} aria-label="Filtrar conversas">
          <option value="ATENDIMENTO_HUMANO">Pediram atendente</option>
          <option value="">Últimas 100 conversas</option>
        </select>
        <button type="button" className="botao-texto" onClick={() => conversas.recarregar()}>Atualizar</button>
      </div>
      <p className="dica">
        Aqui aparecem as conversas em que a cliente pediu atendente ou o robô não soube responder (“Alguém já irá te
        atender”). Viu uma dúvida que se repete? Cadastre em Perguntas frequentes. O robô fica em silêncio por 12 horas para a equipe responder pelo celular do salão. Ao terminar,
        devolva a conversa ao robô.
      </p>
      <Carga estado={conversas} vazio="Nenhuma conversa aqui.">
        <ul className="lista-simples">
          {(conversas.dados || []).map((c) => (
            <li key={c.telefone}>
              <span>
                <a href={linkWhatsapp(c.telefone)} target="_blank" rel="noreferrer">{telefoneVisivel(c.telefone)}</a>{' '}
                <span className="dica">
                  {ESTADOS[c.estado] || c.estado}, última mensagem{' '}
                  {capitalizar(new Date(c.ultimaInteracao).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' }))}
                </span>
                {c.ultimaMensagem && <span className="conversa-ultima">“{c.ultimaMensagem}”</span>}
              </span>
              {c.estado === 'ATENDIMENTO_HUMANO' && (
                <button type="button" className="botao botao-secundario botao-pequeno" onClick={() => devolver(c)}>
                  Devolver ao robô
                </button>
              )}
            </li>
          ))}
        </ul>
      </Carga>
    </section>
  );
}

/** Conversa com o robô sem usar o WhatsApp. Cria agendamentos de verdade. */
function Simulador() {
  const avisos = useAvisos();
  const [telefone, setTelefone] = useState('47900000001');
  const [nome, setNome] = useState('Cliente Teste');
  const [texto, setTexto] = useState('');
  const [mensagens, setMensagens] = useState([]);
  const [enviando, setEnviando] = useState(false);
  const fim = useRef(null);

  async function enviar(e) {
    e.preventDefault();
    if (!texto.trim()) return;
    const enviado = texto.trim();
    setMensagens((m) => [...m, { de: 'cliente', texto: enviado }]);
    setTexto('');
    setEnviando(true);
    try {
      const r = await adminApi.simular({ telefone, nome, texto: enviado });
      setMensagens((m) => [...m, { de: 'robo', texto: r.resposta }]);
    } catch (err) {
      avisos.erro(err.message);
    } finally {
      setEnviando(false);
      setTimeout(() => fim.current?.scrollIntoView({ block: 'nearest' }), 0);
    }
  }

  return (
    <section className="painel">
      <h2>Simulador do robô</h2>
      <p className="nota nota-info">
        Passa pelo mesmo fluxo do WhatsApp real, inclusive criando agendamentos de verdade. Use um telefone de teste.
      </p>
      <div className="grade-2">
        <Campo id="sim-tel" rotulo="Telefone de teste">
          <input id="sim-tel" value={telefone} onChange={(e) => setTelefone(e.target.value)} inputMode="tel" />
        </Campo>
        <Campo id="sim-nome" rotulo="Nome no WhatsApp">
          <input id="sim-nome" value={nome} onChange={(e) => setNome(e.target.value)} />
        </Campo>
      </div>
      <div className="chat" aria-live="polite">
        {mensagens.length === 0 && (
          <p className="dica">Ex.: “Olá, queria marcar unha em gel sexta às 15h”.</p>
        )}
        {mensagens.map((m, i) => (
          <div key={i} className={`balao balao-${m.de}`}>{m.texto}</div>
        ))}
        <div ref={fim} />
      </div>
      <form onSubmit={enviar} className="chat-envio">
        <input value={texto} onChange={(e) => setTexto(e.target.value)} placeholder="Escreva como a cliente escreveria" aria-label="Mensagem" />
        <button type="submit" className="botao botao-primario" disabled={enviando}>Enviar</button>
        <button type="button" className="botao-texto" onClick={() => setMensagens([])}>Limpar</button>
      </form>
    </section>
  );
}
