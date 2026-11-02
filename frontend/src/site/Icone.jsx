/**
 * Ícones de traço (24×24, cor = currentColor). Desenhados aqui para não
 * depender de biblioteca externa.
 */
const CAMINHOS = {
  calendario: (
    <>
      <rect x="3" y="5" width="18" height="16" rx="2" />
      <path d="M16 3v4M8 3v4M3 10h18M9 15l2 2 4-4" />
    </>
  ),
  relogio: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 2" />
    </>
  ),
  usuario: (
    <>
      <circle cx="10" cy="8" r="4" />
      <path d="M3 21c0-4 3-6 7-6s7 2 7 6M17 11l2 2 3-3" />
    </>
  ),
  usuarios: (
    <>
      <circle cx="9" cy="8" r="4" />
      <path d="M2 21c0-4 3-6 7-6s7 2 7 6M16 4a4 4 0 0 1 0 8M22 21c0-3-2-5-4-5.5" />
    </>
  ),
  tesoura: (
    <>
      <circle cx="6" cy="6" r="3" />
      <circle cx="6" cy="18" r="3" />
      <path d="M8.1 8.1 20 20M8.1 15.9 20 4" />
    </>
  ),
  brilho: (
    <path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8zM19 16l.7 2 2 .7-2 .7-.7 2-.7-2-2-.7 2-.7z" />
  ),
  flor: (
    <>
      <circle cx="12" cy="12" r="2.5" />
      <path d="M12 9.5C12 6 10 4.5 12 3c2 1.5 0 3 0 6.5zM14.5 12c3.5 0 5-2 6.5 0-1.5 2-3 0-6.5 0zM12 14.5c0 3.5 2 5 0 6.5-2-1.5 0-3 0-6.5zM9.5 12C6 12 4.5 14 3 12c1.5-2 3 0 6.5 0z" />
    </>
  ),
  local: (
    <>
      <path d="M12 21s-7-6.2-7-12a7 7 0 0 1 14 0c0 5.8-7 12-7 12z" />
      <circle cx="12" cy="9" r="2.5" />
    </>
  ),
  seta: <path d="M5 12h14M13 6l6 6-6 6" />,
  setaDiagonal: <path d="M7 17 17 7M9 7h8v8" />,
  chevron: <path d="m6 9 6 6 6-6" />,
  mensagem: <path d="M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12zM8.5 12h.01M12 12h.01M15.5 12h.01" />,
  sino: <path d="M6 16V11a6 6 0 1 1 12 0v5l2 2H4zM10 21h4" />,
  instagram: (
    <>
      <rect x="3" y="3" width="18" height="18" rx="5" />
      <circle cx="12" cy="12" r="4" />
      <path d="M17.5 6.5h.01" />
    </>
  ),
  telefone: <path d="M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2z" />,
  externo: <path d="M14 4h6v6M20 4l-9 9M18 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h5" />,
  menu: <path d="M4 7h16M4 12h16M4 17h16" />,
  check: <path d="M5 12.5l4.5 4.5L19 7.5" />,
  proibido: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M5.7 5.7l12.6 12.6" />
    </>
  ),
  estrela: <path d="m12 3 2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1-4.4-4.3 6.1-.9z" fill="currentColor" />,
  fechar: <path d="M6 6l12 12M18 6 6 18" />,
};

export default function Icone({ nome, tamanho = 20, className }) {
  return (
    <svg
      width={tamanho}
      height={tamanho}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      className={className}
    >
      {CAMINHOS[nome]}
    </svg>
  );
}

/** Ícone de cada categoria de serviço. */
export const ICONE_CATEGORIA = { CABELO: 'tesoura', UNHAS: 'brilho', ESTETICA: 'flor', OUTROS: 'brilho' };
