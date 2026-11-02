package br.com.jofashionhair.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jofashionhair.domain.HorarioFuncionamento;
import br.com.jofashionhair.repository.HorarioFuncionamentoRepository;

/** "O salão está aberto agora?" e "quando abre de novo?" (para o robô). */
@Service
@Transactional(readOnly = true)
public class FuncionamentoService {

    private static final String[] DIAS = {"", "na segunda", "na terça", "na quarta", "na quinta", "na sexta",
            "no sábado", "no domingo"};

    private final HorarioFuncionamentoRepository repository;
    private final RelogioSalao relogio;

    public FuncionamentoService(HorarioFuncionamentoRepository repository, RelogioSalao relogio) {
        this.repository = repository;
        this.relogio = relogio;
    }

    public boolean abertoAgora() {
        LocalDateTime agora = relogio.agora();
        HorarioFuncionamento h = horario(agora.getDayOfWeek());
        LocalTime t = agora.toLocalTime();
        return h != null && !h.isFechado() && h.getHoraAbertura() != null
                && !t.isBefore(h.getHoraAbertura()) && t.isBefore(h.getHoraFechamento());
    }

    /** "hoje às 09:00", "amanhã às 09:00", "na segunda às 09:00"; null se nunca abre. */
    public String proximaAbertura() {
        LocalDateTime agora = relogio.agora();
        HorarioFuncionamento hoje = horario(agora.getDayOfWeek());
        if (hoje != null && !hoje.isFechado() && agora.toLocalTime().isBefore(hoje.getHoraAbertura())) {
            return "hoje às " + hoje.getHoraAbertura();
        }
        for (int i = 1; i <= 7; i++) {
            DayOfWeek dia = agora.plusDays(i).getDayOfWeek();
            HorarioFuncionamento h = horario(dia);
            if (h != null && !h.isFechado()) {
                return (i == 1 ? "amanhã" : DIAS[dia.getValue()]) + " às " + h.getHoraAbertura();
            }
        }
        return null;
    }

    private HorarioFuncionamento horario(DayOfWeek dia) {
        return repository.findById((short) dia.getValue()).orElse(null);
    }
}
