import { useState } from 'react';
import { adminApi } from '../../api/admin.js';
import { useAoVivo } from '../../aovivo/AoVivo.jsx';
import { useCarregar } from '../../hooks/useCarregar.js';
import { duracao, lerValor, moeda } from '../../util/formato.js';
import { urlImagem } from '../../api/http.js';
import { Campo, Carga, EnviarImagem, Modal, useAvisos } from '../componentes.jsx';
import PrecoDuracaoModal from './PrecoDuracaoModal.jsx';

const CATEGORIAS = [
  ['CABELO', 'Cabelo'],
  ['UNHAS', 'Unhas'],
  ['ESTETICA', 'Estética'],
  ['OUTROS', 'Outros'],
];
const nomeCategoria = (c) => CATEGORIAS.find(([v]) => v === c)?.[1] || c;

export default function Servicos() {
  const avisos = useAvisos();
  const servicos = useCarregar(() => adminApi.servicos(), []);
  const [rapido, setRapido] = useState(null);   // serviço no modal de preço/duração
  const [edicao, setEdicao] = useState(null);   // {} = novo, objeto = editar

  useAoVivo(() => servicos.recarregar(true), ['servicos']);

  async function alternarAtivo(s) {
    const acao = s.ativo ? 'desativar' : 'reativar';
    if (s.ativo && !window.confirm(`Desativar "${s.nome}"? Ele some do site e do WhatsApp, mas o histórico é mantido.`)) return;
    try {
      await adminApi.servicoAtivo(s.id, !s.ativo);
      avisos.sucesso(s.ativo ? `${s.nome} desativado.` : `${s.nome} reativado.`);
      servicos.recarregar(true);
    } catch (e) {
      avisos.erro(`Não foi possível ${acao}: ${e.message}`);
    }
  }

  return (
    <div className="pagina">
      <header className="pagina-cabeca">
        <div>
          <h1>Serviços e preços</h1>
          <p className="pagina-sub">O que estiver ativo aparece no site e no WhatsApp, com estes valores e tempos.</p>
        </div>
        <button type="button" className="botao botao-primario" onClick={() => setEdicao({})}>
          Novo serviço
        </button>
      </header>

      <Carga estado={servicos} vazio="Nenhum serviço cadastrado. Crie o primeiro em “Novo serviço”.">
        <div className="tabela-rolagem">
          <table className="tabela">
            <thead>
              <tr>
                <th>Serviço</th>
                <th>Categoria</th>
                <th className="numero">Duração</th>
                <th className="numero">Preço</th>
                <th>Situação</th>
                <th><span className="visualmente-oculto">Ações</span></th>
              </tr>
            </thead>
            <tbody>
              {(servicos.dados || []).map((s) => (
                <tr key={s.id} className={s.ativo ? undefined : 'inativo'}>
                  <td>
                    <div className="celula-com-foto">
                      <span className="miniatura" aria-hidden="true">
                        {s.fotoUrl ? <img src={urlImagem(s.fotoUrl)} alt="" /> : 'sem foto'}
                      </span>
                      <span>
                        <strong>{s.nome}</strong>
                        {s.exigeSinal && <span className="etiqueta etiqueta-sinal">sinal {moeda(s.valorSinal)}</span>}
                        {s.descricao && <span className="dica d-block">{s.descricao}</span>}
                      </span>
                    </div>
                  </td>
                  <td>{nomeCategoria(s.categoria)}</td>
                  <td className="numero">{duracao(s.duracaoMinutos)}</td>
                  <td className="numero">{moeda(s.preco)}</td>
                  <td>{s.ativo ? 'Ativo' : 'Desativado'}</td>
                  <td className="acoes">
                    <button type="button" className="botao botao-secundario botao-pequeno" onClick={() => setRapido(s)}>
                      Preço e duração
                    </button>
                    <button type="button" className="botao-texto" onClick={() => setEdicao(s)}>Editar</button>
                    <button type="button" className="botao-texto" onClick={() => alternarAtivo(s)}>
                      {s.ativo ? 'Desativar' : 'Reativar'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Carga>

      {rapido && (
        <PrecoDuracaoModal
          servico={rapido}
          aoFechar={() => setRapido(null)}
          aoSalvar={() => {
            setRapido(null);
            servicos.recarregar(true);
          }}
        />
      )}
      {edicao && (
        <ServicoModal
          servico={edicao.id ? edicao : null}
          aoFechar={() => setEdicao(null)}
          aoSalvar={() => {
            setEdicao(null);
            servicos.recarregar(true);
          }}
        />
      )}
    </div>
  );
}

function ServicoModal({ servico, aoFechar, aoSalvar }) {
  const avisos = useAvisos();
  const [form, setForm] = useState({
    nome: servico?.nome || '',
    categoria: servico?.categoria || 'CABELO',
    descricao: servico?.descricao || '',
    preco: servico ? String(servico.preco).replace('.', ',') : '',
    duracaoMinutos: servico ? String(servico.duracaoMinutos) : '60',
    ordemExibicao: servico ? String(servico.ordemExibicao ?? 0) : '0',
    fotoUrl: servico?.fotoUrl || '',
    exigeSinal: servico?.exigeSinal || false,
    valorSinal: servico?.valorSinal != null ? String(servico.valorSinal).replace('.', ',') : '',
  });
  const [erros, setErros] = useState({});
  const [salvando, setSalvando] = useState(false);
  const mudar = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }));

  async function salvar(e) {
    e.preventDefault();
    const preco = lerValor(form.preco);
    const minutos = Number(form.duracaoMinutos);
    const novos = {};
    if (!form.nome.trim()) novos.nome = 'Informe o nome.';
    if (preco === null || preco < 0) novos.preco = 'Informe um valor, por exemplo 80,00.';
    if (!Number.isInteger(minutos) || minutos < 5 || minutos > 720) novos.duracaoMinutos = 'Entre 5 e 720 minutos.';
    const valorSinal = form.exigeSinal ? lerValor(form.valorSinal) : null;
    if (form.exigeSinal && (valorSinal === null || valorSinal <= 0)) novos.valorSinal = 'Informe o valor do sinal, por exemplo 50,00.';
    else if (form.exigeSinal && preco !== null && valorSinal > preco) novos.valorSinal = 'O sinal não pode ser maior que o preço.';
    setErros(novos);
    if (Object.keys(novos).length) return;

    const corpo = {
      nome: form.nome.trim(),
      categoria: form.categoria,
      descricao: form.descricao.trim() || null,
      preco,
      duracaoMinutos: minutos,
      ordemExibicao: Number(form.ordemExibicao) || 0,
      fotoUrl: form.fotoUrl || null,
      exigeSinal: form.exigeSinal,
      valorSinal,
    };
    setSalvando(true);
    try {
      if (servico) await adminApi.atualizarServico(servico.id, corpo);
      else await adminApi.criarServico(corpo);
      avisos.sucesso(servico ? 'Serviço atualizado.' : 'Serviço criado. Vincule as profissionais que o realizam.');
      aoSalvar();
    } catch (err) {
      if (err.campos) setErros(err.campos);
      avisos.erro(err.message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <Modal titulo={servico ? `Editar ${servico.nome}` : 'Novo serviço'} aoFechar={aoFechar}>
      <form onSubmit={salvar} noValidate>
        <div className="modal-corpo grade-2">
          <div className="grade-inteira">
            <Campo id="sv-nome" rotulo="Nome" erro={erros.nome}>
              <input id="sv-nome" value={form.nome} onChange={mudar('nome')} maxLength={120} />
            </Campo>
          </div>
          <Campo id="sv-cat" rotulo="Categoria">
            <select id="sv-cat" value={form.categoria} onChange={mudar('categoria')}>
              {CATEGORIAS.map(([v, r]) => <option key={v} value={v}>{r}</option>)}
            </select>
          </Campo>
          <Campo id="sv-ordem" rotulo="Ordem no site" dica="Menor aparece primeiro.">
            <input id="sv-ordem" type="number" min="0" value={form.ordemExibicao} onChange={mudar('ordemExibicao')} />
          </Campo>
          <Campo id="sv-preco" rotulo="Preço (R$)" erro={erros.preco}>
            <input id="sv-preco" inputMode="decimal" value={form.preco} onChange={mudar('preco')} placeholder="80,00" />
          </Campo>
          <Campo id="sv-min" rotulo="Duração (minutos)" erro={erros.duracaoMinutos}>
            <input id="sv-min" type="number" min="5" max="720" step="5" value={form.duracaoMinutos} onChange={mudar('duracaoMinutos')} />
          </Campo>
          <div className="grade-inteira">
            <EnviarImagem
              id="sv-foto"
              rotulo="Foto do serviço"
              valor={form.fotoUrl}
              aoMudar={(url) => setForm((f) => ({ ...f, fotoUrl: url }))}
              erro={erros.fotoUrl}
              dica="Foto de um trabalho feito no salão (horizontal). Sem foto, o site mostra uma imagem de exemplo."
            />
          </div>
          <div className="grade-inteira caixa-sinal">
            <label className="check">
              <input
                type="checkbox"
                checked={form.exigeSinal}
                onChange={(e) => setForm((f) => ({ ...f, exigeSinal: e.target.checked }))}
              />
              Cobrar sinal por Pix para agendar (site e WhatsApp)
            </label>
            {form.exigeSinal && (
              <Campo
                id="sv-sinal"
                rotulo="Valor do sinal (R$)"
                erro={erros.valorSinal}
                dica="A cliente paga por Pix logo após confirmar. O sinal é descontado do valor do serviço."
              >
                <input
                  id="sv-sinal"
                  inputMode="decimal"
                  value={form.valorSinal}
                  onChange={(e) => setForm((f) => ({ ...f, valorSinal: e.target.value }))}
                  placeholder="50,00"
                />
              </Campo>
            )}
          </div>
          <div className="grade-inteira">
            <Campo id="sv-desc" rotulo="Descrição (opcional)" dica="Aparece no site, abaixo do nome.">
              <textarea id="sv-desc" value={form.descricao} onChange={mudar('descricao')} maxLength={2000} />
            </Campo>
          </div>
        </div>
        <div className="modal-rodape">
          <button type="button" className="botao botao-secundario" onClick={aoFechar}>Cancelar</button>
          <button type="submit" className="botao botao-primario" disabled={salvando}>
            {salvando ? 'Salvando…' : servico ? 'Salvar serviço' : 'Criar serviço'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
