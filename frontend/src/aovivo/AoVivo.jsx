import { createContext, useContext, useEffect, useRef } from 'react';
import { urlApi } from '../api/http.js';

/**
 * Atualização ao vivo: UMA conexão SSE por aba com /api/public/eventos.
 *
 * Quando o dono altera algo no /admin (ou alguém agenda/cancela), o backend
 * envia "catalogo-atualizado" com o tipo: servicos | profissionais | salao | agenda.
 * Cada parte da tela escuta só os tipos que lhe interessam e recarrega em silêncio.
 *
 * Também recarrega tudo quando:
 *  - a conexão volta depois de cair ("reconexao": pode ter perdido avisos);
 *  - a aba volta a ficar visível ("foco": celular que ficou em segundo plano).
 */
const OuvintesCtx = createContext(null);

export function AoVivoProvider({ children }) {
  const ouvintes = useRef(new Set());

  useEffect(() => {
    let fonte;
    let timer;
    let encerrado = false;
    let jaConectou = false;

    const avisar = (tipo) =>
      ouvintes.current.forEach((fn) => {
        try {
          fn(tipo);
        } catch (e) {
          console.error(e);
        }
      });

    const conectar = () => {
      fonte = new EventSource(urlApi('/api/public/eventos'));
      fonte.onopen = () => {
        if (jaConectou) avisar('reconexao');
        jaConectou = true;
      };
      fonte.addEventListener('catalogo-atualizado', (e) => {
        let tipo = null;
        try {
          tipo = JSON.parse(e.data).tipo;
        } catch {
          /* evento sem tipo: recarrega tudo */
        }
        avisar(tipo);
      });
      fonte.onerror = () => {
        // o navegador já tenta reconectar sozinho; se desistiu (CLOSED), tentamos em 5s
        if (fonte.readyState === EventSource.CLOSED && !encerrado) {
          clearTimeout(timer);
          timer = setTimeout(conectar, 5000);
        }
      };
    };

    conectar();
    const aoVoltarParaAba = () => {
      if (document.visibilityState === 'visible') avisar('foco');
    };
    document.addEventListener('visibilitychange', aoVoltarParaAba);

    return () => {
      encerrado = true;
      clearTimeout(timer);
      if (fonte) fonte.close();
      document.removeEventListener('visibilitychange', aoVoltarParaAba);
    };
  }, []);

  return <OuvintesCtx.Provider value={ouvintes}>{children}</OuvintesCtx.Provider>;
}

/**
 * Executa `aoMudar` quando chegar um aviso de um dos `tipos` (null = todos).
 * Avisos em sequência são agrupados (250 ms) para não recarregar várias vezes.
 */
export function useAoVivo(aoMudar, tipos = null) {
  const ouvintes = useContext(OuvintesCtx);
  const callback = useRef(aoMudar);
  callback.current = aoMudar;
  const chave = tipos ? tipos.join(',') : '*';

  useEffect(() => {
    if (!ouvintes) return undefined;
    const interessa = chave === '*' ? null : chave.split(',');
    let timer;
    const ouvinte = (tipo) => {
      const sempre = !tipo || tipo === 'reconexao' || tipo === 'foco';
      if (!sempre && interessa && !interessa.includes(tipo)) return;
      clearTimeout(timer);
      timer = setTimeout(() => callback.current(tipo), 250);
    };
    ouvintes.current.add(ouvinte);
    return () => {
      clearTimeout(timer);
      ouvintes.current.delete(ouvinte);
    };
  }, [ouvintes, chave]);
}
