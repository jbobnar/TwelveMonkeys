/*
 * Copyright (c) 2012, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.tiff;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;

import com.twelvemonkeys.imageio.metadata.exif.TIFF;
import com.twelvemonkeys.imageio.spi.ImageReaderSpiBase;

import static com.twelvemonkeys.imageio.util.IIOUtil.lookupProviderByName;

/**
 * TIFFImageReaderSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFFImageReaderSpi.java,v 1.0 08.05.12 15:14 haraldk Exp$
 */
public final class TIFFImageReaderSpi extends ImageReaderSpiBase {
    /**
     * Creates a {@code TIFFImageReaderSpi}.
     */
    @SuppressWarnings("WeakerAccess")
    public TIFFImageReaderSpi() {
        super(new TIFFProviderInfo());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onRegistration(final ServiceRegistry registry, final Class<?> category) {
        // Make sure we're ordered before the new JEP 262 JRE bundled TIFF plugin
        // or the Apple-provided TIFF plugin on OS X (which both happen to have the same class name)...
        ImageReaderSpi sunSpi = lookupProviderByName(registry, "com.sun.imageio.plugins.tiff.TIFFImageReaderSpi", ImageReaderSpi.class);

        if (sunSpi != null && sunSpi.getVendorName() != null
                && (sunSpi.getVendorName().startsWith("Apple") || sunSpi.getVendorName().startsWith("Oracle"))) {
            registry.setOrdering((Class<ImageReaderSpi>) category, this, sunSpi);
        }

        moveForward(registry, category, "com.twelvemonkeys.imageio.plugins.cr2.CR2ImageReaderSpi");
//        moveForward(registry, category, "com.twelvemonkeys.imageio.plugins.dng.DNGImageReaderSpi");
        moveForward(registry, category, "com.twelvemonkeys.imageio.plugins.nef.NEFImageReaderSpi");
        Iterator<ImageReaderSpi> spi = registry.getServiceProviders((Class<ImageReaderSpi>)category, true);
        ImageReaderSpi irs;
        while(spi.hasNext()) {
            irs = spi.next();
            if (irs.getClass().getName().startsWith("it.tidalwave")) {
                registry.setOrdering((Class<ImageReaderSpi>) category, irs, this);
            }
        }
    }

    private void moveForward(ServiceRegistry registry, Class<?> category, String spiClass) {
        try {
            Class<ImageReaderSpi> providerClass = (Class<ImageReaderSpi>) Class.forName(spiClass);
            ImageReaderSpi spi = registry.getServiceProviderByClass(providerClass);

            if (spi != null) {
                registry.setOrdering((Class<ImageReaderSpi>) category, spi, this);
            }
        }
        catch (ClassNotFoundException ignore) {
            // This is actually OK, now we don't have to do anything
        }
    }

    public boolean canDecodeInput(final Object pSource) throws IOException {
        return canDecodeAs(pSource, TIFF.TIFF_MAGIC);
    }

    static boolean canDecodeAs(final Object pSource, final int magic) throws IOException {
        if (!(pSource instanceof ImageInputStream)) {
            return false;
        }

        ImageInputStream stream = (ImageInputStream) pSource;

        stream.mark();
        try {
            byte[] bom = new byte[2];
            stream.readFully(bom);

            ByteOrder originalOrder = stream.getByteOrder();

            try {
                if (bom[0] == 'I' && bom[1] == 'I') {
                    stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
                }
                else if (bom[0] == 'M' && bom[1] == 'M') {
                    stream.setByteOrder(ByteOrder.BIG_ENDIAN);
                }
                else  {
                    return false;
                }

                return stream.readUnsignedShort() == magic;
            }
            finally {
                stream.setByteOrder(originalOrder);
            }
        }
        finally {
            stream.reset();
        }
    }

    public TIFFImageReader createReaderInstance(final Object pExtension) {
        return new TIFFImageReader(this);
    }

    public String getDescription(final Locale pLocale) {
        return "Aldus/Adobe Tagged Image File Format (TIFF) image reader";
    }
}
