package org.openrepose.filters.translation.httpx.processor.common;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public interface Element {
   void outputElement(ContentHandler handler) throws SAXException;
}
