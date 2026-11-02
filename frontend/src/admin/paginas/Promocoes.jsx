import { useEffect, useState } from 'react';
import { adminApi } from '../../api/admin.js';
import { useAoVivo } from '../../aovivo/AoVivo.jsx';
import { useCarregar } from '../../hooks/useCarregar.js';
import { rotuloDias } from '../../util/promocoes.js';
import { Campo, Carga, Modal, useAvisos } from '../componentes.jsx';

const DIAS = [[1, 'Seg'], [2, 'Ter'], [3, 'Qua'], [4, 'Qui'], [5, 'Sex'], [6, 'Sáb'], [7, 'Dom']];
const dataBr = (iso) => (iso ? iso.split('-').reverse().join('/') : null);

/**
 * Dias de promoção (o desconto entra sozinho no agendamento) e envio por
 * WhatsApp para clientes com histórico.
 */
export default function Promocoes() {
  const avisos = useAvisos();
  const promocoes = useCarregar(() => adminApi.promocoes(), []);
  const campanhas = useCarregar(() => adminApi.campanhas(), []);
  const [edicao, setEdicao] = useState(null);
  const [enviando, setEnviando] = useState(null);

  useAoVivo(() => promocoes.recarregar(true), ['promocoes']);

  // enquanto um envio estiver em andamento, atualiza o andamento a cada 5 s
  const emAndamento = (campanhas.dados || []).some((c) => c.status === 'ENVIANDO');
  useEffect(() => {
    if (!emAndamento) return undefined;
    const t = setInterval(() => campanhas.recarregar(true), 5000);
    return () => clearInterval(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [emAndamento]);

  async function alternar(p) {
    try {
      await adminApi.promocaoAtiva(p.id, !p.ativo);
      avisos.sucesso(p.ativo ? 'Promoção pausada. Os preços voltam ao normal.' : 'Promoção ativada.');
      promocoes.recarregar(true);
    } catch (e) {
      avisos.erro(e.message);
    }
  }

  async function excluir(p) {
    if (!window.confirm(`Excluir a promoção "${p.titulo}"? Agendamentos já feitos mantêm o preço com desconto.`)) return;
    try {
      await adminApi.removerPromocao(p.id);
      avisos.sucesso('Promoção excluída.');
      promocoes.recarregar(true);
    } catch (e) {
      avisos.erro(e.message);
    }
  }

  return (
    <div className="pagina">
      <header className="pagina-cabeca">
        <div>
          <h1>Promoções</h1>
          <p className="pagina-sub">
            Escolha os dias e os serviços com desconto. O desconto entra sozinho quando a cliente agenda para esses dias,
            pelo site ou pelo WhatsApp.
          </p>
        </div>
        <button type="button" className="botao botao-primario" onClick={() => setEdicao({})}>Nova promoção</button>
      </header>

      <Carga estado={promocoes} vazio="Nenhuma promoção cadastrada.">
        <ul className="lista-perguntas">
          {(promocoes.dados || []).map((p) => (
            <li key={p.id} className={`item-pergunta${p.ativo ? '' : ' inativo'}`}>
              <div className="item-pergunta-topo">
                <span className="promo-pilula">-{p.descontoPercentual}%</span>
                <strong>{p.titulo}</strong>
                {!p.ativo && <span className="etiqueta etiqueta-cancelado">pausada</span>}
              </div>
              <p className="item-pergunta-resposta">
                {rotuloDias(p.diasSemana)} · {p.todosServicos ? 'todos os serviços' : p.servicoNomes.join(', ')}
                {(p.dataInicio || p.dataFim) &&
                  ` · ${p.dataInicio ? `de ${dataBr(p.dataInicio)} ` : ''}${p.dataFim ? `até ${dataBr(p.dataFim)}` : ''}`}
              </p>
              {p.descricao && <p className="dica">{p.descricao}</p>}
              <div className="acoes">
                <button type="button" className="botao botao-primario botao-pequeno" onClick={() => setEnviando(p)} disabled={!p.ativo}>
                  Enviar por WhatsApp
                </button>
                <button type="button" className="botao-texto" onClick={() => setEdicao(p)}>Editar</button>
                <button type="button" className="botao-texto" onClick={() => alternar(p)}>{p.ativo ? 'Pausar' : 'Ativar'}</button>
                <button type="button" className="botao-texto texto-perigo" onClick={() => excluir(p)}>Excluir</button>
              </div>
            </li>
          ))}
        </ul>
      </Carga>

      <section className="painel">
        <div className="painel-cabeca">
          <h2>Envios por WhatsApp</h2>
          <button type="button" className="botao-texto" onClick={() => campanhas.recarregar(true)}>Atualizar</button>
        </div>
        <Carga estado={campanhas} vazio="Nenhum envio feito ainda.">
          <div className="tabela-rolagem">
            <table className="tabela">
              <thead>
                <tr>
                  <th>Data</th>
                  <th>Promoção</th>
                  <th>Público</th>
                  <th className="numero">Enviadas</th>
                  <th>Situação</th>
                </tr>
              </thead>
              <tbody>
                {(campanhas.dados || []).map((c) => (
                  <tr key={c.id}>
                    <td>{new Date(c.criadoEm).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })}</td>
                    <td>{c.titulo}</td>
                    <td>{c.publico}</td>
                    <td className="numero">
                      {c.enviados}/{c.total}
                      {c.falhas > 0 && <span className="texto-perigo"> ({c.falhas} falharam)</span>}
                    </td>
                    <td>
                      <span className={`etiqueta ${c.status === 'ENVIANDO' ? 'etiqueta-pendente' : 'etiqueta-concluido'}`}>
                        {c.status === 'ENVIANDO' ? 'Enviando…' : 'Concluído'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Carga>
      </section>

      {edicao && (
        <PromocaoModal
          promocao={edicao.id ? edicao : null}
          aoFechar={() => setEdicao(null)}
          aoSalvar={() => {
            setEdicao(null);
            promocoes.recarregar(true);
          }}
        />
      )}
      {enviando && (
        <EnvioModal
          promocao={enviando}
          aoFechar={() => setEnviando(null)}
          aoEnviar={() => {
            setEnviando(null);
            campanhas.recarregar(true);
          }}
        />
      )}
    </div>
  );
}

function PromocaoModal({ promocao, aoFechar, aoSalvar }) {
  const avisos = useAvisos();
  const servicos = useCarregar(() => adminApi.servicos(false), []);
  const [form, setForm] = useState({
    titulo: promocao?.titulo || '',
    descricao: promocao?.descricao || '',
    descontoPercentual: String(promocao?.descontoPercentual ?? 10),
    diasSemana: promocao?.diasSemana || [],
    dataInicio: promocao?.dataInicio || '',
    dataFim: promocao?.dataFim || '',
    todosServicos: promocao ? promocao.todosServicos : true,
    servicoIds: promocao?.servicoIds || [],
  });
  const [erros, setErros] = useState({});
  const [salvando, setSalvando] = useState(false);
  const mudar = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }));
  const alternarDia = (d) =>
    setForm((f) => ({ ...f, diasSemana: f.diasSemana.includes(d) ? f.diasSemana.filter((x) => x !== d) : [...f.diasSemana, d] }));
  const alternarServico = (id) =>
    setForm((f) => ({ ...f, servicoIds: f.servicoIds.includes(id) ? f.servicoIds.filter((x) => x !== id) : [...f.servicoIds, id] }));

  async function salvar(e) {
    e.preventDefault();
    const desconto = Number(form.descontoPercentual);
    const novos = {};
    if (!form.titulo.trim()) novos.titulo = 'Informe o título.';
    if (!Number.isInteger(desconto) || desconto < 1 || desconto > 90) novos.descontoPercentual = 'Entre 1% e 90%.';
    if (form.dataInicio && form.dataFim && form.dataFim < form.dataInicio) novos.dataFim = 'Deve ser depois da data inicial.';
    if (!form.todosServicos && form.servicoIds.length === 0) novos.servicos = 'Escolha ao menos um serviço.';
    setErros(novos);
    if (Object.keys(novos).length) return;

    const corpo = {
      titulo: form.titulo.trim(),
      descricao: form.descricao.trim() || null,
      descontoPercentual: desconto,
      diasSemana: [...form.diasSemana].sort(),
      dataInicio: form.dataInicio || null,
      dataFim: form.dataFim || null,
      todosServicos: form.todosServicos,
      servicoIds: form.todosServicos ? [] : form.servicoIds,
      ativo: promocao ? promocao.ativo : true,
    };
    setSalvando(true);
    try {
      if (promocao) await adminApi.atualizarPromocao(promocao.id, corpo);
      else await adminApi.criarPromocao(corpo);
      avisos.sucesso('Promoção salva. O site e o WhatsApp já aplicam o desconto.');
      aoSalvar();
    } catch (err) {
      if (err.campos) setErros(err.campos);
      avisos.erro(err.message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <Modal titulo={promocao ? 'Editar promoção' : 'Nova promoção'} aoFechar={aoFechar} largo>
      <form onSubmit={salvar} noValidate>
        <div className="modal-corpo grade-2">
          <Campo id="pm-titulo" rotulo="Título" erro={erros.titulo}>
            <input id="pm-titulo" value={form.titulo} onChange={mudar('titulo')} maxLength={120} placeholder="Ex.: Terça da escova" />
          </Campo>
          <Campo id="pm-desc" rotulo="Desconto (%)" erro={erros.descontoPercentual}>
            <input id="pm-desc" type="number" min="1" max="90" value={form.descontoPercentual} onChange={mudar('descontoPercentual')} />
          </Campo>
          <div className="grade-inteira">
            <fieldset className="campo">
              <legend>Dias da semana</legend>
              <div className="chips">
                {DIAS.map(([d, rotulo]) => (
                  <button
                    key={d}
                    type="button"
                    className={`chip${form.diasSemana.includes(d) ? ' ativo' : ''}`}
                    aria-pressed={form.diasSemana.includes(d)}
                    onClick={() => alternarDia(d)}
                  >
                    {rotulo}
                  </button>
                ))}
              </div>
              <span className="dica">Nenhum marcado = vale todos os dias.</span>
            </fieldset>
          </div>
          <Campo id="pm-ini" rotulo="Começa em (opcional)">
            <input id="pm-ini" type="date" value={form.dataInicio} onChange={mudar('dataInicio')} />
          </Campo>
          <Campo id="pm-fim" rotulo="Termina em (opcional)" erro={erros.dataFim}>
            <input id="pm-fim" type="date" value={form.dataFim} onChange={mudar('dataFim')} />
          </Campo>
          <div className="grade-inteira">
            <fieldset className="campo">
              <legend>Serviços com desconto</legend>
              <label className="check">
                <input
                  type="checkbox"
                  checked={form.todosServicos}
                  onChange={(e) => setForm((f) => ({ ...f, todosServicos: e.target.checked }))}
                />
                Todos os serviços
              </label>
              {!form.todosServicos && (
                <Carga estado={servicos}>
                  <div className="lista-checks">
                    {(servicos.dados || []).map((s) => (
                      <label key={s.id} className="check">
                        <input type="checkbox" checked={form.servicoIds.includes(s.id)} onChange={() => alternarServico(s.id)} />
                        {s.nome}
                      </label>
                    ))}
                  </div>
                </Carga>
              )}
              {erros.servicos && <span className="erro-texto">{erros.servicos}</span>}
            </fieldset>
          </div>
          <div className="grade-inteira">
            <Campo id="pm-txt" rotulo="Descrição (opcional)" dica="Aparece no site, no cartão da promoção.">
              <textarea id="pm-txt" value={form.descricao} onChange={mudar('descricao')} rows={2} maxLength={1000} />
            </Campo>
          </div>
        </div>
        <div className="modal-rodape">
          <button type="button" className="botao botao-secundario" onClick={aoFechar}>Cancelar</button>
          <button type="submit" className="botao botao-primario" disabled={salvando}>
            {salvando ? 'Salvando…' : 'Salvar promoção'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

function EnvioModal({ promocao, aoFechar, aoEnviar }) {
  const avisos = useAvisos();
  const servicosTexto = promocao.todosServicos ? 'todos os serviços' : promocao.servicoNomes.join(', ');
  const [mensagem, setMensagem] = useState(
    `Olá, {nome}! 🔥 *${promocao.titulo}*: ${promocao.descontoPercentual}% de desconto em ${servicosTexto} ` +
      `(${rotuloDias(promocao.diasSemana).toLowerCase()}). Quer garantir seu horário? É só responder esta mensagem com o serviço, o dia e o horário ✂️`,
  );
  const [soAutorizadas, setSoAutorizadas] = useState(true);
  const [soQuemFez, setSoQuemFez] = useState(false);
  const [total, setTotal] = useState(null);
  const [enviandoAgora, setEnviandoAgora] = useState(false);

  useEffect(() => {
    let ativo = true;
    setTotal(null);
    adminApi
      .publicoPromocao(promocao.id, { soAutorizadas, soQuemFezServicos: soQuemFez })
      .then((r) => ativo && setTotal(r.total))
      .catch(() => ativo && setTotal(0));
    return () => {
      ativo = false;
    };
  }, [promocao.id, soAutorizadas, soQuemFez]);

  async function enviar() {
    if (!mensagem.trim()) return;
    if (!window.confirm(`Enviar a promoção para ${total} cliente(s) pelo WhatsApp? Não dá para desfazer.`)) return;
    setEnviandoAgora(true);
    try {
      await adminApi.enviarPromocao(promocao.id, { mensagem: mensagem.trim(), soAutorizadas, soQuemFezServicos: soQuemFez });
      avisos.sucesso('Envio iniciado. As mensagens saem uma a cada 3 segundos; acompanhe em “Envios por WhatsApp”.');
      aoEnviar();
    } catch (e) {
      avisos.erro(e.message);
    } finally {
      setEnviandoAgora(false);
    }
  }

  return (
    <Modal titulo={`Enviar: ${promocao.titulo}`} aoFechar={aoFechar} largo>
      <div className="modal-corpo">
        <Campo id="ev-msg" rotulo="Mensagem" dica="{nome} vira o primeiro nome da cliente. No fim entra sozinho: “Para não receber mais promoções, responda SAIR”.">
          <textarea id="ev-msg" value={mensagem} onChange={(e) => setMensagem(e.target.value)} rows={5} maxLength={1000} />
        </Campo>

        <fieldset className="campo">
          <legend>Quem recebe (clientes com pelo menos um atendimento concluído)</legend>
          <label className="check">
            <input type="radio" name="publico" checked={soAutorizadas} onChange={() => setSoAutorizadas(true)} />
            Só quem autorizou receber promoções <strong>(recomendado)</strong>
          </label>
          <label className="check">
            <input type="radio" name="publico" checked={!soAutorizadas} onChange={() => setSoAutorizadas(false)} />
            Todas com histórico, menos quem pediu para sair
          </label>
          {!soAutorizadas && (
            <p className="nota nota-erro">
              Mandar promoção para quem não autorizou aumenta o risco de denúncias e de bloqueio do número do WhatsApp.
            </p>
          )}
          {!promocao.todosServicos && (
            <label className="check">
              <input type="checkbox" checked={soQuemFez} onChange={(e) => setSoQuemFez(e.target.checked)} />
              Só quem já fez {servicosTexto}
            </label>
          )}
        </fieldset>

        <p className="nota nota-info">
          {total == null ? 'Contando clientes…' : total === 0 ? 'Nenhuma cliente se encaixa nesse público.' : `${total} cliente(s) vão receber.`}
        </p>
      </div>
      <div className="modal-rodape">
        <button type="button" className="botao botao-secundario" onClick={aoFechar}>Cancelar</button>
        <button type="button" className="botao botao-primario" onClick={enviar} disabled={enviandoAgora || !total}>
          {enviandoAgora ? 'Iniciando…' : `Enviar para ${total ?? '…'}`}
        </button>
      </div>
    </Modal>
  );
}
