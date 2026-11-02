import { useEffect, useState } from 'react';
import { adminApi } from '../../api/admin.js';
import { useCarregar } from '../../hooks/useCarregar.js';
import { dataCurta, dataDeIso, horaDe, linkWhatsapp, moeda, telefoneVisivel } from '../../util/formato.js';
import { Carga, Modal, StatusEtiqueta, useAvisos } from '../componentes.jsx';

/** Busca de clientes com o histórico de comparecimento (controle de no-show). */
export default function Clientes() {
  const [texto, setTexto] = useState('');
  const [busca, setBusca] = useState('');
  const [selecionado, setSelecionado] = useState(null);

  // espera a pessoa parar de digitar antes de buscar
  useEffect(() => {
    const t = setTimeout(() => setBusca(texto.trim()), 350);
    return () => clearTimeout(t);
  }, [texto]);

  const clientes = useCarregar(() => adminApi.clientes(busca), [busca]);

  return (
    <div className="pagina">
      <header className="pagina-cabeca">
        <div>
          <h1>Clientes e faltas</h1>
          <p className="pagina-sub">Veja quem já faltou antes de confirmar um encaixe. A partir de 2 faltas, a cliente fica em destaque.</p>
        </div>
      </header>

      <div className="filtros">
        <input
          type="search"
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
          placeholder="Buscar por nome ou telefone"
          aria-label="Buscar cliente"
        />
      </div>

      <Carga estado={clientes} vazio={busca ? 'Nenhuma cliente encontrada.' : 'Nenhuma cliente ainda.'}>
        <div className="tabela-rolagem">
          <table className="tabela">
            <thead>
              <tr>
                <th>Cliente</th>
                <th className="numero">Agendamentos</th>
                <th className="numero">Concluídos</th>
                <th className="numero">Cancelados</th>
                <th className="numero">Faltas</th>
                <th>Última falta</th>
                <th><span className="visualmente-oculto">Ações</span></th>
              </tr>
            </thead>
            <tbody>
              {(clientes.dados || []).map((c) => (
                <tr key={c.clienteId} className={c.totalFaltas >= 2 ? 'destaque' : undefined}>
                  <td>
                    <div>{c.nome}</div>
                    {c.telefone ? (
                      <a className="link-suave" href={linkWhatsapp(c.telefone)} target="_blank" rel="noreferrer">
                        {telefoneVisivel(c.telefone)}
                      </a>
                    ) : (
                      <span className="dica">Dados pessoais removidos</span>
                    )}
                  </td>
                  <td className="numero">{c.totalAgendamentos}</td>
                  <td className="numero">{c.totalConcluidos}</td>
                  <td className="numero">{c.totalCancelados}</td>
                  <td className="numero"><strong>{c.totalFaltas}</strong></td>
                  <td>{c.ultimaFaltaEm ? new Date(c.ultimaFaltaEm).toLocaleDateString('pt-BR') : 'Nunca'}</td>
                  <td className="acoes">
                    <button type="button" className="botao-texto" onClick={() => setSelecionado(c)}>Histórico</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Carga>

      {selecionado && (
        <HistoricoModal
          cliente={selecionado}
          aoFechar={() => setSelecionado(null)}
          aoExcluir={() => {
            setSelecionado(null);
            clientes.recarregar(true);
          }}
        />
      )}
    </div>
  );
}

function HistoricoModal({ cliente, aoFechar, aoExcluir }) {
  const avisos = useAvisos();
  const historico = useCarregar(() => adminApi.historicoCliente(cliente.clienteId), [cliente.clienteId]);
  const [ocupado, setOcupado] = useState(false);
  const removido = !cliente.telefone;

  async function exportar() {
    try {
      await adminApi.exportarCliente(cliente.clienteId);
      avisos.sucesso('Arquivo baixado. Envie para a cliente que pediu os dados.');
    } catch (e) {
      avisos.erro(e.message);
    }
  }

  async function excluir() {
    const ok = window.confirm(
      `Excluir ${cliente.nome} do sistema?\n\nA cliente e todo o histórico de agendamentos dela serão apagados ` +
        'e não aparecerão mais no painel. Isso não pode ser desfeito.',
    );
    if (!ok) return;
    setOcupado(true);
    try {
      await adminApi.excluirCliente(cliente.clienteId);
      avisos.sucesso(`${cliente.nome} foi excluída do sistema.`);
      aoExcluir();
    } catch (e) {
      avisos.erro(e.message);
    } finally {
      setOcupado(false);
    }
  }

  return (
    <Modal titulo={`Histórico de ${cliente.nome}`} aoFechar={aoFechar} largo>
      <div className="modal-corpo">
        {!removido && (
          <div className="lgpd-acoes">
            <p className="dica">Pedidos da cliente pela LGPD:</p>
            <button type="button" className="botao botao-secundario botao-pequeno" onClick={exportar}>
              Exportar dados
            </button>
            <button type="button" className="botao botao-perigo botao-pequeno" onClick={excluir} disabled={ocupado}>
              {ocupado ? 'Excluindo…' : 'Excluir cliente e histórico'}
            </button>
          </div>
        )}
        <Carga estado={historico} vazio="Nenhum agendamento.">
          <ul className="lista-simples">
            {(historico.dados || []).map((a) => (
              <li key={a.id}>
                <span>
                  {dataCurta(dataDeIso(a.inicio))}, {horaDe(a.inicio)}: {a.servico} com {a.profissional} ({moeda(a.preco)})
                </span>
                <StatusEtiqueta status={a.status} />
              </li>
            ))}
          </ul>
        </Carga>
      </div>
    </Modal>
  );
}
