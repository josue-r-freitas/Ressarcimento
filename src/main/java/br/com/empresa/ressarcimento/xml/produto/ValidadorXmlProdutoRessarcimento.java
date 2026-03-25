package br.com.empresa.ressarcimento.xml.produto;

import java.io.IOException;
import java.io.StringReader;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

@Component
public class ValidadorXmlProdutoRessarcimento {

    private volatile Schema schemaCache;

    private Schema schema() {
        if (schemaCache == null) {
            synchronized (this) {
                if (schemaCache == null) {
                    try {
                        ClassPathResource res =
                                new ClassPathResource("schema/produto/enviProdutoRessarcimento_v1.00.xsd");
                        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                        // Não restringir ACCESS_EXTERNAL_SCHEMA: os xs:include relativos precisam ler leiaute/tipos no mesmo diretório.
                        schemaCache = factory.newSchema(res.getURL());
                    } catch (SAXException | IOException e) {
                        throw new IllegalStateException("Falha ao carregar XSD enviProdutoRessarcimento v1.00", e);
                    }
                }
            }
        }
        return schemaCache;
    }

    public void validar(String xml) {
        try {
            Validator validator = schema().newValidator();
            validator.validate(new StreamSource(new StringReader(xml)));
        } catch (SAXException e) {
            throw new IllegalArgumentException(
                    "XML gerado não confere ao XSD de produtos (enviProdutoRessarcimento v1.00): " + e.getMessage(),
                    e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
