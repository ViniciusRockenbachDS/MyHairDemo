import { useState } from 'react';
import { adminApi } from '../../api/admin.js';
import { useCarregar } from '../../hooks/useCarregar.js';
import { Campo, Carga, Modal, useAvisos } from '../componentes.jsx';

/**
 * Respostas automáticas do robô do WhatsApp, por palavras-chave (sem IA).
 * Sem resposta para a mensagem, o robô chama a equipe ("Pediram atendente").
 */
export default function Perguntas() {
  const avisos = useAvisos();
  const lista = useCarregar(() => adminApi.perguntas(), []);
  const [edicao, setEdicao] = useState(null); // {} = nova, objeto = editar

  async function alternar(p) {
    try {
      await adminApi.atualizarPergunta(p.id, { ...p, ativo: !p.ativo });
      avisos.sucesso(p.ativo ? 'Resposta desativada.' : 'Resposta ativada.');
      lista.recarregar(true);
    } catch (e) {
      avisos.erro(e.message);
    }
  }

  async function excluir(p) {
    if (!window.confirm(`Excluir a pergunta "${p.pergunta}"?`)) return;
    try {
      await adminApi.removerPergunta(p.id);
      avisos.sucesso('Pergunta excluída.');
      lista.recarregar(true);
    } catch (e) {
      avisos.erro(e.message);
    }
  }

  return (
    <div className="pagina">
      <header className="pagina-cabeca">
        <div>
          <h1>Perguntas frequentes</h1>
          <p className="pagina-sub">
            O robô do WhatsApp responde quando a mensagem da cliente tem as palavras-chave. Sem resposta cadastrada,
            ele chama a equipe.
          </p>
        </div>
        <button type="button" className="botao botao-primario" onClick={() => setEdicao({})}>
          Nova pergunta
        </button>
      </header>

      <Testar />

      <Carga estado={lista} vazio="Nenhuma pergunta cadastrada. Comece pelas dúvidas que as clientes mais mandam.">
        <ul className="lista-perguntas">
          {(lista.dados || []).map((p) => (
            <li key={p.id} className={`item-pergunta${p.ativo ? '' : ' inativo'}`}>
              <div className="item-pergunta-topo">
                <strong>{p.pergunta}</strong>
                {!p.ativo && <span className="etiqueta etiqueta-cancelado">desativada</span>}
              </div>
              <p className="item-pergunta-resposta">{p.resposta}</p>
              <div className="item-pergunta-chaves">
                {p.palavrasChave.split(/[,;\n]/).map((k) => k.trim()).filter(Boolean).map((k) => (
                  <span key={k} className="chave">{k}</span>
                ))}
              </div>
              <div className="acoes">
                <button type="button" className="botao-texto" onClick={() => setEdicao(p)}>Editar</button>
                <button type="button" className="botao-texto" onClick={() => alternar(p)}>
                  {p.ativo ? 'Desativar' : 'Ativar'}
                </button>
                <button type="button" className="botao-texto texto-perigo" onClick={() => excluir(p)}>Excluir</button>
              </div>
            </li>
          ))}
        </ul>
      </Carga>

      {edicao && (
        <PerguntaModal
          pergunta={edicao.id ? edicao : null}
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

/** Digita uma mensagem como a cliente e vê qual resposta o robô daria. */
function Testar() {
  const [texto, setTexto] = useState('');
  const [resultado, setResultado] = useState(null);
  const [testando, setTestando] = useState(false);

  async function testar(e) {
    e.preventDefault();
    if (!texto.trim()) return;
    setTestando(true);
    try {
      setResultado(await adminApi.testarPergunta(texto.trim()));
    } catch (err) {
      setResultado({ erro: err.message });
    } finally {
      setTestando(false);
    }
  }

  return (
    <section className="painel">
      <h2>Testar uma mensagem</h2>
      <form onSubmit={testar} className="chat-envio">
        <input
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
          placeholder="Escreva como a cliente escreveria. Ex.: vocês aceitam cartão?"
          aria-label="Mensagem de teste"
        />
        <button type="submit" className="botao botao-secundario" disabled={testando}>Testar</button>
      </form>
      {resultado && (
        resultado.erro ? (
          <p className="erro-texto">{resultado.erro}</p>
        ) : resultado.encontrou ? (
          <div className="nota nota-info">
            <p><strong>O robô responderia</strong> (pergunta “{resultado.pergunta}”):</p>
            <p className="pre-linha">{resultado.resposta}</p>
          </div>
        ) : (
          <p className="nota nota-erro">
            Nenhuma resposta encontrada. O robô pediria para reformular e, na segunda vez, chamaria a equipe.
            Cadastre uma pergunta com as palavras dessa mensagem.
          </p>
        )
      )}
    </section>
  );
}

function PerguntaModal({ pergunta, aoFechar, aoSalvar }) {
  const avisos = useAvisos();
  const [form, setForm] = useState({
    pergunta: pergunta?.pergunta || '',
    palavrasChave: pergunta?.palavrasChave || '',
    resposta: pergunta?.resposta || '',
    ordem: String(pergunta?.ordem ?? 0),
  });
  const [erros, setErros] = useState({});
  const [salvando, setSalvando] = useState(false);
  const mudar = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }));

  async function salvar(e) {
    e.preventDefault();
    const novos = {};
    if (!form.pergunta.trim()) novos.pergunta = 'Escreva a pergunta.';
    if (!form.palavrasChave.trim()) novos.palavrasChave = 'Informe ao menos uma palavra-chave.';
    if (!form.resposta.trim()) novos.resposta = 'Escreva a resposta.';
    setErros(novos);
    if (Object.keys(novos).length) return;

    const corpo = {
      pergunta: form.pergunta.trim(),
      palavrasChave: form.palavrasChave.trim(),
      resposta: form.resposta.trim(),
      ordem: Number(form.ordem) || 0,
      ativo: pergunta ? pergunta.ativo : true,
    };
    setSalvando(true);
    try {
      if (pergunta) await adminApi.atualizarPergunta(pergunta.id, corpo);
      else await adminApi.criarPergunta(corpo);
      avisos.sucesso(pergunta ? 'Resposta atualizada. O robô já usa a nova versão.' : 'Pergunta cadastrada. O robô já responde.');
      aoSalvar();
    } catch (err) {
      if (err.campos) setErros(err.campos);
      avisos.erro(err.message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <Modal titulo={pergunta ? 'Editar pergunta' : 'Nova pergunta'} aoFechar={aoFechar} largo>
      <form onSubmit={salvar} noValidate>
        <div className="modal-corpo grade-2">
          <div className="grade-inteira">
            <Campo id="pf-perg" rotulo="Pergunta" erro={erros.pergunta} dica="Como a cliente costuma perguntar. Serve de título aqui no painel.">
              <input id="pf-perg" value={form.pergunta} onChange={mudar('pergunta')} maxLength={200} placeholder="Quais formas de pagamento?" />
            </Campo>
          </div>
          <div className="grade-inteira">
            <Campo
              id="pf-chaves"
              rotulo="Palavras-chave"
              erro={erros.palavrasChave}
              dica="Separadas por vírgula. Acentos e plural não importam. Expressões também valem, como “forma de pagamento”."
            >
              <input id="pf-chaves" value={form.palavrasChave} onChange={mudar('palavrasChave')} maxLength={1000} placeholder="pix, cartão, crédito, débito, dinheiro, pagamento" />
            </Campo>
          </div>
          <div className="grade-inteira">
            <Campo id="pf-resp" rotulo="Resposta do robô" erro={erros.resposta} dica="Use *texto* para negrito no WhatsApp. Pode usar emojis.">
              <textarea id="pf-resp" value={form.resposta} onChange={mudar('resposta')} rows={5} maxLength={2000} />
            </Campo>
          </div>
          <Campo id="pf-ordem" rotulo="Prioridade" dica="Em empate, vence a de menor número.">
            <input id="pf-ordem" type="number" min="0" value={form.ordem} onChange={mudar('ordem')} />
          </Campo>
        </div>
        <div className="modal-rodape">
          <button type="button" className="botao botao-secundario" onClick={aoFechar}>Cancelar</button>
          <button type="submit" className="botao botao-primario" disabled={salvando}>
            {salvando ? 'Salvando…' : pergunta ? 'Salvar' : 'Cadastrar'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
