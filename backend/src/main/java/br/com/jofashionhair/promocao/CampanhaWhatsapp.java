package br.com.jofashionhair.promocao;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Um envio de promoção por WhatsApp (histórico e andamento). */
@Entity
@Table(name = "campanhas_whatsapp")
public class CampanhaWhatsapp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "promocao_id")
    private Long promocaoId;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, columnDefinition = "text")
    private String mensagem;

    @Column(nullable = false, length = 40)
    private String publico;

    @Column(nullable = false)
    private int total;

    @Column(nullable = false)
    private int enviados;

    @Column(nullable = false)
    private int falhas;

    @Column(nullable = false, length = 20)
    private String status = "ENVIANDO";

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "concluido_em")
    private OffsetDateTime concluidoEm;

    protected CampanhaWhatsapp() {
    }

    public CampanhaWhatsapp(Long promocaoId, String titulo, String mensagem, String publico, int total,
                            OffsetDateTime agora) {
        this.promocaoId = promocaoId;
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.publico = publico;
        this.total = total;
        this.criadoEm = agora;
    }

    public void registrar(boolean sucesso) {
        if (sucesso) enviados++; else falhas++;
    }

    public void concluir(OffsetDateTime agora) {
        status = "CONCLUIDA";
        concluidoEm = agora;
    }

    public Long getId() { return id; }
    public Long getPromocaoId() { return promocaoId; }
    public String getTitulo() { return titulo; }
    public String getMensagem() { return mensagem; }
    public String getPublico() { return publico; }
    public int getTotal() { return total; }
    public int getEnviados() { return enviados; }
    public int getFalhas() { return falhas; }
    public String getStatus() { return status; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public OffsetDateTime getConcluidoEm() { return concluidoEm; }
}
