/*
 * Protex Plugin Integration
 * Copyright (C) 2015 Black Duck Software, Inc.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package com.blackducksoftware.protex.plugin.xml;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public abstract class ForwardingContentHandler implements ContentHandler {

    public static class SimpleForwardingContentHandler extends ForwardingContentHandler {
        private final ContentHandler delegate;

        public SimpleForwardingContentHandler(ContentHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        protected final ContentHandler delegate() {
            return delegate;
        }
    }

    protected abstract ContentHandler delegate();

    @Override
    public void setDocumentLocator(Locator locator) {
        delegate().setDocumentLocator(locator);
    }

    @Override
    public void startDocument() throws SAXException {
        delegate().startDocument();
    }

    @Override
    public void endDocument() throws SAXException {
        delegate().endDocument();
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        delegate().startPrefixMapping(prefix, uri);
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        delegate().endPrefixMapping(prefix);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        delegate().startElement(uri, localName, qName, atts);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        delegate().endElement(uri, localName, qName);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        delegate().characters(ch, start, length);
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        delegate().ignorableWhitespace(ch, start, length);
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        delegate().processingInstruction(target, data);
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        delegate().skippedEntity(name);
    }

}
