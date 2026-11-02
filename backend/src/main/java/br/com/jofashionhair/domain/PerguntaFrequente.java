package br.com.jofashionhair.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Pergunta frequente que o robô do WhatsApp responde pelas palavras-chave. */
@Entity
@Table(name = "perguntas_frequentes")
public class PerguntaFrequente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String pergunta;

    /** Separadas por vírgula. Ex.: "pix, cartão, pagamento". */
    @Column(name = "palavras_chave", nullable = false, columnDefinition = "text")
    private String palavrasChave;

    @Column(nullable = false, columnDefinition = "text")
    private String resposta;

    @Column(nullable = false)
    private Integer ordem = 0;

    @Column(nullable = false)
    private boolean ativo = true;

    public PerguntaFrequente() {
    }

    public void atualizar(String pergunta, String palavrasChave, String resposta, int ordem, boolean ativo) {
        this.pergunta = pergunta;
        this.palavrasChave = palavrasChave;
        this.resposta = resposta;
        this.ordem = ordem;
        this.ativo = ativo;
    }

    public Long getId() { return id; }
    public String getPergunta() { return pergunta; }
    public String getPalavrasChave() { return palavrasChave; }
    public String getResposta() { return resposta; }
    public Integer getOrdem() { return ordem; }
    public boolean isAtivo() { return ativo; }
}
