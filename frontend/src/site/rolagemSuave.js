/**
 * Rolagem suave do site, sem bibliotecas.
 *
 * - Roda do mouse (computador): em vez de pular em "degraus", desliza até o
 *   destino com uma curva suave.
 * - Touchpad e celular: ficam com a rolagem nativa (já é suave e tem inércia).
 * - Links do menu (#servicos, #equipe...): mesma curva, com duração ajustada
 *   à distância.
 * - Quem pediu "reduzir movimento" no aparelho: nada muda.
 * - Dentro de algo que rola sozinho (listas, mapa) ou com
 *   data-rolagem-nativa: rolagem normal.
 */

const DESLOCAMENTO_CABECALHO = 96; // mesmo valor do scroll-padding-top do CSS

let ativa = false;
let alvo = 0;
let atual = 0;
let quadro = null;
let animacao = null; // { inicio, de, para, duracao } para links do menu

const reduzirMovimento = () => window.matchMedia('(prefers-reduced-motion: reduce)').matches;
const limite = () => Math.max(0, document.documentElement.scrollHeight - window.innerHeight);
const prender = (y) => Math.min(Math.max(y, 0), limite());
const curva = (t) => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2); // ease-in-out

function passo(agora) {
  if (animacao) {
    const t = Math.min(1, (agora - animacao.inicio) / animacao.duracao);
    atual = animacao.de + (animacao.para - animacao.de) * curva(t);
    if (t >= 1) {
      animacao = null;
      alvo = atual;
    }
  } else {
    atual += (alvo - atual) * 0.11;
    if (Math.abs(alvo - atual) < 0.5) atual = alvo;
  }
  window.scrollTo(0, atual);
  quadro = atual !== alvo || animacao ? requestAnimationFrame(passo) : null;
}

function iniciar() {
  if (!quadro) quadro = requestAnimationFrame(passo);
}

/** Algum elemento entre o alvo do evento e a página rola nessa direção? */
function rolaPorDentro(elemento, deltaY) {
  for (let el = elemento; el && el !== document.body; el = el.parentElement) {
    if (el.hasAttribute?.('data-rolagem-nativa')) return true;
    const estilo = getComputedStyle(el);
    if (/(auto|scroll)/.test(estilo.overflowY) && el.scrollHeight > el.clientHeight) {
      if (deltaY > 0 && el.scrollTop + el.clientHeight < el.scrollHeight) return true;
      if (deltaY < 0 && el.scrollTop > 0) return true;
    }
  }
  return false;
}

function aoGirarRoda(e) {
  if (e.ctrlKey || e.defaultPrevented) return; // zoom
  if (rolaPorDentro(e.target, e.deltaY)) return;
  // touchpad manda passos pequenos e quebrados: deixa nativo
  const pixels = e.deltaMode === 1 ? e.deltaY * 40 : e.deltaMode === 2 ? e.deltaY * window.innerHeight : e.deltaY;
  if (e.deltaMode === 0 && Math.abs(e.deltaY) < 50 && !Number.isInteger(e.deltaY)) return;
  e.preventDefault();
  if (!quadro) atual = window.scrollY;
  animacao = null;
  alvo = prender(alvo + pixels);
  iniciar();
}

/** Rolagem vinda de outro lugar (teclado, barra de rolagem): sincroniza. */
function aoRolar() {
  if (!quadro) {
    atual = window.scrollY;
    alvo = atual;
  }
}

/** Desliza até um elemento (links do menu, voltar ao topo). */
export function rolarAte(elemento) {
  if (!elemento) return;
  const destino = prender(elemento.getBoundingClientRect().top + window.scrollY - DESLOCAMENTO_CABECALHO);
  if (reduzirMovimento()) {
    window.scrollTo(0, destino);
    return;
  }
  const de = window.scrollY;
  const distancia = Math.abs(destino - de);
  atual = de;
  alvo = destino;
  animacao = { inicio: performance.now(), de, para: destino, duracao: Math.min(1300, Math.max(550, distancia * 0.45)) };
  if (quadro) cancelAnimationFrame(quadro);
  quadro = requestAnimationFrame(passo);
}

/** Links "#secao" da própria página usam a mesma curva. */
function aoClicar(e) {
  if (e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey) return;
  const link = e.target.closest?.('a[href^="#"]');
  if (!link) return;
  const id = decodeURIComponent(link.getAttribute('href').slice(1));
  const destino = id ? document.getElementById(id) : null;
  if (!destino) return;
  e.preventDefault();
  rolarAte(destino);
  history.replaceState(null, '', `#${id}`);
}

/** Liga a rolagem suave. Devolve a função que desliga. */
export function ativarRolagemSuave() {
  if (ativa || reduzirMovimento()) return () => {};
  ativa = true;
  // só computador com mouse: celular e tablet ficam com a rolagem nativa
  const comMouse = window.matchMedia('(hover: hover) and (pointer: fine)').matches;
  document.documentElement.classList.add('rolagem-suave');
  if (comMouse) window.addEventListener('wheel', aoGirarRoda, { passive: false });
  window.addEventListener('scroll', aoRolar, { passive: true });
  document.addEventListener('click', aoClicar);
  atual = window.scrollY;
  alvo = atual;
  return () => {
    window.removeEventListener('wheel', aoGirarRoda);
    window.removeEventListener('scroll', aoRolar);
    document.removeEventListener('click', aoClicar);
    document.documentElement.classList.remove('rolagem-suave');
    if (quadro) cancelAnimationFrame(quadro);
    quadro = null;
    animacao = null;
    ativa = false;
  };
}
