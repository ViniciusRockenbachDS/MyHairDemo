import { useEffect, useState } from 'react';
import { adminApi } from '../../api/admin.js';
import { useAoVivo } from '../../aovivo/AoVivo.jsx';
import { useCarregar } from '../../hooks/useCarregar.js';
import { hora, NOMES_DIAS, telefoneVisivel } from '../../util/formato.js';
import { Campo, Carga, EnviarImagem, Modal, useAvisos } from '../componentes.jsx';

const DIAS = [1, 2, 3, 4, 5, 6, 7];

export default function Profissionais() {
  const avisos = useAvisos();
  const profissionais = useCarregar(() => adminApi.profissionais(), []);
  const servicos = useCarregar(() => adminApi.servicos(), []);
  const [cadastro, setCadastro] = useState(null);     // {} = nova, objeto = editar
  const [configurar, setConfigurar] = useState(null); // profissional para serviços e horários

  useAoVivo(() => profissionais.recarregar(true), ['profissionais']);
  useAoVivo(() => servicos.recarregar(true), ['servicos']);

  const nomeServico = (id) => servicos.dados?.find((s) => s.id === id)?.nome;

  async function alternarAtiva(p) {
    if (p.ativo && !window.confirm(`Desativar ${p.nome}? Ela some do site e do WhatsApp. Agendamentos já marcados não são cancelados.`)) return;
    try {
      await adminApi.profissionalAtiva(p.id, !p.ativo);
      avisos.sucesso(p.ativo ? `${p.nome} desativada.` : `${p.nome} reativada.`);
      profissionais.recarregar(true);
    } catch (e) {
      avisos.erro(e.message);
    }
  }

  return (
    <div className="pagina">
      <header className="pagina-cabeca">
        <div>
          <h1>Profissionais</h1>
          <p className="pagina-sub">Cada profissional só aparece para os serviços marcados e nos horários do expediente dela.</p>
        </div>
        <button type="button" className="botao botao-primario" onClick={() => setCadastro({})}>
          Nova profissional
        </button>
      </header>

      <Carga estado={profissionais} vazio="Nenhuma profissional cadastrada.">
        <ul className="lista-cartoes">
          {(profissionais.dados || []).map((p) => (
            <li key={p.id} className={`cartao-profissional${p.ativo ? '' : ' inativo'}`}>
              <div className="cartao-profissional-topo">
                <div>
                  <h2>{p.nome}</h2>
                  <p className="dica">
                    {p.especialidade || 'Sem função definida'}
                    {p.telefone ? `, ${telefoneVisivel(p.telefone)}` : ''}
                    {!p.ativo && ', desativada'}
                  </p>
                </div>
              </div>
              <p className="cartao-profissional-servicos">
                {p.servicoIds.length
                  ? p.servicoIds.map(nomeServico).filter(Boolean).join(', ')
                  : 'Nenhum serviço vinculado: ela não aparece para agendamento.'}
              </p>
              <div className="acoes">
                <button type="button" className="botao botao-secundario botao-pequeno" onClick={() => setConfigurar(p)}>
                  Serviços e horários
                </button>
                <button type="button" className="botao-texto" onClick={() => setCadastro(p)}>Editar dados</button>
                <button type="button" className="botao-texto" onClick={() => alternarAtiva(p)}>
                  {p.ativo ? 'Desativar' : 'Reativar'}
                </button>
              </div>
            </li>
          ))}
        </ul>
      </Carga>

      {cadastro && (
        <CadastroModal
          profissional={cadastro.id ? cadastro : null}
          aoFechar={() => setCadastro(null)}
          aoSalvar={(salva, nova) => {
            setCadastro(null);
            profissionais.recarregar(true);
            if (nova) setConfigurar(salva); // já abre serviços e horários
          }}
        />
      )}
      {configurar && (
        <ConfigurarModal
          profissional={configurar}
          servicos={servicos.dados || []}
          aoFechar={() => setConfigurar(null)}
          aoSalvar={() => profissionais.recarregar(true)}
        />
      )}
    </div>
  );
}

function CadastroModal({ profissional, aoFechar, aoSalvar }) {
  const avisos = useAvisos();
  const [nome, setNome] = useState(profissional?.nome || '');
  const [especialidade, setEspecialidade] = useState(profissional?.especialidade || '');
  const [telefone, setTelefone] = useState(profissional?.telefone ? telefoneVisivel(profissional.telefone) : '');
  const [bio, setBio] = useState(profissional?.bio || '');
  const [fotoUrl, setFotoUrl] = useState(profissional?.fotoUrl || '');
  const [erro, setErro] = useState(null);
  const [salvando, setSalvando] = useState(false);

  async function salvar(e) {
    e.preventDefault();
    if (!nome.trim()) {
      setErro('Informe o nome.');
      return;
    }
    setSalvando(true);
    const corpo = {
      nome: nome.trim(),
      especialidade: especialidade.trim() || null,
      telefone: telefone.trim() || null,
      bio: bio.trim() || null,
      fotoUrl: fotoUrl.trim() || null,
    };
    try {
      const salva = profissional
        ? await adminApi.atualizarProfissional(profissional.id, corpo)
        : await adminApi.criarProfissional(corpo);
      avisos.sucesso(profissional ? 'Dados atualizados.' : `${salva.nome} cadastrada. Agora defina serviços e horários.`);
      aoSalvar(salva, !profissional);
    } catch (err) {
      avisos.erro(err.message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <Modal titulo={profissional ? `Editar ${profissional.nome}` : 'Nova profissional'} aoFechar={aoFechar}>
      <form onSubmit={salvar} noValidate>
        <div className="modal-corpo grade-2">
          <div className="grade-inteira">
            <Campo id="pf-nome" rotulo="Nome" erro={erro}>
              <input id="pf-nome" value={nome} onChange={(e) => setNome(e.target.value)} maxLength={120} />
            </Campo>
          </div>
          <Campo id="pf-esp" rotulo="Função" dica="Aparece no site. Ex.: Cabeleireira, Manicure.">
            <input id="pf-esp" value={especialidade} onChange={(e) => setEspecialidade(e.target.value)} maxLength={120} />
          </Campo>
          <Campo id="pf-tel" rotulo="Telefone (opcional)">
            <input id="pf-tel" value={telefone} onChange={(e) => setTelefone(e.target.value)} inputMode="tel" maxLength={20} />
          </Campo>
          <div className="grade-inteira">
            <Campo id="pf-bio" rotulo="Apresentação no site" dica="Uma ou duas frases sobre ela. Aparece no card da equipe.">
              <textarea id="pf-bio" value={bio} onChange={(e) => setBio(e.target.value)} maxLength={1000} />
            </Campo>
          </div>
          <div className="grade-inteira">
            <EnviarImagem
              id="pf-foto"
              rotulo="Foto (opcional)"
              formato="redondo"
              valor={fotoUrl}
              aoMudar={setFotoUrl}
              dica="Sem foto, o site mostra a inicial do nome."
            />
          </div>
        </div>
        <div className="modal-rodape">
          <button type="button" className="botao botao-secundario" onClick={aoFechar}>Cancelar</button>
          <button type="submit" className="botao botao-primario" disabled={salvando}>
            {salvando ? 'Salvando…' : profissional ? 'Salvar dados' : 'Cadastrar'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

/** Serviços que a profissional realiza + grade semanal de expediente. */
function ConfigurarModal({ profissional, servicos, aoFechar, aoSalvar }) {
  const avisos = useAvisos();
  const detalhe = useCarregar(() => adminApi.profissional(profissional.id), [profissional.id]);
  const salao = useCarregar(() => adminApi.salaoHorarios(), []);
  const [marcados, setMarcados] = useState(new Set(profissional.servicoIds));
  const [faixas, setFaixas] = useState(null); // { 1: [{inicio, fim}], ... }
  const [salvando, setSalvando] = useState(false);

  useEffect(() => {
    if (!detalhe.dados) return;
    const porDia = Object.fromEntries(DIAS.map((d) => [d, []]));
    (detalhe.dados.horarios || []).forEach((f) =>
      porDia[f.diaSemana].push({ inicio: hora(f.horaInicio), fim: hora(f.horaFim) }),
    );
    setFaixas(porDia);
    setMarcados(new Set(detalhe.dados.servicoIds));
  }, [detalhe.dados]);

  const funcionamento = (dia) => salao.dados?.find((h) => h.diaSemana === dia);

  const alternarServico = (id) =>
    setMarcados((m) => {
      const novo = new Set(m);
      if (novo.has(id)) novo.delete(id);
      else novo.add(id);
      return novo;
    });

  const mudarFaixa = (dia, i, campo, valor) =>
    setFaixas((f) => ({ ...f, [dia]: f[dia].map((x, j) => (j === i ? { ...x, [campo]: valor } : x)) }));

  const adicionarFaixa = (dia) => {
    const h = funcionamento(dia);
    const padrao = { inicio: h && !h.fechado ? hora(h.abertura) : '09:00', fim: h && !h.fechado ? hora(h.fechamento) : '18:00' };
    setFaixas((f) => ({ ...f, [dia]: [...f[dia], padrao] }));
  };

  const removerFaixa = (dia, i) => setFaixas((f) => ({ ...f, [dia]: f[dia].filter((_, j) => j !== i) }));

  const copiarSegundaParaSemana = () =>
    setFaixas((f) => {
      const novo = { ...f };
      [2, 3, 4, 5].forEach((d) => {
        if (!funcionamento(d)?.fechado) novo[d] = f[1].map((x) => ({ ...x }));
      });
      return novo;
    });

  async function salvar() {
    setSalvando(true);
    try {
      await adminApi.servicosDaProfissional(profissional.id, [...marcados]);
      const lista = DIAS.flatMap((d) =>
        faixas[d].map((x) => ({ diaSemana: d, horaInicio: x.inicio, horaFim: x.fim })),
      );
      await adminApi.horariosDaProfissional(profissional.id, lista);
      avisos.sucesso(`Serviços e horários de ${profissional.nome} salvos. O site já foi atualizado.`);
      aoSalvar();
      aoFechar();
    } catch (err) {
      avisos.erro(err.message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <Modal titulo={`Serviços e horários: ${profissional.nome}`} aoFechar={aoFechar} largo>
      <div className="modal-corpo">
        <Carga estado={detalhe}>
          <fieldset className="grupo">
            <legend>Serviços que ela realiza</legend>
            {servicos.length === 0 ? (
              <p className="vazio">Cadastre serviços primeiro.</p>
            ) : (
              <div className="grade-checks">
                {servicos.map((s) => (
                  <label key={s.id} className={`check${s.ativo ? '' : ' inativo'}`}>
                    <input type="checkbox" checked={marcados.has(s.id)} onChange={() => alternarServico(s.id)} />
                    {s.nome}
                    {!s.ativo && <span className="dica"> (desativado)</span>}
                  </label>
                ))}
              </div>
            )}
          </fieldset>

          {faixas && (
            <fieldset className="grupo">
              <legend>Expediente semanal</legend>
              <p className="dica">
                Use duas faixas no mesmo dia para o intervalo de almoço (ex.: 09:00 às 12:00 e 13:30 às 19:00).
                O expediente precisa ficar dentro do horário de funcionamento do salão.
              </p>
              <button type="button" className="botao-texto" onClick={copiarSegundaParaSemana}>
                Copiar a segunda-feira para terça a sexta
              </button>
              <div className="semana">
                {DIAS.map((dia) => {
                  const h = funcionamento(dia);
                  const fechado = h?.fechado;
                  return (
                    <div key={dia} className="semana-dia">
                      <div className="semana-dia-nome">
                        <strong>{NOMES_DIAS[dia]}</strong>
                        <span className="dica">
                          {!h ? '' : fechado ? 'Salão fechado' : `Salão: ${hora(h.abertura)} às ${hora(h.fechamento)}`}
                        </span>
                      </div>
                      <div className="semana-faixas">
                        {faixas[dia].length === 0 && <span className="dica">Não trabalha</span>}
                        {faixas[dia].map((x, i) => (
                          <div key={i} className="faixa">
                            <input
                              type="time"
                              value={x.inicio}
                              onChange={(e) => mudarFaixa(dia, i, 'inicio', e.target.value)}
                              aria-label={`${NOMES_DIAS[dia]}, início da faixa ${i + 1}`}
                            />
                            <span>às</span>
                            <input
                              type="time"
                              value={x.fim}
                              onChange={(e) => mudarFaixa(dia, i, 'fim', e.target.value)}
                              aria-label={`${NOMES_DIAS[dia]}, fim da faixa ${i + 1}`}
                            />
                            <button type="button" className="botao-texto" onClick={() => removerFaixa(dia, i)}>
                              Remover
                            </button>
                          </div>
                        ))}
                        {!fechado && (
                          <button type="button" className="botao-texto" onClick={() => adicionarFaixa(dia)}>
                            Adicionar faixa
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </fieldset>
          )}
        </Carga>
      </div>
      <div className="modal-rodape">
        <button type="button" className="botao botao-secundario" onClick={aoFechar}>Cancelar</button>
        <button type="button" className="botao botao-primario" onClick={salvar} disabled={salvando || !faixas}>
          {salvando ? 'Salvando…' : 'Salvar serviços e horários'}
        </button>
      </div>
    </Modal>
  );
}
