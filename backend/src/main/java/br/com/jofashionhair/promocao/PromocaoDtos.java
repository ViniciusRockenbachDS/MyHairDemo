package br.com.jofashionhair.promocao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import br.com.jofashionhair.domain.Servico;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Contratos da API de promoções e campanhas. */
public final class PromocaoDtos {

    private PromocaoDtos() {
    }

    public record PromocaoDto(
            Long id,
            @NotBlank(message = "Informe o título.") @Size(max = 120) String titulo,
            @Size(max = 1000) String descricao,
            @NotNull @Min(value = 1, message = "Desconto mínimo: 1%.") @Max(value = 90, message = "Desconto máximo: 90%.")
            Integer descontoPercentual,
            List<@Min(1) @Max(7) Integer> diasSemana,
            LocalDate dataInicio,
            LocalDate dataFim,
            Boolean todosServicos,
            List<Long> servicoIds,
            List<String> servicoNomes,
            Boolean ativo) {

        public static PromocaoDto de(Promocao p) {
            List<Servico> servicos = p.getServicos().stream().toList();
            return new PromocaoDto(p.getId(), p.getTitulo(), p.getDescricao(), p.getDescontoPercentual(), p.dias(),
                    p.getDataInicio(), p.getDataFim(), p.isTodosServicos(),
                    servicos.stream().map(Servico::getId).sorted().toList(),
                    servicos.stream().map(Servico::getNome).sorted().toList(),
                    p.isAtivo());
        }
    }

    /** Quem recebe o envio. */
    public record PublicoRequest(Boolean soAutorizadas, Boolean soQuemFezServicos) {
    }

    public record EnvioRequest(
            @NotBlank(message = "Escreva a mensagem.") @Size(max = 1000) String mensagem,
            Boolean soAutorizadas,
            Boolean soQuemFezServicos) {
    }

    public record PublicoResponse(int total) {
    }

    public record CampanhaResponse(Long id, String titulo, String publico, int total, int enviados, int falhas,
                                   String status, LocalDateTime criadoEm) {
    }
}
