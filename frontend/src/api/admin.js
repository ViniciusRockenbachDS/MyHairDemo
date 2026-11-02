import { baixarArquivo, enviarArquivo, qs, requisicao } from './http.js';

const req = (caminho, method = 'GET', body) => requisicao(caminho, { method, body, admin: true });

/** Endpoints do painel /admin (exigem login). */
export const adminApi = {
  me: () => req('/api/admin/me'),

  // Dispositivo novo (código por e-mail no primeiro login)
  enviarCodigoDispositivo: () => req('/api/admin/dispositivo/enviar-codigo', 'POST'),
  confirmarDispositivo: (codigo) => req('/api/admin/dispositivo/confirmar', 'POST', { codigo }),
  dispositivos: () => req('/api/admin/dispositivos'),
  removerDispositivo: (id) => req(`/api/admin/dispositivos/${id}`, 'DELETE'),
  sairDosOutrosDispositivos: () => req('/api/admin/dispositivos/sair-dos-outros', 'POST'),

  // Minha conta (troca de senha com código por e-mail)
  pedirCodigoSenha: () => req('/api/admin/conta/senha/codigo', 'POST'),
  trocarSenha: (d) => req('/api/admin/conta/senha', 'PUT', d),

  // Acessos da equipe (só dono)
  usuarios: () => req('/api/admin/usuarios'),
  convidarUsuario: (d) => req('/api/admin/usuarios', 'POST', d),
  reenviarConvite: (id) => req(`/api/admin/usuarios/${id}/convite`, 'POST'),
  usuarioAtivo: (id, ativo) => req(`/api/admin/usuarios/${id}/ativo`, 'PATCH', { ativo }),

  // Serviços
  servicos: () => req('/api/admin/servicos'),
  criarServico: (d) => req('/api/admin/servicos', 'POST', d),
  atualizarServico: (id, d) => req(`/api/admin/servicos/${id}`, 'PUT', d),
  precoDuracao: (id, d) => req(`/api/admin/servicos/${id}/preco-duracao`, 'PATCH', d),
  servicoAtivo: (id, ativo) => req(`/api/admin/servicos/${id}/ativo`, 'PATCH', { ativo }),

  // Produtos (vitrine do site)
  produtos: () => req('/api/admin/produtos'),
  criarProduto: (d) => req('/api/admin/produtos', 'POST', d),
  atualizarProduto: (id, d) => req(`/api/admin/produtos/${id}`, 'PUT', d),
  removerProduto: (id) => req(`/api/admin/produtos/${id}`, 'DELETE'),

  // Promoções e envios por WhatsApp
  promocoes: () => req('/api/admin/promocoes'),
  criarPromocao: (d) => req('/api/admin/promocoes', 'POST', d),
  atualizarPromocao: (id, d) => req(`/api/admin/promocoes/${id}`, 'PUT', d),
  promocaoAtiva: (id, ativo) => req(`/api/admin/promocoes/${id}/ativo`, 'PATCH', { ativo }),
  removerPromocao: (id) => req(`/api/admin/promocoes/${id}`, 'DELETE'),
  publicoPromocao: (id, d) => req(`/api/admin/promocoes/${id}/publico`, 'POST', d),
  enviarPromocao: (id, d) => req(`/api/admin/promocoes/${id}/enviar`, 'POST', d),
  campanhas: () => req('/api/admin/campanhas'),

  // Sinal por Pix
  sinalRecebido: (id) => req(`/api/admin/agendamentos/${id}/sinal-recebido`, 'POST'),
  situacaoPagamentos: () => req('/api/admin/pagamentos/situacao'),

  // Lookbook (fotos de trabalhos)
  lookbook: () => req('/api/admin/lookbook'),
  criarFotoLookbook: (d) => req('/api/admin/lookbook', 'POST', d),
  atualizarFotoLookbook: (id, d) => req(`/api/admin/lookbook/${id}`, 'PUT', d),
  removerFotoLookbook: (id) => req(`/api/admin/lookbook/${id}`, 'DELETE'),

  // Profissionais
  profissionais: () => req('/api/admin/profissionais'),
  profissional: (id) => req(`/api/admin/profissionais/${id}`),
  criarProfissional: (d) => req('/api/admin/profissionais', 'POST', d),
  atualizarProfissional: (id, d) => req(`/api/admin/profissionais/${id}`, 'PUT', d),
  profissionalAtiva: (id, ativo) => req(`/api/admin/profissionais/${id}/ativo`, 'PATCH', { ativo }),
  servicosDaProfissional: (id, servicoIds) => req(`/api/admin/profissionais/${id}/servicos`, 'PUT', { servicoIds }),
  horariosDaProfissional: (id, faixas) => req(`/api/admin/profissionais/${id}/horarios`, 'PUT', { faixas }),

  // Salão
  salaoInformacoes: () => req('/api/admin/salao/informacoes'),
  salvarSalaoInformacoes: (d) => req('/api/admin/salao/informacoes', 'PUT', d),
  salaoHorarios: () => req('/api/admin/salao/horarios'),
  salvarSalaoHorarios: (dias) => req('/api/admin/salao/horarios', 'PUT', { dias }),

  // Agenda
  agenda: (de, ate, profissionalId, status) =>
    req(`/api/admin/agendamentos${qs({ de, ate, profissionalId, status })}`),
  agendamentoManual: (d) => req('/api/admin/agendamentos', 'POST', d),
  alterarStatus: (id, status) => req(`/api/admin/agendamentos/${id}/status`, 'PATCH', { status }),
  semBaixa: () => req('/api/admin/agendamentos/sem-baixa'),
  lembrete: (id) => req(`/api/admin/agendamentos/${id}/lembrete`, 'POST'),

  // Bloqueios
  bloqueios: (de, ate, profissionalId) => req(`/api/admin/bloqueios${qs({ de, ate, profissionalId })}`),
  criarBloqueio: (d) => req('/api/admin/bloqueios', 'POST', d),
  removerBloqueio: (id) => req(`/api/admin/bloqueios/${id}`, 'DELETE'),

  // Clientes
  clientes: (busca) => req(`/api/admin/clientes${qs({ busca })}`),
  historicoCliente: (id) => req(`/api/admin/clientes/${id}/agendamentos`),
  exportarCliente: (id) => baixarArquivo(`/api/admin/clientes/${id}/exportar`, `dados-cliente-${id}.json`),
  excluirCliente: (id) => req(`/api/admin/clientes/${id}`, 'DELETE'),

  // Imagens (logo, fotos do salão, equipe, avaliações)
  enviarImagem: (arquivo) => enviarArquivo('/api/admin/uploads', arquivo),

  // Avaliações
  avaliacoes: () => req('/api/admin/avaliacoes'),
  criarAvaliacao: (d) => req('/api/admin/avaliacoes', 'POST', d),
  atualizarAvaliacao: (id, d) => req(`/api/admin/avaliacoes/${id}`, 'PUT', d),
  removerAvaliacao: (id) => req(`/api/admin/avaliacoes/${id}`, 'DELETE'),
  resumoAvaliacoes: () => req('/api/admin/avaliacoes/resumo'),
  salvarResumoAvaliacoes: (d) => req('/api/admin/avaliacoes/resumo', 'PUT', d),

  // Perguntas frequentes (respostas automáticas do robô)
  perguntas: () => req('/api/admin/perguntas'),
  criarPergunta: (d) => req('/api/admin/perguntas', 'POST', d),
  atualizarPergunta: (id, d) => req(`/api/admin/perguntas/${id}`, 'PUT', d),
  removerPergunta: (id) => req(`/api/admin/perguntas/${id}`, 'DELETE'),
  testarPergunta: (texto) => req('/api/admin/perguntas/testar', 'POST', { texto }),

  // WhatsApp
  conversas: (estado) => req(`/api/admin/whatsapp/conversas${qs({ estado })}`),
  reativarBot: (telefone) => req(`/api/admin/whatsapp/conversas/${telefone}/reativar-bot`, 'POST'),
  simular: (d) => req('/api/admin/whatsapp/simular', 'POST', d),
};
