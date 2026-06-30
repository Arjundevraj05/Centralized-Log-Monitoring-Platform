package com.logmonitor.util;

import com.logmonitor.exception.InvalidLogPathException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LogbackXmlParser {

    private static final Pattern DATE_TOKEN = Pattern.compile("%d\\{[^}]+}");

    public record ParsedLogback(String currentLogPath, String archivedPathPattern) {}

    public ParsedLogback parse(String logbackXml) {
        if (logbackXml == null || logbackXml.isBlank()) {
            throw new InvalidLogPathException("logback.xml is empty");
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);

            Document doc = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(logbackXml.getBytes(StandardCharsets.UTF_8)));

            String currentPath = firstText(doc, "file");
            String pattern = firstText(doc, "fileNamePattern");

            if (currentPath == null || currentPath.isBlank()) {
                throw new InvalidLogPathException("No <file> element found in logback.xml");
            }

            currentPath = currentPath.trim();
            if (pattern != null) {
                pattern = pattern.trim();
            }

            return new ParsedLogback(currentPath, pattern);
        } catch (InvalidLogPathException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidLogPathException("Failed to parse logback.xml: " + ex.getMessage());
        }
    }

    public String resolveArchivedPath(String pattern, String date) {
        if (pattern == null || pattern.isBlank()) {
            throw new InvalidLogPathException("No archived log pattern configured for this application");
        }
        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new InvalidLogPathException("Date must be YYYY-MM-DD");
        }
        Matcher matcher = DATE_TOKEN.matcher(pattern);
        if (!matcher.find()) {
            throw new InvalidLogPathException("Archived path pattern has no date token");
        }
        return matcher.replaceFirst(date);
    }

    private String firstText(Document doc, String tag) {
        NodeList nodes = doc.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return null;
        }
        return nodes.item(0).getTextContent();
    }
}
