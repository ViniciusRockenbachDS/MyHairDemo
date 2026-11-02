import { useEffect, useMemo } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { urlImagem } from '../api/http.js';
import Agendador from './Agendador.jsx';
import Icone from './Icone.jsx';
import { useDadosSite } from './useDadosSite.js';
import '../styles/site.css';

/** /agendar  (ou /agendar?servico=3 vindo de um card de serviço) */
export default function PaginaAgendar() {
  const { servicos, info, horarios, nome, promocoes } = useDadosSite();
  const [parametros] = useSearchParams();
  const servicoInicial = parametros.get('servico');
  const pedido = useMemo(() => (servicoInicial ? { servicoId: servicoInicial } : null), [servicoInicial]);
  const endereco = [info?.endereco, info?.cidade].filter(Boolean).join(', ');

  useEffect(() => {
    window.scrollTo(0, 0);
    document.title = `Agendar horário | ${nome}`;
  }, [nome]);

  return (
    <div className="ag-pagina">
      <header className="ag-cabeca">
        <div className="ag-cabeca-conteudo">
          <Link to="/" className="ag-marca" aria-label={`${nome}, voltar ao site`}>
            <span className="logo-selo" aria-hidden="true">
              {info?.logoUrl ? <img src={urlImagem(info.logoUrl)} alt="" /> : <span>{nome.trim().charAt(0)}</span>}
            </span>
            <span className="ag-marca-textos">
              <span className="ag-marca-rotulo">Agendamento online</span>
              <span className="ag-marca-nome">{nome.toLowerCase()}</span>
            </span>
          </Link>
          {endereco && (
            <p className="ag-endereco">
              <Icone nome="local" tamanho={18} /> {endereco}
            </p>
          )}
        </div>
      </header>
      <main className="ag-corpo">
        <Agendador
          servicos={servicos.dados}
          erroServicos={servicos.erro}
          horariosSalao={horarios}
          pedido={pedido}
          promocoes={promocoes}
          whatsappSalao={info?.whatsapp}
        />
        <Link to="/" className="ag-link-site">Voltar ao site</Link>
      </main>
    </div>
  );
}
