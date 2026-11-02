package br.com.jofashionhair.web.admin;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.jofashionhair.service.HorarioFuncionamentoDto;
import br.com.jofashionhair.service.SalaoInformacoesDto;
import br.com.jofashionhair.service.SalaoService;
import br.com.jofashionhair.service.admin.AdminDtos.HorariosFuncionamentoRequest;
import jakarta.validation.Valid;

/**
 * /api/admin/salao — dados do salão exibidos no site e usados pelo robô.
 * <pre>
 * GET/PUT /informacoes  nome, slogan, descrição, endereço, cidade, telefone, WhatsApp, Instagram, mapa
 * </pre>
 * Horário de funcionamento:
 * <pre>
 * GET  → os 7 dias
 * PUT  {"dias":[{"diaSemana":6,"fechado":false,"abertura":"08:00","fechamento":"16:00"},
 *               {"diaSemana":7,"fechado":true}]}
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/salao")
public class SalaoAdminController {

    private final SalaoService salaoService;

    public SalaoAdminController(SalaoService salaoService) {
        this.salaoService = salaoService;
    }

    @GetMapping("/horarios")
    public List<HorarioFuncionamentoDto> listar() {
        return salaoService.listarHorarios();
    }

    @PutMapping("/horarios")
    public List<HorarioFuncionamentoDto> definir(@Valid @RequestBody HorariosFuncionamentoRequest req) {
        return salaoService.definirHorarios(req.dias());
    }

    @GetMapping("/informacoes")
    public SalaoInformacoesDto informacoes() {
        return salaoService.informacoes();
    }

    @PutMapping("/informacoes")
    public SalaoInformacoesDto atualizarInformacoes(@Valid @RequestBody SalaoInformacoesDto dto) {
        return salaoService.atualizarInformacoes(dto);
    }
}
