package br.com.jofashionhair.service;

import br.com.jofashionhair.domain.ConfiguracaoSalao;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Informações do salão exibidas no site (cabeçalho, "sobre", rodapé, contato). */
public record SalaoInformacoesDto(
        @NotBlank(message = "Informe o nome do salão.") @Size(max = 120) String nome,
        @Size(max = 200) String slogan,
        @Size(max = 5000) String descricao,
        @Size(max = 250) String endereco,
        @Size(max = 120) String cidade,
        @Size(max = 20) String telefone,
        @Size(max = 20) String whatsapp,
        @Size(max = 60) @Pattern(regexp = "^@?[A-Za-z0-9._]*$", message = "Instagram inválido.") String instagram,
        @Size(max = 500) @Pattern(regexp = "^$|^https://.*", message = "O link do mapa deve começar com https://")
        String linkMapa,
        @Size(max = 500) @Pattern(regexp = LINK_IMAGEM, message = "O logo deve ser um link (https://...).")
        String logoUrl,
        @Size(max = 500) @Pattern(regexp = LINK_IMAGEM, message = "A foto deve ser um link (https://...).")
        String fotoEspacoUrl,
        @Size(max = 500) @Pattern(regexp = LINK_IMAGEM, message = "A foto deve ser um link (https://...).")
        String fotoVisitaUrl,
        @Size(max = 200) String razaoSocial,
        @Size(max = 20) @Pattern(regexp = "^$|^[0-9./-]{14,18}$", message = "CNPJ inválido.") String cnpj,
        @Size(max = 160) @Email(message = "E-mail inválido.") String emailPrivacidade,
        @Size(max = 30000) String politicaPrivacidade,
        @Min(value = 6, message = "Mínimo de 6 meses.") @Max(value = 120, message = "Máximo de 120 meses.")
        Integer retencaoMeses,
        @Size(max = 8000) String infoAtendimento,
        @Size(max = 500) @Pattern(regexp = LINK_IMAGEM, message = "A foto deve ser um link (https://...).")
        String fotoHeroUrl,
        @Size(max = 140) String chavePix,
        @Size(max = 25, message = "Até 25 letras.") String pixNomeRecebedor,
        @Size(max = 15, message = "Até 15 letras.") String pixCidade,
        @Min(value = 10, message = "Mínimo de 10 minutos.") @Max(value = 1440, message = "Máximo de 24 horas.")
        Integer sinalPrazoMinutos) {

    /** Vazio, link http(s) ou caminho do próprio site (ex.: /fotos/espaco.jpg). */
    public static final String LINK_IMAGEM = "^$|^(https?://|/).*";

    public static SalaoInformacoesDto de(ConfiguracaoSalao c) {
        return new SalaoInformacoesDto(c.getNome(), c.getSlogan(), c.getDescricao(), c.getEndereco(),
                c.getCidade(), c.getTelefone(), c.getWhatsapp(), c.getInstagram(), c.getLinkMapa(),
                c.getLogoUrl(), c.getFotoEspacoUrl(), c.getFotoVisitaUrl(),
                c.getRazaoSocial(), c.getCnpj(), c.getEmailPrivacidade(), c.getPoliticaPrivacidade(),
                c.getRetencaoMeses(), c.getInfoAtendimento(), c.getFotoHeroUrl(),
                c.getChavePix(), c.getPixNomeRecebedor(), c.getPixCidade(), c.getSinalPrazoMinutos());
    }
}
