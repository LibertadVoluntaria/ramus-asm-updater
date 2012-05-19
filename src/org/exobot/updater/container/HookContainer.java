package org.exobot.updater.container;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import org.exobot.updater.Condition;
import org.exobot.updater.Task;
import org.exobot.updater.Updater;
import org.exobot.updater.processor.AddInterfaceProcessor;
import org.exobot.updater.processor.Processor;
import org.exobot.updater.processor.SetSuperProcessor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * @author Ramus
 */
public abstract class HookContainer implements Condition {

	public static final String ACCESSOR_DESC = "org/exobot/game/bot/client/";

	private final List<Processor> processors = new LinkedList<Processor>();
	private final Condition policy;
	private Task[] tasks = null;
	private boolean hidden;

	public HookContainer() {
		this((Task) null);
		if (!(this instanceof Task)) {
			throw new RuntimeException("Invalid HookContainer.");
		}
		setTask((Task) this);
	}

	public HookContainer(final Task task) {
		this(null, new Task[]{task});
	}

	public HookContainer(final Task[] tasks) {
		this(null, tasks);
	}

	public HookContainer(final Condition policy, final Task task) {
		this(policy, new Task[]{task});
	}

	public HookContainer(final Condition policy, final Task[] tasks) {
		this.policy = policy;
		this.tasks = tasks;
		this.hidden = false;
	}

	protected final void addProcessor(final Processor p) {
		processors.add(p);
	}

	private static boolean canRun(final List<HookContainer> containers, final HookContainer container) {
		final Class<?>[] dependencies = container.getDependencies();
		int found = 0;
		for (final HookContainer hc : containers) {
			for (final Class<?> dependency : dependencies) {
				if (hc.getClass().equals(dependency)) {
					found++;
				}
			}
		}
		return found == dependencies.length;
	}

	public Class<?>[] getDependencies() {
		return new Class<?>[0];
	}

	public final String getName() {
		final String name = getClass().getSimpleName();
		return name.substring(0, name.length() - 9);
	}

	public final Task[] getTasks() {
		return tasks;
	}

	public int getGetters() {
		return 0;
	}

	public int getInterfaces() {
		return 0;
	}

	public int getSignatures() {
		return 0;
	}

	public int getSupers() {
		return 0;
	}

	public final List<Processor> getProcessors() {
		Collections.sort(processors, new Comparator<Processor>() {

			@Override
			public int compare(final Processor p, final Processor q) {
				if (p instanceof AddInterfaceProcessor || p instanceof SetSuperProcessor) {
					return -1;
				}
				return 1;
			}
		});
		return processors;
	}

	public final int getSize(final Class<? extends Processor> clazz) {
		int size = 0;
		for (final Processor p : processors) {
			if (p.getClass().equals(clazz)) {
				size++;
			}
		}
		return size;
	}

	public boolean isHidden() {
		return hidden;
	}

	private String refactorDesc(final String desc) {
		if (desc.length() < 6 && desc.startsWith("L") && desc.endsWith(";")) {
			return "Ljava/lang/Object;";
		}
		return desc;
	}

	public final String processSignature(final ClassNode cn) {
		final StringBuilder signature = new StringBuilder();
		final List<String> temp = new LinkedList<String>();
		final Map<String, ClassNode> classes = Updater.getInstance().getClasses();
		temp.add(refactorDesc(cn.superName));
		for (final String iface : cn.interfaces) {
			temp.add(refactorDesc(iface));
		}
		for (final FieldNode fn : cn.fields) {
			if ((fn.access & Opcodes.ACC_STATIC) != 0) {
				continue;
			}
			temp.add(refactorDesc(fn.desc));
		}
		Collections.sort(temp, new Comparator<String>() {

			@Override
			public int compare(final String s, final String t) {
				return s.compareToIgnoreCase(t);
			}
		});
		for (final String s : temp) {
			signature.append(s);
		}
		try {
			final MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(signature.toString().getBytes(), 0, signature.length());
			return new BigInteger(1, md.digest()).toString(16);
		} catch (final NoSuchAlgorithmException ignored) {
		}
		return signature.toString();
	}

	public final void setHidden(final boolean hidden) {
		this.hidden = hidden;
	}

	public final void setTask(final Task task) {
		setTasks(new Task[]{task});
	}

	public final void setTasks(final Task[] tasks) {
		this.tasks = tasks;
	}

	public static void sort(final List<HookContainer> containers) {
		final List<HookContainer> sorted = new LinkedList<HookContainer>();
		while (!containers.isEmpty()) {
			for (int i = 0; i < containers.size(); i++) {
				final HookContainer container = containers.get(i);
				if (canRun(sorted, container)) {
					sorted.add(container);
					containers.remove(container);
				}
			}
		}
		containers.addAll(sorted);
	}

	@Override
	public boolean validate(final String name, final ClassNode cn) {
		return policy != null && policy.validate(name, cn);
	}
}