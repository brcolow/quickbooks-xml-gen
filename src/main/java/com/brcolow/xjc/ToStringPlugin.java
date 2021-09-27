/*
 * Copyright (c) 2005-2014, Alexey Valikov.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
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
package com.brcolow.xjc;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;

import java.util.Iterator;
import java.util.Map;

import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;

public class ToStringPlugin extends Plugin {
    @Override
    public String getOptionName() {
        return "Xtostring";
    }

    @Override
    public String getUsage() {
        return "  -Xtostring    :  xjc plugin that generates tostring implementation";
    }

    @Override
    public boolean run(Outline outline, Options options, ErrorHandler errorHandler) {
        JCodeModel m = new JCodeModel();
        for (ClassOutline classOutline : outline.getClasses()) {
            JMethod method = classOutline.implClass.method(PUBLIC, String.class, "toString");
            method.annotate(Override.class);
            JBlock body = method.body();
            JClass stringBuilderType = m.ref(StringBuilder.class);
            JVar stringBuilder = body.decl(0, stringBuilderType, "toStringBuilder", JExpr._new(stringBuilderType));

            body.invoke(stringBuilder, "append").arg(classOutline.implClass.name() + " [");
            Iterator<Map.Entry<String, JFieldVar>> fieldItr = classOutline.implClass.fields().entrySet().iterator();
            while (fieldItr.hasNext()) {
                Map.Entry<String, JFieldVar> fieldEntry = fieldItr.next();
                JFieldVar fieldValue = fieldEntry.getValue();
                // Skip null (Object) fields to reduce verbosity of XML structures.
                JBlock ifNotNullForObject;
                if (fieldValue.type().isPrimitive()) {
                    ifNotNullForObject = body;
                } else {
                    ifNotNullForObject = body._if(fieldValue.ne((JExpr._null())))._then();
                }
                if ((fieldValue.mods().getValue() & STATIC) == 0) {
                    ifNotNullForObject.invoke(stringBuilder, "append").arg(fieldEntry.getKey() + " = ");
                    ifNotNullForObject.invoke(stringBuilder, "append").arg(fieldValue);
                    if (fieldItr.hasNext()) {
                        ifNotNullForObject.invoke(stringBuilder, "append").arg(",\n");
                    }
                }

            }
            body.invoke(stringBuilder, "append").arg("]");
            body._return(JExpr.invoke(stringBuilder, "toString"));
        }
        return true;
    }
}
