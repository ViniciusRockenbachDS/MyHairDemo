import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { adminApi } from '../api/admin.js';
import { urlImagem } from '../api/http.js';

/* ---------------------------------------------------------------- Modal */
export function Modal({ titulo, aoFechar, children, largo = false }) {
  const caixa = useRef(null);
  const fechar = useRef(aoFechar);
  fechar.current = aoFechar;

  useEffect(() => {
    const anterior = document.activeElement;
    const primeiro = caixa.current?.querySelector('input, select, textarea');
    (primeiro || caixa.current)?.focus();
    const tecla = (e) => {
      if (e.key === 'Escape') fechar.current();
    };
    document.addEventListener('keydown', tecla);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', tecla);
      document.body.style.overflow = '';
      if (anterior && anterior.focus) anterior.focus();
    };
  }, []);

  return createPortal(
    <div
      className="modal-fundo"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) fechar.current();
      }}
    >
      <div
        className={`modal${largo ? ' modal-largo' : ''}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-titulo"
        ref={caixa}
        tabIndex={-1}
      >
        <header className="modal-cabeca">
          <h2 id="modal-titulo">{titulo}</h2>
          <button type="button" className="modal-fechar" onClick={() => fechar.current()} aria-label="Fechar">
            ×
          </button>
        </header>
        {children}
      </div>
    </div>,
    document.body,
  );
}

/* ---------------------------------------------------------------- Avisos (toasts) */
const AvisosCtx = createContext({ sucesso: () => {}, erro: () => {} });

export function AvisosProvider({ children }) {
  const [lista, setLista] = useState([]);
  const mostrar = useCallback((texto, tipo) => {
    const id = `${Date.now()}-${Math.random()}`;
    setLista((l) => [...l, { id, texto, tipo }]);
    setTimeout(() => setLista((l) => l.filter((a) => a.id !== id)), tipo === 'erro' ? 7000 : 4500);
  }, []);
  const api = useMemo(
    () => ({ sucesso: (t) => mostrar(t, 'sucesso'), erro: (t) => mostrar(t, 'erro') }),
    [mostrar],
  );
  return (
    <AvisosCtx.Provider value={api}>
      {children}
      <div className="avisos" aria-live="polite">
        {lista.map((a) => (
          <div key={a.id} className={`aviso aviso-${a.tipo}`} role={a.tipo === 'erro' ? 'alert' : 'status'}>
            {a.texto}
          </div>
        ))}
      </div>
    </AvisosCtx.Provider>
  );
}

export const useAvisos = () => useContext(AvisosCtx);

/* ---------------------------------------------------------------- Diversos */
const STATUS = {
  AGUARDANDO_SINAL: 'Aguardando sinal',
  PENDENTE: 'Pendente',
  CONFIRMADO: 'Confirmado',
  CONCLUIDO: 'Concluído',
  CANCELADO: 'Cancelado',
  FALTOU: 'Faltou',
};

export function StatusEtiqueta({ status }) {
  return <span className={`etiqueta etiqueta-${status.toLowerCase()}`}>{STATUS[status] || status}</span>;
}

export const OPCOES_STATUS = Object.entries(STATUS);

/** Mostra "carregando", erro com "tentar de novo", vazio ou o conteúdo. */
export function Carga({ estado, vazio, children }) {
  if (estado.carregando && !estado.dados) return <p className="estado">Carregando…</p>;
  if (estado.erro && !estado.dados) {
    return (
      <p className="erro-texto">
        {estado.erro.message}{' '}
        <button type="button" className="botao-texto" onClick={() => estado.recarregar()}>
          Tentar de novo
        </button>
      </p>
    );
  }
  if (vazio && Array.isArray(estado.dados) && estado.dados.length === 0) {
    return <p className="vazio">{vazio}</p>;
  }
  return children;
}

export function Campo({ id, rotulo, erro, dica, children }) {
  return (
    <div className="campo">
      <label htmlFor={id}>{rotulo}</label>
      {children}
      {dica && !erro && <span className="dica">{dica}</span>}
      {erro && <span className="erro-texto">{erro}</span>}
    </div>
  );
}

/* ---------------------------------------------------------------- Envio de imagem */

/**
 * Reduz fotos grandes (de celular costumam ter 4–8 MB) antes de enviar:
 * no máximo 1600 px no lado maior. PNG continua PNG (mantém fundo transparente
 * de logos); o resto vira JPG. Se o navegador não conseguir ler, envia o original.
 */
async function prepararImagem(arquivo, ladoMaximo = 1600) {
  if (!arquivo.type.startsWith('image/')) {
    throw new Error('Escolha um arquivo de imagem (JPG, PNG ou WEBP).');
  }
  try {
    const bitmap = await createImageBitmap(arquivo);
    const escala = Math.min(1, ladoMaximo / Math.max(bitmap.width, bitmap.height));
    const formatoAceito = /image\/(jpeg|png|webp)/.test(arquivo.type);
    if (escala === 1 && formatoAceito && arquivo.size <= 1.5 * 1024 * 1024) return arquivo;

    const canvas = document.createElement('canvas');
    canvas.width = Math.round(bitmap.width * escala);
    canvas.height = Math.round(bitmap.height * escala);
    canvas.getContext('2d').drawImage(bitmap, 0, 0, canvas.width, canvas.height);
    const tipo = arquivo.type === 'image/png' ? 'image/png' : 'image/jpeg';
    const blob = await new Promise((ok) => canvas.toBlob(ok, tipo, 0.85));
    if (!blob) return arquivo;
    return new File([blob], tipo === 'image/png' ? 'imagem.png' : 'imagem.jpg', { type: tipo });
  } catch {
    return arquivo;
  }
}

/**
 * Campo de imagem: prévia + "Escolher do dispositivo" + "Remover".
 * A imagem é enviada na hora; o valor (endereço) só vale depois de salvar o formulário.
 */
export function EnviarImagem({ id, rotulo, valor, aoMudar, dica, formato = 'retangulo', erro }) {
  const avisos = useAvisos();
  const entrada = useRef(null);
  const [enviando, setEnviando] = useState(false);

  async function escolher(e) {
    const arquivo = e.target.files?.[0];
    e.target.value = '';
    if (!arquivo) return;
    setEnviando(true);
    try {
      const pronto = await prepararImagem(arquivo);
      const r = await adminApi.enviarImagem(pronto);
      aoMudar(r.url);
      avisos.sucesso('Imagem carregada. Clique em salvar para aplicar no site.');
    } catch (err) {
      avisos.erro(err.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="campo">
      <span className="rotulo" id={`${id}-rotulo`}>{rotulo}</span>
      <div className={`imagem-envio imagem-${formato}`}>
        <div className="imagem-previa">
          {valor ? <img src={urlImagem(valor)} alt="" /> : <span>Sem imagem</span>}
        </div>
        <div className="imagem-acoes">
          <input
            ref={entrada}
            id={id}
            type="file"
            accept="image/jpeg,image/png,image/webp,image/*"
            className="visualmente-oculto"
            aria-labelledby={`${id}-rotulo`}
            onChange={escolher}
          />
          <button
            type="button"
            className="botao botao-secundario botao-pequeno"
            onClick={() => entrada.current?.click()}
            disabled={enviando}
          >
            {enviando ? 'Enviando…' : valor ? 'Trocar imagem' : 'Escolher do dispositivo'}
          </button>
          {valor && !enviando && (
            <button type="button" className="botao-texto texto-perigo" onClick={() => aoMudar('')}>
              Remover
            </button>
          )}
          {dica && <span className="dica">{dica}</span>}
          {erro && <span className="erro-texto">{erro}</span>}
        </div>
      </div>
    </div>
  );
}
