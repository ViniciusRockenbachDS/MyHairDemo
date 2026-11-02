import { useState } from 'react';
import { Link } from 'react-router-dom';
import { urlImagem } from '../api/http.js';
import { agruparHorarios } from '../util/funcionamento.js';
import { linkMapa } from '../util/formato.js';
import Icone from './Icone.jsx';

export const SECOES = [
  ['sobre', 'Sobre'],
  ['servicos', 'Serviços'],
  ['produtos', 'Produtos'],
  ['visita', 'Visita'],
  ['avaliacoes', 'Avaliações'],
  ['equipe', 'Equipe'],
  ['horarios', 'Horários'],
  ['duvidas', 'Dúvidas'],
];

/** Na home, "#secao"; nas outras páginas, "/#secao". */
const ancora = (naHome, id) => (naHome ? `#${id}` : `/#${id}`);

export function Logo({ info, nome, subtitulo }) {
  return (
    <span className="logo">
      <span className="logo-selo" aria-hidden="true">
        {info?.logoUrl ? <img src={urlImagem(info.logoUrl)} alt="" /> : <span>{nome.trim().charAt(0)}</span>}
      </span>
      <span className="logo-textos">
        <span className="logo-nome">{nome.toLowerCase()}</span>
        {subtitulo && <span className="logo-sub">{subtitulo}</span>}
      </span>
    </span>
  );
}

/** Esconde "Avaliações" e "Produtos" enquanto essas seções estiverem vazias. */
const secoesVisiveis = (temAvaliacoes, temProdutos) =>
  SECOES.filter(([id]) => (id !== 'avaliacoes' || temAvaliacoes) && (id !== 'produtos' || temProdutos));

export function Cabecalho({ info, nome, naHome = true, temAvaliacoes = false, temProdutos = false }) {
  const [aberto, setAberto] = useState(false);
  return (
    <header className="topo">
      <div className="topo-conteudo">
        {naHome ? (
          <a href="#inicio" className="topo-marca" aria-label={`${nome}, voltar ao início`}>
            <Logo info={info} nome={nome} />
          </a>
        ) : (
          <Link to="/" className="topo-marca" aria-label={`${nome}, início`}>
            <Logo info={info} nome={nome} />
          </Link>
        )}
        <nav aria-label="Seções do site" className={`topo-nav${aberto ? ' aberto' : ''}`} id="menu-site">
          {secoesVisiveis(temAvaliacoes, temProdutos).map(([id, rotulo]) => (
            <a key={id} href={ancora(naHome, id)} onClick={() => setAberto(false)}>
              {rotulo}
            </a>
          ))}
        </nav>
        <Link to="/agendar" className="botao botao-primario topo-cta">Agendar</Link>
        <button
          type="button"
          className="topo-menu"
          aria-expanded={aberto}
          aria-controls="menu-site"
          onClick={() => setAberto((v) => !v)}
        >
          <Icone nome={aberto ? 'fechar' : 'menu'} tamanho={22} />
          <span className="visualmente-oculto">Menu</span>
        </button>
      </div>
    </header>
  );
}

export function Rodape({ info, nome, horarios, naHome = true, temAvaliacoes = false, temProdutos = false }) {
  const mapa = linkMapa(info);
  const endereco = [info?.endereco, info?.cidade].filter(Boolean).join(', ');
  return (
    <footer className="rodape">
      <div className="rodape-conteudo">
        <div className="rodape-marca">
          <Logo info={info} nome={nome} subtitulo={`Salão de beleza${info?.cidade ? ` · ${info.cidade}` : ''}`} />
          <p>
            Cabelo, unhas e estética com hora marcada e a profissional da sua escolha.
            Agende online em um minuto.
          </p>
        </div>
        <div>
          <p className="rodape-titulo">Links</p>
          <ul className="rodape-links">
            {secoesVisiveis(temAvaliacoes, temProdutos).map(([id, rotulo]) => (
              <li key={id}><a href={ancora(naHome, id)}>{rotulo}</a></li>
            ))}
            <li><Link to="/agendar" className="rodape-agendar">Agendar horário</Link></li>
          </ul>
        </div>
        <div>
          <p className="rodape-titulo">Contato e horários</p>
          <ul className="rodape-contato">
            {endereco && (
              <li>
                <Icone nome="local" tamanho={18} />
                {mapa ? <a href={mapa} target="_blank" rel="noreferrer">{endereco}</a> : <span>{endereco}</span>}
              </li>
            )}
            {agruparHorarios(horarios).map((g) => (
              <li key={g.rotulo}>
                <Icone nome="relogio" tamanho={18} />
                <span>{g.rotulo}: <strong>{g.texto}</strong></span>
              </li>
            ))}
            {info?.instagram && (
              <li>
                <Icone nome="instagram" tamanho={18} />
                <a href={`https://instagram.com/${info.instagram}`} target="_blank" rel="noreferrer">@{info.instagram}</a>
              </li>
            )}
          </ul>
        </div>
      </div>
      <div className="rodape-base">
        <p>© {new Date().getFullYear()} {nome.toLowerCase()}. Todos os direitos reservados.</p>
        <Link to="/privacidade">Política de privacidade</Link>
      </div>
    </footer>
  );
}

/** Botão fixo de agendar no celular. */
export function BarraMobile() {
  return (
    <div className="barra-mobile">
      <Link to="/agendar" className="botao botao-primario botao-largo">
        Agendar horário <Icone nome="seta" />
      </Link>
    </div>
  );
}

/** Foto com fundo elegante enquanto o salão não cadastra a imagem. */
export function Foto({ src, alt, className = '', children }) {
  const [falhou, setFalhou] = useState(false);
  const temFoto = src && !falhou;
  return (
    <figure className={`foto${temFoto ? '' : ' foto-vazia'} ${className}`}>
      {temFoto && <img src={urlImagem(src)} alt={alt} loading="lazy" onError={() => setFalhou(true)} />}
      {children && <figcaption>{children}</figcaption>}
    </figure>
  );
}

/**
 * Mapa do Google carregado SÓ quando a pessoa pede (LGPD: o Google pode
 * gravar cookies; sem o clique, nada do Google é carregado).
 */
export function MapaSobDemanda({ src, titulo }) {
  const [carregar, setCarregar] = useState(false);
  if (carregar) {
    return <iframe className="mapa" title={titulo} src={src} loading="lazy" referrerPolicy="no-referrer-when-downgrade" />;
  }
  return (
    <div className="mapa mapa-capa">
      <Icone nome="local" tamanho={30} />
      <button type="button" className="botao botao-claro" onClick={() => setCarregar(true)}>
        Ver mapa
      </button>
      <span className="dica">O mapa é carregado pelo Google.</span>
    </div>
  );
}
