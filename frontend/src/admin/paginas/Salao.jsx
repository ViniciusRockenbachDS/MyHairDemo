import { useEffect, useState } from 'react';
import { adminApi } from '../../api/admin.js';
import { useCarregar } from '../../hooks/useCarregar.js';
import { hora, NOMES_DIAS, telefoneVisivel } from '../../util/formato.js';
import { Campo, Carga, EnviarImagem, useAvisos } from '../componentes.jsx';

const CAMPOS_VAZIOS = {
  nome: '', slogan: '', descricao: '', endereco: '', cidade: '', telefone: '', whatsapp: '', instagram: '', linkMapa: '',
  logoUrl: '', fotoEspacoUrl: '', fotoVisitaUrl: '', fotoHeroUrl: '',
  razaoSocial: '', cnpj: '', emailPrivacidade: '', politicaPrivacidade: '', retencaoMeses: '24',
  chavePix: '', pixNomeRecebedor: '', pixCidade: '', sinalPrazoMinutos: '30',
  infoAtendimento: '',
};

/** Informações do salão e horário de funcionamento: tudo aparece no site na hora. */
export default function Salao() {
  return (
    <div className="pagina">
      <header className="pagina-cabeca">
        <div>
          <h1>Salão e horários</h1>
          <p className="pagina-sub">Tudo o que você salvar aqui aparece no site na mesma hora, sem precisar publicar nada.</p>
        </div>
      </header>
      <Informacoes />
      <Funcionamento />
    </div>
  );
}

function Informacoes() {
  const avisos = useAvisos();
  const dados = useCarregar(() => adminApi.salaoInformacoes(), []);
  const [form, setForm] = useState(CAMPOS_VAZIOS);
  const [erros, setErros] = useState({});
  const [salvando, setSalvando] = useState(false);

  useEffect(() => {
    if (!dados.dados) return;
    const d = dados.dados;
    setForm({
      ...CAMPOS_VAZIOS,
      ...Object.fromEntries(Object.entries(d).map(([k, v]) => [k, v ?? ''])),
      telefone: d.telefone ? telefoneVisivel(d.telefone) : '',
      whatsapp: d.whatsapp ? telefoneVisivel(d.whatsapp) : '',
      retencaoMeses: String(d.retencaoMeses ?? 24),
      sinalPrazoMinutos: String(d.sinalPrazoMinutos ?? 30),
    });
  }, [dados.dados]);

  const mudar = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }));

  async function salvar(e) {
    e.preventDefault();
    if (!form.nome.trim()) {
      setErros({ nome: 'Informe o nome do salão.' });
      return;
    }
    setErros({});
    setSalvando(true);
    try {
      await adminApi.salvarSalaoInformacoes({
        ...form,
        retencaoMeses: Number(form.retencaoMeses) || 24,
        sinalPrazoMinutos: Number(form.sinalPrazoMinutos) || 30,
      });
      avisos.sucesso('Informações salvas. O site já foi atualizado.');
      dados.recarregar(true);
    } catch (err) {
      if (err.campos) setErros(err.campos);
      avisos.erro(err.message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <section className="painel">
      <h2>Informações do site</h2>
      <Carga estado={dados}>
        <form onSubmit={salvar} noValidate className="grade-2">
          <Campo id="sl-nome" rotulo="Nome do salão" erro={erros.nome}>
            <input id="sl-nome" value={form.nome} onChange={mudar('nome')} maxLength={120} />
          </Campo>
          <Campo id="sl-slogan" rotulo="Frase principal do site" dica="Título grande do topo. A última palavra aparece em dourado itálico." erro={erros.slogan}>
            <input id="sl-slogan" value={form.slogan} onChange={mudar('slogan')} maxLength={200} />
          </Campo>
          <div className="grade-inteira">
            <Campo id="sl-desc" rotulo="Sobre o salão" dica="Texto da seção “O espaço”. Deixe uma linha em branco entre os parágrafos.">
              <textarea id="sl-desc" value={form.descricao} onChange={mudar('descricao')} maxLength={5000} />
            </Campo>
          </div>
          <Campo id="sl-end" rotulo="Endereço" dica="Rua, número e bairro.">
            <input id="sl-end" value={form.endereco} onChange={mudar('endereco')} maxLength={250} />
          </Campo>
          <Campo id="sl-cidade" rotulo="Cidade">
            <input id="sl-cidade" value={form.cidade} onChange={mudar('cidade')} maxLength={120} />
          </Campo>
          <Campo id="sl-wpp" rotulo="WhatsApp do salão">
            <input id="sl-wpp" value={form.whatsapp} onChange={mudar('whatsapp')} inputMode="tel" maxLength={20} />
          </Campo>
          <Campo id="sl-tel" rotulo="Telefone fixo (opcional)">
            <input id="sl-tel" value={form.telefone} onChange={mudar('telefone')} inputMode="tel" maxLength={20} />
          </Campo>
          <Campo id="sl-insta" rotulo="Instagram" dica="Só o usuário, sem @." erro={erros.instagram}>
            <input id="sl-insta" value={form.instagram} onChange={mudar('instagram')} maxLength={60} />
          </Campo>
          <Campo id="sl-mapa" rotulo="Link do Google Maps (opcional)" dica="Sem link, o site monta um a partir do endereço." erro={erros.linkMapa}>
            <input id="sl-mapa" value={form.linkMapa} onChange={mudar('linkMapa')} maxLength={500} placeholder="https://" />
          </Campo>
          <div className="grade-inteira">
            <h3 className="subtitulo-form">Imagens do site</h3>
            <p className="dica">Escolha as fotos do computador ou do celular. Sem foto, o site mostra um fundo escuro no lugar.</p>
          </div>
          <div className="grade-inteira">
            <EnviarImagem
              id="sl-hero"
              rotulo="Foto de fundo da tela principal"
              valor={form.fotoHeroUrl}
              aoMudar={(url) => setForm((f) => ({ ...f, fotoHeroUrl: url }))}
              erro={erros.fotoHeroUrl}
              dica="Foto horizontal e grande, de preferência do salão. Ela fica escurecida para o texto continuar legível. Sem foto, aparece o nome do salão ao fundo."
            />
          </div>
          <EnviarImagem
            id="sl-logo"
            rotulo="Logo"
            formato="quadrado"
            valor={form.logoUrl}
            aoMudar={(url) => setForm((f) => ({ ...f, logoUrl: url }))}
            erro={erros.logoUrl}
            dica="Imagem quadrada. Aparece no topo e no rodapé."
          />
          <EnviarImagem
            id="sl-foto1"
            rotulo="Foto do espaço"
            valor={form.fotoEspacoUrl}
            aoMudar={(url) => setForm((f) => ({ ...f, fotoEspacoUrl: url }))}
            erro={erros.fotoEspacoUrl}
            dica="Foto deitada (horizontal). Seção “O espaço”."
          />
          <EnviarImagem
            id="sl-foto2"
            rotulo="Foto da visita"
            formato="vertical"
            valor={form.fotoVisitaUrl}
            aoMudar={(url) => setForm((f) => ({ ...f, fotoVisitaUrl: url }))}
            erro={erros.fotoVisitaUrl}
            dica="Foto em pé (vertical). Seção “Visita”."
          />
          <div className="grade-inteira">
            <h3 className="subtitulo-form">Sinal por Pix</h3>
            <SituacaoPagamentos />
          </div>
          <Campo id="sl-pix" rotulo="Chave Pix do salão" erro={erros.chavePix} dica="CPF/CNPJ, e-mail, telefone (+55...) ou chave aleatória. Usada quando a confirmação é manual.">
            <input id="sl-pix" value={form.chavePix} onChange={mudar('chavePix')} maxLength={140} />
          </Campo>
          <Campo id="sl-pix-prazo" rotulo="Prazo para pagar o sinal (minutos)" erro={erros.sinalPrazoMinutos} dica="Depois disso, sem pagamento, o horário é liberado.">
            <input id="sl-pix-prazo" type="number" min="10" max="1440" value={form.sinalPrazoMinutos} onChange={mudar('sinalPrazoMinutos')} />
          </Campo>
          <Campo id="sl-pix-nome" rotulo="Nome de quem recebe" erro={erros.pixNomeRecebedor} dica="Até 25 letras, como no banco. Vazio = nome do salão.">
            <input id="sl-pix-nome" value={form.pixNomeRecebedor} onChange={mudar('pixNomeRecebedor')} maxLength={25} />
          </Campo>
          <Campo id="sl-pix-cidade" rotulo="Cidade de quem recebe" erro={erros.pixCidade} dica="Até 15 letras. Vazio = cidade do salão.">
            <input id="sl-pix-cidade" value={form.pixCidade} onChange={mudar('pixCidade')} maxLength={15} />
          </Campo>
          <div className="grade-inteira">
            <h3 className="subtitulo-form">Privacidade (LGPD)</h3>
            <p className="dica">
              Esses dados aparecem na página <a href="/privacidade" target="_blank" rel="noreferrer">Política de Privacidade</a>.
              Peça para um advogado ou contador revisar o texto.
            </p>
          </div>
          <Campo id="sl-razao" rotulo="Razão social" erro={erros.razaoSocial} dica="Vazio = usa o nome do salão.">
            <input id="sl-razao" value={form.razaoSocial} onChange={mudar('razaoSocial')} maxLength={200} />
          </Campo>
          <Campo id="sl-cnpj" rotulo="CNPJ" erro={erros.cnpj}>
            <input id="sl-cnpj" value={form.cnpj} onChange={mudar('cnpj')} maxLength={20} placeholder="00.000.000/0000-00" />
          </Campo>
          <Campo id="sl-email" rotulo="E-mail para assuntos de privacidade" erro={erros.emailPrivacidade}>
            <input id="sl-email" type="email" value={form.emailPrivacidade} onChange={mudar('emailPrivacidade')} maxLength={160} />
          </Campo>
          <Campo id="sl-retencao" rotulo="Guardar dados de clientes por (meses)" erro={erros.retencaoMeses} dica="Depois desse prazo sem atendimento, a cliente e o histórico dela são excluídos automaticamente.">
            <input id="sl-retencao" type="number" min="6" max="120" value={form.retencaoMeses} onChange={mudar('retencaoMeses')} />
          </Campo>
          <div className="grade-inteira">
            <Campo
              id="sl-politica"
              rotulo="Texto próprio da política (opcional)"
              erro={erros.politicaPrivacidade}
              dica="Deixe em branco para usar o modelo pronto. Se escrever o seu: linha em branco separa parágrafos, e uma linha começando com ## vira título."
            >
              <textarea id="sl-politica" value={form.politicaPrivacidade} onChange={mudar('politicaPrivacidade')} rows={8} maxLength={30000} />
            </Campo>
          </div>
          <div className="grade-inteira acoes-form">
            <button type="submit" className="botao botao-primario" disabled={salvando}>
              {salvando ? 'Salvando…' : 'Salvar informações'}
            </button>
          </div>
        </form>
      </Carga>
    </section>
  );
}

function Funcionamento() {
  const avisos = useAvisos();
  const dados = useCarregar(() => adminApi.salaoHorarios(), []);
  const [dias, setDias] = useState(null);
  const [salvando, setSalvando] = useState(false);

  useEffect(() => {
    if (!dados.dados) return;
    setDias(
      [1, 2, 3, 4, 5, 6, 7].map((d) => {
        const h = dados.dados.find((x) => x.diaSemana === d);
        return {
          diaSemana: d,
          aberto: h ? !h.fechado : false,
          abertura: h?.abertura ? hora(h.abertura) : '08:00',
          fechamento: h?.fechamento ? hora(h.fechamento) : '18:00',
        };
      }),
    );
  }, [dados.dados]);

  const mudar = (d, campo, valor) => setDias((l) => l.map((x) => (x.diaSemana === d ? { ...x, [campo]: valor } : x)));

  async function salvar(e) {
    e.preventDefault();
    const invalido = dias.find((d) => d.aberto && d.fechamento <= d.abertura);
    if (invalido) {
      avisos.erro(`${NOMES_DIAS[invalido.diaSemana]}: o fechamento deve ser depois da abertura.`);
      return;
    }
    setSalvando(true);
    try {
      await adminApi.salvarSalaoHorarios(
        dias.map((d) => ({
          diaSemana: d.diaSemana,
          fechado: !d.aberto,
          abertura: d.aberto ? d.abertura : null,
          fechamento: d.aberto ? d.fechamento : null,
        })),
      );
      avisos.sucesso('Horário de funcionamento salvo. O site e o WhatsApp já usam o novo horário.');
      dados.recarregar(true);
    } catch (err) {
      avisos.erro(err.message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <section className="painel">
      <h2>Horário de funcionamento</h2>
      <p className="dica">
        Nenhum horário fora deste período é oferecido no site ou no WhatsApp. O expediente de cada profissional
        precisa caber dentro dele.
      </p>
      <Carga estado={dados}>
        {dias && (
          <form onSubmit={salvar}>
            <table className="tabela tabela-funcionamento">
              <thead>
                <tr>
                  <th>Dia</th>
                  <th>Abre?</th>
                  <th>Abertura</th>
                  <th>Fechamento</th>
                </tr>
              </thead>
              <tbody>
                {dias.map((d) => (
                  <tr key={d.diaSemana}>
                    <th scope="row">{NOMES_DIAS[d.diaSemana]}</th>
                    <td>
                      <input
                        type="checkbox"
                        checked={d.aberto}
                        onChange={(e) => mudar(d.diaSemana, 'aberto', e.target.checked)}
                        aria-label={`${NOMES_DIAS[d.diaSemana]} aberto`}
                      />
                    </td>
                    <td>
                      <input
                        type="time"
                        value={d.abertura}
                        disabled={!d.aberto}
                        onChange={(e) => mudar(d.diaSemana, 'abertura', e.target.value)}
                        aria-label={`${NOMES_DIAS[d.diaSemana]}, abertura`}
                      />
                    </td>
                    <td>
                      <input
                        type="time"
                        value={d.fechamento}
                        disabled={!d.aberto}
                        onChange={(e) => mudar(d.diaSemana, 'fechamento', e.target.value)}
                        aria-label={`${NOMES_DIAS[d.diaSemana]}, fechamento`}
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="acoes-form">
              <button type="submit" className="botao botao-primario" disabled={salvando}>
                {salvando ? 'Salvando…' : 'Salvar horário de funcionamento'}
              </button>
            </div>
          </form>
        )}
      </Carga>
    </section>
  );
}

/** Mostra se a confirmação do sinal é automática (Mercado Pago) ou manual. */
function SituacaoPagamentos() {
  const situacao = useCarregar(() => adminApi.situacaoPagamentos(), []);
  if (!situacao.dados) return null;
  return situacao.dados.automatico ? (
    <p className="nota nota-info">
      ✅ <strong>Confirmação automática ligada (Mercado Pago).</strong> Cada agendamento gera um Pix próprio e o horário é
      confirmado sozinho quando o pagamento cai.
    </p>
  ) : (
    <p className="nota nota-info">
      <strong>Confirmação manual.</strong> O Pix é gerado com a chave abaixo; quando o comprovante chegar, clique em
      “Sinal recebido” na Agenda. Para confirmar sozinho, configure o Mercado Pago no servidor.
    </p>
  );
}
