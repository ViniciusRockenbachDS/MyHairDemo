import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Carrega dados de uma função assíncrona e expõe { dados, erro, carregando, recarregar }.
 * - Muda `deps` → carrega de novo (mostrando "carregando").
 * - recarregar(true) → atualização silenciosa, mantendo os dados na tela (usado no ao vivo).
 * - Respostas antigas que chegam atrasadas são descartadas.
 */
export function useCarregar(carregar, deps = []) {
  const [estado, setEstado] = useState({ dados: null, erro: null, carregando: true });
  const versao = useRef(0);
  const funcao = useRef(carregar);
  funcao.current = carregar;

  const recarregar = useCallback(async (silencioso = false) => {
    const minha = ++versao.current;
    if (!silencioso) setEstado({ dados: null, erro: null, carregando: true });
    try {
      const dados = await funcao.current();
      if (minha === versao.current) setEstado({ dados, erro: null, carregando: false });
    } catch (erro) {
      if (minha === versao.current) setEstado((e) => ({ ...e, erro, carregando: false }));
    }
  }, []);

  useEffect(() => {
    recarregar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return { ...estado, recarregar };
}
