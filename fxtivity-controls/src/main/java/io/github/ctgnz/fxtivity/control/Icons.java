package io.github.ctgnz.fxtivity.control;

import java.io.IOException;
import java.io.InputStream;
import java.util.StringJoiner;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javafx.scene.layout.Region;
import javafx.scene.shape.SVGPath;

import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The controls' icons: single-colour SVG files on the classpath, beside this class under {@code icons/}, drawn as the shape of a {@link Region}.
 * <p>
 * Only each file's {@code <path>} data is read - which is all a single-colour icon is, including every FontAwesome icon. The region is filled with the text colour of whatever it
 * sits in, so an icon in a button follows the button's colour, and its look when disabled, from the stylesheet rather than from the file.
 */
final class Icons {

    /** The size an icon is drawn at, in pixels, whatever the size of its source. */
    static final double SIZE = 10;

    private Icons() {
    }

    /**
     * The icon in {@code icons/<name>.svg}.
     *
     * @param name
     *            the file's name, without {@code .svg}
     * @return a region drawing it
     * @throws IllegalArgumentException
     *             if there is no such icon, or it has no path to draw
     */
    static Region load(String name) {
        SVGPath shape = new SVGPath();
        shape.setContent(pathData(name));
        Region icon = new Region();
        icon.setShape(shape);
        icon.setScaleShape(true);
        icon.setMinSize(SIZE, SIZE);
        icon.setPrefSize(SIZE, SIZE);
        icon.setMaxSize(SIZE, SIZE);
        icon.getStyleClass().add("fxtivity-icon");
        icon.setStyle("-fx-background-color: -fx-text-base-color;");
        return icon;
    }

    private static String pathData(String name) {
        String resource = "icons/" + name + ".svg";
        try (InputStream in = Icons.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalArgumentException("No icon " + resource + " beside " + Icons.class.getName());
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // The icons are the library's own, but parse them as safely as any other XML: no DTDs, no external entities.
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setNamespaceAware(true);
            NodeList paths = factory.newDocumentBuilder().parse(in).getElementsByTagNameNS("*", "path");
            StringJoiner data = new StringJoiner(" ");
            for (int i = 0; i < paths.getLength(); i++) {
                String d = paths.item(i).getAttributes().getNamedItem("d") == null ? "" : paths.item(i).getAttributes().getNamedItem("d").getNodeValue();
                if (!d.isBlank()) {
                    data.add(d);
                }
            }
            if (data.length() == 0) {
                throw new IllegalArgumentException("Icon " + resource + " has no path to draw");
            }
            return data.toString();
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new IllegalArgumentException("Icon " + resource + " could not be read", e);
        }
    }

}
