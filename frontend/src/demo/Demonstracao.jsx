import { createContext, useContext, useEffect, useState } from 'react';
import { apiPublica } from '../api/publico.js';
import './demo.css';

/**
 * Modo demonstração (só existe quando o site é construído com VITE_DEMO=true).
 *
 * Antes de mostrar o site, pede ao servidor a "cópia" particular deste
 * visitante. Com ?mestre=CHAVE no endereço, entra no modo do dono, que edita
 * os dados originais (com login).
 */
const DemoContexto = createContext(null);

/** null fora da demonstração; { modo: 'VISITANTE' | 'BASE', horas } dentro dela. */
export const useDemo = () => useContext(DemoContexto);

export const DEMO_LIGADA = import.meta.env.VITE_DEMO === 'true';

export function Demonstracao({ children }) {
  const [estado, setEstado] = useState(null);
  const [erro, setErro] = useState(null);
  const [tentativa, setTentativa] = useState(0);

  useEffect(() => {
    let ativo = true;
    const url = new URL(window.location.href);
    const mestre = url.searchParams.get('mestre');
    if (mestre) {
      // tira a chave da barra de endereço (não fica no histórico nem em prints)
      url.searchParams.delete('mestre');
      window.history.replaceState(null, '', url.pathname + url.search + url.hash);
    }
    setErro(null);
    apiPublica
      .iniciarDemo(mestre)
      .then((r) => ativo && setEstado(r))
      .catch((e) => ativo && setErro(e.message || 'Não foi possível abrir a demonstração.'));
    return () => {
      ativo = false;
    };
  }, [tentativa]);

  if (erro) {
    return (
      <main className="demo-espera">
        <h1>Demonstração indisponível</h1>
        <p>{erro}</p>
        <button type="button" className="botao botao-primario" onClick={() => setTentativa((n) => n + 1)}>
          Tentar de novo
        </button>
      </main>
    );
  }
  if (!estado) {
    return (
      <main className="demo-espera" aria-busy="true">
        <span className="demo-carregando" aria-hidden="true" />
        <h1>Preparando sua demonstração…</h1>
        <p>Estamos criando uma cópia só sua do sistema. Na primeira visita do dia isso pode levar alguns segundos.</p>
      </main>
    );
  }
  return (
    <DemoContexto.Provider value={estado}>
      {children}
      <FaixaDemo estado={estado} />
    </DemoContexto.Provider>
  );
}

function FaixaDemo({ estado }) {
  const [aberta, setAberta] = useState(true);
  if (!aberta) {
    return (
      <button type="button" className="demo-faixa demo-faixa-mini" onClick={() => setAberta(true)} aria-label="Sobre a demonstração">
        Demo
      </button>
    );
  }
  return (
    <div className={`demo-faixa${estado.modo === 'BASE' ? ' demo-faixa-dono' : ''}`} role="note">
      {estado.modo === 'BASE' ? (
        <span>
          <strong>Modo dono da demonstração.</strong> O que você mudar aqui vale para todos os visitantes.
        </span>
      ) : (
        <span>
          <strong>Demonstração.</strong> Fique à vontade: o painel em <a href="/admin">/admin</a> abre sem senha, e suas
          alterações valem só para você (somem depois de {estado.horas} h sem uso).
        </span>
      )}
      <button type="button" className="demo-faixa-fechar" onClick={() => setAberta(false)} aria-label="Minimizar aviso">
        ×
      </button>
    </div>
  );
}
