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
 *
 */
package org.xwiki.xml.internal.html;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.CleanerTransformations;
import org.htmlcleaner.ContentToken;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.JDomSerializer;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.TagTransformation;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.output.DOMOutputter;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.html.filter.CleaningFilter;

/**
 * Default implementation for {@link org.xwiki.xml.html.HTMLCleaner} using the <a href="HTML Cleaner
 * framework>http://htmlcleaner.sourceforge.net/</a>.
 * 
 * @version $Id: $
 * @since 1.6M1
 */
public class DefaultHTMLCleaner implements HTMLCleaner, Initializable
{
    /**
     * List of default cleaning filters to call when cleaning code with HTML Cleaner. This is for cases when there are
     * no <a href="http://htmlcleaner.sourceforge.net/parameters.php">properties</a> defined in HTML Cleaner.
     */
    private List<CleaningFilter> filters;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        // The clean method below is thread safe. However it seems that DOMOutputter.output() is not fully thread safe
        // since it causes the following exception on the first time it's called from different threads:
        //  Caused by: org.jdom.JDOMException: Reflection failed while creating new JAXP document:
        //  duplicate class definition: org/apache/xerces/jaxp/DocumentBuilderFactoryImpl
        //  at org.jdom.adapters.JAXPDOMAdapter.createDocument(JAXPDOMAdapter.java:191)
        //  at org.jdom.adapters.AbstractDOMAdapter.createDocument(AbstractDOMAdapter.java:133)
        //  at org.jdom.output.DOMOutputter.createDOMDocument(DOMOutputter.java:208)
        //  at org.jdom.output.DOMOutputter.output(DOMOutputter.java:127)
        // Since this only happens once, we call it first here at initialization time (since there's no thread
        // contention at that time). Note: This email thread seems to say it's thread safe but that's not what we see
        // here: http:osdir.com/ml/text.xml.xforms.chiba.devel/2006-09/msg00025.html
        clean(new StringReader(""));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.xml.html.HTMLCleaner#clean(String)
     */
    public org.w3c.dom.Document clean(Reader originalHtmlContent)
    {                
        return clean(originalHtmlContent, getDefaultCleanerProperties(), getDefaultCleanerTransformations());
    }

    /**
     * {@inheritDoc}
     * <p>
     * {@link DefaultHTMLCleaner} supports following cleaning parameters:<br/>
     * 'namespacesAware' : if set to 'true' namespace information will be preserved during cleaning.<br/>
     * </p>
     */
    public org.w3c.dom.Document clean(Reader originalHtmlContent, Map<String, String> cleaningParameters)
    {
        CleanerProperties cleanerProperties = getDefaultCleanerProperties();
        String param = cleaningParameters.get(NAMESPACES_AWARE);
        boolean namespacesAware = param != null ? param.equals(TRUE) : cleanerProperties.isNamespacesAware();
        cleanerProperties.setNamespacesAware(namespacesAware);
        return clean(originalHtmlContent, cleanerProperties, getDefaultCleanerTransformations());
    }

    /**
     * Cleans the given html content with supplied {@link CleanerProperties} and {@link CleanerTransformations}.
     * 
     * @param originalHtmlContent original html content.
     * @param cleanerProperties {@link CleanerProperties} to be used for cleaning.
     * @param cleanerTransformations {@link CleanerTransformations} to be used when cleaning.
     * @return the cleaned html as a {@link org.w3c.dom.Document}.
     */
    private org.w3c.dom.Document clean(Reader originalHtmlContent, CleanerProperties cleanerProperties,
        CleanerTransformations cleanerTransformations) 
    {
        org.w3c.dom.Document result;
        // HtmlCleaner is not threadsafe. Thus we need to recreate an instance at each run since otherwise we would need
        // to synchronize this clean() method which would slow down the whole system by queuing up cleaning requests.
        // See http://sourceforge.net/tracker/index.php?func=detail&aid=2139927&group_id=183053&atid=903699
        HtmlCleaner cleaner = new HtmlCleaner(cleanerProperties);
        cleaner.setTransformations(cleanerTransformations);        
        TagNode cleanedNode;
        try {
            cleanedNode = cleaner.clean(originalHtmlContent);
        } catch (Exception e) {
            // This shouldn't happen since we're not doing any IO... I consider this a flaw in the design of HTML
            // Cleaner.
            throw new RuntimeException("Unhandled error when cleaning HTML", e);
        }
        // Fix cleaned node bug
        fixCleanedNodeBug(cleanedNode);
        Document document = new JDomSerializer(cleanerProperties, false).createJDom(cleanedNode);
        // Perform other cleaning operation this time using the W3C Document interface.
        for (CleaningFilter filter : this.filters) {
            filter.filter(document);
        }
        try {
            DOMOutputter outputter = new DOMOutputter();
            result = outputter.output(document);
        } catch (JDOMException e) {
            throw new RuntimeException("Failed to convert JDOM Document to W3C Document", e);
        }
        return result;        
    }
    
    /**
     * @return the default {@link CleanerProperties} to be used for cleaning.
     */
    private CleanerProperties getDefaultCleanerProperties()
    {
        CleanerProperties defaultProperties = new CleanerProperties();
        defaultProperties.setOmitUnknownTags(true);
        // By default HTMLCleaner treats style and script tags as CDATA. This is causing errors if we use the best
        // practice of using CDATA inside a script. For example:
        //  <script type="text/javascript">
        //  <![CDATA[
        //  ...
        //  ]]>
        //  </script>
        // Thus we need to turn off this feature.
        defaultProperties.setUseCdataForScriptAndStyle(false);        
        return defaultProperties;
    }
    
    /**
     * @return the default cleaning transformations to perform on tags, in addition to the base transformations done
     *         by HTML Cleaner
     */
    private CleanerTransformations getDefaultCleanerTransformations()
    {
        CleanerTransformations defaultTransformations = new CleanerTransformations();

        TagTransformation tt = new TagTransformation(HTMLConstants.B, HTMLConstants.STRONG, false);
        defaultTransformations.addTransformation(tt);

        tt = new TagTransformation(HTMLConstants.I, HTMLConstants.EM, false);
        defaultTransformations.addTransformation(tt);

        tt = new TagTransformation(HTMLConstants.U, HTMLConstants.INS, false);
        defaultTransformations.addTransformation(tt);

        tt = new TagTransformation(HTMLConstants.S, HTMLConstants.DEL, false);
        defaultTransformations.addTransformation(tt);

        tt = new TagTransformation(HTMLConstants.STRIKE, HTMLConstants.DEL, false);
        defaultTransformations.addTransformation(tt);

        tt = new TagTransformation(HTMLConstants.CENTER, HTMLConstants.P, false);
        tt.addAttributeTransformation(HTMLConstants.STYLE_ATTRIBUTE, "text-align:center");
        defaultTransformations.addTransformation(tt);

        tt = new TagTransformation(HTMLConstants.FONT, HTMLConstants.SPAN, false);
        tt.addAttributeTransformation(HTMLConstants.STYLE_ATTRIBUTE,
            "color:${color};font-family=${face};font-size=${size}pt;");
        defaultTransformations.addTransformation(tt);

        return defaultTransformations;
    }

    /**
     * There's a known limitation (bug?) in HTML Cleaner where if there's a XML declaration specified it'll be copied as
     * the first element of the body. Thus remove it if it's there. See
     * https://sourceforge.net/forum/message.php?msg_id=4657800
     * 
     * @param cleanedNode the cleaned node (ie after the HTML cleaning)
     */
    private void fixCleanedNodeBug(TagNode cleanedNode)
    {
        TagNode body = cleanedNode.getElementsByName("body", false)[0];
        if (body.getChildren().size() > 0) {
            Object firstBodyChild = body.getChildren().get(0);
            if (firstBodyChild != null && ContentToken.class.isAssignableFrom(firstBodyChild.getClass())) {
                ContentToken token = (ContentToken) firstBodyChild;
                if (token.getContent().startsWith("<?xml")) {
                    body.removeChild(token);
                }
            }
        }
    }
}
