package org.exobot.updater.container;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.exobot.updater.Condition;
import org.exobot.updater.Task;
import org.exobot.updater.processor.AddInterfaceProcessor;
import org.exobot.updater.processor.Processor;
import org.exobot.updater.processor.SetSuperProcessor;
import org.objectweb.asm.tree.ClassNode;

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

	public final boolean isHidden() {
		return hidden;
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