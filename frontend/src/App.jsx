import { lazy, Suspense, useEffect } from 'react';
import { Route, Routes, useLocation } from 'react-router-dom';
import { AoVivoProvider } from './aovivo/AoVivo.jsx';
import PaginaAgendar from './site/PaginaAgendar.jsx';
import PaginaPrivacidade from './site/PaginaPrivacidade.jsx';
import SitePublico from './site/SitePublico.jsx';
import { ativarRolagemSuave } from './site/rolagemSuave.js';

// O painel é carregado sob demanda: quem só agenda não baixa o código do /admin.
const AdminApp = lazy(() => import('./admin/AdminApp.jsx'));

/** Rolagem suave só nas páginas do site (o painel fica com a rolagem normal). */
function RolagemSuave() {
  const { pathname } = useLocation();
  const noPainel = pathname.startsWith('/admin');
  useEffect(() => (noPainel ? undefined : ativarRolagemSuave()), [noPainel]);
  return null;
}

export default function App() {
  return (
    <AoVivoProvider>
      <RolagemSuave />
      <Routes>
        <Route path="/agendar" element={<PaginaAgendar />} />
        <Route path="/privacidade" element={<PaginaPrivacidade />} />
        <Route
          path="/admin/*"
          element={
            <Suspense fallback={<p className="carregando-pagina">Carregando o painel…</p>}>
              <AdminApp />
            </Suspense>
          }
        />
        <Route path="*" element={<SitePublico />} />
      </Routes>
    </AoVivoProvider>
  );
}
