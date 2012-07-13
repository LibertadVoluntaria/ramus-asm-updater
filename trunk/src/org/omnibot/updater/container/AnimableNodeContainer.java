package org.omnibot.updater.container;

import org.omnibot.updater.Task;
import org.omnibot.updater.Updater;
import org.omnibot.updater.processor.AddGetterProcessor;
import org.omnibot.updater.processor.AddInterfaceProcessor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Ramus
 */
public class AnimableNodeContainer extends HookContainer implements Task {

	@Override
	public Class<?>[] getDependencies() {
		return new Class<?>[]{AnimableContainer.class};
	}

	@Override
	public int getGetters() {
		return 2;
	}

	@Override
	public int getInterfaces() {
		return 1;
	}

	@Override
	public void execute(final String name, final ClassNode cn) {
		Updater.getInstance().getClasses().set("AnimableNode", cn);
		addProcessor(new AddInterfaceProcessor(this, cn.name, ACCESSOR_DESC + "AnimableNode"));
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) != 0) {
				continue;
			}
			if (fn.desc.equals("L" + cn.name + ";")) {
				addProcessor(new AddGetterProcessor(this, "getNext", "L" + ACCESSOR_DESC + "AnimableNode;", cn.name, fn.name, fn.desc, false));
			} else {
				addProcessor(new AddGetterProcessor(this, "getAnimable", "L" + ACCESSOR_DESC + "Animable;", cn.name, fn.name, fn.desc, false));
			}
		}
	}

	@Override
	public boolean isValid(final String name, final ClassNode cn) {
		if (!cn.superName.equals("java/lang/Object")) {
			return false;
		}
		final String anim = Updater.getInstance().getClasses().get("Animable").name;
		int nonstatic = 0;
		int selfInst = 0;
		int anims = 0;
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) != 0) {
				continue;
			}
			nonstatic++;
			if (fn.desc.equals("L" + cn.name + ";")) {
				selfInst++;
			} else if (fn.desc.equals("L" + anim + ";")) {
				anims++;
			}
		}
		if (selfInst != 1 || nonstatic != 2 || anims != 1) {
			return false;
		}
		nonstatic = 0;
		for (final MethodNode mn : cn.methods) {
			if ((mn.access & Opcodes.ACC_STATIC) != 0) {
				continue;
			}
			nonstatic++;
		}
		return nonstatic > 0;
	}
}