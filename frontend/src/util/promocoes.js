import { dataDeIso, diaSemanaIso } from './formato.js';

/**
 * Regras de promoção no site — iguais às do servidor (PromocaoService):
 * vale no período, nos dias da semana escolhidos (vazio = todos) e nos
 * serviços escolhidos (ou todos). Se várias valerem, fica a de maior desconto.
 */
const NO_DIA = ['', 'às segundas', 'às terças', 'às quartas', 'às quintas', 'às sextas', 'aos sábados', 'aos domingos'];
const CURTO = ['', 'seg', 'ter', 'qua', 'qui', 'sex', 'sáb', 'dom'];

export function rotuloDias(dias) {
  if (!dias?.length) return 'Todos os dias';
  if (dias.length === 1) return NO_DIA[dias[0]].charAt(0).toUpperCase() + NO_DIA[dias[0]].slice(1);
  return dias.map((d) => CURTO[d]).join(', ');
}

/** Para o selo: "às terças" / "seg, qua" / "todos os dias" */
export function rotuloDiasCurto(dias) {
  if (!dias?.length) return 'todos os dias';
  if (dias.length === 1) return NO_DIA[dias[0]];
  return dias.map((d) => CURTO[d]).join(', ');
}

const valeParaServico = (p, servicoId) => p.todosServicos || (p.servicoIds || []).includes(Number(servicoId));

function valeNaData(p, servicoId, iso) {
  if (!valeParaServico(p, servicoId)) return false;
  if (p.dataInicio && iso < p.dataInicio) return false;
  if (p.dataFim && iso > p.dataFim) return false;
  if (p.diasSemana?.length && !p.diasSemana.includes(diaSemanaIso(dataDeIso(iso)))) return false;
  return true;
}

const maior = (lista) => lista.reduce((m, p) => (!m || p.descontoPercentual > m.descontoPercentual ? p : m), null);

/** Promoção que vale para o serviço NESTA data (ou null). */
export const melhorPromocao = (promocoes, servicoId, iso) =>
  !promocoes || !servicoId || !iso ? null : maior(promocoes.filter((p) => valeNaData(p, servicoId, iso)));

/** Maior promoção do serviço em qualquer dia (para o selo do card). */
export const melhorPromocaoDoServico = (promocoes, servicoId) =>
  !promocoes ? null : maior(promocoes.filter((p) => valeParaServico(p, servicoId)));

/** Mesmo arredondamento do servidor (meio para cima, 2 casas). */
export const precoComDesconto = (preco, percentual) => Math.round(Number(preco) * (100 - percentual)) / 100;
