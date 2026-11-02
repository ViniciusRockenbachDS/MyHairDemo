import { useState } from 'react';
import { adminApi } from '../../api/admin.js';
import { urlImagem } from '../../api/http.js';
import { useAoVivo } from '../../aovivo/AoVivo.jsx';
import { useCarregar } from '../../hooks/useCarregar.js';
import { Campo, Carga, EnviarImagem, Modal, useAvisos } from '../componentes.jsx';

/** Fotos de trabalhos feitos no salão (seção Lookbook do site). */
export default function Lookbook() {
  const avisos = useAvisos();
  const fotos = useCarregar(() => adminApi.lookbook(), []);
  const [edicao, setEdicao] = useState(null);

  useAoVivo(() => fotos.recarregar(true), ['lookbook']);

  async function alternar(f) {
    try {
      await adminApi.atualizarFotoLookbook(f.id, { ...f, ativo: !f.ativo });
      avisos.sucesso(f.ativo ? 'Foto ocultada do site.' : 'Foto visível no site.');
      fotos.recarregar(true);
    } catch (e) {
      avisos.erro(e.message);
    }
  }

  async function excluir(f) {
    if (!window.confirm('Excluir esta foto do Lookbook?')) return;
    try {
      await adminApi.removerFotoLookbook(f.id);
      avisos.sucesso('Foto excluída.');
      fotos.recarregar(true);
    } catch (e) {
      avisos.erro(e.message);
    }
  }

  return (
    <div className="pagina">
      <header className="pagina-cabeca">
        <div>
          <h1>Lookbook</h1>
          <p className="pagina-sub">
            Fotos de trabalhos feitos no salão. Com 4 fotos ou mais, o site mostra uma esteira que anda sozinha.
          </p>
        </div>
        <button type="button" className="botao botao-primario" onClick={() => setEdicao({})}>Adicionar foto</button>
      </header>

      <Carga estado={fotos} vazio="Nenhuma foto ainda. Sem fotos, a seção Lookbook fica escondida no site.">
        <ul className="grade-fotos-admin">
          {(fotos.dados || []).map((f) => (
            <li key={f.id} className={f.ativo ? undefined : 'inativo'}>
              <img src={urlImagem(f.fotoUrl)} alt={f.legenda || ''} loading="lazy" />
              <div className="grade-fotos-admin-corpo">
                <span>{f.legenda || <span className="dica">sem legenda</span>}</span>
                {!f.ativo && <span className="etiqueta etiqueta-cancelado">oculta</span>}
                <div className="acoes">
                  <button type="button" className="botao-texto" onClick={() => setEdicao(f)}>Editar</button>
                  <button type="button" className="botao-texto" onClick={() => alternar(f)}>{f.ativo ? 'Ocultar' : 'Mostrar'}</button>
                  <button type="button" className="botao-texto texto-perigo" onClick={() => excluir(f)}>Excluir</button>
                </div>
              </div>
            </li>
          ))}
        </ul>
      </Carga>

      {edicao && (
        <FotoModal
          foto={edicao.id ? edicao : null}
          aoFechar={() => setEdicao(null)}
          aoSalvar={() => {
            setEdicao(null);
            fotos.recarregar(true);
          }}
        />
      )}
    </div>
  );
}

function FotoModal({ foto, aoFechar, aoSalvar }) {
  const avisos = useAvisos();
  const [form, setForm] = useState({
    fotoUrl: foto?.fotoUrl || '',
    legenda: foto?.legenda || '',
    ordem: String(foto?.ordem ?? 0),
  });
  const [erros, setErros] = useState({});
  const [salvando, setSalvando] = useState(false);

  async function salvar(e) {
    e.preventDefault();
    if (!form.fotoUrl) {
      setErros({ fotoUrl: 'Escolha a foto.' });
      return;
    }
    setSalvando(true);
    try {
      const corpo = {
        fotoUrl: form.fotoUrl,
        legenda: form.legenda.trim() || null,
        ordem: Number(form.ordem) || 0,
        ativo: foto ? foto.ativo : true,
      };
      if (foto) await adminApi.atualizarFotoLookbook(foto.id, corpo);
      else await adminApi.criarFotoLookbook(corpo);
      avisos.sucesso(foto ? 'Foto atualizada.' : 'Foto adicionada ao Lookbook.');
      aoSalvar();
    } catch (err) {
      if (err.campos) setErros(err.campos);
      avisos.erro(err.message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <Modal titulo={foto ? 'Editar foto' : 'Adicionar foto'} aoFechar={aoFechar}>
      <form onSubmit={salvar} noValidate>
        <div className="modal-corpo">
          <EnviarImagem
            id="lb-foto"
            rotulo="Foto"
            formato="vertical"
            valor={form.fotoUrl}
            aoMudar={(url) => setForm((f) => ({ ...f, fotoUrl: url }))}
            erro={erros.fotoUrl}
            dica="De preferência em pé (vertical), mostrando o resultado do trabalho."
          />
          <Campo id="lb-leg" rotulo="Legenda (opcional)" dica="Ex.: Corte bob com mechas">
            <input id="lb-leg" value={form.legenda} maxLength={120} onChange={(e) => setForm((f) => ({ ...f, legenda: e.target.value }))} />
          </Campo>
          <Campo id="lb-ord" rotulo="Ordem" dica="Menor aparece primeiro.">
            <input id="lb-ord" type="number" min="0" value={form.ordem} onChange={(e) => setForm((f) => ({ ...f, ordem: e.target.value }))} />
          </Campo>
        </div>
        <div className="modal-rodape">
          <button type="button" className="botao botao-secundario" onClick={aoFechar}>Cancelar</button>
          <button type="submit" className="botao botao-primario" disabled={salvando}>{salvando ? 'Salvando…' : 'Salvar'}</button>
        </div>
      </form>
    </Modal>
  );
}
