package br.com.jofashionhair.service.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import br.com.jofashionhair.domain.HorarioTrabalho;
import br.com.jofashionhair.domain.Servico;
import br.com.jofashionhair.domain.StatusAgendamento;
import br.com.jofashionhair.service.HorarioFuncionamentoDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Contratos (entrada/saída) da API do /admin, agrupados num só lugar.
 */
public final class AdminDtos {

    private AdminDtos() {
    }

    // ===================================================================== SERVIÇOS

    public record ServicoRequest(
            @NotBlank(message = "Informe o nome.") @Size(max = 120)
            String nome,

            @Size(max = 2000)
            String descricao,

            @NotBlank @Pattern(regexp = "CABELO|UNHAS|ESTETICA|OUTROS", message = "Categoria inválida.")
            String categoria,

            @NotNull(message = "Informe o preço.")
            @DecimalMin(value = "0.00", message = "O preço não pode ser negativo.")
            @Digits(integer = 8, fraction = 2)
            BigDecimal preco,

            @NotNull(message = "Informe a duração.")
            @Min(value = 5, message = "Duração mínima: 5 minutos.")
            @Max(value = 720, message = "Duração máxima: 720 minutos.")
            Integer duracaoMinutos,

            @Min(0)
            Integer ordemExibicao,

            @Size(max = 500)
            @Pattern(regexp = br.com.jofashionhair.service.SalaoInformacoesDto.LINK_IMAGEM, message = "Foto inválida.")
            String fotoUrl,

            Boolean exigeSinal,

            @DecimalMin(value = "0.01", message = "O sinal deve ser maior que zero.")
            @Digits(integer = 8, fraction = 2)
            BigDecimal valorSinal) {
    }

    /** Edição rápida (modal do /admin). Envie um ou os dois campos. */
    public record PrecoDuracaoRequest(
            @DecimalMin(value = "0.00", message = "O preço não pode ser negativo.")
            @Digits(integer = 8, fraction = 2)
            BigDecimal preco,

            @Min(value = 5, message = "Duração mínima: 5 minutos.")
            @Max(value = 720, message = "Duração máxima: 720 minutos.")
            Integer duracaoMinutos) {
    }

    public record ServicoAdminResponse(
            Long id, String nome, String descricao, String categoria,
            BigDecimal preco, Integer duracaoMinutos, Integer ordemExibicao, boolean ativo, String fotoUrl,
            boolean exigeSinal, BigDecimal valorSinal) {

        public static ServicoAdminResponse de(Servico s) {
            return new ServicoAdminResponse(s.getId(), s.getNome(), s.getDescricao(), s.getCategoria(),
                    s.getPreco(), s.getDuracaoMinutos(), s.getOrdemExibicao(), s.isAtivo(), s.getFotoUrl(),
                    s.isExigeSinal(), s.getValorSinal());
        }
    }

    public record AtivoRequest(@NotNull Boolean ativo) {
    }

    // ===================================================================== PROFISSIONAIS

    public record ProfissionalRequest(
            @NotBlank(message = "Informe o nome.") @Size(max = 120) String nome,
            @Size(max = 120) String especialidade,
            @Size(max = 20) String telefone,
            @Size(max = 1000) String bio,
            @Size(max = 500)
            @Pattern(regexp = "^$|^(https?://|/).*", message = "A foto deve ser um link (https://...).")
            String fotoUrl) {
    }

    public record ServicosDaProfissionalRequest(@NotNull Set<Long> servicoIds) {
    }

    /** Faixa de expediente. diaSemana: 1 = segunda ... 7 = domingo. */
    public record FaixaHorario(
            @NotNull @Min(1) @Max(7) Integer diaSemana,
            @NotNull LocalTime horaInicio,
            @NotNull LocalTime horaFim) {

        public static FaixaHorario de(HorarioTrabalho h) {
            return new FaixaHorario(h.getDiaSemana().intValue(), h.getHoraInicio(), h.getHoraFim());
        }
    }

    public record HorariosRequest(@NotNull List<@Valid FaixaHorario> faixas) {
    }

    /** {@code horarios} vem só no detalhe (GET /{id}); na lista fica de fora. */
    public record ProfissionalAdminResponse(
            Long id, String nome, String especialidade, String telefone, String bio, String fotoUrl,
            boolean ativo, List<Long> servicoIds, List<FaixaHorario> horarios) {
    }

    // ===================================================================== BLOQUEIOS

    public record BloqueioRequest(
            @NotNull(message = "Informe a profissional.") Long profissionalId,
            @NotNull(message = "Informe o início.") LocalDateTime inicio,
            @NotNull(message = "Informe o fim.") LocalDateTime fim,
            @Size(max = 200) String motivo) {
    }

    public record BloqueioResponse(
            Long id, Long profissionalId, String profissional,
            LocalDateTime inicio, LocalDateTime fim, String motivo) {
    }

    // ===================================================================== SALÃO

    /** PUT /api/admin/salao/horarios — pode enviar só os dias que mudaram. */
    public record HorariosFuncionamentoRequest(@NotNull List<@Valid HorarioFuncionamentoDto> dias) {
    }

    // ===================================================================== AGENDA

    /** Agendamento feito no balcão/telefone pelo /admin. */
    public record AgendamentoManualRequest(
            @NotNull(message = "Informe o serviço.") Long servicoId,
            Long profissionalId,
            @NotNull(message = "Informe a data.") LocalDate data,
            @NotNull(message = "Informe o horário.") LocalTime hora,
            @NotBlank(message = "Informe o nome do cliente.") @Size(max = 120) String nome,
            @NotBlank(message = "Informe o telefone.")
            @Pattern(regexp = "^[0-9()+\\-\\s]{10,20}$", message = "Telefone inválido.")
            String telefone,
            @Size(max = 500) String observacoes) {
    }

    public record StatusRequest(@NotNull(message = "Informe o status.") StatusAgendamento status) {
    }
}
