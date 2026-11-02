import { useEffect, useState } from 'react';
import { Navigate, NavLink, Route, Routes } from 'react-router-dom';
import { adminApi } from '../api/admin.js';
import { useDemo } from '../demo/Demonstracao.jsx';
import { credenciais, definirAoDispositivoPendente, definirAoNaoAutorizado } from '../api/http.js';
import { AvisosProvider } from './componentes.jsx';
import Login from './Login.jsx';
import Acessos from './paginas/Acessos.jsx';
import Agenda from './paginas/Agenda.jsx';
import Avaliacoes from './paginas/Avaliacoes.jsx';
import Bloqueios from './paginas/Bloqueios.jsx';
import Clientes from './paginas/Clientes.jsx';
import MinhaConta from './paginas/MinhaConta.jsx';
import Lookbook from './paginas/Lookbook.jsx';
import Perguntas from './paginas/Perguntas.jsx';
import Produtos from './paginas/Produtos.jsx';
import Promocoes from './paginas/Promocoes.jsx';
import Profissionais from './paginas/Profissionais.jsx';
import Salao from './paginas/Salao.jsx';
import Servicos from './paginas/Servicos.jsx';
import WhatsApp from './paginas/WhatsApp.jsx';
import '../styles/admin.css';

const MENU = [
  ['agenda', 'Agenda'],
  ['servicos', 'Serviços e preços'],
  ['produtos', 'Produtos'],
  ['promocoes', 'Promoções'],
  ['lookbook', 'Lookbook'],
  ['profissionais', 'Profissionais'],
  ['salao', 'Salão e horários'],
  ['avaliacoes', 'Avaliações'],
  ['bloqueios', 'Bloqueios'],
  ['clientes', 'Clientes e faltas'],
  ['whatsapp', 'WhatsApp'],
  ['perguntas', 'Perguntas frequentes'],
];

export default function AdminApp() {
  // demonstração: o visitante entra direto, sem login (só na cópia dele)
  const visitante = useDemo()?.modo === 'VISITANTE';
  const [logado, setLogado] = useState(() => visitante || !!credenciais.obter());
  const [expirou, setExpirou] = useState(false);
  const [menuAberto, setMenuAberto] = useState(false);
  const [usuario, setUsuario] = useState(null);

  // Se este dispositivo deixar de ser confirmado (removido em "Minha conta" ou senha trocada
  // em outro aparelho), volta para a tela de entrar.
  useEffect(() => {
    definirAoDispositivoPendente(
      logado
        ? () => {
            credenciais.limpar();
            setExpirou(false);
            setLogado(false);
          }
        : () => {},
    );
  }, [logado]);

  // quem está logado (nome, e-mail e se é dono)
  useEffect(() => {
    if (!logado) {
      setUsuario(null);
      return;
    }
    adminApi.me().then(setUsuario).catch(() => {});
  }, [logado]);

  useEffect(() => {
    definirAoNaoAutorizado(() => {
      credenciais.limpar();
      setExpirou(true);
      setLogado(false);
    });
    document.title = 'Painel | Jo Fashion Hair';
    return () => definirAoNaoAutorizado(() => {});
  }, []);

  // Saída automática: qualquer uso do painel adia; 30 min parado, sai sozinho.
  useEffect(() => {
    if (!logado || visitante) return undefined;
    let ultimaMarcacao = 0;
    const marcarUso = () => {
      const agora = Date.now();
      if (agora - ultimaMarcacao > 15_000) {   // no máximo a cada 15 s
        ultimaMarcacao = agora;
        credenciais.renovar();
      }
    };
    const eventos = ['pointerdown', 'keydown', 'scroll', 'touchstart'];
    eventos.forEach((e) => window.addEventListener(e, marcarUso, { passive: true }));
    const verificador = setInterval(() => {
      if (!credenciais.obter()) {
        setExpirou(true);
        setLogado(false);
      }
    }, 30_000);
    return () => {
      eventos.forEach((e) => window.removeEventListener(e, marcarUso));
      clearInterval(verificador);
    };
  }, [logado, visitante]);

  if (!logado) {
    return (
      <Login
        sessaoExpirada={expirou}
        aoEntrar={() => {
          setExpirou(false);
          setLogado(true);
        }}
      />
    );
  }

  const sair = () => {
    credenciais.limpar();
    setLogado(false);
  };

  return (
    <AvisosProvider>
      <div className="admin">
        <aside className={`admin-menu${menuAberto ? ' aberto' : ''}`}>
          <div className="admin-menu-topo">
            <span className="admin-marca">Painel do salão</span>
            <button
              type="button"
              className="admin-menu-botao"
              aria-expanded={menuAberto}
              aria-controls="admin-nav"
              onClick={() => setMenuAberto((v) => !v)}
            >
              Menu
            </button>
          </div>
          <nav id="admin-nav" aria-label="Painel">
            {MENU.map(([rota, rotulo]) => (
              <NavLink key={rota} to={rota} onClick={() => setMenuAberto(false)}>
                {rotulo}
              </NavLink>
            ))}
            {usuario?.papel === 'DONO' && (
              <NavLink to="acessos" onClick={() => setMenuAberto(false)}>Acessos</NavLink>
            )}
            <NavLink to="conta" onClick={() => setMenuAberto(false)}>Minha conta</NavLink>
          </nav>
          <div className="admin-menu-rodape">
            {usuario && <span className="admin-usuario">{usuario.nome}</span>}
            <a href="/" target="_blank" rel="noreferrer">Ver o site</a>
            {!visitante && <button type="button" className="botao-texto" onClick={sair}>Sair</button>}
          </div>
        </aside>
        <main className="admin-conteudo">
          <Routes>
            <Route index element={<Navigate to="agenda" replace />} />
            <Route path="agenda" element={<Agenda />} />
            <Route path="servicos" element={<Servicos />} />
            <Route path="produtos" element={<Produtos />} />
            <Route path="promocoes" element={<Promocoes />} />
            <Route path="lookbook" element={<Lookbook />} />
            <Route path="profissionais" element={<Profissionais />} />
            <Route path="salao" element={<Salao />} />
            <Route path="avaliacoes" element={<Avaliacoes />} />
            <Route path="bloqueios" element={<Bloqueios />} />
            <Route path="clientes" element={<Clientes />} />
            <Route path="whatsapp" element={<WhatsApp />} />
            <Route path="perguntas" element={<Perguntas />} />
            <Route path="conta" element={<MinhaConta />} />
            {usuario?.papel === 'DONO' && <Route path="acessos" element={<Acessos />} />}
            <Route path="*" element={<Navigate to="agenda" replace />} />
          </Routes>
        </main>
      </div>
    </AvisosProvider>
  );
}
