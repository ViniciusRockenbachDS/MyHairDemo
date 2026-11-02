import { diaSemanaIso, hora, NOMES_DIAS } from './formato.js';

const pad = (n) => String(n).padStart(2, '0');

/**
 * "Aberto agora · fecha às 19:00" | "Fechado · abre amanhã às 09:00" | ...
 * Calculado no navegador a partir do horário de funcionamento do /admin.
 */
export function statusFuncionamento(horarios, agora = new Date()) {
  if (!horarios?.length) return null;
  const porDia = new Map(horarios.map((h) => [h.diaSemana, h]));
  const hhmm = `${pad(agora.getHours())}:${pad(agora.getMinutes())}`;
  const hoje = porDia.get(diaSemanaIso(agora));

  if (hoje && !hoje.fechado) {
    const abre = hora(hoje.abertura);
    const fecha = hora(hoje.fechamento);
    if (hhmm >= abre && hhmm < fecha) return { aberto: true, texto: `Aberto agora · fecha às ${fecha}` };
    if (hhmm < abre) return { aberto: false, texto: `Fechado · abre hoje às ${abre}` };
  }
  for (let i = 1; i <= 7; i++) {
    const d = new Date(agora);
    d.setDate(d.getDate() + i);
    const h = porDia.get(diaSemanaIso(d));
    if (h && !h.fechado) {
      const quando = i === 1 ? 'amanhã' : NOMES_DIAS[diaSemanaIso(d)].toLowerCase();
      return { aberto: false, texto: `Fechado · abre ${quando} às ${hora(h.abertura)}` };
    }
  }
  return { aberto: false, texto: 'Fechado' };
}

/** Junta dias seguidos com o mesmo horário: "Segunda a sexta: 09:00–19:00". */
export function agruparHorarios(horarios) {
  const grupos = [];
  [...(horarios || [])]
    .sort((a, b) => a.diaSemana - b.diaSemana)
    .forEach((h) => {
      const texto = h.fechado ? 'Fechado' : `${hora(h.abertura)}–${hora(h.fechamento)}`;
      const ultimo = grupos[grupos.length - 1];
      if (ultimo && ultimo.texto === texto && ultimo.fim === h.diaSemana - 1) ultimo.fim = h.diaSemana;
      else grupos.push({ inicio: h.diaSemana, fim: h.diaSemana, texto });
    });
  return grupos.map((g) => ({
    rotulo: g.inicio === g.fim ? NOMES_DIAS[g.inicio] : `${NOMES_DIAS[g.inicio]} a ${NOMES_DIAS[g.fim].toLowerCase()}`,
    texto: g.texto,
  }));
}
