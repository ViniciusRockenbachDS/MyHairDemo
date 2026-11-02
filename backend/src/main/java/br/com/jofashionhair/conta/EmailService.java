package br.com.jofashionhair.conta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import br.com.jofashionhair.config.AppProperties;

/**
 * Envia e-mails pelo SMTP configurado (ex.: e-mail da Hostinger).
 * Em segundo plano (@Async): a resposta da tela não espera o e-mail, e o
 * tempo de resposta não revela se um e-mail está cadastrado ou não.
 * Com app.email.enviar=false (dev), o e-mail só aparece no log da API.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final AppProperties.Email config;

    public EmailService(ObjectProvider<JavaMailSender> mailSender, AppProperties props) {
        this.mailSender = mailSender;
        this.config = props.email();
    }

    @Async
    public void enviar(String para, String assunto, String texto) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (config == null || !config.enviar() || sender == null) {
            log.info("[E-MAIL DESLIGADO] para {} | {}\n{}", para, assunto, texto);
            return;
        }
        try {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setFrom(config.remetente());
            mensagem.setTo(para);
            mensagem.setSubject(assunto);
            mensagem.setText(texto);
            sender.send(mensagem);
        } catch (MailException e) {
            log.error("Falha ao enviar e-mail para {}: {}", para, e.getMessage());
        }
    }
}
