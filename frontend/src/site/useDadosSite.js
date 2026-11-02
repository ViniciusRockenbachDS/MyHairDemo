import { useEffect, useState } from 'react';
import { apiPublica } from '../api/publico.js';
import { useAoVivo } from '../aovivo/AoVivo.jsx';
import { useCarregar } from '../hooks/useCarregar.js';

export const NOME_PADRAO = 'Jo Fashion Hair';

/**
 * Dados do site (serviços, equipe, salão), recarregados ao vivo quando o
 * dono altera algo no /admin.
 */
export function useDadosSite() {
  const servicos = useCarregar(() => apiPublica.servicos(), []);
  const equipe = useCarregar(() => apiPublica.equipe(), []);
  const salao = useCarregar(() => apiPublica.salao(), []);
  const avaliacoes = useCarregar(() => apiPublica.avaliacoes().catch(() => null), []);
  const produtos = useCarregar(() => apiPublica.produtos().catch(() => []), []);
  const promocoes = useCarregar(() => apiPublica.promocoes().catch(() => []), []);
  const lookbook = useCarregar(() => apiPublica.lookbook().catch(() => []), []);

  useAoVivo(() => servicos.recarregar(true), ['servicos', 'profissionais']);
  useAoVivo(() => equipe.recarregar(true), ['profissionais', 'servicos']);
  useAoVivo(() => salao.recarregar(true), ['salao']);
  useAoVivo(() => avaliacoes.recarregar(true), ['avaliacoes']);
  useAoVivo(() => produtos.recarregar(true), ['produtos']);
  useAoVivo(() => promocoes.recarregar(true), ['promocoes', 'servicos']);
  useAoVivo(() => lookbook.recarregar(true), ['lookbook']);

  const info = salao.dados?.informacoes || null;
  return {
    servicos,
    equipe,
    salao,
    // sem avaliações cadastradas, a seção fica escondida
    avaliacoes: avaliacoes.dados?.avaliacoes?.length ? avaliacoes.dados : null,
    // sem produtos cadastrados, a vitrine fica escondida
    produtos: produtos.dados?.length ? produtos.dados : null,
    promocoes: promocoes.dados?.length ? promocoes.dados : null,
    lookbook: lookbook.dados?.length ? lookbook.dados : null,
    info,
    horarios: salao.dados?.horarios || [],
    nome: info?.nome || NOME_PADRAO,
  };
}

/** "Agora", atualizado a cada minuto (para o aberto/fechado mudar sozinho). */
export function useAgora() {
  const [agora, setAgora] = useState(() => new Date());
  useEffect(() => {
    const t = setInterval(() => setAgora(new Date()), 60_000);
    return () => clearInterval(t);
  }, []);
  return agora;
}
