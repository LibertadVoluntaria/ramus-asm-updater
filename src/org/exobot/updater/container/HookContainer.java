package org.exobot.updater.container;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.exobot.updater.processor.Processor;
import org.objectweb.asm.tree.ClassNode;

/**
 * @author Ramus
 */
public abstract class HookContainer implements Condition {

	public static final String ACCESSOR_DESC = "org/exobot/bot/accessors/";

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

	public final String getName() {
		final String name = getClass().getSimpleName();
		return name.substring(0, name.length() - 9);
	}

	public final Task[] getTasks() {
		return tasks;
	}

	public int getInterfaces() {
		return 0;
	}

	public int getMethods() {
		return 0;
	}

	public final List<Processor> getProcessors() {
		Collections.sort(processors, new Comparator<Processor>() {

			@Override
			public int compare(final Processor p, final Processor q) {
				return Integer.compare(q.getId(), p.getId());
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

	@Override
	public boolean validate(final String name, final ClassNode cn) {
		return policy != null && policy.validate(name, cn);
	}
}