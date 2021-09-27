/*
 * jaxb-inherit - (C) 2013 - J.W. Janssen &lt;j.w.janssen@lxtreme.nl&gt;.
 *
 * Licensed under the Apache-2.0 license.
 */
package com.brcolow.xjc;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JDefinedClass;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.model.CCustomizations;
import com.sun.tools.xjc.model.CElementInfo;
import com.sun.tools.xjc.model.CPluginCustomization;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.ElementOutline;
import com.sun.tools.xjc.outline.EnumOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import java.util.List;

public class InheritPlugin extends Plugin {
    private static final String NS_URI = "http://brcolow.com/xjc/inherit";
    private static final String EXTENDS = "extends";
    private static final String NAME = "name";

    @Override
    public String getOptionName() {
        return "Xinherit";
    }

    @Override
    public String getUsage() {
        return "  -Xinherit    :  xjc plugin that makes annotated class extend the given class";

    }

    @Override
    public List<String> getCustomizationURIs() {
        return List.of(NS_URI);
    }

    @Override
    public boolean isCustomizationTagName(String nsUri, String localName) {
        return NS_URI.equals(nsUri) && EXTENDS.equals(localName);
    }

    @Override
    public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws SAXException {
        for (ClassOutline classOutline : outline.getClasses()) {
            processClassOutline(classOutline);
        }
        for (EnumOutline enumOutline : outline.getEnums()) {
            processEnumOutline(enumOutline);
        }
        for (CElementInfo elementInfo : outline.getModel().getAllElements()) {
            ElementOutline elementOutline = outline.getElement(elementInfo);
            if (elementOutline != null) {
                processElementOutline(elementOutline);
            }
        }
        return true;
    }

    private void processClassOutline(ClassOutline classOutline) {
        JDefinedClass implClass = classOutline.implClass;
        CCustomizations customizations = classOutline.target.getCustomizations();

        generateExtends(implClass, findExtendsCustomization(customizations));
    }

    private void processElementOutline(ElementOutline elementOutline) {
        JDefinedClass implClass = elementOutline.implClass;
        CCustomizations customizations = elementOutline.target.getCustomizations();

        generateExtends(implClass, findExtendsCustomization(customizations));
    }

    private void processEnumOutline(EnumOutline enumOutline) {
        JDefinedClass implClass = enumOutline.clazz;
        CCustomizations customizations = enumOutline.target.getCustomizations();

        generateExtends(implClass, findExtendsCustomization(customizations));
    }

    private void generateExtends(JDefinedClass implClass, CPluginCustomization customization) {
        if (customization == null) {
            return;
        }

        String className = customization.element.getAttribute(NAME);
        if (className != null) {
            JClass iface = implClass.owner().ref(className);

            implClass._extends(iface);
        }
    }

    private boolean isNullOrEmpty(String value) {
        return value == null || "".equals(value.trim());
    }

    private CPluginCustomization findExtendsCustomization(CCustomizations customizations) {
        CPluginCustomization result = customizations.find(NS_URI, EXTENDS);
        if (result != null && !isNullOrEmpty(result.element.getAttribute(NAME))) {
            result.markAsAcknowledged();
        }
        return result;
    }
}
