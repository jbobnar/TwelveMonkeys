/*
 * Copyright (c) 2014, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.nef;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;

import com.twelvemonkeys.imageio.metadata.CompoundDirectory;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.exif.EXIFReader;
import com.twelvemonkeys.imageio.metadata.exif.TIFF;
import com.twelvemonkeys.imageio.spi.ProviderInfo;

/**
 * CR2ImageReaderSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: CR2ImageReaderSpi.java,v 1.0 07.04.14 21:26 haraldk Exp$
 */
public final class NEFImageReaderSpi extends ImageReaderSpi {
    public NEFImageReaderSpi() {
        this(new ProviderInfo(NEFImageReaderSpi.class.getPackage()));
    }

    private NEFImageReaderSpi(final ProviderInfo pProviderInfo) {
        super(pProviderInfo.getVendorName(), pProviderInfo.getVersion(), new String[] { "nef", "NEF" },
                new String[] { "nef" }, new String[] { "image/x-nikon-nef", // TODO: Look up
        }, "com.twelvemonkeys.imageio.plugins.nef.NEFImageReader", new Class[] { ImageInputStream.class }, null, true,
                null, null, null, null, true, null, null, null, null);
    }

    public boolean canDecodeInput(final Object pSource) throws IOException {
        if (!(pSource instanceof ImageInputStream)) {
            return false;
        }

        ImageInputStream stream = (ImageInputStream) pSource;

        stream.mark();
        ByteOrder originalOrder = stream.getByteOrder();
        try {
            Directory dir = (CompoundDirectory) new EXIFReader().read(stream); // NOTE: Sets byte order as a side effect
            Iterator<Entry> it = dir.iterator();
            boolean makePassed = false;
            boolean modelPassed = false;
            while(it.hasNext()) {
                Entry e = it.next();
                if ((Integer)e.getIdentifier() == TIFF.TAG_MAKE) {
                    String make = e.getValueAsString();
                    if (make == null || !make.toLowerCase(Locale.UK).contains("nikon")) {
                        return false;
                    }
                    makePassed = true;
                    if (modelPassed) return true;
                }
                if ((Integer)e.getIdentifier() == TIFF.TAG_MODEL) {
                    String make = e.getValueAsString();
                    if (make == null || !make.toLowerCase(Locale.UK).contains("nikon")) {
                        return false;
                    }
                    modelPassed = true;
                    if (makePassed) return true;
                }
            }
            return false;
        } finally {
            stream.setByteOrder(originalOrder);
            stream.reset();
        }
    }

    @Override
    public ImageReader createReaderInstance(Object extension) throws IOException {
        return new NEFImageReader(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "Adobe Digital Negative (DNG) format Reader";
    }

    @Override
    public void onDeregistration(ServiceRegistry registry, Class<?> category) {
        super.onDeregistration(registry, category);
        Iterator<ImageReaderSpi> spi = registry.getServiceProviders((Class<ImageReaderSpi>) category, true);
        ImageReaderSpi irs;
        while (spi.hasNext()) {
            irs = spi.next();
            if (irs.getClass().getName().startsWith("it.tidalwave")) {
                registry.setOrdering((Class<ImageReaderSpi>) category, irs, this);
            }
        }
    }
}
