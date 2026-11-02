import { useState } from 'react';
import { adminApi } from '../../api/admin.js';
import { useAoVivo } from '../../aovivo/AoVivo.jsx';
import { useCarregar } from '../../hooks/useCarregar.js';
import { dataCurta, dataDeIso, horaDe, isoData } from '../../util/formato.js';
import { Campo, Carga, useAvisos } from '../componentes.jsx';

const DIAS_A_FRENTE = 60;

/** Folgas, cursos, consultas: o período bloqueado some do site e do WhatsApp. */
export default function Bloqueios() {
  const avisos = useAvisos();
  const hoje = isoData(new Date());
  const limite = (() => {
    const d = new Date();
    d.setDate(d.getDate() + DIAS_A_FRENTE);
    return isoData(d);
  })();

  const profissionais = useCarregar(() => adminApi.profissionais(), []);
  const bloqueios = useCarregar(() => adminApi.bloqueios(hoje, limite), []);
  useAoVivo(() => bloqueios.recarregar(true), ['agenda']);

  const [form, setForm] = useState({ profissionalId: '', inicio: `${hoje}T08:00`, fim: `${hoje}T12:00`, motivo: '' });
  const [erro, setErro] = useState(null);
  const [salvando, setSalvando] = useState(false);
  const mudar = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }));

  async function criar(e) {
    e.preventDefault();
    if (!form.profissionalId) {
      setErro('Escolha a profissional.');
      return;
    }
    if (form.fim <= form.inicio) {
      setErro('O fim deve ser depois do início.');
      return;
    }
    setErro(null);
    setSalvando(true);
    try {
      await adminApi.criarBloqueio({
        profissionalId: Number(form.profissionalId),
        inicio: form.inicio,
        fim: form.fim,
        motivo: form.motivo.trim() || null,
      });
      avisos.sucesso('Período bloqueado. Ele já não aparece mais para agendamento.');
      setForm((f) => ({ ...f, motivo: '' }));
      bloqueios.recarregar(true);
    } catch (err) {
      setErro(err.message); // ex.: lista os agendamentos que já existem no período
    } finally {
      setSalvando(false);
    }
  }

  async function remover(b) {
    if (!window.confirm(`Remover o bloqueio de ${b.profissional}? O período volta a ficar livre para agendamento.`)) return;
    try {
      await adminApi.removerBloqueio(b.id);
      avisos.sucesso('Bloqueio removido.');
      bloqueios.recarregar(true);
    } catch (err) {
      avisos.erro(err.message);
    }
  }

  return (
    <div className="pagina">
      <header className="pagina-cabeca">
        <div>
          <h1>Bloqueios</h1>
          <p className="pagina-sub">Feche um período na agenda de uma profissional: folga, curso, consulta.</p>
        </div>
      </header>

      <section className="painel">
        <h2>Novo bloqueio</h2>
        <form onSubmit={criar} noValidate className="grade-2">
          <Campo id="bl-prof" rotulo="Profissional">
            <select id="bl-prof" value={form.profissionalId} onChange={mudar('profissionalId')}>
              <option value="">Escolha</option>
              {(profissionais.dados || []).filter((p) => p.ativo).map((p) => (
                <option key={p.id} value={p.id}>{p.nome}</option>
              ))}
            </select>
          </Campo>
          <Campo id="bl-motivo" rotulo="Motivo (opcional)" dica="Só aparece aqui no painel.">
            <input id="bl-motivo" value={form.motivo} onChange={mudar('motivo')} maxLength={200} />
          </Campo>
          <Campo id="bl-inicio" rotulo="Início">
            <input id="bl-inicio" type="datetime-local" value={form.inicio} onChange={mudar('inicio')} />
          </Campo>
          <Campo id="bl-fim" rotulo="Fim">
            <input id="bl-fim" type="datetime-local" value={form.fim} onChange={mudar('fim')} />
          </Campo>
          {erro && <p className="nota nota-erro grade-inteira" role="alert">{erro}</p>}
          <div className="grade-inteira acoes-form">
            <button type="submit" className="botao botao-primario" disabled={salvando}>
              {salvando ? 'Bloqueando…' : 'Bloquear período'}
            </button>
          </div>
        </form>
      </section>

      <section className="painel">
        <h2>Próximos {DIAS_A_FRENTE} dias</h2>
        <Carga estado={bloqueios} vazio="Nenhum bloqueio programado.">
          <ul className="lista-simples">
            {(bloqueios.dados || []).map((b) => (
              <li key={b.id}>
                <span>
                  <strong>{b.profissional}</strong>: {dataCurta(dataDeIso(b.inicio))} {horaDe(b.inicio)} até{' '}
                  {dataCurta(dataDeIso(b.fim))} {horaDe(b.fim)}
                  {b.motivo && <span className="dica">, {b.motivo}</span>}
                </span>
                <button type="button" className="botao-texto texto-perigo" onClick={() => remover(b)}>Remover</button>
              </li>
            ))}
          </ul>
        </Carga>
      </section>
    </div>
  );
}
