package hvbargen.tagged.pdf;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfDate;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfDocument;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfString;
import com.lowagie.text.pdf.PdfStructureElement;
import com.lowagie.text.pdf.PdfStructureTreeRoot;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfDocument.PdfInfo;
import com.lowagie.text.xml.xmp.DublinCoreSchema;
import com.lowagie.text.xml.xmp.LangAlt;
import com.lowagie.text.xml.xmp.PdfA1Schema;
import com.lowagie.text.xml.xmp.PdfSchema;
import com.lowagie.text.xml.xmp.XmpArray;
import com.lowagie.text.xml.xmp.XmpBasicSchema;
import com.lowagie.text.xml.xmp.XmpWriter;

public class HelloWorld {


    private static final PdfName ACTUAL_TEXT = new PdfName("ActualText");

    private static float x = 100f, y = 800f;


    private static class DublinCoreAccessibleSchema extends DublinCoreSchema {
            
        public DublinCoreAccessibleSchema() {
            super();
        }

        @Override
        public String getXmlns() {
            return super.getXmlns() + " xmlns:pdfuaid=\"http://www.aiim.org/pdfua/ns/id/\"";
        }

        public void addPdfUAId() {
            setProperty("pdfuaid:part", "1");
        }

    }


    /**
     * @return an XmpMetadata byte array
     */
    private static byte[] createXmpMetadataBytes(PdfWriter writer) {
        PdfDictionary info = writer.getInfo();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            int PdfXConformance = writer.getPDFXConformance();
            // XmpWriter xmp = new XmpWriter(baos, pdf.getInfo(), pdfxConformance.getPDFXConformance());
            XmpWriter xmp = new XmpWriter(baos, "UTF-8", 4);
            DublinCoreAccessibleSchema dc = new DublinCoreAccessibleSchema();
            PdfSchema p = new PdfSchema();
            XmpBasicSchema basic = new XmpBasicSchema();
            PdfName key;
            PdfObject obj;
            for (PdfName pdfName : info.getKeys()) {
                key = pdfName;
                obj = info.get(key);
                if (obj == null)
                    continue;
                if (PdfName.TITLE.equals(key)) {
                    LangAlt langAlt = new LangAlt(((PdfString) obj).toUnicodeString());
                    langAlt.addLanguage("de_DE", "Das ist der Titel für deutsche Leser");
                    dc.setProperty(DublinCoreSchema.TITLE, langAlt);
                }
                if (PdfName.AUTHOR.equals(key)) {
                    dc.addAuthor(((PdfString) obj).toUnicodeString());
                }
                if (PdfName.SUBJECT.equals(key)) {
                    dc.addSubject(((PdfString) obj).toUnicodeString());
                    dc.addDescription(((PdfString) obj).toUnicodeString());
                }
                if (PdfName.KEYWORDS.equals(key)) {
                    p.addKeywords(((PdfString) obj).toUnicodeString());
                }
                if (PdfName.CREATOR.equals(key)) {
                    basic.addCreatorTool(((PdfString) obj).toUnicodeString());
                }
                if (PdfName.PRODUCER.equals(key)) {
                    p.addProducer(((PdfString) obj).toUnicodeString());
                }
                if (PdfName.CREATIONDATE.equals(key)) {
                    basic.addCreateDate(((PdfDate) obj).getW3CDate());
                }
                if (PdfName.MODDATE.equals(key)) {
                    basic.addModDate(((PdfDate) obj).getW3CDate());
                }
            }
            dc.addPdfUAId();
            if (dc.size() > 0) xmp.addRdfDescription(dc);
            if (p.size() > 0) xmp.addRdfDescription(p);
            if (basic.size() > 0) xmp.addRdfDescription(basic);
            if (PdfXConformance == PdfWriter.PDFA1A || PdfXConformance == PdfWriter.PDFA1B) {
                PdfA1Schema a1 = new PdfA1Schema();
                if (PdfXConformance == PdfWriter.PDFA1A)
                    a1.addConformance("A");
                else
                    a1.addConformance("B");
                xmp.addRdfDescription(a1);
            }

            xmp.close();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return baos.toByteArray();
    }


    public static void main(String[] args)
    {
        String outPath = "C:\\TEMP\\simple-openpdf.pdf";
        Document doc = new Document();
        PdfString us_english = new PdfString("en-US", PdfObject.TEXT_UNICODE);
        PdfString german = new PdfString("de-DE", PdfObject.TEXT_UNICODE);

        try {
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(outPath));
            doc.addTitle("This is the title");
            doc.setDocumentLanguage(us_english.toString());
            writer.setPdfVersion(PdfWriter.VERSION_1_7);
            writer.setTagged();
            writer.setViewerPreferences(PdfWriter.DisplayDocTitle);
            doc.open();
            byte[] xmpMetadata = createXmpMetadataBytes(writer);
            writer.setXmpMetadata(xmpMetadata);
            PdfStructureTreeRoot root = writer.getStructureTreeRoot();
            PdfStructureElement document = new PdfStructureElement(root, new PdfName("Document"));
            PdfDictionary extraCatalog = writer.getExtraCatalog();
            String lang = doc.getDocumentLanguage();
            extraCatalog.put(PdfName.LANG, new PdfString(lang, PdfObject.TEXT_UNICODE));
            PdfContentByte cb = writer.getDirectContent();
            BaseFont bf = BaseFont.createFont("C:\\windows\\fonts\\arial.ttf", BaseFont.WINANSI, BaseFont.EMBEDDED);

            PdfStructureElement p_en = new PdfStructureElement(document, PdfName.P);
            p_en.put(PdfName.LANG, us_english);
            cb.beginText();
            cb.beginMarkedContentSequence(p_en);
            cb.setFontAndSize(bf, 12);
            cb.moveText(x, y);
            draw(cb, "This is some english text.");
            cb.endMarkedContentSequence();
            cb.endText();

            PdfStructureElement p_de = new PdfStructureElement(document, PdfName.P);
            p_de.put(PdfName.LANG, german);
            cb.beginText();
            cb.beginMarkedContentSequence(p_de);
            cb.setFontAndSize(bf, 12);
            cb.moveText(x, y);
            draw(cb, "Und hier ist ein Text für deutschsprachige Leser.");
            cb.endMarkedContentSequence();
            cb.endText();

            // Now we try some text which spans several lines,
            // with hyphenation.
            p_de = new PdfStructureElement(document, PdfName.P);
            p_de.put(PdfName.LANG, german);
            p_de.put(ACTUAL_TEXT, new PdfString("Das ist ein längerer Text mit dem schönen Wort Bundestagspräsident.", PdfObject.TEXT_UNICODE));
            cb.beginText();
            cb.beginMarkedContentSequence(p_de);
            cb.setFontAndSize(bf, 12);
            cb.setLeading(14.0f);
            cb.moveText(50, 200);
            cb.showText("Das ist ein längerer Text mit dem schönen Wort Bundestags-");
            cb.newlineShowText("präsident.");
            cb.endMarkedContentSequence();
            cb.endText();

            doc.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private static void draw(PdfContentByte cb, String text)
    {
        cb.showText(text);
        x += cb.getEffectiveStringWidth(text, false);
    }
   
}
