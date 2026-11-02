import { useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { urlImagem } from '../api/http.js';
import { statusFuncionamento } from '../util/funcionamento.js';
import { diaSemanaIso, duracao, hora, linkMapa, linkWhatsapp, moeda, NOMES_DIAS, tempoRelativo } from '../util/formato.js';
import Icone, { ICONE_CATEGORIA } from './Icone.jsx';
import { BarraMobile, Cabecalho, Foto, MapaSobDemanda, Rodape } from './Layout.jsx';
import { atrasoCascata, useRevelar } from './useRevelar.js';
import { rolarAte } from './rolagemSuave.js';
import { melhorPromocaoDoServico, rotuloDias, rotuloDiasCurto } from '../util/promocoes.js';
import { useAgora, useDadosSite } from './useDadosSite.js';
import '../styles/site.css';

const SLOGAN_PADRAO = 'Beleza com hora marcada, atendimento exclusivo.';
const SOBRE_PADRAO =
  'Cabelo, unhas e estética em um só lugar, com profissionais que conhecem o seu estilo.\n\n' +
  'Você agenda pelo celular em um minuto. Na hora marcada, a sua cadeira já está esperando.';

/** Título com a última palavra em itálico dourado: "…, atendimento *exclusivo.*" */
function TituloDestaque({ texto }) {
  const t = texto.trim();
  const i = t.lastIndexOf(' ');
  if (i < 0) return <em>{t}</em>;
  return (
    <>
      {t.slice(0, i)} <em>{t.slice(i + 1)}</em>
    </>
  );
}

function Status({ horarios, agora, className = '' }) {
  const s = statusFuncionamento(horarios, agora);
  if (!s) return null;
  return (
    <p className={`status ${s.aberto ? 'status-aberto' : ''} ${className}`}>
      <span className="status-ponto" aria-hidden="true" />
      {s.texto}
    </p>
  );
}

const DIFERENCIAIS = [
  ['calendario', 'Hora marcada', 'Você escolhe o dia e o horário; a cadeira fica reservada no seu nome, sem fila e sem espera.'],
  ['usuarios', 'Profissional à sua escolha', 'Cada profissional tem a própria agenda. Atendimento sempre com quem você prefere.'],
  ['relogio', 'Sem pressa', 'O tempo do serviço é seu. Nada de encaixe apertado ou atendimento corrido.'],
  ['sino', 'Lembrete no WhatsApp', 'Um dia antes, você recebe um lembrete para confirmar. Precisou desmarcar? É só responder.'],
];

/**
 * Página inicial. Tudo vem do /admin e se atualiza sozinho:
 * nome, frase, textos, fotos, endereço, contatos, serviços, preços,
 * durações, equipe e horários.
 */
export default function SitePublico() {
  const { servicos, equipe, avaliacoes, produtos, promocoes, lookbook, info, horarios, nome } = useDadosSite();
  const agora = useAgora();
  const local = useLocation();
  const mapa = linkMapa(info);
  const endereco = [info?.endereco, info?.cidade].filter(Boolean).join(', ');
  const qtdServicos = servicos.dados?.length ?? 0;
  const qtdEquipe = equipe.dados?.length ?? 0;
  const hojeIso = diaSemanaIso(agora);
  const paragrafosSobre = (info?.descricao || SOBRE_PADRAO).split(/\n\s*\n/).filter(Boolean);

  // aparecer ao rolar: procura os elementos de novo quando os dados chegam
  useRevelar([servicos.dados, produtos, promocoes, lookbook, equipe.dados, avaliacoes, info, horarios.length]);
  const mapaEmbed = endereco
    ? `https://www.google.com/maps?q=${encodeURIComponent(`${nome}, ${endereco}`)}&z=16&hl=pt-BR&output=embed`
    : null;

  useEffect(() => {
    document.title = `${nome} | Salão de beleza${info?.cidade ? ` em ${info.cidade}` : ''}`;
  }, [nome, info?.cidade]);

  // Vindo de outra página com "/#servicos": rola até a seção.
  useEffect(() => {
    if (!local.hash) return;
    // "auto" segue o CSS: desliza, ou pula direto para quem pediu menos movimento
    const t = setTimeout(
      () => rolarAte(document.getElementById(local.hash.slice(1))),
      150,
    );
    return () => clearTimeout(t);
  }, [local.hash]);

  return (
    <div className="site">
      <Cabecalho info={info} nome={nome} temAvaliacoes={!!avaliacoes} temProdutos={!!produtos} />

      <main>
        {/* ------------------------------------------------------------ HERO */}
        <section className={`hero escuro${info?.fotoHeroUrl ? ' hero-com-foto' : ''}`} id="inicio">
          {info?.fotoHeroUrl ? (
            <div className="hero-fundo" aria-hidden="true">
              <img src={urlImagem(info.fotoHeroUrl)} alt="" />
            </div>
          ) : (
            <span className="hero-marca-dagua" aria-hidden="true">{nome}</span>
          )}
          <div className="conteudo hero-grade">
            <div className="hero-texto revelar">
              <p className="sobretitulo">Salão de beleza{info?.cidade ? ` · ${info.cidade}` : ''}</p>
              <h1>
                <TituloDestaque texto={info?.slogan || SLOGAN_PADRAO} />
              </h1>
              <p className="hero-sub">
                Escolha o serviço, a profissional e o horário. Chegue na hora marcada, sem fila e sem ligação.
              </p>
              <div className="hero-acoes">
                <Link to="/agendar" className="botao botao-primario botao-grande">
                  Agendar horário <Icone nome="seta" />
                </Link>
                <a href="#servicos" className="botao botao-escuro botao-grande">Ver serviços</a>
              </div>
              <ul className="hero-vantagens">
                <li><span className="icone-caixa"><Icone nome="relogio" /></span>Agende em um minuto</li>
                <li><span className="icone-caixa"><Icone nome="usuario" /></span>Escolha a profissional</li>
                <li><span className="icone-caixa"><Icone nome="calendario" /></span>Horário garantido</li>
              </ul>
            </div>

            <aside className="hero-cartao revelar" style={atrasoCascata(2)} aria-label="Informações do salão">
              <Status horarios={horarios} agora={agora} className="status-claro" />
              {endereco && (
                <p className="hero-cartao-linha"><Icone nome="local" />{endereco}</p>
              )}
              {(qtdEquipe > 0 || qtdServicos > 0) && (
                <p className="hero-cartao-linha">
                  <Icone nome="tesoura" />
                  {qtdEquipe} {qtdEquipe === 1 ? 'profissional' : 'profissionais'} · {qtdServicos}{' '}
                  {qtdServicos === 1 ? 'serviço' : 'serviços'}
                </p>
              )}
              {mapa && (
                <a href={mapa} target="_blank" rel="noreferrer" className="botao botao-escuro botao-pequeno-site">
                  <Icone nome="local" /> Como chegar
                </a>
              )}
            </aside>
          </div>
        </section>

        {/* ------------------------------------------------------------ SOBRE */}
        <section className="secao claro" id="sobre">
          <div className="conteudo duas-colunas">
            <div className="revelar">
              <p className="sobretitulo">O espaço</p>
              <h2>Um salão feito para você, com hora marcada.</h2>
              {paragrafosSobre.map((p, i) => (
                <p key={i} className="texto-secao">{p}</p>
              ))}
              <Link to="/agendar" className="botao botao-suave">
                Agendar horário <Icone nome="seta" />
              </Link>
            </div>
            <Foto key={info?.fotoEspacoUrl} src={info?.fotoEspacoUrl} alt={`Espaço do ${nome}`} className="foto-sobre revelar">
              <em>Sinta-se em casa.</em>
            </Foto>
          </div>
        </section>

        {/* ------------------------------------------------------------ DIFERENCIAIS */}
        <section className="secao escuro">
          <div className="conteudo">
            <div className="secao-cabeca revelar">
              <p className="sobretitulo">Diferenciais</p>
              <h2>Por que marcar aqui</h2>
              <p>O que faz a diferença entre ir ao salão e ser bem atendida.</p>
            </div>
            <ul className="grade-4">
              {DIFERENCIAIS.map(([icone, titulo, texto], i) => (
                <li key={titulo} className="cartao-escuro revelar" style={atrasoCascata(i)}>
                  <span className="icone-ouro"><Icone nome={icone} tamanho={22} /></span>
                  <h3>{titulo}</h3>
                  <p>{texto}</p>
                </li>
              ))}
            </ul>
          </div>
        </section>

        {/* ------------------------------------------------------------ PROMOÇÕES */}
        {promocoes && (
          <section className="secao claro secao-promocoes" id="promocoes">
            <div className="conteudo">
              <div className="secao-cabeca revelar">
                <p className="sobretitulo">Promoções</p>
                <h2>Dias de desconto</h2>
                <p>O desconto já entra no valor quando você agenda para o dia da promoção.</p>
              </div>
              <ul className="grade-promocoes">
                {promocoes.map((p, i) => (
                  <li key={p.id} className="cartao-promocao revelar" style={atrasoCascata(i)}>
                    <span className="promo-desconto">-{p.descontoPercentual}%</span>
                    <h3>{p.titulo}</h3>
                    {p.descricao && <p className="promo-descricao">{p.descricao}</p>}
                    <ul className="promo-detalhes">
                      <li>
                        <Icone nome="calendario" tamanho={16} /> {rotuloDias(p.diasSemana)}
                        {p.dataFim && `, até ${p.dataFim.split('-').reverse().slice(0, 2).join('/')}`}
                      </li>
                      <li>
                        <Icone nome="tesoura" tamanho={16} />{' '}
                        {p.todosServicos ? 'Todos os serviços' : p.servicoNomes.join(', ')}
                      </li>
                    </ul>
                    <Link
                      to={!p.todosServicos && p.servicoIds.length === 1 ? `/agendar?servico=${p.servicoIds[0]}` : '/agendar'}
                      className="botao botao-primario"
                    >
                      Agendar com desconto <Icone nome="seta" />
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          </section>
        )}

        {/* ------------------------------------------------------------ SERVIÇOS */}
        <section className="secao claro" id="servicos">
          <div className="conteudo">
            <div className="secao-cabeca revelar">
              <p className="sobretitulo">Serviços</p>
              <h2>O que fazemos</h2>
              <p>Valor e duração de cada serviço. Toque em um deles para ver os horários livres de cada profissional.</p>
            </div>
            {!servicos.dados ? (
              <p className="estado">{servicos.erro ? 'Não foi possível carregar os serviços.' : 'Carregando serviços…'}</p>
            ) : (
              <ul className="grade-servicos">
                {servicos.dados.map((s, i) => {
                  const promo = melhorPromocaoDoServico(promocoes, s.id);
                  return (
                    <li key={s.id} className="revelar" style={atrasoCascata(i)}>
                      <Link to={`/agendar?servico=${s.id}`} className="cartao-servico">
                        <span className={`cartao-servico-foto${s.fotoUrl ? '' : ' foto-exemplo'}`}>
                          {s.fotoUrl ? (
                            <img src={urlImagem(s.fotoUrl)} alt="" loading="lazy" decoding="async" />
                          ) : (
                            <Icone nome={ICONE_CATEGORIA[s.categoria] || 'brilho'} tamanho={44} />
                          )}
                          {promo && (
                            <span className="selo-promo">
                              -{promo.descontoPercentual}% {rotuloDiasCurto(promo.diasSemana)}
                            </span>
                          )}
                        </span>
                        <span className="cartao-servico-texto">
                          <span className="cartao-servico-rotulo">{ROTULO_CATEGORIA[s.categoria] || 'Serviço'}</span>
                          <span className="cartao-servico-nome">{s.nome}</span>
                          <span className="cartao-servico-linha" aria-hidden="true" />
                          {s.descricao && <span className="cartao-servico-descricao">{s.descricao}</span>}
                          <span className="cartao-servico-info">
                            {duracao(s.duracaoMinutos)} · {moeda(s.preco)}
                          </span>
                          <span className="cartao-servico-agendar">
                            Agendar <Icone nome="seta" />
                          </span>
                        </span>
                      </Link>
                    </li>
                  );
                })}
              </ul>
            )}
            <Link to="/agendar" className="botao botao-primario botao-grande secao-cta">
              Ver horários e valores <Icone nome="seta" />
            </Link>
          </div>
        </section>

        {/* ------------------------------------------------------------ PRODUTOS */}
        {produtos && (
          <section className="secao escuro" id="produtos">
            <div className="conteudo">
              <div className="secao-cabeca revelar">
                <p className="sobretitulo">Produtos</p>
                <h2>Para cuidar em casa</h2>
                <p>Os produtos que usamos no salão, disponíveis para você levar. Fale com a gente para reservar.</p>
              </div>
              {produtos.length >= 4 ? (
                // esteira: a lista aparece 2x para dar a volta sem emenda; para com o mouse em cima
                <div className="esteira revelar" style={{ '--duracao': `${produtos.length * 6}s` }}>
                  <ul className="esteira-trilho">
                    {[...produtos, ...produtos].map((p, i) => (
                      <CartaoProduto key={`${p.id}-${i}`} p={p} info={info} copia={i >= produtos.length} />
                    ))}
                  </ul>
                </div>
              ) : (
                <ul className="grade-produtos">
                  {produtos.map((p, i) => (
                    <CartaoProduto key={p.id} p={p} info={info} className="revelar" style={atrasoCascata(i)} />
                  ))}
                </ul>
              )}
            </div>
          </section>
        )}

        {/* ------------------------------------------------------------ VISITA */}
        <section className="secao claro secao-visita" id="visita">
          <div className="conteudo duas-colunas">
            <div className="revelar">
              <p className="sobretitulo">Visita</p>
              <h2>Venha conhecer o salão</h2>
              <p className="texto-secao">
                Passe para conhecer o espaço, conversar com a equipe e tirar suas dúvidas sobre os serviços.
                Sem compromisso.
              </p>
              <p className="frase-ouro">Será um prazer te receber.</p>
              {mapa && (
                <a href={mapa} target="_blank" rel="noreferrer" className="botao botao-claro">
                  <Icone nome="local" /> Como chegar
                </a>
              )}
              <Status horarios={horarios} agora={agora} />
            </div>
            <div className="visita-foto revelar" style={atrasoCascata(2)}>
              <span className="icone-ouro icone-flutuante"><Icone nome="brilho" tamanho={26} /></span>
              <Foto key={info?.fotoVisitaUrl} src={info?.fotoVisitaUrl} alt={`Ambiente do ${nome}`} className="foto-visita">
                <strong>Seu momento, do seu jeito.</strong>
                <span>Atendimento atencioso, produtos profissionais e tempo para cuidar de você.</span>
              </Foto>
            </div>
          </div>
        </section>

        {/* ------------------------------------------------------------ LOOKBOOK */}
        {lookbook && (
          <section className="secao secao-lookbook" id="lookbook">
            <div className="conteudo">
              <div className="secao-cabeca revelar">
                <p className="sobretitulo">Lookbook</p>
                <h2>Trabalhos feitos aqui</h2>
                <p>Uma amostra do que a nossa equipe faz no dia a dia.</p>
              </div>
            </div>
            {lookbook.length >= 4 ? (
              // mesma esteira dos produtos, mas não para com o mouse em cima
              <div className="esteira esteira-lookbook esteira-continua revelar" style={{ '--duracao': `${lookbook.length * 5}s` }}>
                <ul className="esteira-trilho">
                  {[...lookbook, ...lookbook].map((f, i) => (
                    <FotoTrabalho key={`${f.id}-${i}`} foto={f} copia={i >= lookbook.length} />
                  ))}
                </ul>
              </div>
            ) : (
              <div className="conteudo">
                <ul className="grade-lookbook">
                  {lookbook.map((f, i) => (
                    <FotoTrabalho key={f.id} foto={f} className="revelar" style={atrasoCascata(i)} />
                  ))}
                </ul>
              </div>
            )}
          </section>
        )}

        {/* ------------------------------------------------------------ AVALIAÇÕES (cadastradas no /admin) */}
        {avaliacoes && (
          <section className="secao escuro" id="avaliacoes">
            <div className="conteudo">
              <div className="avaliacoes-cabeca">
                <div className="secao-cabeca revelar">
                  <p className="sobretitulo">Avaliações</p>
                  <h2>O que as clientes estão dizendo</h2>
                  <p>A opinião de quem já passou pelo {nome}.</p>
                </div>
                {avaliacoes.nota != null && (
                  <SeloNota resumo={avaliacoes} />
                )}
              </div>
              <ul className="grade-avaliacoes">
                {avaliacoes.avaliacoes.map((a, i) => (
                  <li key={a.id} className="cartao-avaliacao revelar" style={atrasoCascata(i)}>
                    <Estrelas nota={a.nota} />
                    <blockquote>“{a.texto}”</blockquote>
                    <div className="avaliacao-autor">
                      <span className="avaliacao-foto">
                        {a.fotoUrl ? (
                          <img src={urlImagem(a.fotoUrl)} alt="" loading="lazy" />
                        ) : (
                          <span aria-hidden="true">{a.autor.trim().charAt(0)}</span>
                        )}
                      </span>
                      <span className="avaliacao-nome">
                        <strong>{a.autor}</strong>
                        {a.data && <span>{tempoRelativo(a.data)}</span>}
                      </span>
                      {a.link && (
                        <a className="avaliacao-link" href={a.link} target="_blank" rel="noreferrer" aria-label={`Ver a avaliação de ${a.autor}`}>
                          <Icone nome="setaDiagonal" />
                        </a>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
              {avaliacoes.link && (
                <a href={avaliacoes.link} target="_blank" rel="noreferrer" className="botao botao-escuro avaliacoes-rodape">
                  Ver todas as avaliações <Icone nome="externo" />
                </a>
              )}
            </div>
          </section>
        )}

        {/* ------------------------------------------------------------ EQUIPE */}
        <section className="secao claro" id="equipe">
          <div className="conteudo">
            <div className="secao-cabeca revelar">
              <p className="sobretitulo">Equipe</p>
              <h2>Quem vai te atender</h2>
              <p>Cada profissional tem a própria agenda. Você escolhe com quem quer ser atendida e vê só os horários dela.</p>
            </div>
            {equipe.dados?.length > 0 && (
              <ul className="grade-equipe">
                {equipe.dados.map((p, i) => (
                  <li key={p.id} className="cartao-equipe revelar" style={atrasoCascata(i)}>
                    <span className="equipe-foto">
                      {p.fotoUrl ? <img src={urlImagem(p.fotoUrl)} alt="" loading="lazy" /> : <span aria-hidden="true">{p.nome.trim().charAt(0)}</span>}
                    </span>
                    <h3>{p.nome}</h3>
                    {p.especialidade && <p className="equipe-funcao">{p.especialidade}</p>}
                    <span className="equipe-linha" aria-hidden="true" />
                    {p.bio ? (
                      <blockquote>“{p.bio}”</blockquote>
                    ) : (
                      p.servicos.length > 0 && <p className="equipe-servicos">{p.servicos.join(', ')}</p>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </section>

        {/* ------------------------------------------------------------ HORÁRIOS E ENDEREÇO */}
        <section className="secao claro" id="horarios">
          <div className="conteudo">
            <div className="secao-cabeca revelar">
              <p className="sobretitulo">Quando e onde</p>
              <h2>Horários e endereço</h2>
            </div>
            <div className="grade-local">
              <div className="cartao-claro revelar">
                <div className="cartao-claro-topo">
                  <h3><Icone nome="relogio" /> Funcionamento</h3>
                  <Status horarios={horarios} agora={agora} className="status-pequeno" />
                </div>
                <table className="tabela-horarios">
                  <tbody>
                    {horarios.map((h) => (
                      <tr key={h.diaSemana} className={h.diaSemana === hojeIso ? 'hoje' : undefined}>
                        <th scope="row">
                          {NOMES_DIAS[h.diaSemana]}
                          {h.diaSemana === hojeIso && <span className="marca-hoje">hoje</span>}
                        </th>
                        <td>{h.fechado ? 'Fechado' : `${hora(h.abertura)} – ${hora(h.fechamento)}`}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="cartao-claro revelar" style={atrasoCascata(1)}>
                <div className="cartao-claro-topo">
                  <h3><Icone nome="local" /> Onde estamos</h3>
                </div>
                <p className="endereco">{endereco || 'Endereço em breve.'}</p>
                {mapaEmbed && <MapaSobDemanda src={mapaEmbed} titulo={`Mapa: ${nome}`} />}
                {mapa && (
                  <a href={mapa} target="_blank" rel="noreferrer" className="botao botao-claro botao-largo">
                    <Icone nome="local" /> Clique aqui para abrir no Google Maps
                  </a>
                )}
              </div>
            </div>
          </div>
        </section>

        {/* ------------------------------------------------------------ DÚVIDAS */}
        <section className="secao claro" id="duvidas">
          <div className="conteudo duvidas-grade">
            <div className="revelar">
              <p className="sobretitulo">Dúvidas frequentes</p>
              <h2>Antes de marcar</h2>
              <p className="texto-secao">As perguntas que mais chegam, respondidas de uma vez.</p>
            </div>
            <div className="duvidas revelar" style={atrasoCascata(1)}>
              <Pergunta titulo="Preciso agendar ou atendem por ordem de chegada?">
                Atendemos com hora marcada. Você agenda online em um minuto, sem cadastro e sem senha: escolhe o serviço,
                a profissional e o horário, e a cadeira fica reservada no seu nome.
              </Pergunta>
              <Pergunta titulo="Posso escolher a profissional?">
                Sim. Cada profissional tem a própria agenda; ao escolher uma delas você vê só os horários em que ela atende.
                Se não tiver preferência, mostramos os horários de todas.
              </Pergunta>
              <Pergunta titulo="Como faço para remarcar ou cancelar?">
                Responda <strong>cancelar</strong> na nossa conversa do WhatsApp. O horário é liberado na hora e você já
                pode escolher um novo.
              </Pergunta>
              <Pergunta titulo="Vou receber lembrete?">
                Sim. Um dia antes e algumas horas antes, você recebe uma mensagem no WhatsApp para confirmar a presença.
              </Pergunta>
              <Pergunta titulo="Quais são os horários de funcionamento?">
                {horarios.length
                  ? horarios.map((h) => `${NOMES_DIAS[h.diaSemana]}: ${h.fechado ? 'fechado' : `${hora(h.abertura)}–${hora(h.fechamento)}`}`).join('. ') + '.'
                  : 'Veja a seção Horários acima.'}
              </Pergunta>
              <Pergunta titulo="Onde fica o salão?">
                {endereco ? `${endereco}. Toque em “Como chegar” para abrir o mapa.` : 'Fale com a gente pelo WhatsApp.'}
              </Pergunta>
            </div>
          </div>
        </section>

        {/* ------------------------------------------------------------ CHAMADA FINAL */}
        <section className="chamada-final escuro">
          <div className="conteudo revelar">
            <p className="sobretitulo">Sem cadastro, sem senha</p>
            <h2>Pronto para o seu próximo horário?</h2>
            <p>Seu horário fica reservado na hora. Só nome e WhatsApp.</p>
            <Link to="/agendar" className="botao botao-primario botao-grande">
              Agendar horário <Icone nome="seta" />
            </Link>
          </div>
        </section>
      </main>

      <Rodape info={info} nome={nome} horarios={horarios} temAvaliacoes={!!avaliacoes} temProdutos={!!produtos} />
      <BarraMobile />
    </div>
  );
}

/** "4,9 ★★★★★ 78 avaliações no Google" (o "Google" só aparece se o link for do Google). */
function SeloNota({ resumo }) {
  const nota = Number(resumo.nota);
  const doGoogle = /google|goo\.gl|g\.page/i.test(resumo.link || '');
  const conteudo = (
    <>
      {doGoogle && <span className="nota-google-g">Google</span>}
      <span className="nota-google-numero">{nota.toFixed(1).replace('.', ',')}</span>
      <span className="nota-google-detalhe">
        <Estrelas nota={Math.round(nota)} />
        {resumo.total != null && `${resumo.total} avaliações${doGoogle ? ' no Google' : ''}`}
      </span>
    </>
  );
  return resumo.link ? (
    <a className="nota-google" href={resumo.link} target="_blank" rel="noreferrer">{conteudo}</a>
  ) : (
    <div className="nota-google">{conteudo}</div>
  );
}

function Estrelas({ nota }) {
  return (
    <span className="estrelas" role="img" aria-label={`${nota} de 5 estrelas`}>
      {[1, 2, 3, 4, 5].map((n) => (
        <Icone key={n} nome="estrela" tamanho={18} className={n <= nota ? undefined : 'apagada'} />
      ))}
    </span>
  );
}

function Pergunta({ titulo, children }) {
  return (
    <details>
      <summary>
        {titulo}
        <Icone nome="chevron" className="duvida-seta" />
      </summary>
      <p>{children}</p>
    </details>
  );
}

/** Cartão de produto (na esteira, as cópias ficam ocultas para leitores de tela e para o Tab). */
function CartaoProduto({ p, info, copia = false, className = '', style }) {
  return (
    <li
      className={`cartao-produto ${className}${p.disponivel ? '' : ' esgotado'}`}
      style={style}
      aria-hidden={copia || undefined}
    >
      <span className={`cartao-produto-foto${p.fotoUrl ? '' : ' foto-exemplo'}`}>
        {p.fotoUrl ? <img src={urlImagem(p.fotoUrl)} alt={copia ? '' : p.nome} loading="lazy" /> : <Icone nome="brilho" tamanho={44} />}
        {!p.disponivel && <span className="selo-esgotado">Esgotado</span>}
      </span>
      <div className="cartao-produto-corpo">
        {p.marca && <p className="cartao-produto-marca">{p.marca}</p>}
        <h3>{p.nome}</h3>
        {p.descricao && <p className="cartao-produto-descricao">{p.descricao}</p>}
        <p className="cartao-produto-preco">{p.preco != null ? moeda(p.preco) : 'Consulte no salão'}</p>
        {p.disponivel && info?.whatsapp ? (
          <a
            className="botao botao-escuro botao-largo"
            href={linkWhatsapp(info.whatsapp, `Olá! Vi no site e tenho interesse no produto: ${p.nome}.`)}
            target="_blank"
            rel="noreferrer"
            tabIndex={copia ? -1 : undefined}
          >
            <Icone nome="mensagem" /> Tenho interesse
          </a>
        ) : (
          <p className="cartao-produto-aviso">{p.disponivel ? 'Disponível no salão' : 'Volta em breve'}</p>
        )}
      </div>
    </li>
  );
}

/** Foto do Lookbook com legenda por cima. */
function FotoTrabalho({ foto, copia = false, className = '', style }) {
  return (
    <li className={`foto-trabalho ${className}`} style={style} aria-hidden={copia || undefined}>
      <img src={urlImagem(foto.fotoUrl)} alt={copia ? '' : foto.legenda || 'Trabalho feito no salão'} loading="lazy" decoding="async" />
      {foto.legenda && <span className="foto-trabalho-legenda">{foto.legenda}</span>}
    </li>
  );
}

const ROTULO_CATEGORIA = { CABELO: 'Cabelo', UNHAS: 'Unhas', ESTETICA: 'Estética' };
