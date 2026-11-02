package br.com.jofashionhair.web.admin;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.jofashionhair.privacidade.ExportacaoCliente;
import br.com.jofashionhair.privacidade.PrivacidadeService;
import br.com.jofashionhair.repository.HistoricoCliente;
import br.com.jofashionhair.service.AgendamentoResumo;
import br.com.jofashionhair.service.admin.ClienteAdminService;

/**
 * /api/admin/clientes
 * <pre>
 * GET /?busca=maria               nome ou telefone, com total de faltas
 * GET /{id}/agendamentos          histórico completo do cliente
 * GET /{id}/exportar              LGPD: arquivo com todos os dados da cliente
 * DELETE /{id}                    LGPD: exclui a cliente e todo o histórico (sem volta)
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/clientes")
public class ClienteAdminController {

    private final ClienteAdminService service;
    private final PrivacidadeService privacidade;

    public ClienteAdminController(ClienteAdminService service, PrivacidadeService privacidade) {
        this.service = service;
        this.privacidade = privacidade;
    }

    @GetMapping("/{id}/exportar")
    public ResponseEntity<ExportacaoCliente> exportar(@PathVariable Long id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cliente-" + id + ".json\"")
                .body(privacidade.exportar(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        privacidade.excluir(id);
    }

    @GetMapping
    public List<HistoricoCliente> buscar(@RequestParam(required = false) String busca) {
        return service.buscar(busca);
    }

    @GetMapping("/{id}/agendamentos")
    public List<AgendamentoResumo> historico(@PathVariable Long id) {
        return service.agendamentosDoCliente(id);
    }
}
