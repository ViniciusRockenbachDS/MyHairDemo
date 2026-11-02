import { useState } from 'react';
import { adminApi } from '../../api/admin.js';
import { duracao, lerValor, moeda } from '../../util/formato.js';
import { Campo, Modal, useAvisos } from '../componentes.jsx';

const ATALHOS_MINUTOS = [-15, -5, 5, 15];

/**
 * Edição rápida de preço e duração de um serviço.
 * Ao salvar, o site e o robô do WhatsApp já usam os novos valores
 * (o site recebe o aviso ao vivo e recarrega sozinho).
 * Agendamentos já marcados mantêm o preço e a duração originais.
 */
export default function PrecoDuracaoModal({ servico, aoFechar, aoSalvar }) {
  const avisos = useAvisos();
  const [preco, setPreco] = useState(String(servico.preco).replace('.', ','));
  const [minutos, setMinutos] = useState(String(servico.duracaoMinutos));
  const [erros, setErros] = useState({});
  const [salvando, setSalvando] = useState(false);

  const novoPreco = lerValor(preco);
  const novosMinutos = Number(minutos);
  const mudouPreco = novoPreco !== null && novoPreco !== Number(servico.preco);
  const mudouDuracao = Number.isInteger(novosMinutos) && novosMinutos !== servico.duracaoMinutos;

  function ajustarMinutos(delta) {
    const atual = Number(minutos) || 0;
    setMinutos(String(Math.min(720, Math.max(5, atual + delta))));
  }

  async function salvar(e) {
    e.preventDefault();
    const novosErros = {};
    if (novoPreco === null || novoPreco < 0) novosErros.preco = 'Informe um valor, por exemplo 120,00.';
    if (!Number.isInteger(novosMinutos) || novosMinutos < 5 || novosMinutos > 720) {
      novosErros.minutos = 'Use um número entre 5 e 720 minutos.';
    }
    setErros(novosErros);
    if (Object.keys(novosErros).length) return;
    if (!mudouPreco && !mudouDuracao) {
      aoFechar();
      return;
    }

    setSalvando(true);
    try {
      const atualizado = await adminApi.precoDuracao(servico.id, {
        preco: mudouPreco ? novoPreco : null,
        duracaoMinutos: mudouDuracao ? novosMinutos : null,
      });
      avisos.sucesso(`${atualizado.nome} atualizado. O site e o WhatsApp já mostram os novos valores.`);
      aoSalvar(atualizado);
    } catch (err) {
      avisos.erro(err.message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <Modal titulo={`Preço e duração: ${servico.nome}`} aoFechar={aoFechar}>
      <form onSubmit={salvar} noValidate>
        <div className="modal-corpo grade-2">
          <Campo id="pd-preco" rotulo="Preço (R$)" erro={erros.preco} dica={`Atual: ${moeda(servico.preco)}`}>
            <input
              id="pd-preco"
              inputMode="decimal"
              value={preco}
              onChange={(e) => setPreco(e.target.value)}
              aria-invalid={!!erros.preco}
            />
          </Campo>
          <Campo id="pd-minutos" rotulo="Duração (minutos)" erro={erros.minutos} dica={`Atual: ${duracao(servico.duracaoMinutos)}`}>
            <input
              id="pd-minutos"
              type="number"
              min="5"
              max="720"
              step="5"
              value={minutos}
              onChange={(e) => setMinutos(e.target.value)}
              aria-invalid={!!erros.minutos}
            />
          </Campo>
          <div className="atalhos" aria-label="Ajuste rápido da duração">
            {ATALHOS_MINUTOS.map((d) => (
              <button key={d} type="button" className="chip" onClick={() => ajustarMinutos(d)}>
                {d > 0 ? `+${d}` : d} min
              </button>
            ))}
          </div>
          {(mudouPreco || mudouDuracao) && (
            <p className="nota nota-info grade-inteira">
              Novos agendamentos: {mudouPreco ? moeda(novoPreco) : moeda(servico.preco)},{' '}
              {mudouDuracao ? duracao(novosMinutos) : duracao(servico.duracaoMinutos)}. Agendamentos já marcados
              continuam com o valor e o tempo originais.
            </p>
          )}
        </div>
        <div className="modal-rodape">
          <button type="button" className="botao botao-secundario" onClick={aoFechar}>Cancelar</button>
          <button type="submit" className="botao botao-primario" disabled={salvando}>
            {salvando ? 'Salvando…' : 'Salvar preço e duração'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
