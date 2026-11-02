import { useEffect } from 'react';

/**
 * Animação de "aparecer ao rolar": todo elemento com a classe `revelar`
 * surge suavemente (sobe e clareia) quando entra na tela.
 *
 * - Marca com o atributo data-revelado (e não com classe), para o React não
 *   apagar a marcação quando atualizar o elemento ao vivo.
 * - Quem pediu "reduzir movimento" (ou navegador antigo) vê tudo direto.
 * - `deps`: quando os dados chegam ou mudam, procura os novos elementos.
 */
export function useRevelar(deps = []) {
  useEffect(() => {
    const elementos = Array.from(document.querySelectorAll('.revelar:not([data-revelado])'));
    if (!elementos.length) return undefined;

    const reduzirMovimento = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reduzirMovimento || !('IntersectionObserver' in window)) {
      elementos.forEach((el) => el.setAttribute('data-revelado', ''));
      return undefined;
    }

    const observador = new IntersectionObserver(
      (entradas) => {
        entradas.forEach((entrada) => {
          if (entrada.isIntersecting) {
            entrada.target.setAttribute('data-revelado', '');
            observador.unobserve(entrada.target);
          }
        });
      },
      { threshold: 0.12, rootMargin: '0px 0px -40px 0px' },
    );
    elementos.forEach((el) => observador.observe(el));
    return () => observador.disconnect();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
}

/** Atraso em cascata para cartões de uma grade (o 1º entra antes do 2º...). */
export const atrasoCascata = (indice) => ({ '--atraso': `${Math.min(indice, 7) * 70}ms` });
