package br.com.jofashionhair.web.publico;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Corpo do POST /api/public/agendamentos.
 * Exemplo:
 * <pre>
 * {"servicoId":4,"profissionalId":null,"data":"2026-10-16","hora":"17:00",
 *  "nome":"Maria","telefone":"(47) 99999-8888"}
 * </pre>
 */
public record NovoAgendamentoRequest(
        @NotNull(message = "Informe o serviço.")
        Long servicoId,

        /* null = sem preferência */
        Long profissionalId,

        @NotNull(message = "Informe a data.")
        LocalDate data,

        @NotNull(message = "Informe o horário.")
        LocalTime hora,

        @NotBlank(message = "Informe seu nome.")
        @Size(max = 120)
        String nome,

        @NotBlank(message = "Informe seu WhatsApp.")
        @Pattern(regexp = "^[0-9()+\\-\\s]{10,20}$", message = "WhatsApp inválido.")
        String telefone,

        @Size(max = 500)
        String observacoes,

        /* caixinha "quero receber promoções pelo WhatsApp" */
        Boolean aceitaPromocoes) {
}
