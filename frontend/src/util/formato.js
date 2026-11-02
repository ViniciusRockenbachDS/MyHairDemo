/** Formatação pt-BR usada no site e no painel. */

const moedaFmt = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const pad = (n) => String(n).padStart(2, '0');

export const moeda = (valor) => moedaFmt.format(Number(valor ?? 0));

/** 40 → "40 min" | 60 → "1h" | 75 → "1h15" */
export function duracao(minutos) {
  const m = Number(minutos) || 0;
  if (m < 60) return `${m} min`;
  const resto = m % 60;
  return resto ? `${Math.floor(m / 60)}h${pad(resto)}` : `${m / 60}h`;
}

/** "17:00:00" ou "17:00" → "17:00" */
export const hora = (valor) => (valor || '').slice(0, 5);

/** "2026-10-16T17:00:00" → "17:00" */
export const horaDe = (dataHora) => (dataHora || '').slice(11, 16);

/** Date → "2026-10-16" (no fuso do navegador) */
export const isoData = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

/** "2026-10-16" → Date local (sem o bug de fuso do new Date("2026-10-16")) */
export function dataDeIso(iso) {
  const [a, m, d] = iso.slice(0, 10).split('-').map(Number);
  return new Date(a, m - 1, d);
}

/** 1 = segunda ... 7 = domingo (igual ao backend) */
export const diaSemanaIso = (d) => ((d.getDay() + 6) % 7) + 1;

export const NOMES_DIAS = ['', 'Segunda', 'Terça', 'Quarta', 'Quinta', 'Sexta', 'Sábado', 'Domingo'];

export const capitalizar = (s) => (s ? s.charAt(0).toUpperCase() + s.slice(1) : s);

/** "sexta-feira, 16/10" */
export const dataLonga = (d) => d.toLocaleDateString('pt-BR', { weekday: 'long', day: '2-digit', month: '2-digit' });

/** "sex 16/10" */
export const dataCurta = (d) =>
  `${d.toLocaleDateString('pt-BR', { weekday: 'short' }).replace('.', '')} ${pad(d.getDate())}/${pad(d.getMonth() + 1)}`;

export const diaSemanaCurto = (d) => d.toLocaleDateString('pt-BR', { weekday: 'short' }).replace('.', '');

/** "5547999998888" → "(47) 99999-8888" */
export function telefoneVisivel(t) {
  const d = (t || '').replace(/\D/g, '');
  const local = d.length >= 12 && d.startsWith('55') ? d.slice(2) : d;
  if (local.length === 11) return `(${local.slice(0, 2)}) ${local.slice(2, 7)}-${local.slice(7)}`;
  if (local.length === 10) return `(${local.slice(0, 2)}) ${local.slice(2, 6)}-${local.slice(6)}`;
  return t || '';
}

/** Máscara enquanto digita: "4799999" → "(47) 9999-9" */
export function mascaraTelefone(valor) {
  const d = valor.replace(/\D/g, '').slice(0, 11);
  if (d.length <= 2) return d.length ? `(${d}` : '';
  if (d.length <= 6) return `(${d.slice(0, 2)}) ${d.slice(2)}`;
  if (d.length <= 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`;
  return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`;
}

export function linkWhatsapp(numero, texto) {
  const d = (numero || '').replace(/\D/g, '');
  const completo = d.length === 10 || d.length === 11 ? `55${d}` : d;
  return `https://wa.me/${completo}${texto ? `?text=${encodeURIComponent(texto)}` : ''}`;
}

export function linkMapa(info) {
  if (info?.linkMapa) return info.linkMapa;
  if (!info?.endereco) return null;
  const consulta = [info.endereco, info.cidade].filter(Boolean).join(', ');
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(consulta)}`;
}

/** "120,50" ou "120.50" → 120.5 (null se inválido) */
export function lerValor(texto) {
  const s = String(texto ?? '').trim();
  if (!s) return null;
  const normalizado = s.includes(',') ? s.replace(/\./g, '').replace(',', '.') : s;
  const n = Number(normalizado);
  return Number.isFinite(n) ? n : null;
}

/** "2026-05-10" → "há 5 meses" */
export function tempoRelativo(iso) {
  if (!iso) return '';
  const dias = Math.max(0, Math.round((Date.now() - dataDeIso(iso).getTime()) / 86_400_000));
  const rtf = new Intl.RelativeTimeFormat('pt-BR', { numeric: 'auto' });
  if (dias < 7) return rtf.format(-dias, 'day');
  if (dias < 30) return rtf.format(-Math.round(dias / 7), 'week');
  if (dias < 365) return rtf.format(-Math.round(dias / 30), 'month');
  return rtf.format(-Math.round(dias / 365), 'year');
}
