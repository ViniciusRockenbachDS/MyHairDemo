import { useEffect, useState } from 'react';
import { adminApi } from '../../api/admin.js';
import { urlImagem } from '../../api/http.js';
import { useCarregar } from '../../hooks/useCarregar.js';
import { dataDeIso, lerValor } from '../../util/formato.js';
import { Campo, Carga, EnviarImagem, Modal, useAvisos } from '../componentes.jsx';

const estrelas = (n) => '★'.repeat(n) + '☆'.repeat(5 - n);

/**
 * Avaliações exibidas no site. Copie do Google (ou de onde quiser) as que
 * gostaria de mostrar. Sem nenhuma avaliação visível, a seção some do site.
 */
export default function Avaliacoes() {
  const avisos = useAvisos();
  const lista = useCarregar(() => adminApi.avaliacoes(), []);
  const [edicao, setEdicao] = useState(null); // {} = nova, objeto = editar

  async function alternarVisivel(a) {
    try {
      await adminApi.atualizarAvaliacao(a.id, { ...a, ativo: !a.ativo });
      avisos.sucesso(a.ativo ? 'Avaliação ocultada do site.' : 'Avaliação visível no site.');
      lista.recarregar(true);
    } catch (e) {
      avisos.erro(e.message);
    }
  }

  async function excluir(a) {
    if (!window.confirm(`Excluir a avaliação de ${a.autor}?`)) return;
    try {
      await adminApi.removerAvaliacao(a.id);
      avisos.sucesso('Avaliação excluída.');
      lista.recarregar(true);
    } catch (e) {
      avisos.erro(e.message);
    }
  }

  return (
    <div className="pagina">
      <header className="pagina-cabeca">
        <div>
          <h1>Avaliações</h1>
          <p className="pagina-sub">
            Cadastre as avaliações que quer mostrar no site, por exemplo as do Google. Elas aparecem na hora.
          </p>
        </div>
        <button type="button" className="botao botao-primario" onClick={() => setEdicao({})}>
          Nova avaliação
        </button>
      </header>

      <Resumo />

      <section className="painel">
        <h2>Avaliações no site</h2>
        <Carga estado={lista} vazio="Nenhuma avaliação cadastrada. Enquanto não houver, a seção fica escondida no site.">
          <ul className="lista-avaliacoes">
            {(lista.dados || []).map((a) => (
              <li key={a.id} className={`item-avaliacao${a.ativo ? '' : ' inativo'}`}>
                <span className="item-avaliacao-foto" aria-hidden="true">
                  {a.fotoUrl ? <img src={urlImagem(a.fotoUrl)} alt="" /> : a.autor.trim().charAt(0)}
                </span>
                <div>
                  <strong>{a.autor}</strong>{' '}
                  <span className="estrelas-texto" aria-label={`${a.nota} de 5 estrelas`}>{estrelas(a.nota)}</span>
                  <div className="dica">
                    {a.data ? dataDeIso(a.data).toLocaleDateString('pt-BR') : 'Sem data'}
                    {!a.ativo && ', oculta no site'}
                  </div>
                  <p className="item-avaliacao-texto">{a.texto}</p>
                </div>
                <div className="acoes">
                  <button type="button" className="botao-texto" onClick={() => setEdicao(a)}>Editar</button>
                  <button type="button" className="botao-texto" onClick={() => alternarVisivel(a)}>
                    {a.ativo ? 'Ocultar' : 'Mostrar'}
                  </button>
                  <button type="button" className="botao-texto texto-perigo" onClick={() => excluir(a)}>Excluir</button>
                </div>
              </li>
            ))}
          </ul>
        </Carga>
      </section>

      {edicao && (
        <AvaliacaoModal
          avaliacao={edicao.id ? edicao : null}
          aoFechar={() => setEdicao(null)}
          aoSalvar={() => {
            setEdicao(null);
            lista.recarregar(true);
          }}
        />
      )}
    </div>
  );
}

/** Selo do topo da seção no site: "4,9 ★★★★★ 78 avaliações" + link "Ver todas". */
function Resumo() {
  const avisos = useAvisos();
  const resumo = useCarregar(() => adminApi.resumoAvaliacoes(), []);
  const [form, setForm] = useState({ nota: '', total: '', link: '' });
  const [erros, setErros] = useState({});
  const [salvando, setSalvando] = useState(false);

  useEffect(() => {
    if (!resumo.dados) return;
    setForm({
      nota: resumo.dados.nota != null ? String(resumo.dados.nota).replace('.', ',') : '',
      total: resumo.dados.total != null ? String(resumo.dados.total) : '',
      link: resumo.dados.link || '',
    });
  }, [resumo.dados]);

  async function salvar(e) {
    e.preventDefault();
    const nota = form.nota.trim() ? lerValor(form.nota) : null;
    const total = form.total.trim() ? Number(form.total) : null;
    const novos = {};
    if (nota !== null && (nota < 1 || nota > 5)) novos.nota = 'Use um número de 1 a 5, ex.: 4,9.';
    if (total !== null && (!Number.isInteger(total) || total < 0)) novos.total = 'Use um número inteiro.';
    setErros(novos);
    if (Object.keys(novos).length) return;

    setSalvando(true);
    try {
      await adminApi.salvarResumoAvaliacoes({
        nota: nota !== null ? Math.round(nota * 10) / 10 : null,
        total,
        link: form.link.trim() || null,
      });
      avisos.sucesso('Resumo salvo. O site já foi atualizado.');
    } catch (err) {
      if (err.campos) setErros(err.campos);
      avisos.erro(err.message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <section className="painel">
      <h2>Resumo no topo da seção</h2>
      <p className="dica">Copie a nota e o total do Google. Deixe a nota em branco para esconder o selo.</p>
      <Carga estado={resumo}>
        <form onSubmit={salvar} noValidate className="grade-2">
          <Campo id="rs-nota" rotulo="Nota geral" erro={erros.nota} dica="Ex.: 4,9">
            <input id="rs-nota" inputMode="decimal" value={form.nota} onChange={(e) => setForm((f) => ({ ...f, nota: e.target.value }))} />
          </Campo>
          <Campo id="rs-total" rotulo="Total de avaliações" erro={erros.total} dica="Ex.: 78">
            <input id="rs-total" type="number" min="0" value={form.total} onChange={(e) => setForm((f) => ({ ...f, total: e.target.value }))} />
          </Campo>
          <div className="grade-inteira">
            <Campo id="rs-link" rotulo="Link “Ver todas as avaliações” (opcional)" erro={erros.link} dica="Cole o link das avaliações do salão no Google.">
              <input id="rs-link" value={form.link} onChange={(e) => setForm((f) => ({ ...f, link: e.target.value }))} placeholder="https://" />
            </Campo>
          </div>
          <div className="grade-inteira acoes-form">
            <button type="submit" className="botao botao-primario" disabled={salvando}>
              {salvando ? 'Salvando…' : 'Salvar resumo'}
            </button>
          </div>
        </form>
      </Carga>
    </section>
  );
}

function AvaliacaoModal({ avaliacao, aoFechar, aoSalvar }) {
  const avisos = useAvisos();
  const [form, setForm] = useState({
    autor: avaliacao?.autor || '',
    nota: String(avaliacao?.nota ?? 5),
    data: avaliacao?.data || '',
    texto: avaliacao?.texto || '',
    fotoUrl: avaliacao?.fotoUrl || '',
    link: avaliacao?.link || '',
    ordem: String(avaliacao?.ordem ?? 0),
  });
  const [erros, setErros] = useState({});
  const [salvando, setSalvando] = useState(false);
  const mudar = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }));

  async function salvar(e) {
    e.preventDefault();
    const novos = {};
    if (!form.autor.trim()) novos.autor = 'Informe o nome de quem avaliou.';
    if (!form.texto.trim()) novos.texto = 'Escreva o texto da avaliação.';
    setErros(novos);
    if (Object.keys(novos).length) return;

    const corpo = {
      autor: form.autor.trim(),
      nota: Number(form.nota),
      data: form.data || null,
      texto: form.texto.trim(),
      fotoUrl: form.fotoUrl || null,
      link: form.link.trim() || null,
      ordem: Number(form.ordem) || 0,
      ativo: avaliacao ? avaliacao.ativo : true,
    };
    setSalvando(true);
    try {
      if (avaliacao) await adminApi.atualizarAvaliacao(avaliacao.id, corpo);
      else await adminApi.criarAvaliacao(corpo);
      avisos.sucesso(avaliacao ? 'Avaliação atualizada.' : 'Avaliação adicionada ao site.');
      aoSalvar();
    } catch (err) {
      if (err.campos) setErros(err.campos);
      avisos.erro(err.message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <Modal titulo={avaliacao ? `Editar avaliação de ${avaliacao.autor}` : 'Nova avaliação'} aoFechar={aoFechar} largo>
      <form onSubmit={salvar} noValidate>
        <div className="modal-corpo grade-2">
          <Campo id="av-autor" rotulo="Nome de quem avaliou" erro={erros.autor}>
            <input id="av-autor" value={form.autor} onChange={mudar('autor')} maxLength={120} />
          </Campo>
          <Campo id="av-nota" rotulo="Estrelas">
            <select id="av-nota" value={form.nota} onChange={mudar('nota')}>
              {[5, 4, 3, 2, 1].map((n) => (
                <option key={n} value={n}>{estrelas(n)} ({n})</option>
              ))}
            </select>
          </Campo>
          <Campo id="av-data" rotulo="Data da avaliação" dica="O site mostra “há 5 meses”, por exemplo.">
            <input id="av-data" type="date" value={form.data} onChange={mudar('data')} />
          </Campo>
          <Campo id="av-ordem" rotulo="Ordem no site" dica="Menor aparece primeiro.">
            <input id="av-ordem" type="number" min="0" value={form.ordem} onChange={mudar('ordem')} />
          </Campo>
          <div className="grade-inteira">
            <Campo id="av-texto" rotulo="Texto" erro={erros.texto}>
              <textarea id="av-texto" value={form.texto} onChange={mudar('texto')} maxLength={3000} rows={5} />
            </Campo>
          </div>
          <EnviarImagem
            id="av-foto"
            rotulo="Foto de quem avaliou (opcional)"
            formato="redondo"
            valor={form.fotoUrl}
            aoMudar={(url) => setForm((f) => ({ ...f, fotoUrl: url }))}
            dica="Sem foto, aparece a inicial do nome."
          />
          <Campo id="av-link" rotulo="Link da avaliação original (opcional)" erro={erros.link}>
            <input id="av-link" value={form.link} onChange={mudar('link')} placeholder="https://" />
          </Campo>
        </div>
        <div className="modal-rodape">
          <button type="button" className="botao botao-secundario" onClick={aoFechar}>Cancelar</button>
          <button type="submit" className="botao botao-primario" disabled={salvando}>
            {salvando ? 'Salvando…' : avaliacao ? 'Salvar avaliação' : 'Adicionar avaliação'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
