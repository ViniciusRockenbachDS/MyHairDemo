import { useMemo, useState } from 'react';
import { adminApi } from '../../api/admin.js';
import { useAoVivo } from '../../aovivo/AoVivo.jsx';
import { useCarregar } from '../../hooks/useCarregar.js';
import {
  capitalizar, dataCurta, dataDeIso, dataLonga, horaDe, isoData, linkWhatsapp, mascaraTelefone, moeda,
  telefoneVisivel,
} from '../../util/formato.js';
import { Campo, Carga, Modal, OPCOES_STATUS, StatusEtiqueta, useAvisos } from '../componentes.jsx';

const MENSAGEM_STATUS = {
  CONFIRMADO: 'Agendamento confirmado.',
  CONCLUIDO: 'Atendimento marcado como concluído.',
  FALTOU: 'Falta registrada no histórico da cliente.',
  CANCELADO: 'Agendamento cancelado. O horário voltou a ficar livre no site.',
};

export default function Agenda() {
  const avisos = useAvisos();
  const [dia, setDia] = useState(() => isoData(new Date()));
  const [profissionalId, setProfissionalId] = useState('');
  const [status, setStatus] = useState('');
  const [novo, setNovo] = useState(false);

  const agenda = useCarregar(
    () => adminApi.agenda(dia, dia, profissionalId || null, status || null),
    [dia, profissionalId, status],
  );
  const semBaixa = useCarregar(() => adminApi.semBaixa(), []);
  const profissionais = useCarregar(() => adminApi.profissionais(), []);
  const servicos = useCarregar(() => adminApi.servicos(), []);

  // Ao vivo: agendamentos feitos pelo site ou pelo WhatsApp aparecem sozinhos.
  useAoVivo(() => {
    agenda.recarregar(true);
    semBaixa.recarregar(true);
  }, ['agenda']);
  useAoVivo(() => {
    profissionais.recarregar(true);
    servicos.recarregar(true);
  }, ['profissionais', 'servicos']);

  function mudarDia(delta) {
    const d = dataDeIso(dia);
    d.setDate(d.getDate() + delta);
    setDia(isoData(d));
  }

  async function alterar(a, novoStatus) {
    if (
      novoStatus === 'CANCELADO' &&
      !window.confirm(`Cancelar o horário de ${a.cliente} às ${horaDe(a.inicio)}? O horário volta a ficar livre no site.`)
    ) {
      return;
    }
    try {
      await adminApi.alterarStatus(a.id, novoStatus);
      avisos.sucesso(MENSAGEM_STATUS[novoStatus]);
      agenda.recarregar(true);
      semBaixa.recarregar(true);
    } catch (e) {
      avisos.erro(e.message);
    }
  }

  async function sinalRecebido(a) {
    if (!window.confirm(`Confirmar que o sinal de ${moeda(a.valorSinal)} de ${a.cliente} caiu na conta?`)) return;
    try {
      await adminApi.sinalRecebido(a.id);
      avisos.sucesso(`Sinal confirmado. ${a.cliente} recebe a confirmação pelo WhatsApp.`);
      agenda.recarregar(true);
    } catch (e) {
      avisos.erro(e.message);
    }
  }

  async function lembrar(a) {
    try {
      await adminApi.lembrete(a.id);
      avisos.sucesso(`Lembrete enviado para ${a.cliente} pelo WhatsApp.`);
    } catch (e) {
      avisos.erro(e.message);
    }
  }

  const total = agenda.dados?.filter((a) => a.status !== 'CANCELADO').length ?? 0;

  return (
    <div className="pagina">
      <header className="pagina-cabeca">
        <div>
          <h1>Agenda</h1>
          <p className="pagina-sub">
            {capitalizar(dataLonga(dataDeIso(dia)))}
            {agenda.dados ? `, ${total} ${total === 1 ? 'atendimento' : 'atendimentos'}` : ''}
          </p>
        </div>
        <button type="button" className="botao botao-primario" onClick={() => setNovo(true)}>
          Novo agendamento
        </button>
      </header>

      {semBaixa.dados?.length > 0 && (
        <section className="painel painel-alerta" aria-labelledby="titulo-baixa">
          <h2 id="titulo-baixa">Falta dar baixa em {semBaixa.dados.length}</h2>
          <p className="dica">Atendimentos que já passaram. Marque se a cliente veio ou faltou, para manter o controle de faltas em dia.</p>
          <ul className="lista-baixa">
            {semBaixa.dados.map((a) => (
              <li key={a.id}>
                <span>
                  {dataCurta(dataDeIso(a.inicio))}, {horaDe(a.inicio)}: <strong>{a.cliente}</strong>, {a.servico} com{' '}
                  {a.profissional}
                </span>
                <span className="acoes">
                  <button type="button" className="botao botao-secundario botao-pequeno" onClick={() => alterar(a, 'CONCLUIDO')}>
                    Veio
                  </button>
                  <button type="button" className="botao botao-perigo botao-pequeno" onClick={() => alterar(a, 'FALTOU')}>
                    Faltou
                  </button>
                </span>
              </li>
            ))}
          </ul>
        </section>
      )}

      <div className="filtros">
        <div className="navegar-dia">
          <button type="button" className="botao botao-secundario botao-pequeno" onClick={() => mudarDia(-1)} aria-label="Dia anterior">
            ‹
          </button>
          <input type="date" value={dia} onChange={(e) => e.target.value && setDia(e.target.value)} aria-label="Dia" />
          <button type="button" className="botao botao-secundario botao-pequeno" onClick={() => mudarDia(1)} aria-label="Próximo dia">
            ›
          </button>
          <button type="button" className="botao-texto" onClick={() => setDia(isoData(new Date()))}>Hoje</button>
        </div>
        <select value={profissionalId} onChange={(e) => setProfissionalId(e.target.value)} aria-label="Filtrar por profissional">
          <option value="">Todas as profissionais</option>
          {(profissionais.dados || []).map((p) => <option key={p.id} value={p.id}>{p.nome}</option>)}
        </select>
        <select value={status} onChange={(e) => setStatus(e.target.value)} aria-label="Filtrar por situação">
          <option value="">Todas as situações</option>
          {OPCOES_STATUS.map(([v, r]) => <option key={v} value={v}>{r}</option>)}
        </select>
      </div>

      <Carga estado={agenda} vazio="Nenhum agendamento neste dia.">
        <div className="tabela-rolagem">
          <table className="tabela">
            <thead>
              <tr>
                <th>Horário</th>
                <th>Cliente</th>
                <th>Serviço</th>
                <th>Profissional</th>
                <th className="numero">Valor</th>
                <th>Situação</th>
                <th><span className="visualmente-oculto">Ações</span></th>
              </tr>
            </thead>
            <tbody>
              {(agenda.dados || []).map((a) => (
                <tr key={a.id} className={a.status === 'CANCELADO' ? 'inativo' : undefined}>
                  <td className="nowrap">{horaDe(a.inicio)} às {horaDe(a.fim)}</td>
                  <td>
                    <div>{a.cliente}</div>
                    <a className="link-suave" href={linkWhatsapp(a.telefone)} target="_blank" rel="noreferrer">
                      {telefoneVisivel(a.telefone)}
                    </a>
                  </td>
                  <td>{a.servico}</td>
                  <td>{a.profissional}</td>
                  <td className="numero">{moeda(a.preco)}</td>
                  <td>
                    <StatusEtiqueta status={a.status} />
                    {a.valorSinal != null && a.sinalPago && (
                      <span className="dica d-block">sinal pago {moeda(a.valorSinal)}</span>
                    )}
                  </td>
                  <td className="acoes">
                    <Acoes a={a} aoAlterar={alterar} aoLembrar={lembrar} aoSinal={sinalRecebido} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Carga>

      {novo && (
        <NovoAgendamentoModal
          diaInicial={dia}
          servicos={(servicos.dados || []).filter((s) => s.ativo)}
          profissionais={(profissionais.dados || []).filter((p) => p.ativo)}
          aoFechar={() => setNovo(false)}
          aoSalvar={(r) => {
            setNovo(false);
            setDia(r.inicio.slice(0, 10));
            agenda.recarregar(true);
          }}
        />
      )}
    </div>
  );
}

function Acoes({ a, aoAlterar, aoLembrar, aoSinal }) {
  if (a.status === 'AGUARDANDO_SINAL') {
    return (
      <>
        <button type="button" className="botao-texto" onClick={() => aoSinal(a)}>Sinal recebido</button>
        <button type="button" className="botao-texto texto-perigo" onClick={() => aoAlterar(a, 'CANCELADO')}>Cancelar</button>
      </>
    );
  }
  if (a.status !== 'PENDENTE' && a.status !== 'CONFIRMADO') return <span className="dica">Sem ações</span>;
  const jaComecou = new Date(a.inicio) <= new Date();
  return (
    <>
      {a.status === 'PENDENTE' && !jaComecou && (
        <button type="button" className="botao-texto" onClick={() => aoAlterar(a, 'CONFIRMADO')}>Confirmar</button>
      )}
      {jaComecou && (
        <>
          <button type="button" className="botao-texto" onClick={() => aoAlterar(a, 'CONCLUIDO')}>Veio</button>
          <button type="button" className="botao-texto texto-perigo" onClick={() => aoAlterar(a, 'FALTOU')}>Faltou</button>
        </>
      )}
      {!jaComecou && (
        <button type="button" className="botao-texto" onClick={() => aoLembrar(a)}>Lembrar</button>
      )}
      <button type="button" className="botao-texto texto-perigo" onClick={() => aoAlterar(a, 'CANCELADO')}>Cancelar</button>
    </>
  );
}

/** Agendamento de balcão/telefone. Pode ser para agora (sem antecedência mínima). */
function NovoAgendamentoModal({ diaInicial, servicos, profissionais, aoFechar, aoSalvar }) {
  const avisos = useAvisos();
  const [form, setForm] = useState({
    servicoId: '', profissionalId: '', data: diaInicial, hora: '', nome: '', telefone: '', observacoes: '',
  });
  const [erros, setErros] = useState({});
  const [problema, setProblema] = useState(null);
  const [salvando, setSalvando] = useState(false);
  const mudar = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }));

  const quemFaz = useMemo(
    () => profissionais.filter((p) => form.servicoId && p.servicoIds.includes(Number(form.servicoId))),
    [profissionais, form.servicoId],
  );

  async function salvar(e) {
    e.preventDefault();
    const novos = {};
    if (!form.servicoId) novos.servicoId = 'Escolha o serviço.';
    if (!form.data) novos.data = 'Informe a data.';
    if (!form.hora) novos.hora = 'Informe o horário.';
    if (!form.nome.trim()) novos.nome = 'Informe o nome da cliente.';
    if (form.telefone.replace(/\D/g, '').length < 10) novos.telefone = 'Informe o telefone com DDD.';
    setErros(novos);
    if (Object.keys(novos).length) return;

    setSalvando(true);
    setProblema(null);
    try {
      const r = await adminApi.agendamentoManual({
        servicoId: Number(form.servicoId),
        profissionalId: form.profissionalId ? Number(form.profissionalId) : null,
        data: form.data,
        hora: form.hora,
        nome: form.nome.trim(),
        telefone: form.telefone.replace(/\D/g, ''),
        observacoes: form.observacoes.trim() || null,
      });
      avisos.sucesso(`Agendado: ${r.cliente}, ${dataCurta(dataDeIso(r.inicio))} às ${horaDe(r.inicio)} com ${r.profissional}.`);
      aoSalvar(r);
    } catch (err) {
      if (err.status === 409) setProblema({ mensagem: err.message, sugestoes: err.sugestoes });
      else if (err.campos) setErros(err.campos);
      else avisos.erro(err.message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <Modal titulo="Novo agendamento" aoFechar={aoFechar} largo>
      <form onSubmit={salvar} noValidate>
        <div className="modal-corpo grade-2">
          <Campo id="na-servico" rotulo="Serviço" erro={erros.servicoId}>
            <select id="na-servico" value={form.servicoId} onChange={(e) => setForm((f) => ({ ...f, servicoId: e.target.value, profissionalId: '' }))}>
              <option value="">Escolha</option>
              {servicos.map((s) => <option key={s.id} value={s.id}>{s.nome} ({moeda(s.preco)})</option>)}
            </select>
          </Campo>
          <Campo id="na-prof" rotulo="Profissional">
            <select id="na-prof" value={form.profissionalId} onChange={mudar('profissionalId')} disabled={!form.servicoId}>
              <option value="">Sem preferência (primeira livre)</option>
              {quemFaz.map((p) => <option key={p.id} value={p.id}>{p.nome}</option>)}
            </select>
          </Campo>
          <Campo id="na-data" rotulo="Data" erro={erros.data}>
            <input id="na-data" type="date" value={form.data} onChange={mudar('data')} />
          </Campo>
          <Campo id="na-hora" rotulo="Horário" erro={erros.hora}>
            <input id="na-hora" type="time" step="300" value={form.hora} onChange={mudar('hora')} />
          </Campo>
          <Campo id="na-nome" rotulo="Nome da cliente" erro={erros.nome}>
            <input id="na-nome" value={form.nome} onChange={mudar('nome')} maxLength={120} />
          </Campo>
          <Campo id="na-tel" rotulo="Telefone (WhatsApp)" erro={erros.telefone} dica="Ela recebe os lembretes neste número.">
            <input id="na-tel" value={form.telefone} onChange={(e) => setForm((f) => ({ ...f, telefone: mascaraTelefone(e.target.value) }))} inputMode="tel" />
          </Campo>
          <div className="grade-inteira">
            <Campo id="na-obs" rotulo="Observações (opcional)">
              <textarea id="na-obs" value={form.observacoes} onChange={mudar('observacoes')} maxLength={500} />
            </Campo>
          </div>
          {problema && (
            <div className="nota nota-erro grade-inteira" role="alert">
              <p>{problema.mensagem}</p>
              {problema.sugestoes?.length > 0 && (
                <div className="chips">
                  {problema.sugestoes.map((s) => (
                    <button
                      key={`${s.inicio}-${s.profissionalId}`}
                      type="button"
                      className="chip"
                      onClick={() => {
                        setForm((f) => ({ ...f, data: s.inicio.slice(0, 10), hora: horaDe(s.inicio), profissionalId: String(s.profissionalId) }));
                        setProblema(null);
                      }}
                    >
                      {dataCurta(dataDeIso(s.inicio))}, {horaDe(s.inicio)} com {s.profissionalNome}
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
        <div className="modal-rodape">
          <button type="button" className="botao botao-secundario" onClick={aoFechar}>Cancelar</button>
          <button type="submit" className="botao botao-primario" disabled={salvando}>
            {salvando ? 'Agendando…' : 'Agendar'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
