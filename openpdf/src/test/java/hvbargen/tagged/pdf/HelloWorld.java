package hvbargen.tagged.pdf;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import com.lowagie.text.Document;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfDate;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfString;
import com.lowagie.text.pdf.PdfStructureElement;
import com.lowagie.text.pdf.PdfStructureTreeRoot;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.xml.xmp.DublinCoreSchema;
import com.lowagie.text.xml.xmp.LangAlt;
import com.lowagie.text.xml.xmp.PdfA1Schema;
import com.lowagie.text.xml.xmp.PdfSchema;
import com.lowagie.text.xml.xmp.XmpBasicSchema;
import com.lowagie.text.xml.xmp.XmpWriter;

/**
 * This file demonstrates how to create a PDF/UA document.
 * To keep it simple, the example only contains 3 paragraphs.
 */
public class HelloWorld {


    private static final PdfName ACTUAL_TEXT = new PdfName("ActualText");

    private static float x = 100f, y = 800f;


    /**
     * We need to override getXmlns because we have to define the pdfuaid namespace.
     */
    private static class DublinCoreAccessibleSchema extends DublinCoreSchema {
            
        public DublinCoreAccessibleSchema() {
            super();
        }

        @Override
        public String getXmlns() {
            return super.getXmlns() + " xmlns:pdfuaid=\"http://www.aiim.org/pdfua/ns/id/\"";
        }

        /**
         * This is what declares the document to be PDF/UA-1,
         * so it must be called.
         */
        public void addPdfUAId() {
            setProperty("pdfuaid:part", "1");
        }

    }


    /**
     * Create the XML for the XMPMetadata.
     * We use the same method from PdfWriter as a template
     * and add what is neeeded for PDF/UA.
     * @return an XmpMetadata byte array
     */
    private static byte[] createXmpMetadataBytes(PdfWriter writer) {
        PdfDictionary info = writer.getInfo();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {

            // We could declare the document to be PDF/A conformant.
            // Note: PDF/A is something completely different from PDF/UA.
            // But the same document can be PDF/A conformant and PDF/UA conformant.
            // Not tested.
            int PdfXConformance = writer.getPDFXConformance();
            XmpWriter xmp = new XmpWriter(baos, "UTF-8", 4);
            DublinCoreAccessibleSchema dc = new DublinCoreAccessibleSchema();
            PdfSchema p = new PdfSchema();
            XmpBasicSchema basic = new XmpBasicSchema();

            // Use the properties from the PDF info to define some XMPMetadata properties.
            PdfName key;
            PdfObject obj;
            for (PdfName pdfName : info.getKeys()) {
                key = pdfName;
                obj = info.get(key);
                if (obj == null)
                    continue;
                if (PdfName.TITLE.equals(key)) {
                    // The XMPMetadata allows defining the title for different languages. 
                    // We add the title in the default language
                    // and a german translation of the title.
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
            // Declare the document to be PDF/UA conformant.
            dc.addPdfUAId();

            if (dc.size() > 0) xmp.addRdfDescription(dc);
            if (p.size() > 0) xmp.addRdfDescription(p);
            if (basic.size() > 0) xmp.addRdfDescription(basic);

            // Declare the document to be PDF/A conformant, if requested by the developer.
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

        // We are using two different languages.
        // We need them in several places, so create PdfString instances that can be reused.
        PdfString us_english = new PdfString("en-US", PdfObject.TEXT_UNICODE);
        PdfString german = new PdfString("de-DE", PdfObject.TEXT_UNICODE);

        try {
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(outPath));
            // A PDF/UA document needs a title, it must be PDF 1.7, it must be "tagged",
            // and it must show the title instead of the file name in a viewer.
            // If the document has a main language, it makes sense to declare it, too.
            doc.addTitle("This is the title");
            doc.setDocumentLanguage(us_english.toString());
            writer.setPdfVersion(PdfWriter.VERSION_1_7);
            writer.setTagged();
            writer.setViewerPreferences(PdfWriter.DisplayDocTitle);
            doc.open();
            byte[] xmpMetadata = createXmpMetadataBytes(writer);
            writer.setXmpMetadata(xmpMetadata);

            // The StructureTree defines the logical structure of the content.
            PdfStructureTreeRoot root = writer.getStructureTreeRoot();
            PdfStructureElement document = new PdfStructureElement(root, new PdfName("Document"));
            
            // In order to declare the main language of the document,
            // we need to use the extraCatalog. That way we don't need to
            // modify existing OpenPDF source code.
            PdfDictionary extraCatalog = writer.getExtraCatalog();
            String lang = doc.getDocumentLanguage();
            extraCatalog.put(PdfName.LANG, new PdfString(lang, PdfObject.TEXT_UNICODE));
            PdfContentByte cb = writer.getDirectContent();

            // For PDF/A, all fonts must be embedded/subsetted.
            // This is not a requirement for PDF/UA AFAIK,
            // but it is good practice anyway.
            BaseFont bf = BaseFont.createFont("C:\\windows\\fonts\\arial.ttf", BaseFont.WINANSI, BaseFont.EMBEDDED);

            // When we create a structure element, we have to specify its parent element and its tag name.
            // The tag name P means "Paragraph".
            // For real world usage, it is important to use the tag that is semantically appropriate.
            PdfStructureElement p_en = new PdfStructureElement(document, PdfName.P);
            p_en.put(PdfName.LANG, us_english);
            cb.beginText();

            // Note how we link page content to structure elements:
            // I think this is working like a stack, here we push:
            cb.beginMarkedContentSequence(p_en);

            cb.setFontAndSize(bf, 12);
            cb.moveText(x, y);
            draw(cb, "This is some english text.");

            // And here we pop from the stack.
            cb.endMarkedContentSequence();

            cb.endText();

            // A second paragraph, marked as german.
            PdfStructureElement p_de = new PdfStructureElement(document, PdfName.P);
            p_de.put(PdfName.LANG, german);
            cb.beginText();
            cb.beginMarkedContentSequence(p_de);
            cb.setFontAndSize(bf, 12);
            cb.moveText(x, y);
            draw(cb, "Und hier ist ein Text für deutschsprachige Leser.");
            cb.endMarkedContentSequence();
            cb.endText();

            // Now we try some german text which spans two lines, with hyphenation.
            // TODO What is best practice here?
            // Should we create some kind of "span" which only contains the hyphenated word?
            // To keep things simple, we define ActualText for the whole paragraph.
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
