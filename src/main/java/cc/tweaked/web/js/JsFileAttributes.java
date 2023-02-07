package cc.tweaked.web.js;

import cc.tweaked.web.stub.BasicFileAttributes;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 * @see BasicFileAttributes
 * @see java.nio.file.attribute.BasicFileAttributes
 */
public interface JsFileAttributes extends JSObject {
    @JSProperty
    double getCreation();

    @JSProperty
    double getModification();

    @JSProperty
    boolean getDirectory();

    @JSProperty
    double getSize();
}
