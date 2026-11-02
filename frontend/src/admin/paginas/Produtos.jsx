import { useState } from 'react';
import { adminApi } from '../../api/admin.js';
import { urlImagem } from '../../api/http.js';
import { useAoVivo } from '../../aovivo/AoVivo.jsx';
import { useCarregar } from '../../hooks/useCarregar.js';
import { lerValor, moeda } from '../../util/formato.js';
import { Campo, Carga, EnviarImagem, Modal, useAvisos } from '../componentes.jsx';

/**
 * Vitrine de produtos do site. A cliente vê foto, marca, preço e o botão
 * "Tenho interesse", que abre o WhatsApp do salão com o nome do produto.
 */
export default function Produtos() {
  const avisos = useAvisos();
  const produtos = useCarregar(() => adminApi.produtos(), []);
  const [edicao, setEdicao] = useState(null); // {} = novo, objeto = editar

  useAoVivo(() => produtos.recarregar(true), ['produtos']);

  async function atualizar(p, mudancas, mensagem) {
    try {
      await adminApi.atualizarProduto(p.id, { ...p, ...mudancas });
      avisos.sucesso(mensagem);
      produtos.recarregar(true);
    } catch (e) {
      avisos.erro(e.message);
    }
  }

  async function excluir(p) {
    if (!window.confirm(`Excluir "${p.nome}"? Se for só por um tempo, prefira "Ocultar".`)) return;
    try {
      await adminApi.removerProduto(p.id);
      avisos.sucesso('Produto excluído.');
      produtos.recarregar(true);
    } catch (e) {
      avisos.erro(e.message);
    }
  }

  return (
    <div className="pagina">
      <header className="pagina-cabeca">
        <div>
          <h1>Produtos</h1>
          <p className="pagina-sub">
            A vitrine do site. Sem nenhum produto visível, a seção “Produtos” fica escondida.
          </p>
        </div>
        <button type="button" className="botao botao-primario" onClick={() => setEdicao({})}>
          Novo produto
        </button>
      </header>

      <Carga estado={produtos} vazio="Nenhum produto cadastrado. Crie o primeiro em “Novo produto”.">
        <div className="tabela-rolagem">
          <table className="tabela">
            <thead>
              <tr>
                <th>Produto</th>
                <th className="numero">Preço</th>
                <th>Estoque</th>
                <th>No site</th>
                <th><span className="visualmente-oculto">Ações</span></th>
              </tr>
            </thead>
            <tbody>
              {(produtos.dados || []).map((p) => (
                <tr key={p.id} className={p.ativo ? undefined : 'inativo'}>
                  <td>
                    <div className="celula-com-foto">
                      <span className="miniatura" aria-hidden="true">
                        {p.fotoUrl ? <img src={urlImagem(p.fotoUrl)} alt="" /> : 'sem foto'}
                      </span>
                      <span>
                        <strong>{p.nome}</strong>
                        {p.marca && <span className="dica d-block">{p.marca}</span>}
                      </span>
                    </div>
                  </td>
                  <td className="numero">{p.preco != null ? moeda(p.preco) : 'Consulte'}</td>
                  <td>
                    <span className={`etiqueta ${p.disponivel ? 'etiqueta-concluido' : 'etiqueta-faltou'}`}>
                      {p.disponivel ? 'Disponível' : 'Esgotado'}
                    </span>
                  </td>
                  <td>{p.ativo ? 'Visível' : 'Oculto'}</td>
                  <td className="acoes">
                    <button type="button" className="botao-texto" onClick={() => setEdicao(p)}>Editar</button>
                    <button
                      type="button"
                      className="botao-texto"
                      onClick={() =>
                        atualizar(p, { disponivel: !p.disponivel },
                          p.disponivel ? `${p.nome} marcado como esgotado.` : `${p.nome} disponível de novo.`)
                      }
                    >
                      {p.disponivel ? 'Marcar esgotado' : 'Marcar disponível'}
                    </button>
                    <button
                      type="button"
                      className="botao-texto"
                      onClick={() =>
                        atualizar(p, { ativo: !p.ativo }, p.ativo ? `${p.nome} ocultado do site.` : `${p.nome} visível no site.`)
                      }
                    >
                      {p.ativo ? 'Ocultar' : 'Mostrar'}
                    </button>
                    <button type="button" className="botao-texto texto-perigo" onClick={() => excluir(p)}>Excluir</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Carga>

      {edicao && (
        <ProdutoModal
          produto={edicao.id ? edicao : null}
          aoFechar={() => setEdicao(null)}
          aoSalvar={() => {
            setEdicao(null);
            produtos.recarregar(true);
          }}
        />
      )}
    </div>
  );
}

function ProdutoModal({ produto, aoFechar, aoSalvar }) {
  const avisos = useAvisos();
  const [form, setForm] = useState({
    nome: produto?.nome || '',
    marca: produto?.marca || '',
    descricao: produto?.descricao || '',
    preco: produto?.preco != null ? String(produto.preco).replace('.', ',') : '',
    fotoUrl: produto?.fotoUrl || '',
    disponivel: produto ? produto.disponivel : true,
    ordem: String(produto?.ordem ?? 0),
  });
  const [erros, setErros] = useState({});
  const [salvando, setSalvando] = useState(false);
  const mudar = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }));

  async function salvar(e) {
    e.preventDefault();
    const preco = form.preco.trim() ? lerValor(form.preco) : null;
    const novos = {};
    if (!form.nome.trim()) novos.nome = 'Informe o nome do produto.';
    if (form.preco.trim() && (preco === null || preco < 0)) novos.preco = 'Informe um valor, por exemplo 59,90.';
    setErros(novos);
    if (Object.keys(novos).length) return;

    const corpo = {
      nome: form.nome.trim(),
      marca: form.marca.trim() || null,
      descricao: form.descricao.trim() || null,
      preco,
      fotoUrl: form.fotoUrl || null,
      disponivel: form.disponivel,
      ordem: Number(form.ordem) || 0,
      ativo: produto ? produto.ativo : true,
    };
    setSalvando(true);
    try {
      if (produto) await adminApi.atualizarProduto(produto.id, corpo);
      else await adminApi.criarProduto(corpo);
      avisos.sucesso(produto ? 'Produto atualizado. O site já mostra as mudanças.' : 'Produto adicionado à vitrine do site.');
      aoSalvar();
    } catch (err) {
      if (err.campos) setErros(err.campos);
      avisos.erro(err.message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <Modal titulo={produto ? `Editar ${produto.nome}` : 'Novo produto'} aoFechar={aoFechar} largo>
      <form onSubmit={salvar} noValidate>
        <div className="modal-corpo grade-2">
          <div className="grade-inteira">
            <EnviarImagem
              id="pr-foto"
              rotulo="Foto do produto"
              formato="produto"
              valor={form.fotoUrl}
              aoMudar={(url) => setForm((f) => ({ ...f, fotoUrl: url }))}
              erro={erros.fotoUrl}
              dica="De preferência a embalagem em fundo claro. Ela aparece inteira, sem cortar."
            />
          </div>
          <Campo id="pr-nome" rotulo="Nome" erro={erros.nome}>
            <input id="pr-nome" value={form.nome} onChange={mudar('nome')} maxLength={120} placeholder="Ex.: Shampoo Hidratante 300 ml" />
          </Campo>
          <Campo id="pr-marca" rotulo="Marca (opcional)">
            <input id="pr-marca" value={form.marca} onChange={mudar('marca')} maxLength={80} />
          </Campo>
          <Campo id="pr-preco" rotulo="Preço (R$)" erro={erros.preco} dica="Deixe vazio para mostrar “Consulte no salão”.">
            <input id="pr-preco" inputMode="decimal" value={form.preco} onChange={mudar('preco')} placeholder="59,90" />
          </Campo>
          <Campo id="pr-ordem" rotulo="Ordem na vitrine" dica="Menor aparece primeiro.">
            <input id="pr-ordem" type="number" min="0" value={form.ordem} onChange={mudar('ordem')} />
          </Campo>
          <div className="grade-inteira">
            <Campo id="pr-desc" rotulo="Descrição curta (opcional)">
              <textarea id="pr-desc" value={form.descricao} onChange={mudar('descricao')} maxLength={2000} rows={3} />
            </Campo>
          </div>
          <label className="check grade-inteira">
            <input
              type="checkbox"
              checked={form.disponivel}
              onChange={(e) => setForm((f) => ({ ...f, disponivel: e.target.checked }))}
            />
            Disponível em estoque (desmarque para mostrar “Esgotado”)
          </label>
        </div>
        <div className="modal-rodape">
          <button type="button" className="botao botao-secundario" onClick={aoFechar}>Cancelar</button>
          <button type="submit" className="botao botao-primario" disabled={salvando}>
            {salvando ? 'Salvando…' : produto ? 'Salvar produto' : 'Adicionar produto'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
