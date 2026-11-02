/**
 * Cliente HTTP único do front-end.
 * - Em dev, VITE_API_URL fica vazio e o Vite repassa /api ao Spring (vite.config.js).
 * - Erros da API (ProblemDetail) viram ErroApi com mensagem pronta para exibir.
 * - Rotas do /admin enviam o login (HTTP Basic) guardado na sessão do navegador.
 */
const BASE = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '');

export const urlApi = (caminho) => `${BASE}${caminho}`;

const CHAVE_LOGIN = 'jfh.admin.login';
const CHAVE_ATIVIDADE = 'jfh.admin.ultimaAtividade';

/** Sem uso por esse tempo, o painel sai sozinho. */
export const INATIVIDADE_MAXIMA_MS = 30 * 60 * 1000;

/**
 * Login do painel. Fica SÓ no sessionStorage (apagado ao fechar a aba) e
 * nunca no localStorage. Se ficar 30 minutos sem uso, deixa de valer —
 * inclusive se a aba for reaberta pelo "reabrir aba fechada" do navegador.
 */
export const credenciais = {
  obter: () => {
    const login = sessionStorage.getItem(CHAVE_LOGIN);
    if (!login) return null;
    const ultima = Number(sessionStorage.getItem(CHAVE_ATIVIDADE) || 0);
    if (Date.now() - ultima > INATIVIDADE_MAXIMA_MS) {
      credenciais.limpar();
      return null;
    }
    return login;
  },
  salvar: (usuario, senha) => {
    sessionStorage.setItem(CHAVE_LOGIN, btoa(unescape(encodeURIComponent(`${usuario}:${senha}`))));
    sessionStorage.setItem(CHAVE_ATIVIDADE, String(Date.now()));
  },
  /** Marca que a pessoa está usando o painel (adia a saída automática). */
  renovar: () => {
    if (sessionStorage.getItem(CHAVE_LOGIN)) sessionStorage.setItem(CHAVE_ATIVIDADE, String(Date.now()));
  },
  limpar: () => {
    sessionStorage.removeItem(CHAVE_LOGIN);
    sessionStorage.removeItem(CHAVE_ATIVIDADE);
  },
};

const MENSAGENS_PADRAO = {
  0: 'Sem conexão com o servidor. Verifique a internet e tente de novo.',
  401: 'Sua sessão expirou. Entre novamente.',
  429: 'Muitas tentativas. Aguarde alguns minutos e tente de novo.',
  403: 'Você não tem permissão para isso.',
  404: 'Não encontrado.',
  500: 'Erro no servidor. Tente de novo em instantes.',
};

export class ErroApi extends Error {
  constructor(status, corpo) {
    super(corpo?.detail || MENSAGENS_PADRAO[status] || 'Algo deu errado. Tente de novo.');
    this.status = status;
    this.campos = corpo?.campos || null;        // erros de validação por campo
    this.sugestoes = corpo?.sugestoes || [];    // horários alternativos (409)
    this.motivo = corpo?.motivo || null;
  }
}

let aoNaoAutorizado = () => {};
/** O painel registra aqui o que fazer quando o login expira (voltar à tela de entrar). */
export function definirAoNaoAutorizado(fn) {
  aoNaoAutorizado = fn;
}

let aoDispositivoPendente = () => {};
/** O painel registra aqui o que fazer se o dispositivo deixar de ser confirmado (ex.: removido). */
export function definirAoDispositivoPendente(fn) {
  aoDispositivoPendente = fn;
}

export const DISPOSITIVO_NAO_VERIFICADO = 'DISPOSITIVO_NAO_VERIFICADO';

export function qs(parametros) {
  const p = new URLSearchParams();
  Object.entries(parametros).forEach(([chave, valor]) => {
    if (valor !== null && valor !== undefined && valor !== '') p.append(chave, valor);
  });
  const texto = p.toString();
  return texto ? `?${texto}` : '';
}

export async function requisicao(caminho, { method = 'GET', body, admin = false } = {}) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (admin) {
    const login = credenciais.obter();
    if (login) headers.Authorization = `Basic ${login}`;
  }

  let resposta;
  try {
    resposta = await fetch(urlApi(caminho), {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      cache: 'no-store',
      // painel: envia o cookie do dispositivo confirmado (HttpOnly, invisível ao JavaScript)
      credentials: admin ? 'include' : 'same-origin',
    });
  } catch {
    throw new ErroApi(0, null);
  }

  if (resposta.status === 204) return null;
  const tipo = resposta.headers.get('content-type') || '';
  const dados = tipo.includes('json') ? await resposta.json().catch(() => null) : null;

  if (!resposta.ok) {
    // demonstração: a cópia deste visitante expirou → recarrega e começa outra
    if (dados?.motivo === 'DEMO_EXPIRADA') window.location.reload();
    if (resposta.status === 401 && admin) aoNaoAutorizado();
    if (resposta.status === 403 && admin && dados?.motivo === DISPOSITIVO_NAO_VERIFICADO) aoDispositivoPendente();
    throw new ErroApi(resposta.status, dados);
  }
  return dados;
}

/**
 * Endereço de uma imagem guardada pela API. Em produção a API fica em outro
 * domínio, então "/api/public/arquivos/x.jpg" ganha o endereço da API na frente.
 */
export function urlImagem(url) {
  if (!url) return url;
  return url.startsWith('/api/') ? urlApi(url) : url;
}

/** Envia um arquivo (multipart) para uma rota do /admin. */
export async function enviarArquivo(caminho, arquivo) {
  const dados = new FormData();
  dados.append('arquivo', arquivo);
  const headers = { Accept: 'application/json' };
  const login = credenciais.obter();
  if (login) headers.Authorization = `Basic ${login}`;

  let resposta;
  try {
    resposta = await fetch(urlApi(caminho), { method: 'POST', headers, body: dados, credentials: 'include' });
  } catch {
    throw new ErroApi(0, null);
  }
  const corpo = await resposta.json().catch(() => null);
  if (!resposta.ok) {
    if (resposta.status === 401) aoNaoAutorizado();
    throw new ErroApi(resposta.status, corpo);
  }
  return corpo;
}

/** Baixa um arquivo de uma rota do /admin (com login) e salva no computador. */
export async function baixarArquivo(caminho, nomeArquivo) {
  const headers = {};
  const login = credenciais.obter();
  if (login) headers.Authorization = `Basic ${login}`;
  let resposta;
  try {
    resposta = await fetch(urlApi(caminho), { headers, credentials: 'include' });
  } catch {
    throw new ErroApi(0, null);
  }
  if (!resposta.ok) {
    const corpo = await resposta.json().catch(() => null);
    if (resposta.status === 401) aoNaoAutorizado();
    throw new ErroApi(resposta.status, corpo);
  }
  const blob = await resposta.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = nomeArquivo;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
