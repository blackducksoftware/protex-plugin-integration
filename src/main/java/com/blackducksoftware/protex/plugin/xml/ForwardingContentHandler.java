/**
 * Protex Plugin Integration
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
