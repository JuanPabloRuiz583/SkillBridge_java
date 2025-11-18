package br.com.fiap.SkillBridge.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço responsável por carregar e extrair texto dos PDFs
 * armazenados em src/main/resources/doc.
 *
 * É utilizado pela camada de IA (AIService) para buscar trechos
 * relevantes da documentação do projeto SkillBridge.
 */
@Service
public class PdfLoaderService {

    private static final Logger log = LoggerFactory.getLogger(PdfLoaderService.class);

    /**
     * Padrão de busca dos arquivos PDF dentro do classpath.
     * Ex.: src/main/resources/doc/*.pdf
     */
    private static final String PDF_GLOB_PATTERN = "classpath:doc/*.pdf";

    /**
     * Resolver para localizar recursos (arquivos) no classpath
     * usando padrões como "classpath:doc/*.pdf".
     */
    private final PathMatchingResourcePatternResolver resolver =
            new PathMatchingResourcePatternResolver();

    /**
     * Carrega todos os PDFs da pasta de documentação (doc/) no classpath
     * e retorna uma lista de DocumentDto contendo:
     *  - nome do arquivo
     *  - texto extraído via PDFBox
     *
     * Em caso de erro em um arquivo específico, apenas loga e continua
     * com os demais, para não quebrar o fluxo da aplicação.
     */
    public List<DocumentDto> loadAllFromClasspathDoc() {
        List<DocumentDto> docs = new ArrayList<>();

        try {
            Resource[] resources = resolver.getResources(PDF_GLOB_PATTERN);

            if (resources.length == 0) {
                log.warn("Nenhum PDF encontrado para o padrão: {}", PDF_GLOB_PATTERN);
                return docs;
            }

            log.info("Encontrados {} PDFs em '{}'", resources.length, PDF_GLOB_PATTERN);

            for (Resource res : resources) {
                String filename = res.getFilename() != null ? res.getFilename() : "desconhecido";

                if (!res.isReadable()) {
                    log.warn("Recurso PDF não legível: {}", filename);
                    continue;
                }

                try (InputStream is = res.getInputStream();
                     PDDocument doc = PDDocument.load(is)) {

                    PDFTextStripper stripper = new PDFTextStripper();
                    String text = stripper.getText(doc);

                    docs.add(new DocumentDto(filename, text));

                    log.debug("PDF carregado com sucesso: {}", filename);
                } catch (Exception e) {
                    // Loga e segue para o próximo arquivo
                    log.warn("Erro ao processar PDF '{}'", filename, e);
                }
            }
        } catch (Exception e) {
            // Falha "global" na resolução do padrão de recursos
            log.error("Erro ao localizar PDFs com o padrão: {}", PDF_GLOB_PATTERN, e);
        }

        return docs;
    }

    /**
     * DTO interno simples representando um documento PDF carregado.
     *
     * Contém:
     *  - name: nome do arquivo (ex: skillbridge-doc.pdf)
     *  - text: conteúdo textual extraído do PDF
     */
    public static class DocumentDto {
        private final String name;
        private final String text;

        public DocumentDto(String name, String text) {
            this.name = name;
            this.text = text != null ? text : "";
        }

        public String getName() {
            return name;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return "DocumentDto{name='" + name + '\'' +
                    ", textLength=" + (text != null ? text.length() : 0) +
                    '}';
        }
    }
}
