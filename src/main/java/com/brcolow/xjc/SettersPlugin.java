/*
 * jaxb-inherit - (C) 2013 - J.W. Janssen &lt;j.w.janssen@lxtreme.nl&gt;.
 *
 * Licensed under the Apache-2.0 license.
 */
package com.brcolow.xjc;

import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldAccessor;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;

public class SettersPlugin extends Plugin {

    private static final JType[] ABSENT = new JType[0];

    @Override
    public String getOptionName() {
        return "Xsetters";
    }

    @Override
    public String getUsage() {
        return "  -Xsetters    :  Generates setters for collections.";

    }

    public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) {
        for (final ClassOutline classOutline : outline.getClasses()) {
            processClassOutline(classOutline);
        }
        return true;
    }

    protected void processClassOutline(ClassOutline classOutline) {
        final JDefinedClass theClass = classOutline.implClass;

        generateSetters(classOutline, theClass);

    }

    public enum Mode {

        accessor {
            @Override
            public void generateSetter(FieldOutline fieldOutline,
                                       JDefinedClass theClass, JMethod setter, JVar value) {
                final FieldAccessor accessor = fieldOutline.create(JExpr
                        ._this());
                accessor.unsetValues(setter.body());
                accessor.fromRawValue(setter.body()._if(value.ne(JExpr._null()))._then(), "draft", value);
            }
        },
        direct {
            @Override
            public void generateSetter(FieldOutline fieldOutline,
                                       JDefinedClass theClass, JMethod setter, JVar value) {

                final JFieldVar field = theClass.fields().get(
                        fieldOutline.getPropertyInfo().getName(false));

                if (field != null) {
                    setter.body().assign(JExpr._this().ref(field), value);
                } else {
                    // Fallback to the accessor
                    Mode.accessor.generateSetter(fieldOutline, theClass,
                            setter, value);
                }
            }
        };

        public abstract void generateSetter(FieldOutline fieldOutline,
                                            JDefinedClass theClass, JMethod setter, JVar value);
    }

    private final Mode mode = Mode.direct;

    private void generateSetters(ClassOutline classOutline,
                                 JDefinedClass theClass) {

        final FieldOutline[] declaredFields = classOutline.getDeclaredFields();
        for (final FieldOutline fieldOutline : declaredFields) {

            final String publicName = fieldOutline.getPropertyInfo().getName(
                    true);

            final String getterName = "get" + publicName;

            final JMethod getter = theClass.getMethod(getterName, ABSENT);

            if (getter != null) {
                final JType type = getter.type();
                final JType rawType = fieldOutline.getRawType();
                final String setterName = "set" + publicName;
                final JMethod boxifiedSetter = theClass.getMethod(setterName,
                        new JType[]{rawType.boxify()});
                final JMethod unboxifiedSetter = theClass.getMethod(setterName,
                        new JType[]{rawType.unboxify()});
                final JMethod setter = boxifiedSetter != null ? boxifiedSetter
                        : unboxifiedSetter;

                if (setter == null) {
                    final JMethod generatedSetter = theClass.method(
                            JMod.PUBLIC, theClass.owner().VOID, setterName);
                    final JVar value = generatedSetter.param(type, "value");

                    mode.generateSetter(fieldOutline, theClass,
                            generatedSetter, value);
                }
            }
        }
    }

}