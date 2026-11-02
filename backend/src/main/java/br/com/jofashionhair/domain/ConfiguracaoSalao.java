package br.com.jofashionhair.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Informações institucionais do salão (linha única, id = 1). */
@Entity
@Table(name = "configuracao_salao")
public class ConfiguracaoSalao {

    public static final short ID_UNICO = 1;

    @Id
    private Short id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 200)
    private String slogan;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(length = 250)
    private String endereco;

    @Column(length = 120)
    private String cidade;

    @Column(length = 20)
    private String telefone;

    @Column(length = 20)
    private String whatsapp;

    @Column(length = 60)
    private String instagram;

    @Column(name = "link_mapa", length = 500)
    private String linkMapa;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "foto_espaco_url", length = 500)
    private String fotoEspacoUrl;

    @Column(name = "foto_visita_url", length = 500)
    private String fotoVisitaUrl;

    /** Foto de fundo da tela principal (vazio = nome do salão ao fundo). */
    @Column(name = "foto_hero_url", length = 500)
    private String fotoHeroUrl;

    // ------------------------------------------------------------ sinal por Pix
    @Column(name = "chave_pix", length = 140)
    private String chavePix;

    @Column(name = "pix_nome_recebedor", length = 25)
    private String pixNomeRecebedor;

    @Column(name = "pix_cidade", length = 15)
    private String pixCidade;

    @Column(name = "sinal_prazo_minutos", nullable = false)
    private Integer sinalPrazoMinutos = 30;

    @Column(name = "avaliacoes_nota", precision = 2, scale = 1)
    private java.math.BigDecimal avaliacoesNota;

    @Column(name = "avaliacoes_total")
    private Integer avaliacoesTotal;

    @Column(name = "avaliacoes_link", length = 500)
    private String avaliacoesLink;

    @Column(name = "razao_social", length = 200)
    private String razaoSocial;

    @Column(length = 20)
    private String cnpj;

    @Column(name = "email_privacidade", length = 160)
    private String emailPrivacidade;

    @Column(name = "politica_privacidade", columnDefinition = "text")
    private String politicaPrivacidade;

    @Column(name = "retencao_meses", nullable = false)
    private Integer retencaoMeses = 24;

    /** Texto livre que a IA do WhatsApp usa para responder (pagamento, estacionamento...). */
    @Column(name = "info_atendimento", columnDefinition = "text")
    private String infoAtendimento;

    protected ConfiguracaoSalao() {
    }

    public void atualizarImagens(String logoUrl, String fotoEspacoUrl, String fotoVisitaUrl) {
        this.logoUrl = logoUrl;
        this.fotoEspacoUrl = fotoEspacoUrl;
        this.fotoVisitaUrl = fotoVisitaUrl;
    }

    public void atualizar(String nome, String slogan, String descricao, String endereco, String cidade,
                          String telefone, String whatsapp, String instagram, String linkMapa) {
        this.nome = nome;
        this.slogan = slogan;
        this.descricao = descricao;
        this.endereco = endereco;
        this.cidade = cidade;
        this.telefone = telefone;
        this.whatsapp = whatsapp;
        this.instagram = instagram;
        this.linkMapa = linkMapa;
    }

    public Short getId() { return id; }
    public String getNome() { return nome; }
    public String getSlogan() { return slogan; }
    public String getDescricao() { return descricao; }
    public String getEndereco() { return endereco; }
    public String getCidade() { return cidade; }
    public String getTelefone() { return telefone; }
    public String getWhatsapp() { return whatsapp; }
    public String getInstagram() { return instagram; }
    public String getLinkMapa() { return linkMapa; }
    public String getLogoUrl() { return logoUrl; }
    public String getFotoEspacoUrl() { return fotoEspacoUrl; }
    public String getFotoVisitaUrl() { return fotoVisitaUrl; }
    public String getFotoHeroUrl() { return fotoHeroUrl; }
    public void setFotoHeroUrl(String fotoHeroUrl) { this.fotoHeroUrl = fotoHeroUrl; }
    public String getChavePix() { return chavePix; }
    public String getPixNomeRecebedor() { return pixNomeRecebedor; }
    public String getPixCidade() { return pixCidade; }
    public Integer getSinalPrazoMinutos() { return sinalPrazoMinutos; }
    public void definirPix(String chave, String nome, String cidade, Integer prazoMinutos) {
        this.chavePix = chave;
        this.pixNomeRecebedor = nome;
        this.pixCidade = cidade;
        this.sinalPrazoMinutos = prazoMinutos == null ? 30 : prazoMinutos;
    }
    public String getRazaoSocial() { return razaoSocial; }
    public String getCnpj() { return cnpj; }
    public String getEmailPrivacidade() { return emailPrivacidade; }
    public String getPoliticaPrivacidade() { return politicaPrivacidade; }
    public Integer getRetencaoMeses() { return retencaoMeses; }
    public String getInfoAtendimento() { return infoAtendimento; }
    public void setInfoAtendimento(String infoAtendimento) { this.infoAtendimento = infoAtendimento; }

    public void atualizarPrivacidade(String razaoSocial, String cnpj, String emailPrivacidade,
                                     String politicaPrivacidade, Integer retencaoMeses) {
        this.razaoSocial = razaoSocial;
        this.cnpj = cnpj;
        this.emailPrivacidade = emailPrivacidade;
        this.politicaPrivacidade = politicaPrivacidade;
        this.retencaoMeses = retencaoMeses;
    }

    public java.math.BigDecimal getAvaliacoesNota() { return avaliacoesNota; }
    public Integer getAvaliacoesTotal() { return avaliacoesTotal; }
    public String getAvaliacoesLink() { return avaliacoesLink; }

    public void atualizarResumoAvaliacoes(java.math.BigDecimal nota, Integer total, String link) {
        this.avaliacoesNota = nota;
        this.avaliacoesTotal = total;
        this.avaliacoesLink = link;
    }
}
