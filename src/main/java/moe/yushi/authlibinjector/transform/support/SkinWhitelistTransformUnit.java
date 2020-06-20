/*
 * Copyright (C) 2020  Haowei Wen <yushijinhun@gmail.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package moe.yushi.authlibinjector.transform.support;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASM7;
import static org.objectweb.asm.Opcodes.IRETURN;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import moe.yushi.authlibinjector.transform.CallbackMethod;
import moe.yushi.authlibinjector.transform.CallbackSupport;
import moe.yushi.authlibinjector.transform.TransformContext;
import moe.yushi.authlibinjector.transform.TransformUnit;

public class SkinWhitelistTransformUnit implements TransformUnit {

	private static final String[] DEFAULT_WHITELISTED_DOMAINS = {
			".minecraft.net",
			".mojang.com"
	};

	private static final List<String> WHITELISTED_DOMAINS = new CopyOnWriteArrayList<>();

	public static List<String> getWhitelistedDomains() {
		return WHITELISTED_DOMAINS;
	}

	@CallbackMethod
	public static boolean isWhitelistedDomain(String url) {
		String domain;
		try {
			domain = new URI(url).getHost();
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid URL '" + url + "'");
		}

		for (String whitelisted : DEFAULT_WHITELISTED_DOMAINS) {
			if (domain.endsWith(whitelisted)) {
				return true;
			}
		}
		for (String whitelisted : WHITELISTED_DOMAINS) {
			if (domain.endsWith(whitelisted)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Optional<ClassVisitor> transform(ClassLoader classLoader, String className, ClassVisitor writer, TransformContext ctx) {
		if ("com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService".equals(className)) {
			return Optional.of(new ClassVisitor(ASM7, writer) {

				@Override
				public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
					if ("isWhitelistedDomain".equals(name) && "(Ljava/lang/String;)Z".equals(desc)) {
						ctx.markModified();
						MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
						mv.visitCode();
						mv.visitVarInsn(ALOAD, 0);
						CallbackSupport.invoke(ctx, mv, SkinWhitelistTransformUnit.class, "isWhitelistedDomain");
						mv.visitInsn(IRETURN);
						mv.visitMaxs(-1, -1);
						mv.visitEnd();
						return null;
					} else {
						return super.visitMethod(access, name, desc, signature, exceptions);
					}
				}

			});
		} else {
			return Optional.empty();
		}
	}

	@Override
	public String toString() {
		return "Texture Whitelist Transformer";
	}
}
