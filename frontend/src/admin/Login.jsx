import { useState } from 'react';
import { useLocation, useSearchParams } from 'react-router-dom';
import { adminApi } from '../api/admin.js';
import { credenciais, DISPOSITIVO_NAO_VERIFICADO } from '../api/http.js';
import { apiPublica } from '../api/publico.js';

/**
 * Tela de entrada do painel, com três modos:
 *  - entrar: e-mail e senha;
 *  - esqueci: pede o código por e-mail;
 *  - codigo: código de 6 números + nova senha (também usado no primeiro acesso por convite,
 *    link /admin/primeiro-acesso?email=... que chega no e-mail).
 */
export default function Login({ aoEntrar, sessaoExpirada }) {
  const local = useLocation();
  const [parametros] = useSearchParams();
  const primeiroAcesso = local.pathname.endsWith('/primeiro-acesso');

  const [modo, setModo] = useState(primeiroAcesso ? 'codigo' : 'entrar');
  const [email, setEmail] = useState(parametros.get('email') || '');
  const [senha, setSenha] = useState('');
  const [codigo, setCodigo] = useState('');
  const [nova, setNova] = useState('');
  const [confirmacao, setConfirmacao] = useState('');
  const [mensagem, setMensagem] = useState(
    primeiroAcesso ? 'Bem-vindo(a)! Digite o código que chegou no seu e-mail e crie sua senha.' : null,
  );
  const [erro, setErro] = useState(
    sessaoExpirada ? 'Por segurança, você saiu do painel após um tempo sem uso. Entre novamente.' : null,
  );
  const [carregando, setCarregando] = useState(false);

  function trocarModo(novoModo) {
    setModo(novoModo);
    setErro(null);
    setMensagem(null);
  }

  async function entrar(e) {
    e.preventDefault();
    setCarregando(true);
    setErro(null);
    credenciais.salvar(email.trim().toLowerCase(), senha);
    try {
      await adminApi.me();
      aoEntrar();
    } catch (err) {
      if (err.status === 403 && err.motivo === DISPOSITIVO_NAO_VERIFICADO) {
        // senha certa, mas dispositivo novo: pede o código do e-mail
        setModo('dispositivo');
        setCodigo('');
        await enviarCodigoDispositivo();
      } else {
        credenciais.limpar();
        setErro(err.status === 401 ? 'E-mail ou senha incorretos.' : err.message);
      }
    } finally {
      setCarregando(false);
    }
  }

  async function enviarCodigoDispositivo() {
    setErro(null);
    try {
      const r = await adminApi.enviarCodigoDispositivo();
      setMensagem(r.mensagem);
    } catch (err) {
      setErro(err.message);
    }
  }

  async function confirmarDispositivo(e) {
    e.preventDefault();
    setErro(null);
    if (!/^\d{6}$/.test(codigo.trim())) {
      setErro('O código tem 6 números.');
      return;
    }
    setCarregando(true);
    try {
      await adminApi.confirmarDispositivo(codigo.trim());
      await adminApi.me();
      aoEntrar();
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregando(false);
    }
  }

  function cancelarDispositivo() {
    credenciais.limpar();
    setSenha('');
    setCodigo('');
    trocarModo('entrar');
  }

  async function pedirCodigo(e) {
    e.preventDefault();
    setCarregando(true);
    setErro(null);
    try {
      const r = await apiPublica.esqueciSenha(email.trim());
      setModo('codigo');
      setMensagem(r.mensagem);
    } catch (err) {
      setErro(err.campos?.email || err.message);
    } finally {
      setCarregando(false);
    }
  }

  async function redefinir(e) {
    e.preventDefault();
    setErro(null);
    if (!/^\d{6}$/.test(codigo.trim())) {
      setErro('O código tem 6 números.');
      return;
    }
    if (nova.length < 8 || !/[A-Za-zÀ-ÿ]/.test(nova) || !/\d/.test(nova)) {
      setErro('A senha precisa ter pelo menos 8 caracteres, com letras e números.');
      return;
    }
    if (nova !== confirmacao) {
      setErro('As duas senhas não são iguais.');
      return;
    }
    setCarregando(true);
    try {
      await apiPublica.redefinirSenha({ email: email.trim(), codigo: codigo.trim(), novaSenha: nova });
      setModo('entrar');
      setSenha('');
      setCodigo('');
      setNova('');
      setConfirmacao('');
      setMensagem('Senha salva! Agora entre com seu e-mail e a nova senha.');
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregando(false);
    }
  }

  return (
    <main className="login">
      {modo === 'entrar' && (
        <form className="login-cartao" onSubmit={entrar}>
          <h1>Painel do salão</h1>
          <p className="dica">Entre com o seu e-mail e senha.</p>
          {mensagem && <p className="nota nota-info" role="status">{mensagem}</p>}
          <div className="campo">
            <label htmlFor="login-email">E-mail</label>
            <input id="login-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="username" required />
          </div>
          <div className="campo">
            <label htmlFor="login-senha">Senha</label>
            <input id="login-senha" type="password" value={senha} onChange={(e) => setSenha(e.target.value)} autoComplete="current-password" required />
          </div>
          {erro && <p className="erro-texto" role="alert">{erro}</p>}
          <button type="submit" className="botao botao-primario botao-largo" disabled={carregando}>
            {carregando ? 'Entrando…' : 'Entrar'}
          </button>
          <button type="button" className="botao-texto login-link" onClick={() => trocarModo('esqueci')}>
            Esqueci minha senha
          </button>
        </form>
      )}

      {modo === 'dispositivo' && (
        <form className="login-cartao" onSubmit={confirmarDispositivo} noValidate>
          <h1>Confirme este dispositivo</h1>
          <p className="dica">
            É a primeira vez que você entra por este computador ou celular. Para proteger o painel, confirme com o
            código enviado para o seu e-mail.
          </p>
          {mensagem && <p className="nota nota-info" role="status">{mensagem}</p>}
          <div className="campo">
            <label htmlFor="disp-codigo">Código recebido por e-mail</label>
            <input
              id="disp-codigo"
              value={codigo}
              onChange={(e) => setCodigo(e.target.value.replace(/\D/g, '').slice(0, 6))}
              inputMode="numeric"
              autoComplete="one-time-code"
              placeholder="000000"
              className="campo-codigo"
              autoFocus
            />
          </div>
          {erro && <p className="erro-texto" role="alert">{erro}</p>}
          <button type="submit" className="botao botao-primario botao-largo" disabled={carregando}>
            {carregando ? 'Confirmando…' : 'Confirmar e entrar'}
          </button>
          <p className="dica">Depois de confirmado, este dispositivo não pede código por 90 dias.</p>
          <button type="button" className="botao-texto login-link" onClick={enviarCodigoDispositivo}>
            Não recebi / enviar outro código
          </button>
          <button type="button" className="botao-texto login-link" onClick={cancelarDispositivo}>
            Cancelar
          </button>
        </form>
      )}

      {modo === 'esqueci' && (
        <form className="login-cartao" onSubmit={pedirCodigo}>
          <h1>Recuperar senha</h1>
          <p className="dica">Vamos enviar um código de 6 números para o seu e-mail.</p>
          <div className="campo">
            <label htmlFor="esq-email">E-mail cadastrado</label>
            <input id="esq-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="username" required />
          </div>
          {erro && <p className="erro-texto" role="alert">{erro}</p>}
          <button type="submit" className="botao botao-primario botao-largo" disabled={carregando}>
            {carregando ? 'Enviando…' : 'Enviar código'}
          </button>
          <button type="button" className="botao-texto login-link" onClick={() => trocarModo('codigo')}>
            Já tenho um código
          </button>
          <button type="button" className="botao-texto login-link" onClick={() => trocarModo('entrar')}>
            Voltar para entrar
          </button>
        </form>
      )}

      {modo === 'codigo' && (
        <form className="login-cartao" onSubmit={redefinir} noValidate>
          <h1>{primeiroAcesso ? 'Criar sua senha' : 'Nova senha'}</h1>
          {mensagem && <p className="nota nota-info" role="status">{mensagem}</p>}
          <div className="campo">
            <label htmlFor="cod-email">E-mail</label>
            <input id="cod-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="username" required />
          </div>
          <div className="campo">
            <label htmlFor="cod-codigo">Código recebido por e-mail</label>
            <input
              id="cod-codigo"
              value={codigo}
              onChange={(e) => setCodigo(e.target.value.replace(/\D/g, '').slice(0, 6))}
              inputMode="numeric"
              autoComplete="one-time-code"
              placeholder="000000"
              className="campo-codigo"
            />
          </div>
          <div className="campo">
            <label htmlFor="cod-nova">Nova senha</label>
            <input id="cod-nova" type="password" value={nova} onChange={(e) => setNova(e.target.value)} autoComplete="new-password" />
            <span className="dica">Pelo menos 8 caracteres, com letras e números.</span>
          </div>
          <div className="campo">
            <label htmlFor="cod-conf">Repita a nova senha</label>
            <input id="cod-conf" type="password" value={confirmacao} onChange={(e) => setConfirmacao(e.target.value)} autoComplete="new-password" />
          </div>
          {erro && <p className="erro-texto" role="alert">{erro}</p>}
          <button type="submit" className="botao botao-primario botao-largo" disabled={carregando}>
            {carregando ? 'Salvando…' : 'Salvar senha'}
          </button>
          <button type="button" className="botao-texto login-link" onClick={() => trocarModo('esqueci')}>
            Não recebi o código / enviar outro
          </button>
          <button type="button" className="botao-texto login-link" onClick={() => trocarModo('entrar')}>
            Voltar para entrar
          </button>
        </form>
      )}
    </main>
  );
}
