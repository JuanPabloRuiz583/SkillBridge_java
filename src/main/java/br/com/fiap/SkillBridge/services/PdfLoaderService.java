package br.com.fiap.SkillBridge.services;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfLoaderService {

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public List<DocumentDto> loadAllFromClasspathDoc() {
        List<DocumentDto> docs = new ArrayList<>();
        try {
            Resource[] resources = resolver.getResources("classpath:doc/*.pdf");
            for (Resource res : resources) {
                try (InputStream is = res.getInputStream(); PDDocument doc = PDDocument.load(is)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    String text = stripper.getText(doc);
                    docs.add(new DocumentDto(res.getFilename(), text != null ? text : ""));
                } catch (Exception e) {
                    // trate/log conforme necessário
                }
            }
        } catch (Exception e) {
            // trate/log conforme necessário
        }
        return docs;
    }

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
    }
}