/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.localization.internal;

import java.util.Collection;
import java.util.Locale;

import org.xwiki.localization.Bundle;
import org.xwiki.localization.BundleContext;
import org.xwiki.localization.Translation;
import org.xwiki.localization.message.TranslationMessage;
import org.xwiki.rendering.block.Block;

public class AbstractTranslation implements Translation
{
    private BundleContext context;

    private LocaleBundle localeBundle;

    private String key;

    private TranslationMessage message;

    public AbstractTranslation(BundleContext context, LocaleBundle localeBundle, String key, TranslationMessage message)
    {
        this.context = context;
        this.localeBundle = localeBundle;
        this.key = key;
        this.message = message;
    }

    @Override
    public Bundle getBundle()
    {
        return this.localeBundle.getBundle();
    }

    @Override
    public Locale getLocale()
    {
        return this.localeBundle.getLocale();
    }

    @Override
    public String getKey()
    {
        return this.key;
    }

    @Override
    public String getRawSource()
    {
        return this.message.getRawSource();
    }

    // Render

    @Override
    public Block render(Locale locale, Collection<Bundle> bundles, Object... parameters)
    {
        return this.message.render(locale != null ? locale : getLocale(), bundles, parameters);
    }

    private Collection<Bundle> getCurrentBundles()
    {
        return this.context != null ? this.context.getBundles() : null;
    }

    @Override
    public Block render(Object... parameters)
    {
        return render(getLocale(), getCurrentBundles(), parameters);
    }
}
