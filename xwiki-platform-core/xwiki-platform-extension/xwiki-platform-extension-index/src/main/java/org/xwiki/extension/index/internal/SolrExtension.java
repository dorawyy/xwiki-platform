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
package org.xwiki.extension.index.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionAuthor;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.RemoteExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.wrap.AbstractWrappingExtension;

/**
 * An extension stored in the Solr based index of extensions.
 * 
 * @version $Id$
 * @since 12.9RC1
 */
public class SolrExtension extends AbstractWrappingExtension<Extension> implements RemoteExtension
{
    private final ExtensionRepository repository;

    private final ExtensionId extensionId;

    private List<Version> versions;

    private Date indexDate;

    /**
     * @param repository the repository where this extension comes from
     * @param extensionId the extension identifier
     */
    public SolrExtension(ExtensionRepository repository, ExtensionId extensionId)
    {
        this.repository = repository;
        this.extensionId = extensionId;
    }

    @Override
    protected Extension resolveWrapped()
    {
        try {
            return this.repository.resolve(this.extensionId);
        } catch (ResolveException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    // Extension

    @Override
    public ExtensionId getId()
    {
        return this.extensionId;
    }

    @Override
    public ExtensionRepository getRepository()
    {
        return this.repository;
    }

    /**
     * @param name the display name of the extension
     */
    public void setName(String name)
    {
        this.overwrites.put(FIELD_NAME, name);
    }

    /**
     * @param type the type to set
     */
    public void setType(String type)
    {
        this.overwrites.put(FIELD_TYPE, type);
    }

    /**
     * @param summary a short description of the extension
     */
    public void setSummary(String summary)
    {
        this.overwrites.put(FIELD_SUMMARY, summary);
    }

    /**
     * @param website an URL for the extension website
     */
    public void setWebsite(String website)
    {
        this.overwrites.put(FIELD_WEBSITE, website);
    }

    /**
     * @param categrory the category of the extension;
     */
    public void setCategory(String categrory)
    {
        this.overwrites.put(FIELD_CATEGORY, categrory);
    }

    /**
     * @param namespaces the namespaces where it's allowed to install this extension
     */
    public void setAllowedNamespaces(Collection<String> namespaces)
    {
        this.overwrites.put(FIELD_ALLOWEDNAMESPACES,
            namespaces != null ? Collections.unmodifiableCollection(namespaces) : null);
    }

    /**
     * @param features the {@link ExtensionId}s also provided by this extension
     */
    public void setExtensionFeatures(Collection<ExtensionId> features)
    {
        this.overwrites.put(FIELD_EXTENSIONFEATURES,
            features != null ? Collections.unmodifiableCollection(features) : null);
    }

    /**
     * @param authors the authors of the extension
     */
    public void setAuthors(Collection<? extends ExtensionAuthor> authors)
    {
        this.overwrites.put(FIELD_AUTHORS, authors);
    }

    // RemoteExtension

    @Override
    public boolean isRecommended()
    {
        if (this.overwrites.containsKey(FIELD_RECOMMENDED)) {
            return (boolean) this.overwrites.get(FIELD_RECOMMENDED);
        }

        return getWrapped() instanceof RemoteExtension && ((RemoteExtension) getWrapped()).isRecommended();
    }

    /**
     * @param recommended true if the extension is recommended
     * @see #isRecommended()
     */
    public void setRecommended(boolean recommended)
    {
        this.overwrites.put(FIELD_RECOMMENDED, recommended);
    }

    // SolrExtension

    /**
     * @return the versions available for the extension
     */
    public List<Version> getVersions()
    {
        return this.versions;
    }

    /**
     * @return the date at which the extension was indexed
     */
    public Date getIndexDate()
    {
        return this.indexDate;
    }
}
