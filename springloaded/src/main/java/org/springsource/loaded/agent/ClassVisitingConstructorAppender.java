/*
 * Copyright 2010-2012 VMware and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springsource.loaded.agent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.springsource.loaded.Constants;


/**
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class ClassVisitingConstructorAppender extends ClassVisitor implements Constants {

	private String calleeOwner;

	private String calleeName;

	/**
	 * This ClassAdapter will visit a class and within the constructors it will add a call to the specified method
	 * (assumed static) just before each constructor returns. The target of the call should be a collecting method that
	 * will likely do something with the instances later on class reload.
	 * 
	 * @param owner the owning class of the method to call
	 * @param name the method to call
	 */
	public ClassVisitingConstructorAppender(String owner, String name) {
		super(ASM5, new ClassWriter(0)); // TODO review 0 here
		this.calleeOwner = owner;
		this.calleeName = name;
	}

	public byte[] getBytes() {
		return ((ClassWriter) cv).toByteArray();
	}

	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (name.equals("<init>")) {
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			return new ConstructorAppender(mv);
		}
		else {
			return super.visitMethod(access, name, desc, signature, exceptions);
		}
	}

	/**
	 * This constructor appender includes a couple of instructions at the end of each constructor it is asked to visit.
	 * It recognizes the end by observing a RETURN instruction. The instructions are inserted just before the RETURN.
	 */
	class ConstructorAppender extends MethodVisitor implements Constants {

		public ConstructorAppender(MethodVisitor mv) {
			super(ASM5, mv);
		}

		@Override
		public void visitInsn(int opcode) {
			if (opcode == RETURN) {
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKESTATIC, calleeOwner, calleeName, "(Ljava/lang/Object;)V", false);
			}
			super.visitInsn(opcode);
		}

	}
}
