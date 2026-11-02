package br.com.jofashionhair.whatsapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.jofashionhair.domain.Servico;
import br.com.jofashionhair.domain.Telefone;
import br.com.jofashionhair.whatsapp.InterpretadorMensagem.DataInterpretada;

/**
 * Teste puro (sem Spring nem banco): roda em milissegundos com "mvn test".
 * "Hoje" fixo = segunda-feira, 12/10/2026.
 */
class InterpretadorMensagemTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 12);
    private final InterpretadorMensagem i = new InterpretadorMensagem();

    private DataInterpretada data(String texto) {
        return i.extrairData(i.normalizar(texto), HOJE);
    }

    private LocalTime hora(String texto) {
        return i.extrairHora(i.normalizar(texto)).orElse(null);
    }

    @Test
    void mensagemDoExemploComDataCoerente() {
        String msg = "Olá, queria marcar para sexta-feira (dia 16) às 17:00.";
        assertEquals(LocalDate.of(2026, 10, 16), data(msg).data());
        assertEquals(LocalTime.of(17, 0), hora(msg));
    }

    @Test
    void diaDaSemanaQueNaoBateComADataGeraDuvida() {
        DataInterpretada d = data("queria marcar para sexta-feira (dia 18) às 17:00");
        assertNull(d.data());
        assertNotNull(d.duvida());
        assertTrue(d.duvida().contains("domingo"));
        assertTrue(d.duvida().contains("16/10"));
    }

    @Test
    void datasRelativasEExplicitas() {
        assertEquals(HOJE, data("hoje às 15h").data());
        assertEquals(HOJE.plusDays(1), data("amanhã às 9h").data());
        assertEquals(HOJE.plusDays(2), data("depois de amanhã").data());
        assertEquals(LocalDate.of(2026, 10, 16), data("sexta").data());
        assertEquals(LocalDate.of(2026, 10, 17), data("sábado de manhã").data());
        assertEquals(LocalDate.of(2026, 10, 20), data("20/10").data());
        assertEquals(LocalDate.of(2026, 11, 5), data("dia 5").data());          // dia 5 já passou em outubro
        assertEquals(LocalDate.of(2027, 1, 10), data("10/01").data());          // janeiro do próximo ano
        assertNull(data("quero fazer escova").data());
    }

    @Test
    void horarios() {
        assertEquals(LocalTime.of(17, 0), hora("às 17h"));
        assertEquals(LocalTime.of(14, 30), hora("16/10 14h30"));
        assertEquals(LocalTime.of(9, 0), hora("amanhã às 9"));
        assertEquals(LocalTime.of(15, 0), hora("3 da tarde"));
        assertEquals(LocalTime.of(20, 0), hora("8 da noite"));
        assertEquals(LocalTime.of(12, 0), hora("meio-dia"));
        assertEquals(LocalTime.of(17, 0), hora("17 horas"));
        assertNull(hora("dia 18"));
        assertNull(hora("16/10"));
    }

    @Test
    void opcoesDeMenu() {
        assertEquals(1, i.extrairOpcao(i.normalizar("1")));
        assertEquals(2, i.extrairOpcao(i.normalizar("2.")));
        assertEquals(3, i.extrairOpcao(i.normalizar("opção 3")));
        assertEquals(1, i.extrairOpcao(i.normalizar("1️⃣")));
        assertNull(i.extrairOpcao(i.normalizar("dia 18")));
    }

    @Test
    void intencoes() {
        assertTrue(i.ehSemPreferencia(i.normalizar("Tanto faz")));
        assertTrue(i.pedeAtendente(i.normalizar("quero falar com um atendente")));
        assertTrue(i.pedeCancelamento(i.normalizar("preciso cancelar meu horário")));
        assertTrue(i.ehSim(i.normalizar("Sim, confirmo")));
        assertTrue(i.ehNao(i.normalizar("Não vou conseguir")));
        assertTrue(i.ehMenu(i.normalizar("Menu")));
        assertFalse(i.ehSim(i.normalizar("sexta às 17h")));
    }

    @Test
    void tabelaESaudacao() {
        assertTrue(i.pedeTabela(i.normalizar("Pode me mandar a tabela de valores?")));
        assertTrue(i.pedeTabela(i.normalizar("Quanto custa?")));
        assertFalse(i.pedeTabela(i.normalizar("quero fazer escova sexta")));
        assertTrue(i.ehSaudacao(i.normalizar("Oi")));
        assertTrue(i.ehSaudacao(i.normalizar("Boa tarde, tudo bem?")));
        assertFalse(i.ehSaudacao(i.normalizar("Oi, vocês aceitam pix?")));
        assertTrue(i.ehPergunta(i.normalizar("Vocês aceitam pix")));
        assertTrue(i.ehPergunta(i.normalizar("tem estacionamento?")));
        assertFalse(i.ehPergunta(i.normalizar("escova sexta às 15h")));
        assertTrue(i.pedeSairPromocoes(i.normalizar("SAIR")));
        assertTrue(i.pedeSairPromocoes(i.normalizar("Não quero mais receber promoções")));
        assertFalse(i.pedeSairPromocoes(i.normalizar("quero sair às 15h")));
    }

    @Test
    void reconheceServicos() {
        Servico escova = new Servico("Escova", "CABELO", new BigDecimal("60"), 40);
        Servico gel = new Servico("Unha em Gel", "UNHAS", new BigDecimal("120"), 60);
        Servico manicure = new Servico("Manicure", "UNHAS", new BigDecimal("40"), 45);
        List<Servico> catalogo = List.of(escova, gel, manicure);

        assertEquals(List.of(gel), i.encontrarServicos(i.normalizar("Unha em Gel."), catalogo));
        assertEquals(List.of(gel), i.encontrarServicos(i.normalizar("quero fazer as unhas"), catalogo));
        assertEquals(List.of(escova), i.encontrarServicos(i.normalizar("uma ESCOVA sexta"), catalogo));
        assertTrue(i.encontrarServicos(i.normalizar("oi, tudo bem?"), catalogo).isEmpty());
    }

    @Test
    void telefoneBrasileiroGanhaNonoDigito() {
        assertEquals("5547999998888", Telefone.normalizar("554799998888"));      // como o WhatsApp manda
        assertEquals("5547999998888", Telefone.normalizar("(47) 99999-8888"));   // como a cliente digita
        assertEquals("554733334444", Telefone.normalizar("47 3333-4444"));       // fixo continua igual
    }
}
