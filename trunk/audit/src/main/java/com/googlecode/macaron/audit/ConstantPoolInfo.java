/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html or
 * glassfish/bootstrap/legal/CDDLv1.0.txt.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at glassfish/bootstrap/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 */
package com.googlecode.macaron.audit;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 * @author dochez
 */
// See http://google.com/codesearch/p?hl=fr#h0n4vxU5-X4/glassfish/appserv-commons/src/java/com/sun/enterprise/deployment/annotation/introspection/ConstantPoolInfo.java&q=glassfish%20AnnotationProcessor
public class ConstantPoolInfo {

    byte[] bytes = new byte[Short.MAX_VALUE];
    private CustomAnnotationScanner customScanner = null;

    /** Creates a new instance of ConstantPoolInfo */
    public ConstantPoolInfo() {
    }

    public ConstantPoolInfo(CustomAnnotationScanner scanner) {
        customScanner = scanner;
    }

    /**
     * Read the input channel and initialize instance data
     * structure.
     */
    public boolean containsAnnotation(int constantPoolSize, final ByteBuffer buffer) throws IOException {

        for (int i=1;i<constantPoolSize;i++) {
            final byte type = buffer.get();
            switch(type) {
                case ASCIZ:
                case UNICODE:
                    final short length = buffer.getShort();
                    if (length<0 || length>Short.MAX_VALUE) {
                        return true;
                    }
                    buffer.get(bytes, 0, length);
                    /* to speed up the process, I am comparing the first few
                     * bytes to Ljava since all annotations are in the java
                     * package, the reduces dramatically the number or String
                     * construction
                     */
                    if (bytes[0]=='L' && bytes[1]=='j' && bytes[2]=='a') {
                        String stringValue;
                        if (type==ASCIZ) {
                            stringValue = new String(bytes, 0, length,"US-ASCII");
                        } else {
                            stringValue = new String(bytes, 0, length);
                        }
//                        if (customScanner != null) {
                            if (customScanner.isAnnotation(stringValue)) {
                                return true;
                            }
//                        } else {
//                            if (AnnotationScanner.isAnnotation(stringValue)) {
//                                return true;
//                            }
//                        }
                    }
                    break;
                case CLASS:
                case STRING:
                    buffer.getShort();
                    break;
                case FIELDREF:
                case METHODREF:
                case INTERFACEMETHODREF:
                case INTEGER:
                case FLOAT:
                    buffer.position(buffer.position()+4);
                    break;
                case LONG:
                case DOUBLE:
                    buffer.position(buffer.position()+8);
                    // for long, and double, they use 2 constantPool
                    i++;
                    break;
                case NAMEANDTYPE:
                    buffer.getShort();
                    buffer.getShort();
                    break;
                default:
//                    DOLUtils.getDefaultLogger().severe("Unknow type constant pool " + type + " at position" + i);
                    break;
            }
        }
        return false;
    }


    public static final byte CLASS = 7;
    public static final int FIELDREF = 9;
    public static final int METHODREF = 10;
    public static final int STRING = 8;
    public static final int INTEGER = 3;
    public static final int FLOAT = 4;
    public static final int LONG = 5;
    public static final int DOUBLE = 6;
    public static final int INTERFACEMETHODREF = 11;
    public static final int NAMEANDTYPE = 12;
    public static final int ASCIZ = 1;
    public static final int UNICODE = 2;
}

