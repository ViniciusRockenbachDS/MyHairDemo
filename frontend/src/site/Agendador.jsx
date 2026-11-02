import { useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { urlImagem } from '../api/http.js';
import { apiPublica } from '../api/publico.js';
import { useAoVivo } from '../aovivo/AoVivo.jsx';
import { useCarregar } from '../hooks/useCarregar.js';
import {
  capitalizar, dataDeIso, dataLonga, diaSemanaIso, duracao, hora, horaDe, isoData, mascaraTelefone, moeda,
  linkWhatsapp,
} from '../util/formato.js';
import { melhorPromocao, melhorPromocaoDoServico, precoComDesconto } from '../util/promocoes.js';
import Icone from './Icone.jsx';

const TOTAL_PASSOS = 5;
const DIAS_EXIBIDOS = 14;
const PERIODOS = [
  ['Manhã', 0, 12],
  ['Tarde', 12, 18],
  ['Noite', 18, 24],
];

/** "quinta" (sem o "-feira") */
const diaSemana = (d) => d.toLocaleDateString('pt-BR', { weekday: 'long' }).replace('-feira', '');
/** "out" */
const mesCurto = (d) => d.toLocaleDateString('pt-BR', { month: 'short' }).replace('.', '');
/** "quarta, 14 out" */
const dataEtiqueta = (iso) => {
  const d = dataDeIso(iso);
  return `${diaSemana(d)}, ${d.getDate()} ${mesCurto(d)}`;
};

function proximosDias(horariosSalao) {
  const fechados = new Set((horariosSalao || []).filter((h) => h.fechado).map((h) => h.diaSemana));
  const hoje = new Date();
  hoje.setHours(0, 0, 0, 0);
  return Array.from({ length: DIAS_EXIBIDOS }, (_, i) => {
    const d = new Date(hoje);
    d.setDate(hoje.getDate() + i);
    return {
      iso: isoData(d),
      data: d,
      aberto: !fechados.has(diaSemanaIso(d)),
      rotulo: i === 0 ? 'Hoje' : i === 1 ? 'Amanhã' : diaSemana(d),
    };
  });
}

function Avatar({ foto, nome, icone }) {
  return (
    <span className="ag-avatar" aria-hidden="true">
      {foto ? <img src={urlImagem(foto)} alt="" /> : icone ? <Icone nome={icone} tamanho={22} /> : nome.trim().charAt(0)}
    </span>
  );
}

function Radio({ marcado }) {
  return (
    <span className={`ag-radio${marcado ? ' marcado' : ''}`} aria-hidden="true">
      {marcado && <Icone nome="check" tamanho={16} />}
    </span>
  );
}

/**
 * Agendamento em 5 passos: serviço → profissional → data → horário → dados.
 *
 * Preços, durações, profissionais e horários livres vêm da API e são
 * recarregados ao vivo: se o salão mudar algo no /admin ou alguém reservar o
 * horário escolhido, a tela se ajusta sozinha.
 */
export default function Agendador({
  servicos, erroServicos, horariosSalao, pedido, aoConcluir, promocoes = null, whatsappSalao = null,
}) {
  const [passo, setPasso] = useState(1);
  const [servicoId, setServicoId] = useState(null);
  const [profissionalId, setProfissionalId] = useState(undefined); // undefined = ainda não escolheu; null = sem preferência
  const [data, setData] = useState(null);
  const [horaEscolhida, setHoraEscolhida] = useState(null);
  const [nome, setNome] = useState('');
  const [telefone, setTelefone] = useState('');
  const [aceitaPromocoes, setAceitaPromocoes] = useState(false);
  const [errosCampos, setErrosCampos] = useState({});
  const [enviando, setEnviando] = useState(false);
  const [problema, setProblema] = useState(null); // { mensagem, sugestoes }
  const [aviso, setAviso] = useState(null);
  const [reserva, setReserva] = useState(null);
  const [estadoSinal, setEstadoSinal] = useState(null); // null | 'PAGO' | 'EXPIRADO'
  const cartao = useRef(null);
  const primeiraRenderizacao = useRef(true);

  const servico = useMemo(() => (servicos || []).find((s) => s.id === servicoId) || null, [servicos, servicoId]);
  const dias = useMemo(() => proximosDias(horariosSalao), [horariosSalao]);

  const profissionais = useCarregar(
    () => (servicoId ? apiPublica.profissionaisDoServico(servicoId) : Promise.resolve([])),
    [servicoId],
  );
  const disponibilidade = useCarregar(
    () =>
      servicoId && data && profissionalId !== undefined
        ? apiPublica.disponibilidade(servicoId, data, profissionalId)
        : Promise.resolve(null),
    [servicoId, data, profissionalId],
  );

  useAoVivo(() => {
    profissionais.recarregar(true);
    disponibilidade.recarregar(true);
  }, ['agenda', 'profissionais', 'servicos', 'salao']);

  const horariosLivres = useMemo(() => {
    if (!disponibilidade.dados) return [];
    const todos = new Set();
    disponibilidade.dados.forEach((p) => p.horarios.forEach((h) => todos.add(hora(h))));
    return [...todos].sort();
  }, [disponibilidade.dados]);

  const profissional = profissionais.dados?.find((p) => p.id === profissionalId) || null;
  const promoDoDia = melhorPromocao(promocoes, servicoId, data);
  const nomeProfissional = profissionalId === null ? 'Sem preferência' : profissional?.nome;

  // ------------------------------------------------------------------ navegação

  function irPara(n) {
    setPasso(n);
  }

  // Ao trocar de passo no celular, volta o cartão para o topo da tela.
  useEffect(() => {
    if (primeiraRenderizacao.current) {
      primeiraRenderizacao.current = false;
      return;
    }
    const topo = cartao.current?.getBoundingClientRect().top ?? 0;
    if (topo < 0) cartao.current.scrollIntoView({ block: 'start' });
  }, [passo, reserva]);

  function escolherServico(id) {
    setServicoId(id);
    setProfissionalId(undefined);
    setData(null);
    setHoraEscolhida(null);
    setProblema(null);
    setAviso(null);
    irPara(2);
  }

  function escolherProfissional(id) {
    setProfissionalId(id);
    setHoraEscolhida(null);
    setProblema(null);
    setAviso(null);
    irPara(3);
  }

  function escolherDia(iso) {
    setData(iso);
    setHoraEscolhida(null);
    setProblema(null);
    setAviso(null);
    irPara(4);
  }

  function escolherHora(h) {
    setHoraEscolhida(h);
    setProblema(null);
    setAviso(null);
    irPara(5);
  }

  function aplicarSugestao(s) {
    setProfissionalId(s.profissionalId);
    setData(s.inicio.slice(0, 10));
    setHoraEscolhida(horaDe(s.inicio));
    setProblema(null);
    setAviso(null);
    irPara(5);
  }

  // Vindo de um card de serviço do site (/agendar?servico=3): já começa no passo 2.
  useEffect(() => {
    if (pedido?.servicoId) escolherServico(Number(pedido.servicoId));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pedido]);

  // ------------------------------------------------------------------ mudanças ao vivo

  // O salão desativou o serviço escolhido.
  useEffect(() => {
    if (servicoId && servicos && !servico) {
      setServicoId(null);
      setProfissionalId(undefined);
      setHoraEscolhida(null);
      irPara(1);
      setAviso('O serviço escolhido não está mais disponível para agendamento online. Escolha outro.');
    }
  }, [servicos, servico, servicoId]);

  // A profissional escolhida deixou de fazer o serviço.
  useEffect(() => {
    if (profissionalId && profissionais.dados && !profissionais.dados.some((p) => p.id === profissionalId)) {
      setProfissionalId(undefined);
      setHoraEscolhida(null);
      irPara(2);
      setAviso('A profissional escolhida não está mais disponível para este serviço. Escolha outra.');
    }
  }, [profissionais.dados, profissionalId]);

  // Alguém reservou o horário que estava escolhido.
  useEffect(() => {
    if (horaEscolhida && !disponibilidade.carregando && disponibilidade.dados && !horariosLivres.includes(horaEscolhida)) {
      setHoraEscolhida(null);
      irPara(4);
      setAviso('O horário que você escolheu acabou de ser reservado. Escolha outro, por favor.');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [horariosLivres]);

  // ------------------------------------------------------------------ envio

  async function confirmar(e) {
    e.preventDefault();
    const erros = {};
    if (!nome.trim()) erros.nome = 'Informe seu nome.';
    const digitos = telefone.replace(/\D/g, '');
    if (digitos.length < 10 || digitos.length > 11) erros.telefone = 'Informe seu WhatsApp com DDD.';
    setErrosCampos(erros);
    if (Object.keys(erros).length) return;

    setEnviando(true);
    setProblema(null);
    try {
      const r = await apiPublica.agendar({
        servicoId,
        profissionalId,
        data,
        hora: horaEscolhida,
        nome: nome.trim(),
        telefone: digitos,
        aceitaPromocoes,
      });
      setReserva(r);
      if (aoConcluir) aoConcluir(r);
    } catch (erro) {
      if (erro.status === 409) {
        setHoraEscolhida(null);
        setProblema({ mensagem: erro.message, sugestoes: erro.sugestoes || [] });
        disponibilidade.recarregar(true);
        irPara(4);
      } else if (erro.status === 400 && erro.campos) {
        setErrosCampos({ nome: erro.campos.nome, telefone: erro.campos.telefone });
      } else {
        setProblema({ mensagem: erro.message, sugestoes: [] });
      }
    } finally {
      setEnviando(false);
    }
  }

  function recomecar() {
    setReserva(null);
    setEstadoSinal(null);
    setServicoId(null);
    setProfissionalId(undefined);
    setData(null);
    setHoraEscolhida(null);
    setProblema(null);
    setAviso(null);
    irPara(1);
  }

  // ================================================================== confirmação
  if (reserva) {
    const inicio = new Date(reserva.inicio);

    // serviço com sinal: primeiro o Pix, depois a confirmação
    if (reserva.sinal && estadoSinal === 'EXPIRADO') {
      return (
        <div className="ag-cartao ag-sucesso" ref={cartao} role="alert">
          <h2 className="ag-titulo">O prazo do sinal terminou</h2>
          <p className="ag-sub">Como o Pix não foi confirmado a tempo, o horário foi liberado.</p>
          <button type="button" className="botao botao-primario botao-largo" onClick={recomecar}>Agendar de novo</button>
        </div>
      );
    }
    if (reserva.sinal && estadoSinal !== 'PAGO') {
      return (
        <div ref={cartao}>
          <PagamentoSinal reserva={reserva} whatsappSalao={whatsappSalao} aoMudar={setEstadoSinal} />
        </div>
      );
    }

    return (
      <div className="ag-cartao ag-sucesso" ref={cartao} role="status">
        <span className="ag-sucesso-icone" aria-hidden="true"><Icone nome="check" tamanho={30} /></span>
        <h2 className="ag-titulo">{reserva.sinal ? 'Sinal recebido! Horário confirmado.' : 'Horário reservado!'}</h2>
        <p className="ag-sub">Enviamos os detalhes para o seu WhatsApp.</p>
        <div className="ag-resumo">
          <dl>
            <div><dt>Serviço</dt><dd>{reserva.servico}</dd></div>
            <div><dt>Profissional</dt><dd>{reserva.profissional}</dd></div>
            <div><dt>Data</dt><dd>{capitalizar(dataLonga(inicio))}</dd></div>
            <div><dt>Horário</dt><dd>{horaDe(reserva.inicio)}</dd></div>
          </dl>
          {reserva.sinal ? (
            <>
              <div className="ag-resumo-total"><span>Sinal pago</span><strong>{moeda(reserva.sinal.valor)}</strong></div>
              <div className="ag-resumo-total"><span>Falta pagar no dia</span><strong>{moeda(reserva.sinal.restante)}</strong></div>
            </>
          ) : (
            <div className="ag-resumo-total"><span>Total</span><strong>{moeda(reserva.preco)}</strong></div>
          )}
        </div>
        <p className="ag-nota">
          Um dia antes você recebe um lembrete no WhatsApp para confirmar a presença. Precisou desmarcar? É só
          responder <strong>cancelar</strong>.
        </p>
        <button type="button" className="botao botao-secundario botao-largo" onClick={recomecar}>
          Agendar outro horário
        </button>
      </div>
    );
  }

  // ================================================================== passos
  const etiquetas = [
    servico && { passo: 1, icone: 'tesoura', texto: servico.nome },
    passo > 2 && nomeProfissional && { passo: 2, icone: 'usuario', texto: nomeProfissional },
    passo > 3 && data && { passo: 3, icone: 'calendario', texto: dataEtiqueta(data) },
    passo > 4 && horaEscolhida && { passo: 4, icone: 'relogio', texto: horaEscolhida },
  ].filter(Boolean);

  return (
    <div className="ag-cartao" ref={cartao}>
      <div className="ag-topo">
        {passo === 1 ? (
          <span className="ag-comece">Comece por aqui</span>
        ) : (
          <button type="button" className="ag-voltar" onClick={() => irPara(passo - 1)}>
            <Icone nome="seta" tamanho={18} className="ag-voltar-seta" /> Voltar
          </button>
        )}
        <span className="ag-contador">Passo {passo} de {TOTAL_PASSOS}</span>
      </div>
      <div
        className="ag-progresso"
        role="progressbar"
        aria-valuemin={1}
        aria-valuemax={TOTAL_PASSOS}
        aria-valuenow={passo}
        aria-label="Progresso do agendamento"
      >
        {Array.from({ length: TOTAL_PASSOS }, (_, i) => (
          <span key={i} className={i < passo ? 'feito' : undefined} />
        ))}
      </div>

      {etiquetas.length > 0 && (
        <ul className="ag-etiquetas" aria-label="Suas escolhas">
          {etiquetas.map((e) => (
            <li key={e.passo}>
              <button type="button" onClick={() => irPara(e.passo)} aria-label={`Alterar: ${e.texto}`}>
                <Icone nome={e.icone} tamanho={16} />
                {e.texto}
              </button>
            </li>
          ))}
        </ul>
      )}

      {aviso && <p className="ag-aviso" role="status">{aviso}</p>}

      {/* ---------------------------------------------------------- 1. serviço */}
      {passo === 1 && (
        <section aria-labelledby="ag-t1">
          <h2 className="ag-titulo" id="ag-t1">Escolha o serviço</h2>
          <p className="ag-sub">Toque no serviço desejado para continuar</p>
          {!servicos ? (
            <p className="estado">{erroServicos ? 'Não foi possível carregar os serviços. Atualize a página.' : 'Carregando serviços…'}</p>
          ) : servicos.length === 0 ? (
            <p className="estado">Nenhum serviço disponível para agendamento online no momento.</p>
          ) : (
            <ul className="ag-lista" role="radiogroup" aria-labelledby="ag-t1">
              {servicos.map((s) => (
                <li key={s.id}>
                  <button
                    type="button"
                    role="radio"
                    aria-checked={s.id === servicoId}
                    className={`ag-opcao${s.id === servicoId ? ' marcada' : ''}`}
                    onClick={() => escolherServico(s.id)}
                  >
                    <span className="ag-opcao-textos">
                      <span className="ag-opcao-nome">{s.nome}</span>
                      <span className="ag-opcao-info">
                        <strong>{moeda(s.preco)}</strong> · {duracao(s.duracaoMinutos)}
                        {melhorPromocaoDoServico(promocoes, s.id) && (
                          <span className="ag-tag-promo">🔥 até -{melhorPromocaoDoServico(promocoes, s.id).descontoPercentual}%</span>
                        )}
                      </span>
                    </span>
                    <Radio marcado={s.id === servicoId} />
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      {/* ---------------------------------------------------------- 2. profissional */}
      {passo === 2 && (
        <section aria-labelledby="ag-t2">
          <h2 className="ag-titulo" id="ag-t2">Escolha a profissional</h2>
          <p className="ag-sub">Quem vai te atender</p>
          {profissionais.carregando && !profissionais.dados ? (
            <p className="estado">Carregando profissionais…</p>
          ) : profissionais.erro ? (
            <p className="erro-texto">Não foi possível carregar as profissionais. Tente de novo.</p>
          ) : (
            <ul className="ag-lista" role="radiogroup" aria-labelledby="ag-t2">
              <li>
                <button
                  type="button"
                  role="radio"
                  aria-checked={profissionalId === null}
                  className={`ag-opcao${profissionalId === null ? ' marcada' : ''}`}
                  onClick={() => escolherProfissional(null)}
                >
                  <Avatar icone="usuarios" nome="" />
                  <span className="ag-opcao-textos">
                    <span className="ag-opcao-nome">Sem preferência</span>
                    <span className="ag-opcao-info">Mostramos os horários de todas</span>
                  </span>
                  <Radio marcado={profissionalId === null} />
                </button>
              </li>
              {(profissionais.dados || []).map((p) => (
                <li key={p.id}>
                  <button
                    type="button"
                    role="radio"
                    aria-checked={p.id === profissionalId}
                    className={`ag-opcao${p.id === profissionalId ? ' marcada' : ''}`}
                    onClick={() => escolherProfissional(p.id)}
                  >
                    <Avatar foto={p.fotoUrl} nome={p.nome} />
                    <span className="ag-opcao-textos">
                      <span className="ag-opcao-nome">{p.nome}</span>
                      {p.especialidade && <span className="ag-opcao-info">{p.especialidade}</span>}
                    </span>
                    <Radio marcado={p.id === profissionalId} />
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      {/* ---------------------------------------------------------- 3. data */}
      {passo === 3 && (
        <section aria-labelledby="ag-t3">
          <h2 className="ag-titulo" id="ag-t3">Escolha a data</h2>
          <p className="ag-sub">Próximos {DIAS_EXIBIDOS} dias</p>
          <div className="ag-dias" role="radiogroup" aria-labelledby="ag-t3">
            {dias.map((d) => (
              <button
                type="button"
                key={d.iso}
                role="radio"
                aria-checked={data === d.iso}
                aria-label={`${capitalizar(dataLonga(d.data))}${d.aberto ? '' : ', salão fechado'}`}
                className={`ag-dia${data === d.iso ? ' marcada' : ''}`}
                disabled={!d.aberto}
                onClick={() => escolherDia(d.iso)}
              >
                <span className="ag-dia-semana">{d.rotulo}</span>
                <span className="ag-dia-numero">
                  {d.aberto ? d.data.getDate() : <Icone nome="proibido" tamanho={26} />}
                </span>
                <span className="ag-dia-mes">{mesCurto(d.data)}</span>
                {d.aberto && melhorPromocao(promocoes, servicoId, d.iso) && (
                  <span className="ag-dia-promo">-{melhorPromocao(promocoes, servicoId, d.iso).descontoPercentual}%</span>
                )}
              </button>
            ))}
          </div>
        </section>
      )}

      {/* ---------------------------------------------------------- 4. horário */}
      {passo === 4 && (
        <section aria-labelledby="ag-t4">
          <h2 className="ag-titulo" id="ag-t4">Escolha o horário</h2>
          <p className="ag-sub">{data && capitalizar(dataLonga(dataDeIso(data)))}</p>

          {problema && (
            <div className="ag-problema" role="alert">
              <p>{problema.mensagem}</p>
              {problema.sugestoes?.length > 0 && (
                <>
                  <p className="ag-problema-sub">Horários livres mais próximos:</p>
                  <div className="ag-horarios">
                    {problema.sugestoes.map((s) => (
                      <button
                        type="button"
                        className="ag-horario ag-horario-sugestao"
                        key={`${s.inicio}-${s.profissionalId}`}
                        onClick={() => aplicarSugestao(s)}
                      >
                        {dataEtiqueta(s.inicio)}, {horaDe(s.inicio)}
                        <small>com {s.profissionalNome}</small>
                      </button>
                    ))}
                  </div>
                </>
              )}
            </div>
          )}

          <div aria-live="polite">
            {disponibilidade.carregando && !disponibilidade.dados ? (
              <p className="estado">Buscando horários livres…</p>
            ) : disponibilidade.erro ? (
              <p className="erro-texto">
                Não foi possível carregar os horários.{' '}
                <button type="button" className="botao-texto" onClick={() => disponibilidade.recarregar()}>
                  Tentar de novo
                </button>
              </p>
            ) : horariosLivres.length === 0 ? (
              <div className="ag-vazio">
                <p>
                  Não há horários livres neste dia{profissionalId ? ' com esta profissional' : ''}.
                </p>
                <button type="button" className="botao botao-secundario" onClick={() => irPara(3)}>
                  Escolher outra data
                </button>
              </div>
            ) : (
              PERIODOS.map(([rotulo, inicio, fim]) => {
                const lista = horariosLivres.filter((h) => {
                  const hh = Number(h.slice(0, 2));
                  return hh >= inicio && hh < fim;
                });
                if (!lista.length) return null;
                return (
                  <div className="ag-periodo" key={rotulo}>
                    <p className="ag-periodo-nome">{rotulo}</p>
                    <div className="ag-horarios" role="radiogroup" aria-label={`Horários da ${rotulo.toLowerCase()}`}>
                      {lista.map((h) => (
                        <button
                          type="button"
                          key={h}
                          role="radio"
                          aria-checked={horaEscolhida === h}
                          className={`ag-horario${horaEscolhida === h ? ' marcada' : ''}`}
                          onClick={() => escolherHora(h)}
                        >
                          {h}
                        </button>
                      ))}
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </section>
      )}

      {/* ---------------------------------------------------------- 5. dados */}
      {passo === 5 && servico && data && horaEscolhida && (
        <form onSubmit={confirmar} noValidate aria-labelledby="ag-t5">
          <h2 className="ag-titulo" id="ag-t5">Seus dados</h2>
          <p className="ag-sub">Para confirmar o horário no seu nome</p>

          <div className="ag-campos">
            <div className="campo">
              <label htmlFor="ag-nome">Nome completo</label>
              <input
                id="ag-nome"
                value={nome}
                onChange={(e) => setNome(e.target.value)}
                autoComplete="name"
                placeholder="Seu nome"
                aria-invalid={!!errosCampos.nome}
                aria-describedby={errosCampos.nome ? 'ag-nome-erro' : undefined}
              />
              {errosCampos.nome && <span id="ag-nome-erro" className="erro-texto">{errosCampos.nome}</span>}
            </div>
            <div className="campo">
              <label htmlFor="ag-tel">WhatsApp</label>
              <input
                id="ag-tel"
                value={telefone}
                onChange={(e) => setTelefone(mascaraTelefone(e.target.value))}
                inputMode="tel"
                autoComplete="tel-national"
                placeholder="(47) 99999-9999"
                aria-invalid={!!errosCampos.telefone}
                aria-describedby={errosCampos.telefone ? 'ag-tel-erro' : undefined}
              />
              {errosCampos.telefone && <span id="ag-tel-erro" className="erro-texto">{errosCampos.telefone}</span>}
            </div>
          </div>

          <div className="ag-resumo">
            <p className="ag-resumo-titulo">Resumo</p>
            <dl>
              <div><dt>Serviço</dt><dd>{servico.nome}</dd></div>
              <div><dt>Profissional</dt><dd>{profissionalId === null ? 'A primeira livre' : nomeProfissional}</dd></div>
              <div><dt>Data</dt><dd>{capitalizar(dataLonga(dataDeIso(data)))}</dd></div>
              <div><dt>Horário</dt><dd>{horaEscolhida}</dd></div>
              <div><dt>Duração</dt><dd>{duracao(servico.duracaoMinutos)}</dd></div>
            </dl>
            {promoDoDia ? (
              <>
                <p className="ag-resumo-promo">🔥 {promoDoDia.titulo}: -{promoDoDia.descontoPercentual}%</p>
                <div className="ag-resumo-total">
                  <span>Total</span>
                  <strong>
                    <s className="ag-preco-antigo">{moeda(servico.preco)}</s>{' '}
                    {moeda(precoComDesconto(servico.preco, promoDoDia.descontoPercentual))}
                  </strong>
                </div>
              </>
            ) : (
              <div className="ag-resumo-total"><span>Total</span><strong>{moeda(servico.preco)}</strong></div>
            )}
          </div>

          {servico.exigeSinal && (
            <p className="ag-aviso-sinal">
              💰 Para garantir este horário, é cobrado um <strong>sinal de {moeda(servico.valorSinal)}</strong> por Pix logo
              após confirmar. <strong>O sinal é descontado do valor do serviço.</strong>
            </p>
          )}

          {problema && <p className="ag-problema" role="alert">{problema.mensagem}</p>}

          <label className="ag-check-promo">
            <input type="checkbox" checked={aceitaPromocoes} onChange={(e) => setAceitaPromocoes(e.target.checked)} />
            Quero receber promoções do salão pelo WhatsApp
          </label>

          <p className="ag-privacidade">
            Usamos seu nome e WhatsApp só para este agendamento e para enviar lembretes. Veja nossa{' '}
            <Link to="/privacidade" target="_blank" rel="noreferrer">Política de Privacidade</Link>.
          </p>

          <button type="submit" className="botao botao-primario botao-largo ag-confirmar" disabled={enviando}>
            <Icone nome="check" />{' '}
            {enviando ? 'Reservando…' : servico.exigeSinal ? 'Confirmar e pagar o sinal' : 'Confirmar agendamento'}
          </button>
          <p className="ag-nota">Só nome e WhatsApp. Sem cadastro e sem senha.</p>
        </form>
      )}
    </div>
  );
}

/**
 * Pagamento do sinal: QR Code + Pix copia e cola + contagem do prazo.
 * Pergunta ao servidor a cada 5 s se o pagamento já foi confirmado.
 */
function PagamentoSinal({ reserva, whatsappSalao, aoMudar }) {
  const { sinal } = reserva;
  const [copiado, setCopiado] = useState(false);
  const [restante, setRestante] = useState(() => new Date(sinal.expiraEm) - Date.now());

  useEffect(() => {
    let ativo = true;
    const consultar = async () => {
      try {
        const r = await apiPublica.statusSinal(sinal.codigo);
        if (!ativo) return;
        if (r.status === 'CONFIRMADO') aoMudar('PAGO');
        else if (r.status === 'CANCELADO') aoMudar('EXPIRADO');
      } catch {
        /* sem conexão: tenta de novo no próximo ciclo */
      }
    };
    const consulta = setInterval(consultar, 5000);
    const relogio = setInterval(() => setRestante(new Date(sinal.expiraEm) - Date.now()), 1000);
    return () => {
      ativo = false;
      clearInterval(consulta);
      clearInterval(relogio);
    };
  }, [sinal.codigo, sinal.expiraEm, aoMudar]);

  async function copiar() {
    try {
      await navigator.clipboard.writeText(sinal.copiaCola);
      setCopiado(true);
      setTimeout(() => setCopiado(false), 2500);
    } catch {
      setCopiado(false);
    }
  }

  const segundos = Math.max(0, Math.floor(restante / 1000));
  const relogio = `${String(Math.floor(segundos / 60)).padStart(2, '0')}:${String(segundos % 60).padStart(2, '0')}`;
  const quando = `${capitalizar(dataLonga(new Date(reserva.inicio)))} às ${horaDe(reserva.inicio)}`;

  return (
    <div className="ag-cartao ag-pix" role="region" aria-label="Pagamento do sinal">
      <h2 className="ag-titulo">Pague o sinal para garantir seu horário</h2>
      <p className="ag-sub">
        {reserva.servico} com {reserva.profissional}, {quando}.
      </p>

      <div className="ag-pix-valor">
        <span>Sinal</span>
        <strong>{moeda(sinal.valor)}</strong>
      </div>
      <p className="ag-pix-prazo" aria-live="polite">
        Seu horário fica reservado por <strong>{relogio}</strong>
      </p>

      <img className="ag-pix-qr" src={`data:image/png;base64,${sinal.qrCodeBase64}`} alt="QR Code do Pix do sinal" />

      <label className="ag-pix-rotulo" htmlFor="ag-pix-codigo">Pix copia e cola</label>
      <textarea id="ag-pix-codigo" className="ag-pix-codigo" readOnly value={sinal.copiaCola} rows={3} onFocus={(e) => e.target.select()} />
      <button type="button" className="botao botao-primario botao-largo" onClick={copiar}>
        <Icone nome="check" /> {copiado ? 'Código copiado!' : 'Copiar código Pix'}
      </button>

      <p className="ag-aviso-sinal">
        💡 <strong>O sinal é descontado do valor do serviço:</strong> no dia, você paga só {moeda(sinal.restante)}.
      </p>

      {sinal.automatico ? (
        <p className="ag-nota">Assim que o pagamento for confirmado, esta tela muda sozinha. Não precisa enviar comprovante.</p>
      ) : (
        <>
          <p className="ag-nota">
            Depois de pagar, envie o comprovante pelo WhatsApp do salão. Assim que a equipe conferir, esta tela muda sozinha.
          </p>
          {whatsappSalao && (
            <a
              className="botao botao-secundario botao-largo"
              href={linkWhatsapp(whatsappSalao, `Olá! Segue o comprovante do sinal do meu agendamento de ${reserva.servico}, ${quando}.`)}
              target="_blank"
              rel="noreferrer"
            >
              <Icone nome="mensagem" /> Enviar comprovante
            </a>
          )}
        </>
      )}
    </div>
  );
}
