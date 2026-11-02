import { useState } from 'react';
import { adminApi } from '../../api/admin.js';
import { credenciais } from '../../api/http.js';
import { useCarregar } from '../../hooks/useCarregar.js';
import { Campo, Carga, useAvisos } from '../componentes.jsx';

const dataHora = (iso) =>
  new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

const PAPEIS = { DONO: 'Dono', EQUIPE: 'Equipe' };

/** Dados da conta e troca de senha com verificação por e-mail (2 etapas). */
export default function MinhaConta() {
  const avisos = useAvisos();
  const conta = useCarregar(() => adminApi.me(), []);
  const [etapa, setEtapa] = useState('inicio'); // inicio | codigo
  const [mensagem, setMensagem] = useState(null);
  const [codigo, setCodigo] = useState('');
  const [nova, setNova] = useState('');
  const [confirmacao, setConfirmacao] = useState('');
  const [erro, setErro] = useState(null);
  const [ocupado, setOcupado] = useState(false);

  async function pedirCodigo() {
    setOcupado(true);
    setErro(null);
    try {
      const r = await adminApi.pedirCodigoSenha();
      setMensagem(r.mensagem);
      setEtapa('codigo');
    } catch (e) {
      setErro(e.message);
    } finally {
      setOcupado(false);
    }
  }

  async function salvar(e) {
    e.preventDefault();
    setErro(null);
    if (!/^\d{6}$/.test(codigo)) return setErro('O código tem 6 números.');
    if (nova.length < 8 || !/[A-Za-zÀ-ÿ]/.test(nova) || !/\d/.test(nova)) {
      return setErro('A senha precisa ter pelo menos 8 caracteres, com letras e números.');
    }
    if (nova !== confirmacao) return setErro('As duas senhas não são iguais.');
    setOcupado(true);
    try {
      await adminApi.trocarSenha({ codigo, novaSenha: nova });
      credenciais.salvar(conta.dados.email, nova); // continua logado com a senha nova
      avisos.sucesso('Senha alterada. Enviamos um aviso para o seu e-mail.');
      setEtapa('inicio');
      setMensagem(null);
      setCodigo('');
      setNova('');
      setConfirmacao('');
    } catch (err) {
      setErro(err.message);
    } finally {
      setOcupado(false);
    }
  }

  return (
    <div className="pagina">
      <header className="pagina-cabeca">
        <div>
          <h1>Minha conta</h1>
          <p className="pagina-sub">Seus dados de acesso ao painel.</p>
        </div>
      </header>

      <section className="painel">
        <Carga estado={conta}>
          {conta.dados && (
            <dl className="dados-conta">
              <div><dt>Nome</dt><dd>{conta.dados.nome}</dd></div>
              <div><dt>E-mail</dt><dd>{conta.dados.email}</dd></div>
              <div><dt>Acesso</dt><dd>{PAPEIS[conta.dados.papel]}</dd></div>
            </dl>
          )}
        </Carga>
      </section>

      <section className="painel">
        <h2>Alterar senha</h2>
        <p className="dica">
          Por segurança, a troca é confirmada com um código de 6 números enviado para o seu e-mail.
        </p>

        {etapa === 'inicio' ? (
          <div>
            {erro && <p className="erro-texto" role="alert">{erro}</p>}
            <button type="button" className="botao botao-primario" onClick={pedirCodigo} disabled={ocupado || !conta.dados}>
              {ocupado ? 'Enviando…' : 'Enviar código para o meu e-mail'}
            </button>
          </div>
        ) : (
          <form onSubmit={salvar} noValidate className="grade-2">
            {mensagem && <p className="nota nota-info grade-inteira" role="status">{mensagem}</p>}
            <Campo id="mc-codigo" rotulo="Código recebido">
              <input
                id="mc-codigo"
                value={codigo}
                onChange={(e) => setCodigo(e.target.value.replace(/\D/g, '').slice(0, 6))}
                inputMode="numeric"
                autoComplete="one-time-code"
                placeholder="000000"
                className="campo-codigo"
              />
            </Campo>
            <div />
            <Campo id="mc-nova" rotulo="Nova senha" dica="Pelo menos 8 caracteres, com letras e números.">
              <input id="mc-nova" type="password" value={nova} onChange={(e) => setNova(e.target.value)} autoComplete="new-password" />
            </Campo>
            <Campo id="mc-conf" rotulo="Repita a nova senha">
              <input id="mc-conf" type="password" value={confirmacao} onChange={(e) => setConfirmacao(e.target.value)} autoComplete="new-password" />
            </Campo>
            {erro && <p className="erro-texto grade-inteira" role="alert">{erro}</p>}
            <div className="grade-inteira acoes-form">
              <button type="button" className="botao-texto" onClick={pedirCodigo} disabled={ocupado}>
                Enviar outro código
              </button>
              <button type="button" className="botao botao-secundario" onClick={() => { setEtapa('inicio'); setErro(null); }}>
                Cancelar
              </button>
              <button type="submit" className="botao botao-primario" disabled={ocupado}>
                {ocupado ? 'Salvando…' : 'Salvar nova senha'}
              </button>
            </div>
          </form>
        )}
      </section>
      <Dispositivos />
    </div>
  );
}

/** Computadores e celulares confirmados por código de e-mail. */
function Dispositivos() {
  const avisos = useAvisos();
  const lista = useCarregar(() => adminApi.dispositivos(), []);

  async function remover(d) {
    const aviso = d.atual
      ? 'Remover ESTE dispositivo? Você vai sair do painel e precisará confirmar de novo com código.'
      : `Remover "${d.nome}"? Quem estiver usando o painel nele vai precisar confirmar de novo com código.`;
    if (!window.confirm(aviso)) return;
    try {
      await adminApi.removerDispositivo(d.id);
      avisos.sucesso('Dispositivo removido.');
      lista.recarregar(true);
    } catch (e) {
      avisos.erro(e.message);
    }
  }

  async function sairDosOutros() {
    if (!window.confirm('Desconectar todos os outros computadores e celulares? Só este continuará conectado.')) return;
    try {
      const r = await adminApi.sairDosOutrosDispositivos();
      avisos.sucesso(r.removidos ? `${r.removidos} dispositivo(s) desconectado(s).` : 'Nenhum outro dispositivo conectado.');
      lista.recarregar(true);
    } catch (e) {
      avisos.erro(e.message);
    }
  }

  return (
    <section className="painel">
      <div className="painel-cabeca">
        <h2>Dispositivos confirmados</h2>
        <button type="button" className="botao botao-secundario botao-pequeno" onClick={sairDosOutros}>
          Sair de todos os outros
        </button>
      </div>
      <p className="dica">
        Cada computador ou celular novo precisa ser confirmado com um código enviado ao seu e-mail. Viu algum que
        não reconhece? Remova e troque sua senha.
      </p>
      <Carga estado={lista} vazio="Nenhum dispositivo confirmado.">
        <ul className="lista-simples">
          {(lista.dados || []).map((d) => (
            <li key={d.id}>
              <span>
                <strong>{d.nome}</strong>
                {d.atual && <span className="etiqueta etiqueta-confirmado"> este dispositivo</span>}
                <span className="dica"> · confirmado em {dataHora(d.confirmadoEm)} · último uso {dataHora(d.ultimoUso)}</span>
              </span>
              <button type="button" className="botao-texto texto-perigo" onClick={() => remover(d)}>Remover</button>
            </li>
          ))}
        </ul>
      </Carga>
    </section>
  );
}
