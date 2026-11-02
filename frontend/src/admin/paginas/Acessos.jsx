import { useState } from 'react';
import { adminApi } from '../../api/admin.js';
import { useCarregar } from '../../hooks/useCarregar.js';
import { Campo, Carga, Modal, useAvisos } from '../componentes.jsx';

const PAPEIS = [
  ['EQUIPE', 'Equipe (agenda, serviços, clientes)'],
  ['DONO', 'Dono (tudo, inclusive acessos)'],
];

/** Só o dono vê: quem pode entrar no painel. Cada pessoa tem o próprio login. */
export default function Acessos() {
  const avisos = useAvisos();
  const usuarios = useCarregar(() => adminApi.usuarios(), []);
  const eu = useCarregar(() => adminApi.me(), []);
  const [convidando, setConvidando] = useState(false);

  async function reenviar(u) {
    try {
      await adminApi.reenviarConvite(u.id);
      avisos.sucesso(`Convite reenviado para ${u.email}.`);
    } catch (e) {
      avisos.erro(e.message);
    }
  }

  async function alternar(u) {
    if (u.ativo && !window.confirm(`Desativar o acesso de ${u.nome}? Ela não conseguirá mais entrar no painel.`)) return;
    try {
      await adminApi.usuarioAtivo(u.id, !u.ativo);
      avisos.sucesso(u.ativo ? `Acesso de ${u.nome} desativado.` : `Acesso de ${u.nome} reativado.`);
      usuarios.recarregar(true);
    } catch (e) {
      avisos.erro(e.message);
    }
  }

  return (
    <div className="pagina">
      <header className="pagina-cabeca">
        <div>
          <h1>Acessos</h1>
          <p className="pagina-sub">Quem pode entrar no painel. Cada pessoa usa o próprio e-mail e senha.</p>
        </div>
        <button type="button" className="botao botao-primario" onClick={() => setConvidando(true)}>
          Convidar pessoa
        </button>
      </header>

      <Carga estado={usuarios} vazio="Nenhum acesso cadastrado.">
        <div className="tabela-rolagem">
          <table className="tabela">
            <thead>
              <tr>
                <th>Nome</th>
                <th>E-mail</th>
                <th>Acesso</th>
                <th>Situação</th>
                <th><span className="visualmente-oculto">Ações</span></th>
              </tr>
            </thead>
            <tbody>
              {(usuarios.dados || []).map((u) => {
                const souEu = eu.dados?.email === u.email;
                return (
                  <tr key={u.id} className={u.ativo ? undefined : 'inativo'}>
                    <td>{u.nome}{souEu && <span className="dica"> (você)</span>}</td>
                    <td>{u.email}</td>
                    <td>{u.papel === 'DONO' ? 'Dono' : 'Equipe'}</td>
                    <td>{u.ativo ? 'Ativo' : 'Desativado'}</td>
                    <td className="acoes">
                      {!souEu && u.ativo && (
                        <button type="button" className="botao-texto" onClick={() => reenviar(u)}>Reenviar convite</button>
                      )}
                      {!souEu && (
                        <button type="button" className={`botao-texto${u.ativo ? ' texto-perigo' : ''}`} onClick={() => alternar(u)}>
                          {u.ativo ? 'Desativar' : 'Reativar'}
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </Carga>

      <p className="dica">
        “Reenviar convite” manda um código novo para a pessoa criar ou recuperar a senha. Use também quando
        alguém da equipe esquecer a senha e não conseguir recuperar sozinha.
      </p>

      {convidando && (
        <ConviteModal
          aoFechar={() => setConvidando(false)}
          aoSalvar={() => {
            setConvidando(false);
            usuarios.recarregar(true);
          }}
        />
      )}
    </div>
  );
}

function ConviteModal({ aoFechar, aoSalvar }) {
  const avisos = useAvisos();
  const [form, setForm] = useState({ nome: '', email: '', papel: 'EQUIPE' });
  const [erros, setErros] = useState({});
  const [salvando, setSalvando] = useState(false);
  const mudar = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }));

  async function salvar(e) {
    e.preventDefault();
    const novos = {};
    if (!form.nome.trim()) novos.nome = 'Informe o nome.';
    if (!/^\S+@\S+\.\S+$/.test(form.email.trim())) novos.email = 'Informe um e-mail válido.';
    setErros(novos);
    if (Object.keys(novos).length) return;
    setSalvando(true);
    try {
      await adminApi.convidarUsuario({ nome: form.nome.trim(), email: form.email.trim(), papel: form.papel });
      avisos.sucesso(`Convite enviado para ${form.email.trim()}. O código vale por 48 horas.`);
      aoSalvar();
    } catch (err) {
      if (err.campos) setErros(err.campos);
      avisos.erro(err.message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <Modal titulo="Convidar pessoa" aoFechar={aoFechar}>
      <form onSubmit={salvar} noValidate>
        <div className="modal-corpo grade-2">
          <div className="grade-inteira">
            <Campo id="cv-nome" rotulo="Nome" erro={erros.nome}>
              <input id="cv-nome" value={form.nome} onChange={mudar('nome')} maxLength={120} />
            </Campo>
          </div>
          <div className="grade-inteira">
            <Campo id="cv-email" rotulo="E-mail" erro={erros.email} dica="A pessoa recebe um código para criar a própria senha.">
              <input id="cv-email" type="email" value={form.email} onChange={mudar('email')} maxLength={160} />
            </Campo>
          </div>
          <div className="grade-inteira">
            <Campo id="cv-papel" rotulo="Tipo de acesso">
              <select id="cv-papel" value={form.papel} onChange={mudar('papel')}>
                {PAPEIS.map(([v, r]) => <option key={v} value={v}>{r}</option>)}
              </select>
            </Campo>
          </div>
        </div>
        <div className="modal-rodape">
          <button type="button" className="botao botao-secundario" onClick={aoFechar}>Cancelar</button>
          <button type="submit" className="botao botao-primario" disabled={salvando}>
            {salvando ? 'Enviando…' : 'Enviar convite'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
