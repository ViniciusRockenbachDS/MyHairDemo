import { qs, requisicao } from './http.js';

/** Endpoints abertos usados pelo site. */
export const apiPublica = {
  servicos: () => requisicao('/api/public/servicos'),
  equipe: () => requisicao('/api/public/profissionais'),
  profissionaisDoServico: (servicoId) => requisicao(`/api/public/servicos/${servicoId}/profissionais`),
  disponibilidade: (servicoId, data, profissionalId) =>
    requisicao(`/api/public/disponibilidade${qs({ servicoId, data, profissionalId })}`),
  salao: () => requisicao('/api/public/salao'),
  avaliacoes: () => requisicao('/api/public/avaliacoes'),
  produtos: () => requisicao('/api/public/produtos'),
  promocoes: () => requisicao('/api/public/promocoes'),
  lookbook: () => requisicao('/api/public/lookbook'),
  statusSinal: (codigo) => requisicao(`/api/public/sinais/${codigo}`),
  iniciarDemo: (mestre) => requisicao('/api/public/demo/iniciar', { method: 'POST', body: { mestre: mestre || null } }),
  agendar: (dados) => requisicao('/api/public/agendamentos', { method: 'POST', body: dados }),

  // Conta do painel (sem login): esqueci a senha / primeiro acesso
  esqueciSenha: (email) => requisicao('/api/public/conta/esqueci-senha', { method: 'POST', body: { email } }),
  redefinirSenha: (dados) => requisicao('/api/public/conta/redefinir-senha', { method: 'POST', body: dados }),
};
